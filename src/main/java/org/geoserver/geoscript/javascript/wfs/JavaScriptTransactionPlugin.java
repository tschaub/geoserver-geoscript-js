
package org.geoserver.geoscript.javascript.wfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import net.opengis.wfs.TransactionType;

import org.apache.commons.collections.MultiHashMap;
import org.geoserver.geoscript.javascript.JavaScriptModules;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;

/**
 * A plugin that allows hooks during WFS transactions.
 */
public class JavaScriptTransactionPlugin implements TransactionPlugin {
    static Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");
    
    static ThreadLocal<MultiHashMap> affectedFeatures = new ThreadLocal<MultiHashMap>();
    
    private Scriptable getExports() {
        Scriptable exports = null;
        try {
            exports = JavaScriptModules.require("hooks/wfs");
        } catch (JavaScriptException e) {
            // no hooks/wfs - pass
        }
        return exports;
    }
    
    private Scriptable getExport(String name) {
        Scriptable exports = getExports();
        Scriptable export = null;
        if (exports != null) {
            Object obj = exports.get(name, exports);
            if (obj instanceof Scriptable) {
                export = (Scriptable) obj;
            }
        }
        return export;
    }
    
    private Function getFunction(String name) {
        Function function = null;
        Scriptable export = getExport(name);
        if (export instanceof Function) {
            function = (Function) export;
        }
        return function;
    }

    public void dataStoreChange(TransactionEvent event) throws WFSException {
        if (affectedFeatures.get() == null) {
            affectedFeatures.set(new MultiHashMap());
        }
        MultiHashMap map = affectedFeatures.get();
        map.put(event.getType().name(), event.getAffectedFeatures());
    }

    private void handleResult(Object result) throws WFSException {
        if (result instanceof Scriptable) {
            Scriptable error = (Scriptable) result;
            Object codeObj = error.get("code", error);
            String code = null;
            if (codeObj instanceof String) {
                code = (String) codeObj;
            }
            Object messageObj = error.get("message", error);
            String message = null;
            if (messageObj instanceof String) {
                message = (String) messageObj;
            }
            Object locatorObj = error.get("locator", error);
            String locator = null;
            if (locatorObj instanceof String) {
                locator = (String) locatorObj;
            }
            throw new WFSException(message, code, locator);
        }
    }
    
    private void callFunction(Function function, Object[] args)  throws WFSException{
        Object result = null;
        try {
            result = JavaScriptModules.callFunction(function, args);
        } 
        catch(Exception e) {
            throw new WFSException(e.getMessage(), e);
        }
        handleResult(result);
    }

    private Scriptable getTransactionDetails() {
        MultiHashMap eventMap = affectedFeatures.get();
        Context cx = Context.enter();
        Scriptable details = null;
        try {
            details = cx.newObject(JavaScriptModules.sharedGlobal);
            for (Iterator<?> it = eventMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String,ArrayList<SimpleFeatureCollection>> entry = 
                    (Map.Entry<String,ArrayList<SimpleFeatureCollection>>) it.next();
                String eventName = entry.getKey();
                ArrayList<SimpleFeatureCollection> collection = entry.getValue();
                Scriptable array = cx.newArray(JavaScriptModules.sharedGlobal, collection.size()); // length will change
                int index = 0;
                for(Iterator<?> it2 = collection.iterator(); it2.hasNext();) {
                    SimpleFeatureCollection fc = (SimpleFeatureCollection) it2.next();
                    Name schemaName = fc.getSchema().getName();
                    String local = schemaName.getLocalPart();
                    String uri = schemaName.getNamespaceURI();
                    SimpleFeatureIterator features = fc.features();
                    try {
                        while (features.hasNext()) {
                            SimpleFeature feature = features.next();
                            Scriptable o = cx.newObject(JavaScriptModules.sharedGlobal);
                            ScriptableObject.putProperty(o, "uri", uri);
                            ScriptableObject.putProperty(o, "name", local);
                            ScriptableObject.putProperty(o, "id", feature.getID());
                            array.put(index, array, o);
                            index += 1;
                        }
                    } finally {
                        features.close();
                    }
                }
                ScriptableObject.putProperty(details, eventName, array);
            }
        } finally { 
            Context.exit();
        }
        return details;
    }

    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        Function function = getFunction("beforeTransaction");
        if (function != null) {
            Object[] args = { request };
            callFunction(function, args);
        }
        return request;
    }
    

    public void beforeCommit(TransactionType request) throws WFSException {
        Function function = getFunction("beforeCommit");
        if (function != null) {
            Object[] args = { request };
            callFunction(function, args);
        }
    }
    

    public void afterTransaction(TransactionType request, boolean committed) {
        try {
            if (committed) {
                Function function = getFunction("afterTransaction");
                if (function != null) {
                    Object[] args = { request, getTransactionDetails() };
                    callFunction(function, args);
                }
            }
        } finally {
            affectedFeatures.remove();
        }
    }

    public int getPriority() {
        Integer priority = 0;
        Scriptable export = getExport("priority");
        if (export instanceof Number) {
            priority = (Integer) Context.jsToJava(export, Integer.class);
        }
        return priority;
    }

}
