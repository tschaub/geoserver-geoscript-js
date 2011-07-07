package org.geoserver.geoscript.javascript.process;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.Parameter;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class JavaScriptProcessTest extends GeoServerTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = new File(getClass().getResource("scripts").getFile());
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }
    
    public void testJavaScriptProcessAdd() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        assertTrue("created add process", process instanceof org.geotools.process.Process);

        process = new JavaScriptProcess("buffer");
        assertTrue("created buffer process", process instanceof org.geotools.process.Process);
    }

    public void testGetTitleAdd() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        InternationalString title = process.getTitle();
        assertEquals("correct title", "JavaScript Addition Process", title.toString());
    }
    
    public void testGetDescriptionAdd() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        InternationalString description = process.getDescription();
        assertEquals("correct description", "Process that accepts two numbers and returns their sum.", description.toString());
    }
    
    public void testGetParameterInfoAdd() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        Map<String, Parameter<?>> inputs = process.getParameterInfo();
        
        assertTrue("first in inputs", inputs.containsKey("first"));
        Parameter<?> first = inputs.get("first");
        assertEquals("first title", "First Operand", first.getTitle().toString());
        assertEquals("first description", "The first operand.", first.getDescription().toString());
        assertEquals("first type", Float.class, first.getType());
        
        assertTrue("second in inputs", inputs.containsKey("first"));
        Parameter<?> second = inputs.get("second");
        assertEquals("second title", "Second Operand", second.getTitle().toString());
        assertEquals("second description", "The second operand.", second.getDescription().toString());
        assertEquals("second type", Float.class, second.getType());
    }
    
    public void testGetResultInfoAdd() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        Map<String, Parameter<?>> outputs = process.getResultInfo();
        
        assertTrue("sum in outputs", outputs.containsKey("sum"));
        Parameter<?> sum = outputs.get("sum");
        assertEquals("sum title", "Sum", sum.getTitle().toString());
        assertEquals("sum description", "The sum of the two inputs", sum.getDescription().toString());
        assertEquals("sum type", Float.class, sum.getType());
        
    }

    public void testExecuteAdd() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("first", 2.0);
        input.put("second", 4.0);
        Map<String,Object> result = process.execute(input, null);
        assertTrue("sum in results", result.containsKey("sum"));
        Object sum = result.get("sum");
        assertEquals("correct sum", 6.0, (Double) sum, 0.0);
    }
    
    public void testGetParameterInfoBuffer() {
        JavaScriptProcess process = new JavaScriptProcess("buffer");
        Map<String, Parameter<?>> inputs = process.getParameterInfo();
        
        assertTrue("geom in inputs", inputs.containsKey("geom"));
        Parameter<?> geom = inputs.get("geom");
        assertEquals("geom title", "Input Geometry", geom.getTitle().toString());
        assertEquals("geom description", "The target geometry.", geom.getDescription().toString());
        assertEquals("geom type", Geometry.class, geom.getType());
        
        assertTrue("distance in inputs", inputs.containsKey("distance"));
        Parameter<?> distance = inputs.get("distance");
        assertEquals("distance title", "Buffer Distance", distance.getTitle().toString());
        assertEquals("distance description", "The distance by which to buffer the geometry.", distance.getDescription().toString());
        assertEquals("distance type", Double.class, distance.getType());
    }
    
    public void testGetResultInfoBuffer() {
        JavaScriptProcess process = new JavaScriptProcess("buffer");
        Map<String, Parameter<?>> outputs = process.getResultInfo();
        
        assertTrue("result in outputs", outputs.containsKey("result"));
        Parameter<?> result = outputs.get("result");
        assertEquals("result title", "Result", result.getTitle().toString());
        assertEquals("result description", "The buffered geometry.", result.getDescription().toString());
        assertEquals("result type", Geometry.class, result.getType());
        
    }

    public void testExecuteBuffer() throws Exception {
        JavaScriptProcess process = new JavaScriptProcess("buffer");
        WKTReader wktReader = new WKTReader();
        Geometry point = wktReader.read("POINT(1 1)");
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geom", point);
        input.put("distance", 4.0);
        Map<String,Object> result = process.execute(input, null);
        assertTrue("result in results", result.containsKey("result"));
        Object obj = result.get("result");
        assertTrue("got back a geometry", obj instanceof Geometry);
        Geometry geom = (Geometry) obj;
        Double exp = Math.PI * 16;
        assertEquals("correct sum", exp, geom.getArea(), 1.0);
    }

}
