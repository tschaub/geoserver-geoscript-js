
package org.geoserver.geoscript.javascript.wfs;

import java.util.Map;
import java.util.logging.Logger;

import net.opengis.wfs.TransactionType;

import org.geoserver.geoscript.javascript.GeoScriptModules;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.tools.shell.Global;

/**
 * A plugin that allows hooks during WFS transactions.
 */
public class JavaScriptTransactionPlugin implements TransactionPlugin {
    static Logger LOGGER = Logging.getLogger("org.geoserver.geoscript.javascript");
    
    public JavaScriptTransactionPlugin() {
//        Global global = GeoScriptModules.getGlobal();
//        Require require = GeoScriptModules.require;
//
//        Context cx = Context.enter();
//        try {
//            Scriptable exports = (Scriptable) require.call(
//                    cx, global, global, new String[] {"hooks/wfs"});
//        } finally { 
//            Context.exit();
//        }
    }

    public void dataStoreChange(TransactionEvent event) throws WFSException {
        if (event.getType() == TransactionEventType.POST_INSERT) {
            // get feature ids
        }
    }

    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        // run beforeTransaction hook
        return null;
    }

    public void beforeCommit(TransactionType request) throws WFSException {
        // run beforeCommit hook
    }

    public void afterTransaction(TransactionType request, boolean committed) {
        // run afterTransaction hook
    }

    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

}
