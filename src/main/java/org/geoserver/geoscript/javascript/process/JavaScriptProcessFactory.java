/**
 * Copyright (c) 2001 - 2011 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoscript.javascript.process;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * A process factory that presents a JavaScript module as a WPS Process.
 */
public class JavaScriptProcessFactory implements ProcessFactory {
    public static final String JS_NAMESPACE = "js";
    public static final String SCRIPT_SEARCH_PATH = "scripts/processes/";
    private GeoServerResourceLoader resourceLoader = 
        GeoServerExtensions.bean(GeoServerResourceLoader.class);
    private Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");
    
    private File scriptDirectory = null;
    {
        try {
            scriptDirectory = resourceLoader.findOrCreateDirectory(SCRIPT_SEARCH_PATH);
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Trouble accessing or creating directory.", ioe);
        }
    }

    public Set<Name> getNames() {
        Set<Name> result = new HashSet<Name>();
        
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File f, String name) {
                return name.endsWith(".js");
            }
        };

        if (scriptDirectory != null) {
            for (String script : scriptDirectory.list(filter)) {
                result.add(new NameImpl(JS_NAMESPACE, script.substring(0, script.length() - 3)));
            } 
        }

        return result;
    }
    
    /**
     * Creates a GeoTools Process which wraps a JavaScript script file
     * 
     * @param Name a qualified name identifying the script file
     */
    public JavaScriptProcess create(Name name) {
        return new JavaScriptProcess(scriptDirectory, name.getLocalPart());
    }

    public InternationalString getDescription(Name name) {
        JavaScriptProcess process = create(name);
        return process.getDescription();
    }

    public boolean isAvailable() {
        return true; 
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public java.util.Map getImplementationHints() {
        return java.util.Collections.EMPTY_MAP;
    }

    public InternationalString getTitle() {
        return Text.text("GeoScript JS process provider");
    }

    public InternationalString getTitle(Name name) {
        JavaScriptProcess process = create(name);
        return process.getTitle();
    }

    public String getName(Name name) {
        return name.getLocalPart();
    }

    public boolean supportsProgress(Name name) {
        return false;
    }

    public String getVersion(Name name) {
        return "1.0.0";
    }

    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        JavaScriptProcess process = create(name);
        return process.getParameterInfo();
    }

    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> inputs) {
        JavaScriptProcess process = create(name);
        return process.getResultInfo();
    }

    @Override
    public String toString() {
        return "GeoScriptJSProcessFactory";
    }
}
