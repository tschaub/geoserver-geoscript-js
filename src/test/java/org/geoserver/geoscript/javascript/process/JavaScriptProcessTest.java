package org.geoserver.geoscript.javascript.process;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.Parameter;
import org.opengis.util.InternationalString;

public class JavaScriptProcessTest extends GeoServerTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = new File(getClass().getResource("scripts").getFile());
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }
    
    public void testJavaScriptProcess() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        assertTrue("created a process", process instanceof org.geotools.process.Process);
    }
    
    public void testExecute() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("first", 2.0);
        input.put("second", 4.0);
        Map<String,Object> result = process.execute(input, null);
        assertTrue("sum in results", result.containsKey("sum"));
        Object sum = result.get("sum");
        assertEquals("correct sum", 6.0, (Double) sum, 0.0);
    }
    
    public void testGetTitle() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        InternationalString title = process.getTitle();
        assertEquals("correct title", "JavaScript Addition Process", title.toString());
    }
    
    public void testGetDescription() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        InternationalString description = process.getDescription();
        assertEquals("correct description", "Process that accepts two numbers and returns their sum.", description.toString());
    }
    
    public void testGetParameterInfo() {
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
    
    public void testGetResultInfo() {
        JavaScriptProcess process = new JavaScriptProcess("add");
        Map<String, Parameter<?>> outputs = process.getResultInfo();
        
        assertTrue("sum in outputs", outputs.containsKey("sum"));
        Parameter<?> sum = outputs.get("sum");
        assertEquals("sum title", "Sum", sum.getTitle().toString());
        assertEquals("sum description", "The sum of the two inputs", sum.getDescription().toString());
        assertEquals("sum type", Float.class, sum.getType());
        
    }

}
