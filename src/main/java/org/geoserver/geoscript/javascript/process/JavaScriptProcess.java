/**
 * Copyright (c) 2001 - 2011 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoscript.javascript.process;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.tools.shell.Global;

import org.geoserver.geoscript.javascript.GeoScriptModules;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;

import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

public class JavaScriptProcess implements Process{
    private Global scope;
    private Require require;
    private Scriptable jsProcess;
    public String identifier;
    static Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");

    /**
     * Constructs a new process that wraps a JavaScript module.
     * @param processDir The directory containing process modules
     * @param name The process name
     */
    public JavaScriptProcess(File processDir, String name) {
        identifier = name;
        Context cx = Context.enter();
        cx.setLanguageVersion(170);
        scope = new Global();
        scope.initStandardObjects(cx, true);
        // allow logging from js modules
        Object wrappedLogger = Context.javaToJS(LOGGER, scope);
        ScriptableObject.putProperty(scope, "LOGGER", wrappedLogger);        
        String modulePath;
        try {
            modulePath = GeoScriptModules.getModulePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating module path.", e);
        }
        processDir.toURI().toString();
        require = scope.installRequire(
            cx, 
            (List<String>) Arrays.asList(modulePath, processDir.toURI().toString()), 
            false
        );
        Scriptable exports = require.requireMain(cx, name);
        Object jsObject = exports.get("process", exports);
        Context.exit();
        if (jsObject instanceof Scriptable) {
            jsProcess = (Scriptable) jsObject;
        } else {
            throw new RuntimeException(
                "Script for process '" + name + "' doesn't export a process."
            );
        }
    }

    public Map<String, Object> execute(Map<String, Object> input,
            ProgressListener monitor) {

        Context cx = Context.enter();
        Scriptable exports = (Scriptable) require.call(cx, scope, require, new String[] {"geoserver/process"});
        Object executeWrapperObj = (Scriptable) exports.get("execute", exports);
        Function executeWrapper;
        if (executeWrapperObj instanceof Function) {
            executeWrapper = (Function) executeWrapperObj;
        } else {
            throw new RuntimeException(
                "Can't find execute method in geoserver/process module."
            );
        }
        Map<String,Object> results = null;
        Object[] args = {jsProcess, mapToJsObject(input, scope)};
        try {
            Object result = executeWrapper.call(cx, scope, scope, args);
            results = jsObjectToMap((Scriptable)result);
        } finally { 
            Context.exit();
        }

        return results;
    }

    public InternationalString getTitle() {
        Object titleObj = jsProcess.get("title", jsProcess);
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
        Object descriptionObj = jsProcess.get("description", jsProcess);
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
        Scriptable inputs = (Scriptable) jsProcess.get("inputs", jsProcess);
        return getParametersFromObject(inputs);
    }

    Map<String, Parameter<?>> getResultInfo() {
        Scriptable outputs = (Scriptable) jsProcess.get("outputs", jsProcess);
        return getParametersFromObject(outputs);
    }

    private static Scriptable mapToJsObject(Map<String,Object> map, Scriptable scope) {
        Context cx = Context.enter();
        Scriptable obj = cx.newObject(scope);
        try {
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                ScriptableObject.putProperty(
                    obj, 
                    entry.getKey(), 
                    Context.javaToJS(entry.getValue(), scope)
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
