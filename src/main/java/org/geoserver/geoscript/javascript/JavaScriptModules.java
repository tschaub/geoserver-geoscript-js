/**
 * Copyright (c) 2001 - 2011 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geoscript.javascript;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.mozilla.javascript.tools.shell.Global;

/**
 * This class provides a way to require JavaScript modules bundled with the 
 * extension or in a "scripts" directory in the GeoServer data directory.
 * It uses a shared RequireBuilder to cache loaded modules.  New Require 
 * instances are created with the same RequrieBuilder each time {@link #require}
 * is called.  This allows modules that have been updated on disk to be pulled
 * in (module sources are checked for changes every 60 seconds).
 */
public class JavaScriptModules {
    
    private RequireBuilder requireBuilder;
    transient private Global global;
    private Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");
    
    private GeoServerResourceLoader resourceLoader;

    public JavaScriptModules(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns a list of paths to JavaScript modules.  This includes modules
     * bundled with this extension in addition to modules in the "scripts"
     * directory of the data dir.
     */
    public List<String> getModulePaths() {
        // GeoScript modules
        URL gsModuleUrl = getClass().getResource("modules");
        String gsModulePath;
        try {
            gsModulePath = gsModuleUrl.toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating module path.", e);
        }
        // User modules
        File userModuleDir;
        try {
            userModuleDir = resourceLoader.findOrCreateDirectory(
                    "scripts/");
        } catch (IOException e) {
            throw new RuntimeException("Trouble creating scripts directory.", e);
        }
        String userModulePath = userModuleDir.toURI().toString();
        return (List<String>) Arrays.asList(gsModulePath, userModulePath);
    }

    /**
     *  Creates and returns a shared global object.
     *  
     *  @return the shared global object
     */
    public Global getSharedGlobal() {
        if (global == null) {
            synchronized (this) {
                if (global == null) {
                    global = createGlobal();
                }
            }
        }
        return global;
    }

    /**
     * Creates and returns a shared require builder.  This allows loaded
     * modules to be cached.  The require builder is constructed with a module
     * provider that reloads modules only when they have changed on disk (with
     * a 60 second interval).  This require builder will be configured with
     * the module paths returned by {@link getModulePahts()}.
     * 
     * @return a shared require builder
     */
    private RequireBuilder getRequireBuilder() {
        if (requireBuilder == null) {
            synchronized (this) {
                if (requireBuilder == null) {
                    requireBuilder = new RequireBuilder();
                    requireBuilder.setSandboxed(false);
                    List<String> modulePaths = getModulePaths();
                    List<URI> uris = new ArrayList<URI>();
                    if (modulePaths != null) {
                        for (String path : modulePaths) {
                            try {
                                URI uri = new URI(path);
                                if (!uri.isAbsolute()) {
                                    // call resolve("") to canonify the path
                                    uri = new File(path).toURI().resolve("");
                                }
                                if (!uri.toString().endsWith("/")) {
                                    // make sure URI always terminates with slash to
                                    // avoid loading from unintended locations
                                    uri = new URI(uri + "/");
                                }
                                uris.add(uri);
                            } catch (URISyntaxException usx) {
                                throw new RuntimeException(usx);
                            }
                        }
                    }
                    requireBuilder.setModuleScriptProvider(
                            new SoftCachingModuleScriptProvider(
                                    new UrlModuleSourceProvider(uris, null)));
                }
            }
        }
        return requireBuilder;
    }

    /**
     * Create a new global object.
     * 
     * When the shared global object (accessible with {@link getSharedGlobal()}
     * should not be used, this method can be used to create a new global. This
     * global will have standard objects installed (Object, String, Number, 
     * Date, etc.).  In addition, a "require" method will be available for 
     * loading exports from JavaScript modules.  A "LOGGER" object is also
     * available for logging with "info" and "warning" methods.
     * 
     * @return a new global object
     */
    public Global createGlobal() {
        Global global = null;
        Context cx = enterContext();
        try {
            global = new Global();
            global.initStandardObjects(cx, true);
            
            // install require using shared require builder
            installRequire(cx, global);
            
            // allow logging from js modules
            global.put("LOGGER", global, LOGGER);
        } finally {
            Context.exit();
        }
        return global;
    }
    
    private Require installRequire(Context cx, Global global) {
        RequireBuilder rb = getRequireBuilder();
        Require require = rb.createRequire(cx, global);
        require.install(global);
        return require;
    }

    public Scriptable require(String locator) {
        Global global = getSharedGlobal();
        return require(locator, global);
    }
    

    public Scriptable require(String locator, Global global) {
        Scriptable exports = null;
        Context cx = enterContext();
        try {
            Require require = installRequire(cx, global);
            Object exportsObj = require.call(
                    cx, global, global, new String[] {locator});
            if (exportsObj instanceof Scriptable) {
                exports = (Scriptable) exportsObj;
            } else {
                throw new RuntimeException(
                        "Failed to locate exports in module: " + locator);
            }
        } finally { 
            Context.exit();
        }
        return exports;
    }
    
    public Object callFunction(Function function, Object[] args) {
        Context cx = enterContext();
        Object result = null;
        try {
            result = function.call(cx, global, global, args);
        } finally {
            Context.exit();
        }
        return result;
    }
    
    /**
     * Evaluate a JavaScript source string in the global scope.
     * 
     * @param source the string to evaluate as JavaScript
     * @return the result of evaluating the string
     */
    public Object eval(String source) {
        Global global = getSharedGlobal();
        return eval(global, source);
    }
    
    /**
     * Evaluate a JavaScript source string in the scope of the provided global.
     * 
     * @param source the string to evaluate as JavaScript
     * @param global the execution scope
     * @return the result of evaluating the string
     */
    public Object eval(Global global, String source) {
        Object result = null;
        Context cx = enterContext();
        try {
            result = cx.evaluateString(global, source, "<input>", 1, null);
        } finally {
            Context.exit();
        }
        return result;
    }
    
    /**
     * Associate a context with the current thread.  This calls Context.enter()
     * and test the language version to 170.
     * @return a Context associated with the thread
     */
    public Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(170);
        return cx;
    }

}
