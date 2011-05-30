package org.geoserver.geoscript.javascript;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

/**
 *
 */
public class GeoScriptModulesTest extends TestCase {

    /**
     * Test method for {@link org.geoserver.geoscript.javascript.GeoScriptModules#getModulePath()}.
     * @throws URISyntaxException 
     */
    public void testGetModulePath() throws URISyntaxException {
        String path = GeoScriptModules.getModulePath();
        URI uri = new URI(path);
        assertTrue("absolute URI", uri.isAbsolute());
        File file = new File(uri.getPath());
        assertTrue("path is directory", file.isDirectory());
        assertTrue("directory exists", file.exists());
    }

}
