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

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.tools.shell.Global;

/**
 * The sole purpose of this class is to provide a way to look up the resource
 * location for GeoScript JS modules.  Module files will be located in a 
 * "modules" directory relative to this class file.
 */
public class GeoScriptModules {
    
    static public Require require;
    static transient public Global global;

    public static Global getGlobal() {
        if (global == null) {
            synchronized (GeoScriptModules.class) {
                if (global == null) {
                    //create global + require
                    Context cx = Context.enter();
                    try {
                        cx.setLanguageVersion(170);
                        global = new Global();
                        global.initStandardObjects(cx, true);
                        // allow logging from js modules
                        // Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");
                        // Object wrappedLogger = Context.javaToJS(LOGGER, global);
                        // ScriptableObject.putProperty(global, "LOGGER", wrappedLogger);
                        
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
                        require = global.installRequire(
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
        
        return global;
    }
    /**
     * Returns the full path to JavaScript modules bundled with this extension.
     */
    static public String getModulePath() throws URISyntaxException {
        return GeoScriptModules.class.getResource("modules").toURI().toString();
    }

}
