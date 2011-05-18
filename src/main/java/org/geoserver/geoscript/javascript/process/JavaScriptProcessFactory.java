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

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.text.Text;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * A process factory that wraps a JavaScript script file and presents it as a WPS Process.
 * 
 * @author David Winslow <dwinslow@opengeo.org>
 */
public class JavaScriptProcessFactory implements ProcessFactory {
    public static final String JS_NAMESPACE = "js";
    public static final String SCRIPT_SEARCH_PATH = "scripts/processes/";
    private GeoServerResourceLoader resourceLoader = 
        GeoServerExtensions.bean(GeoServerResourceLoader.class);
    
    public Set<Name> getNames() {
        Set<Name> result = new HashSet<Name>();
        
        File scriptDirectory = null;
        try {
            scriptDirectory = resourceLoader.find(SCRIPT_SEARCH_PATH);
        } catch (IOException ioe) {
            // no, it's cool. we might have to handle a null return anyway.
        }
        
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
    
    File findScript(Name name) {
        if(name == null)
            throw new NullPointerException("Process name cannot be null");

        String fileBaseName = name.getLocalPart();

        File scriptDirectory = null;
        try {
            scriptDirectory = resourceLoader.find(SCRIPT_SEARCH_PATH);
        } catch (IOException ioe) {
            // no, it's cool. we might have to handle a null return anyway.
        }

        if (scriptDirectory != null) {        
            File script = new File(scriptDirectory, fileBaseName + ".js");
            if (script.exists()) {
                return script;
            }
        }

        throw new IllegalArgumentException("Unknown process '" + name + "'");
    }

    /**
     * Creates a GeoTools Process which wraps a JavaScript script file
     * 
     * @param Name a qualified name identifying the script file
     */
    public JavaScriptProcess create(Name name) throws IllegalArgumentException {
        File script = findScript(name);
        return new JavaScriptProcess(script);
    }

    public InternationalString getDescription(Name name) {
        // TODO: Implement a description mechanism
        JavaScriptProcess script = create(name);
        return Text.text(script.getMetadata().get("description").toString());
    }

    public boolean isAvailable() { return true; }

    public java.util.Map getImplementationHints() {
        return java.util.Collections.EMPTY_MAP;
    }

    public InternationalString getTitle() {
        return Text.text("GeoScript JS process provider");
    }

    public InternationalString getTitle(Name name) {
        JavaScriptProcess script = create(name);
        return Text.text(script.getMetadata().get("title").toString());
    }

    public String getName(Name name) {
        File f = findScript(name);
        return f.getName().replaceAll(".js$", "");
    }

    public boolean supportsProgress(Name name) {
        // look up the script to make sure we throw an exception if it doesn't exist.  
        // TODO: Is this the expected behavior? (same for getVersion)
        File script = findScript(name);
        return false;
    }

    public String getVersion(Name name) {
    	// TODO: support process version
        File script = findScript(name);
        return "1.0.0";
    }

    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        JavaScriptProcess process = create(name);
        return process.getParameterInfo();
    }

    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> inputs)
            throws IllegalArgumentException {
        JavaScriptProcess process = create(name);
        return process.getResultInfo();
    }

    @Override
    public String toString() {
        return "ScriptletFactory";
    }
}
