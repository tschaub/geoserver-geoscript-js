package org.geoserver.geoscript.javascript;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

/**
 *
 */
public class JavaScriptModulesTest extends GeoServerTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        URL url = getClass().getResource("test_scripts_1");
        File[] scripts = recursiveListFiles(new File(url.getFile()));
        for (File script : scripts) {
            String path = script.getPath();
            // TODO:  script.toURI().relativize(arg0)
//            dataDirectory.copyTo(getClass().getResourceAsStream(path), path);
        }
        super.populateDataDirectory(dataDirectory);
    }
    
    /**
     * Test method for {@link org.geoserver.geoscript.javascript.JavaScriptModules#getModulePaths()}.
     * @throws URISyntaxException 
     */
    public void testGetModulePath() throws URISyntaxException {
        JavaScriptModules jsModules = (JavaScriptModules) applicationContext.getBean("JSModules");
        List<String> paths = jsModules.getModulePaths();
        assertTrue("got some paths", paths.size() > 0);
        for (String path : paths) {
            URI uri = new URI(path);
            assertTrue("absolute URI", uri.isAbsolute());
            File file = new File(uri.getPath());
            assertTrue("path is directory", file.isDirectory());
            assertTrue("directory exists", file.exists());
        }
    }

    private static File[] recursiveListFiles(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }
        List<File> fileList = new ArrayList<File>();
        recursiveListFilesHelper(dir, fileList);
        Collections.sort(fileList);
        return fileList.toArray(new File[fileList.size()]);
    }

    private static void recursiveListFilesHelper(File dir, List<File> fileList) {
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                recursiveListFilesHelper(f, fileList);
            } else {
                fileList.add(f);
            }
        }
    }

}
