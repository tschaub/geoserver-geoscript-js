package org.geoserver.geoscript.javascript.wfs;

import java.io.File;

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
    
    public void testAfter() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
            + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
            + " xmlns:gml=\"http://www.opengis.net/gml\" "
            + " xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\">"
            + "<wfs:Insert handle=\"insert-1\">"
            + " <sf:PrimitiveGeoFeature gml:id=\"cite.gmlsf0-f01\">"
            + "  <gml:description>"
            + "Fusce tellus ante, tempus nonummy, ornare sed, accumsan nec, leo."
            + "Vivamus pulvinar molestie nisl."
            + "</gml:description>"
            + "<gml:name>Aliquam condimentum felis sit amet est.</gml:name>"
            //+ "<gml:name codeSpace=\"http://cite.opengeospatial.org/gmlsf\">cite.gmlsf0-f01</gml:name>"
            + "<sf:curveProperty>"
            + "  <gml:LineString gml:id=\"cite.gmlsf0-g01\" srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
            + "   <gml:posList>47.608284 19.034142 51.286873 16.7836 49.849854 15.764992</gml:posList>"
            + " </gml:LineString>"
            + "</sf:curveProperty>"
            + "<sf:intProperty>1025</sf:intProperty>"
            + "<sf:measurand>7.405E2</sf:measurand>"
            + "<sf:dateTimeProperty>2006-06-23T12:43:12+01:00</sf:dateTimeProperty>"
            + "<sf:decimalProperty>90.62</sf:decimalProperty>"
            + "</sf:PrimitiveGeoFeature>"
            + "</wfs:Insert>"
            + "<wfs:Insert handle=\"insert-2\">"
            + "<sf:AggregateGeoFeature gml:id=\"cite.gmlsf0-f02\">"
            + " <gml:description>"
            + "Duis nulla nisi, molestie vel, rhoncus a, ullamcorper eu, justo. Sed bibendum."
            + " Ut sem. Mauris nec nunc a eros aliquet pharetra. Mauris nonummy, pede et"
            + " tincidunt ultrices, mauris lectus fermentum massa, in ullamcorper lectus"
            + "felis vitae metus. Sed imperdiet sollicitudin dolor."
            + " </gml:description>"
            + " <gml:name codeSpace=\"http://cite.opengeospatial.org/gmlsf\">cite.gmlsf0-f02</gml:name>"
            + " <gml:name>QuisquÃ© viverra</gml:name>"
            + " <gml:boundedBy>"
            + "   <gml:Envelope srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
            + "     <gml:lowerCorner>36.1 8.0</gml:lowerCorner>"
            + "    <gml:upperCorner>52.0 21.1</gml:upperCorner>"
            + "   </gml:Envelope>"
            + "  </gml:boundedBy>"
            + "   <sf:multiPointProperty>"
            + "<gml:MultiPoint srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
            + "<gml:pointMember>"
            + " <gml:Point><gml:pos>49.325176 21.036873</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "<gml:pointMember>"
            + "  <gml:Point><gml:pos>36.142586 13.56189</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "<gml:pointMember>"
            + "  <gml:Point><gml:pos>51.920937 8.014193</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "</gml:MultiPoint>"
            + "</sf:multiPointProperty>"
            +

            "<sf:doubleProperty>2012.78</sf:doubleProperty>"
            + "  <sf:intRangeProperty>43</sf:intRangeProperty>"
            + " <sf:strProperty>"
            + "Donec ligulÃ¤ pede, sodales iÅ„, vehicula eu, sodales et, lÃªo."
            + "</sf:strProperty>"
            + "<sf:featureCode>AK121</sf:featureCode>"
            + "</sf:AggregateGeoFeature>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
//        print(dom);

    }

}
