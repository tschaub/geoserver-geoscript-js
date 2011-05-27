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

import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

public class JavaScriptProcess implements Process{
    private Global scope;
    private Scriptable jsProcess;

    /**
     * Constructs a new process that wraps a JavaScript module.
     * @param processDir The directory containing process modules
     * @param name The process name
     */
    public JavaScriptProcess(File processDir, String name) {
        Context cx = Context.enter();
        cx.setLanguageVersion(170);
        scope = new Global();
        scope.initStandardObjects(cx, true);
        String modulePath;
        try {
            modulePath = GeoScriptModules.class.getResource("modules").toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating module path.", e);
        }
        processDir.toURI().toString();
        Require require = scope.installRequire(
                cx, 
                (List<String>) Arrays.asList(modulePath, processDir.toURI().toString()), 
                false
        );
        Scriptable exports = require.requireMain(cx, name);
        Object jsObject = exports.get("process", exports);
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
        Map<String,Object> results = null;
        Object runner = jsProcess.get("run", jsProcess);
        try {
            if (runner instanceof Function) {
                Function processFn = (Function)runner;
                Object[] args = {mapToJsObject(input, scope)};
                Object result = processFn.call(cx, scope, scope, args);
                results = jsObjectToMap((Scriptable)result);
            } else {
                throw new RuntimeException(
                    "Process for 'TODO: name' has no run method."
                );
            }
        } finally { 
            Context.exit();
        }

        return results;
    }

    public InternationalString getTitle() {
        Object title = jsProcess.get("title", jsProcess);
        return Text.text(title.toString());
    }

    public InternationalString getDescription() {
        Object description = jsProcess.get("description", jsProcess);
        return Text.text(description.toString());
    }

    Map<String, Parameter<?>> getParameterInfo() {
        Scriptable inputs = (Scriptable) jsProcess.get("inputs", jsProcess);

        Map<String, Parameter<?>> parameters = new HashMap<String, Parameter<?>>();

        for (Object key : inputs.getIds()) {
            Scriptable field = (Scriptable) inputs.get((String)key, inputs);
            // TODO: make this less offensive
            AttributeDescriptor descriptor = (AttributeDescriptor) ((Wrapper) field.get("_field", field)).unwrap();

            Parameter parameter = new Parameter(
                (String)key,
                descriptor.getType().getBinding(),
                descriptor.getName().getLocalPart(),
                descriptor.getType().getDescription().toString()
            );
            parameters.put((String)key, parameter);
        }

        return parameters;
    }

    Map<String, Parameter<?>> getResultInfo() {
        Scriptable outputs = (Scriptable) jsProcess.get("outputs", jsProcess);

        Map<String, Parameter<?>> parameters = new HashMap<String, Parameter<?>>();

        for (Object key : outputs.getIds()) {
            Scriptable field = (Scriptable) outputs.get((String)key, outputs);
            // TODO: make this less offensive
            AttributeDescriptor descriptor = (AttributeDescriptor) ((Wrapper) field.get("_field", field)).unwrap();

            Parameter parameter = new Parameter(
                (String)key,
                descriptor.getType().getBinding(),
                descriptor.getName().getLocalPart(),
                descriptor.getType().getDescription().toString()
            );
            parameters.put((String)key, parameter);
        }

        return parameters;
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
