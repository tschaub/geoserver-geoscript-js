
package org.geoserver.geoscript.javascript.wfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.impl.NativeTypeImpl;

import org.apache.commons.collections.MultiHashMap;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
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
import org.mozilla.javascript.tools.shell.Global;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.springframework.util.Assert;

/**
 * A plugin that allows hooks during WFS transactions.
 */
public class JavaScriptTransactionPlugin implements TransactionPlugin {
    
    private JavaScriptModules jsModules;
    private Function featureConverter;
    private Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");
    private static final String JS_TRANSACTION_CACHE = "JS_TRANSACTION_CACHE";
    
    public JavaScriptTransactionPlugin(JavaScriptModules jsModules) {
        this.jsModules = jsModules;
    }
    

    /**
     * Runs a "beforeTransaction" method exported by a wfs.js script in the
     * scripts/hooks directory of the data directory.  This provides an 
     * opportunity for a wfs.js script to interact with the transaction
     * request before it proceeds.
     * 
     * @see net.opengis.wfs.TransactionResponseType#beforeTransaction(net.opengis.wfs.TransactionType)
     */
    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        Function function = getFunction("beforeTransaction");
        if (function != null) {
            Object[] args = { request };
            callFunction(function, args);
        }
        return request;
    }

    /**
     * Caches features affected by the transaction for use in the 
     * {@link beforeCommit} and {@link afterTransaction} hooks.
     * 
     * @see org.geoserver.wfs.TransactionListener#dataStoreChange(org.geoserver.wfs.TransactionEvent)
     */
    public void dataStoreChange(final TransactionEvent event) throws WFSException {
        // only cache features if we'll need them after the transaction
        Function function = getFunction("afterTransaction");
        if (function != null) {
            try {
                cacheFeatures(event);
            } catch (RuntimeException e) {
                // let the transaction succeed, but warn about the exception
                LOGGER.log(Level.WARNING, "Trouble getting features from data store change", e);
            }
        }
    }

    /**
     * Runs a "beforeCommit" method exported by a wfs.js script in the
     * scripts/hooks directory of the data directory.  This provides an 
     * opportunity for a wfs.js script to interact with features affected
     * by the transaction before they are committed.
     * 
     * @see net.opengis.wfs.TransactionResponseType#beforeCommit(net.opengis.wfs.TransactionType)
     */
    public void beforeCommit(TransactionType request) throws WFSException {
        Function function = getFunction("beforeCommit");
        if (function != null) {
            Object[] args = { getTransactionDetail(request), request };
            callFunction(function, args);
        }
    }
    

    /**
     * Runs a "afterTransaction" method exported by a wfs.js script in the
     * scripts/hooks directory of the data directory.  This provides an 
     * opportunity for a wfs.js script to interact with features affected
     * by the transaction after they are committed.
     * 
     * @see net.opengis.wfs.TransactionResponseType#afterTransaction(net.opengis.wfs.TransactionType)
     */
    public void afterTransaction(TransactionType request, TransactionResponseType result, boolean committed) {
        if (committed) {
            Function function = getFunction("afterTransaction");
            if (function != null) {
                Object[] args = { getTransactionDetail(request), request };
                callFunction(function, args);
            }
        }
    }
    
    /**
     * Looks for a "priority" property exported by a wfs.js script in the
     * scripts/hooks directory of the data directory.  This provides an 
     * opportunity for a wfs.js script to set its priority among other
     * WFS transaction listeners.  By default, the wfs.js hooks get no 
     * special priority (level 0).
     * 
     * @see net.opengis.wfs.TransactionResponseType#getPriority()
     */
    public int getPriority() {
        Integer priority = 0;
        Scriptable export = getExport("priority");
        if (export instanceof Number) {
            priority = (Integer) Context.jsToJava(export, Integer.class);
        }
        return priority;
    }

    private Function getFeatureConverter() {
        if (featureConverter == null) {
            synchronized (this) {
               if (featureConverter == null) {
                   Scriptable exports = jsModules.require("geoscript/feature");
                   Scriptable FeatureWrapper = (Scriptable) exports.get("Feature", exports);
                   featureConverter = (Function) FeatureWrapper.get("from_", FeatureWrapper);
               }
            }
        }
        return featureConverter;
    }
    
    private Scriptable getExports() {
        Scriptable exports = null;
        try {
            exports = jsModules.require("hooks/wfs");
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

    private void cacheFeatures(final TransactionEvent event) {
        final Object source = event.getSource();
        if (!(source instanceof InsertElementType || source instanceof UpdateElementType || source instanceof DeleteElementType)) {
            return;
        }

        final EObject originatingTransactionRequest = (EObject) source;
        Assert.notNull(originatingTransactionRequest);

        final SimpleFeatureCollection featureCollection = event.getAffectedFeatures();
        Name schemaName = featureCollection.getSchema().getName();
        String local = schemaName.getLocalPart();
        String uri = schemaName.getNamespaceURI();
        
        MultiHashMap featureCache = getFeatureCache(event.getRequest());
        String type = event.getType().name();

        SimpleFeatureIterator features = featureCollection.features();
        Context cx = jsModules.enterContext();
        Global global = jsModules.getSharedGlobal();
        try {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Scriptable info = cx.newObject(global);
                ScriptableObject.putProperty(info, "name", local);
                ScriptableObject.putProperty(info, "uri", uri);
                Object featureObj = getFeatureConverter().call(
                        cx, global, global, new Object[] { feature });
                ScriptableObject.putProperty(info, "feature", featureObj);
                featureCache.put(type, info);
            }
        } finally {
            features.close();
            Context.exit();
        }
    }
    
    private MultiHashMap getFeatureCache(TransactionType transaction) {
        @SuppressWarnings("unchecked")
        final Map<Object, Object> extendedProperties = transaction.getExtendedProperties();

        MultiHashMap featureCache = (MultiHashMap) extendedProperties.get(JS_TRANSACTION_CACHE);
        if (featureCache == null) {
            featureCache = new MultiHashMap();
            extendedProperties.put(JS_TRANSACTION_CACHE, featureCache);
        }
        return featureCache;
    }
    
    private Scriptable getTransactionDetail(TransactionType transaction) {
        MultiHashMap featureCache = getFeatureCache(transaction);
        EList<?> nativeList = transaction.getNative();
        Scriptable details = null;
        Context cx = jsModules.enterContext();
        Global global = jsModules.getSharedGlobal();
        try {
            details = cx.newObject(global);
            // add map of event -> features
            @SuppressWarnings("unchecked")
            Iterator<Map.Entry<String,ArrayList<Scriptable>>> it = featureCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ArrayList<Scriptable>> entry = it.next();
                String type = entry.getKey();
                ArrayList<Scriptable> featureList = entry.getValue();
                Scriptable array = cx.newArray(global, featureList.size());
                for (int index=0; index<featureList.size(); ++index) {
                    Scriptable info = featureList.get(index);
                    array.put(index, array, info);
                }
                details.put(type, details, array);
            }
            // add native elements
            int len = nativeList.size();
            Scriptable natives = cx.newArray(global, len);
            for (int i=0; i<len; ++i) {
                NativeTypeImpl nat = (NativeTypeImpl) nativeList.get(i);
                Scriptable info = cx.newObject(global);
                info.put("vendorId", info, nat.getVendorId());
                info.put("safeToIgnore", info, nat.isSafeToIgnore());
                info.put("value", info, nat.getValue().trim());
                natives.put(i, natives, info);
            }
            details.put("natives", details, natives);
        } finally {
            Context.exit();
        }
        return details;
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
            result = jsModules.callFunction(function, args);
        } catch(Exception e) {
            throw new WFSException(e.getMessage(), e);
        }
        handleResult(result);
    }

}
