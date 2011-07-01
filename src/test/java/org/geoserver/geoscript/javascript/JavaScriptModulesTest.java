package org.geoserver.geoscript.javascript;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.geoserver.test.GeoServerTestSupport;
import org.mozilla.javascript.Scriptable;

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

    /**
     * Test method for {@link org.geoserver.geoscript.javascript.JavaScriptModules#require()}.
     */
    public void testRequireGeoScript() {
        JavaScriptModules jsModules = (JavaScriptModules) applicationContext.getBean("JSModules");
        Scriptable exports = jsModules.require("geoscript");
        Object geomObj = exports.get("geom", exports);
        assertTrue("geom in exports", geomObj instanceof Scriptable);
        Object projObj = exports.get("proj", exports);
        assertTrue("proj in exports", projObj instanceof Scriptable);
    }

    /**
     * Test method for {@link org.geoserver.geoscript.javascript.JavaScriptModules#require()}.
     */
    public void testRequireGeoServer() {
        JavaScriptModules jsModules = (JavaScriptModules) applicationContext.getBean("JSModules");
        Scriptable exports = jsModules.require("geoserver");
        Object catalogObj = exports.get("catalog", exports);
        assertTrue("catalog in exports", catalogObj instanceof Scriptable);
        Object processObj = exports.get("process", exports);
        assertTrue("process in exports", processObj instanceof Scriptable);
    }

}
