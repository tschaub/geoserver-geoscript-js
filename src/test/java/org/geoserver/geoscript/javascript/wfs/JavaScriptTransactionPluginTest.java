package org.geoserver.geoscript.javascript.wfs;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.wfs.WFSTestSupport;
import org.w3c.dom.Document;

public class JavaScriptTransactionPluginTest extends WFSTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = new File(getClass().getResource("scripts").getFile());
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }
    
    public void testInsert() throws Exception {
        File insertXml = new File(getClass().getResource("xml/insert.xml").getFile());
        String xml = FileUtils.readFileToString(insertXml, "UTF-8");

        Document dom = postAsDOM("wfs", xml);
        assertTrue(dom.getElementsByTagName("wfs:InsertResults").getLength() != 0);
//        print(dom);

    }

}
