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

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
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
    
    static private RequireBuilder requireBuilder;
    static transient public Global sharedGlobal;
    static private Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");

    /**
     * Returns a list of paths to JavaScript modules.  This includes modules
     * bundled with this extension in addition to modules in the "scripts"
     * directory of the data dir.
     */
    public static List<String> getModulePaths() {
        // GeoScript modules
        URL gsModuleUrl = JavaScriptModules.class.getResource("modules");
        String gsModulePath;
        try {
            gsModulePath = gsModuleUrl.toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating module path.", e);
        }
        // User modules
        GeoServerResourceLoader resourceLoader = 
            GeoServerExtensions.bean(GeoServerResourceLoader.class);
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
     *  Create shared global and require builder one time only.
     */
    private static void init() {
        if (sharedGlobal == null) {
            synchronized (JavaScriptModules.class) {
                if (sharedGlobal == null) {
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
                    sharedGlobal = createGlobal();
                }
            }
        }
    }

    static public Global createGlobal() {
        Global global = null;
        Context cx = enterContext();
        try {
            global = new Global();
            global.initStandardObjects(cx, true);
            
            // allow logging from js modules
            Object wrappedLogger = Context.javaToJS(LOGGER, global);
            ScriptableObject.putProperty(global, "LOGGER", wrappedLogger);
        } finally {
            Context.exit();
        }
        return global;
    }
    
    static public Scriptable require(String locator) {
        init();
        return require(locator, sharedGlobal);
    }

    static public Scriptable require(String locator, Global global) {
        init();
        Scriptable exports = null;
        Context cx = enterContext();
        try {
            Require require = requireBuilder.createRequire(cx, global);
            require.install(global);
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
    
    static public Object callFunction(Function function, Object[] args) {
        Context cx = enterContext();
        Object result = null;
        try {
            result = function.call(cx, sharedGlobal, sharedGlobal, args);
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
    static public Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(170);
        return cx;
    }
    
}