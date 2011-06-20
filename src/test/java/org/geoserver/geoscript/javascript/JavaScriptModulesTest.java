package org.geoserver.geoscript.javascript;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.geoserver.test.GeoServerTestSupport;

import junit.framework.TestCase;

/**
 *
 */
public class JavaScriptModulesTest extends GeoServerTestSupport {

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

}
