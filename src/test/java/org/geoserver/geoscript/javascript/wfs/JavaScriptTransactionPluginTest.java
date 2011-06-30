package org.geoserver.geoscript.javascript.wfs;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.wfs.WFSTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *  Tests in this class will get a data directory with a scripts/hooks/wfs.js
 *  script that exports beforeCommit and afterTransaction methods.
 */
public class JavaScriptTransactionPluginTest extends WFSTestSupport {
    
    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }

    /** 
     * Pull in the scripts/hooks/wfs.js script before running tests.  If 
     * present in the hooks directory, methods exported from a wfs.js script
     * will be run by the beforeTransaction, beforeCommit, and afterTransaction
     * methods in a JavaScriptTransactionPlugin.
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = new File(getClass().getResource("scripts").getFile());
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }
    
    /**
     * The before/after methods in wfs.js don't do anything special for a 
     * normal transaction.  This test confirms that a transaction succeeds.
     * 
     * @throws Exception
     */
    public void testTransaction() throws Exception {

        File file = new File(getClass().getResource("xml/transaction.xml").getFile());
        String xml = FileUtils.readFileToString(file, "UTF-8");
        
        Document dom = postAsDOM("wfs", xml);
        assertTrue(dom.getElementsByTagName("wfs:InsertResults").getLength() != 0);
        print(dom);

    }
    
    /**
     * The beforeCommit method exported by the wfs.js hook should cause a 
     * service exception if a transaction contains a specially formatted 
     * native element.
     * 
     * @throws Exception
     */
    public void testTransactionExceptionBefore() throws Exception {

        File file = new File(getClass().getResource("xml/transaction-exception-before.xml").getFile());
        String xml = FileUtils.readFileToString(file, "UTF-8");
        
        Document dom = postAsDOM("wfs", xml);
        print(dom);

        Element exText = getFirstElementByTagName(dom, "ows:ExceptionText");
        assertEquals("correct message", "PreInsert: 2 PreUpdate: 1 PostUpdate: 1 PostDelete: 1 natives: 2", exText.getFirstChild().getNodeValue());

      }

    /**
     * The beforeCommit method exported by the wfs.js hook should cause a 
     * service exception if a transaction contains a specially formatted 
     * native element.
     * 
     * @throws Exception
     */
    public void testTransactionExceptionAfter() throws Exception {

        File file = new File(getClass().getResource("xml/transaction-exception-after.xml").getFile());
        String xml = FileUtils.readFileToString(file, "UTF-8");
        
        Document dom = postAsDOM("wfs", xml);
        print(dom);

        Element exText = getFirstElementByTagName(dom, "ows:ExceptionText");
        assertEquals("correct message", "PreInsert: 2 PreUpdate: 1 PostUpdate: 1 PostDelete: 1 natives: 2", exText.getFirstChild().getNodeValue());

      }

}
