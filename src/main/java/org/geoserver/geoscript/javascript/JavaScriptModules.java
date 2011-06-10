/**
 * Copyright (c) 2001 - 2011 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geoscript.javascript;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
import org.mozilla.javascript.tools.shell.Global;

/**
 * The sole purpose of this class is to provide a way to look up the resource
 * location for GeoScript JS modules.  Module files will be located in a 
 * "modules" directory relative to this class file.
 */
public class JavaScriptModules {
    
    static private Require sharedRequire;
    static transient public Global sharedGlobal;
    static private Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");

    private static void init() {
        if (sharedGlobal == null) {
            synchronized (JavaScriptModules.class) {
                if (sharedGlobal == null) {
                    //create global + require
                    Context cx = Context.enter();
                    try {
                        cx.setLanguageVersion(170);
                        sharedGlobal = new Global();
                        sharedGlobal.initStandardObjects(cx, true);
                        
                        // allow logging from js modules
                         Object wrappedLogger = Context.javaToJS(LOGGER, sharedGlobal);
                         ScriptableObject.putProperty(sharedGlobal, "LOGGER", wrappedLogger);
                        
                        // Require paths
                        // GeoScript
                        String gsModulePath;
                        try {
                            gsModulePath = getModulePath();
                        } catch (URISyntaxException e) {
                            throw new RuntimeException("Trouble evaluating module path.", e);
                        }
                        // User scripts
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
                        sharedRequire = sharedGlobal.installRequire(
                                cx, 
                                (List<String>) Arrays.asList(gsModulePath, 
                                        userModulePath),
                                false);
                    } finally {
                        Context.exit();
                    }
                }
            }
        }
    }
    
    static public Scriptable require(String locator) {
        init();
        Scriptable exports = null;
        Context cx = enterContext();
        try {
            Object exportsObj = sharedRequire.call(
                    cx, sharedGlobal, sharedGlobal, new String[] {locator});
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
    
    static public Object callMethod(Function function, Object[] args) {
        Context cx = enterContext();
        Object result = null;
        try {
            result = function.call(cx, sharedGlobal, sharedGlobal, args);
        } finally {
            Context.exit();
        }
        return result;
    }
    
    static private Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(170);
        return cx;
    }
    
    /**
     * Returns the full path to JavaScript modules bundled with this extension.
     */
    static public String getModulePath() throws URISyntaxException {
        return JavaScriptModules.class.getResource("modules").toURI().toString();
    }

}
