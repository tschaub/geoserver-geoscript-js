/**
 * Copyright (c) 2001 - 2011 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geoscript.javascript;

import java.net.URISyntaxException;

/**
 * The sole purpose of this class is to provide a way to look up the resource
 * location for GeoScript JS modules.  Module files will be located in a 
 * "modules" directory relative to this class file.
 */
public class GeoScriptModules {
    
    /**
     * Returns the full path to JavaScript modules bundled with this extension.
     */
    static public String getModulePath() throws URISyntaxException {
        return GeoScriptModules.class.getResource("modules").toURI().toString();
    }

}
