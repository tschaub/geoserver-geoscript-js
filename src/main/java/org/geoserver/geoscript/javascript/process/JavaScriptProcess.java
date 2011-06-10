/**
 * Copyright (c) 2001 - 2011 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoscript.javascript.process;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.geoscript.javascript.JavaScriptModules;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

public class JavaScriptProcess implements Process{
    public String identifier;
    private Scriptable process;
    static Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");

    /**
     * Constructs a new process that wraps a JavaScript module.
     * @param processDir The directory containing process modules
     * @param name The process name
     */
    public JavaScriptProcess(File processDir, String name) {
        identifier = name;
        Scriptable exports;
        try {
            exports = JavaScriptModules.require("processes/" + name);
        } catch (Exception e) {
            String msg = "Failed to locate process: " + name;
            LOGGER.warning(msg); // exceptions from constructor swallowed in GetCapabilities
            throw new RuntimeException(msg, e);
        }
        Object processObj = exports.get("process", exports);
        if (processObj instanceof Scriptable) {
            process = (Scriptable) processObj;
        } else {
            String msg = "Failed to find 'process' in exports of " + name;
            LOGGER.warning(msg); // exceptions from constructor swallowed in GetCapabilities
            throw new RuntimeException(msg);
        }
    }

    public Map<String, Object> execute(Map<String, Object> input,
            ProgressListener monitor) {
        
        Map<String,Object> results = null;
        
        Scriptable exports = JavaScriptModules.require("geoserver/process");
        Object executeWrapperObj = exports.get("execute", exports);
        Function executeWrapper;
        if (executeWrapperObj instanceof Function) {
            executeWrapper = (Function) executeWrapperObj;
        } else {
            throw new RuntimeException(
                    "Can't find execute method in geoserver/process module.");
        }
        
        Object[] args = {process, mapToJsObject(input)};
        Object result = JavaScriptModules.callFunction(executeWrapper, args);
        results = jsObjectToMap((Scriptable)result);

        return results;
    }

    public InternationalString getTitle() {
        Object titleObj = process.get("title", process);
        String title;
        if (titleObj instanceof String) {
            title = (String) titleObj;
        } else {
            LOGGER.warning("Process '" + identifier + "' missing required title.");
            title = identifier;
        }
        return Text.text(title);
    }

    public InternationalString getDescription() {
        Object descriptionObj = process.get("description", process);
        InternationalString description = null;
        if (descriptionObj instanceof String) {
            description = Text.text((String) descriptionObj);
        }
        return description;
    }
    
    private Parameter<?> getParameterFromField(String id, Scriptable field) {
        Object _field = field.get("_field", field);
        AttributeDescriptor descriptor = (AttributeDescriptor) ((Wrapper) _field).unwrap();

        String title = null;
        Object titleObj = field.get("title", field);
        if (titleObj instanceof String) {
            title = (String) titleObj;
        } else {
            LOGGER.warning("Field '" + id + "' from process '" + identifier + "' missing required title.");
            title = identifier;
        }
        
        InternationalString descriptionObj = descriptor.getType().getDescription();
        String description;
        if (descriptionObj != null) {
            description = descriptionObj.toString();
        } else {
            // spec says optional, but required for Parameter
            description = id;
        }
        
        @SuppressWarnings("unchecked")
        Parameter<?> parameter = new Parameter<Object>(
            id,
            (Class<Object>) descriptor.getType().getBinding(),
            title,
            description
        );
        return parameter;
    }
    
    private Map<String, Parameter<?>> getParametersFromObject(Scriptable obj) {
        Map<String, Parameter<?>> parameters = new HashMap<String, Parameter<?>>();
        for (Object key : obj.getIds()) {
            String id = (String) key;
            Scriptable field = (Scriptable) obj.get(id, obj);
            parameters.put(id, getParameterFromField(id, field));
        }
        return parameters;
    }

    Map<String, Parameter<?>> getParameterInfo() {
        Scriptable inputs = (Scriptable) process.get("inputs", process);
        return getParametersFromObject(inputs);
    }

    Map<String, Parameter<?>> getResultInfo() {
        Scriptable outputs = (Scriptable) process.get("outputs", process);
        return getParametersFromObject(outputs);
    }

    private static Scriptable mapToJsObject(Map<String,Object> map) {
        Context cx = Context.enter();
        Scriptable obj = cx.newObject(JavaScriptModules.sharedGlobal);
        try {
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                ScriptableObject.putProperty(
                    obj, 
                    entry.getKey(), 
                    Context.javaToJS(entry.getValue(), JavaScriptModules.sharedGlobal)
                );
            }
        } finally { 
            Context.exit();
        }
        return obj;
    }

    private static Map<String, Object> jsObjectToMap(Scriptable obj) {
        Object[] ids = obj.getIds();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Object idObj : ids) {
            String id = (String)idObj;
            Object value = obj.get(id, obj);

            if (value instanceof Wrapper) {
                map.put(id, ((Wrapper)value).unwrap());
            } else if (value instanceof Function) {
                // ignore functions?
            } else {
                map.put(id, value);
            }
        }
        return map;
    }
}
