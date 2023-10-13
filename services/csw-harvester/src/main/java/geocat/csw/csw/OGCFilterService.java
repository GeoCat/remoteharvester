package geocat.csw.csw;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

@Component
@Scope("prototype")
public class OGCFilterService {

    public static String GETEXPECTEDNUMBERRECORDS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<csw:GetRecords xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" service=\"CSW\" version=\"2.0.2\" resultType=\"results\" outputSchema=\"http://www.isotc211.org/2005/gmd\" maxRecords=\"1\">\n" +
            "    <csw:Query typeNames=\"csw:Record\">\n" +
            "        <csw:ElementSetName>full</csw:ElementSetName>\n" +
            "              PUT_FILTER_HERE \n" +
            "    </csw:Query>\n" +
            "</csw:GetRecords>\n";


//    public static String GETRECORDS_XML = "<?xml version=\"1.0\"?>\n" +
//            "<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" \n" +
//            "xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:ogc=\"http://www.opengis.net/ogc\" \n" +
//            "service=\"CSW\" version=\"2.0.2\" resultType=\"results\" \n" +
//            "outputFormat=\"application/xml\" \n" +
//            "startPosition=\"PUT_START_POSITION_HERE\" \n"+
//            "maxRecords=\"PUT_MAX_RECORDS_HERE\" \n"+
//            "outputSchema=\"http://www.isotc211.org/2005/gmd\">\n" +
//            "  <csw:Query typeNames=\"csw:Record\">\n" +
//            "    <csw:ElementSetName>full</csw:ElementSetName> \n" +
//            "    <csw:Constraint  version=\"1.1.0\">\n" +
//            "        PUT_FILTER_HERE\n" +
//            "     </csw:Constraint>\n" +
//            " </csw:Query>\n" +
//            "</csw:GetRecords>\n";

    public static String ORDERBY = " <ogc:SortBy>\n" +
            "            <ogc:SortProperty>\n" +
            "                <ogc:PropertyName>PUT_ORDERBYFIELDNAME_HERE</ogc:PropertyName>\n" +
            "                <ogc:SortOrder>DESC</ogc:SortOrder>\n" +
            "            </ogc:SortProperty>\n" +
            "        </ogc:SortBy>\n";

    public static String GETRECORDS_FILTER_XML = "<?xml version=\"1.0\"?>\n" +
            "<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" \n" +
            "xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:ogc=\"http://www.opengis.net/ogc\" \n" +
            "service=\"CSW\" version=\"2.0.2\" resultType=\"results\" \n" +
            "outputFormat=\"application/xml\" \n" +
            "startPosition=\"PUT_START_POSITION_HERE\" \n" +
            "maxRecords=\"PUT_MAX_RECORDS_HERE\" \n" +
            "outputSchema=\"http://www.isotc211.org/2005/gmd\">\n" +
            "  <csw:Query typeNames=\"csw:Record\">\n" +
            "    <csw:ElementSetName>full</csw:ElementSetName> \n" +
            "    <csw:Constraint  version=\"1.1.0\">\n" +
            "        PUT_FILTER_HERE\n" +
            "     </csw:Constraint>\n" +
            "PUT_ORDERBY_HERE\n" +
            " </csw:Query>\n" +
            "</csw:GetRecords>\n";

    public static String GETRECORDS_NO_FILTER_XML = "<?xml version=\"1.0\"?>\n" +
            "<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" \n" +
            "xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:ogc=\"http://www.opengis.net/ogc\" \n" +
            "service=\"CSW\" version=\"2.0.2\" resultType=\"results\" \n" +
            "outputFormat=\"application/xml\" \n" +
            "startPosition=\"PUT_START_POSITION_HERE\" \n" +
            "maxRecords=\"PUT_MAX_RECORDS_HERE\" \n" +
            "outputSchema=\"http://www.isotc211.org/2005/gmd\">\n" +
            "  <csw:Query typeNames=\"csw:Record\">\n" +
            "    <csw:ElementSetName>full</csw:ElementSetName> \n" +
            "PUT_ORDERBY_HERE\n" +
            " </csw:Query>\n" +
            "</csw:GetRecords>\n";


    public static String GETDISCOVERY_XML = "<?xml version=\"1.0\"?>\n" +
            "<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" \n" +
            "xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:ogc=\"http://www.opengis.net/ogc\" \n" +
            "service=\"CSW\" version=\"2.0.2\" resultType=\"results\" \n" +
            "outputFormat=\"application/xml\" \n" +
            "outputSchema=\"http://www.isotc211.org/2005/gmd\">\n" +
            "  <csw:Query typeNames=\"csw:Record\">\n" +
            "    <csw:ElementSetName>full</csw:ElementSetName> \n" +
            "    <csw:Constraint  version=\"1.1.0\">\n" +
            "        PUT_FILTER_HERE\n" +
            "     </csw:Constraint>\n" +
            " </csw:Query>\n" +
            "</csw:GetRecords>\n";

    public static String simpleDiscoveryFilter = "<ogc:Filter>\n" +
            "        <ogc:PropertyIsEqualTo >\n" +
            "          <ogc:PropertyName>ServiceType</ogc:PropertyName>\n" +
            "          <ogc:Literal>discovery</ogc:Literal>\n" +
            "        </ogc:PropertyIsEqualTo>\n" +
            "     </ogc:Filter>\n";

    public static String complexDiscoveryFilter = "<ogc:Filter>\n" +
            "        <ogc:And> \n" +
            "           <ogc:PropertyIsEqualTo >\n" +
            "              <ogc:PropertyName>ServiceType</ogc:PropertyName>\n" +
            "              <ogc:Literal>discovery</ogc:Literal>\n" +
            "            </ogc:PropertyIsEqualTo>\n" +
            "             PUT_FILTER_HERE\n" +
            "        </ogc:And>\n   " +
            "     </ogc:Filter>\n";


    public String getGetdiscoveryXml(String filter) throws Exception {
        String fullFilter;
        if ((filter == null) || (filter.trim().isEmpty())) {
            fullFilter = simpleDiscoveryFilter;
        } else {
            String partialFilter = extractSmallFilter(filter);
            fullFilter = complexDiscoveryFilter.replace("PUT_FILTER_HERE", partialFilter);
        }

        return GETDISCOVERY_XML.replace("PUT_FILTER_HERE", fullFilter);
    }

    public String getRecordsXML(String filter, int startRecord, int endRecord, boolean doNotSort, String identifierFieldName) {
        String xml;
        int nrecords = endRecord - startRecord + 1;
        if ((filter != null) && (!filter.isEmpty())) {
            xml = GETRECORDS_FILTER_XML.replace("PUT_FILTER_HERE", filter)
                    .replace("PUT_START_POSITION_HERE", Integer.toString(startRecord))
                    .replace("PUT_MAX_RECORDS_HERE", Integer.toString(nrecords));

        } else {
            xml = GETRECORDS_NO_FILTER_XML.replace("PUT_START_POSITION_HERE", Integer.toString(startRecord))
                    .replace("PUT_MAX_RECORDS_HERE", Integer.toString(nrecords));
        }
        if (doNotSort || !StringUtils.hasLength(identifierFieldName)) {
            xml = xml.replace("PUT_ORDERBY_HERE", "");
        } else {
            xml = xml.replace("PUT_ORDERBY_HERE", ORDERBY.replace("PUT_ORDERBYFIELDNAME_HERE", identifierFieldName));
        }
        return xml;
    }

    //<ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
    //     <ogc:PropertyIsEqualTo>
    //         <ogc:PropertyName>apiso:subject</ogc:PropertyName>
    //         <ogc:Literal>inspireidentifiziert</ogc:Literal>
    //     </ogc:PropertyIsEqualTo>
    // </ogc:Filter>
    //
    // =====>
    //
    //     <ogc:PropertyIsEqualTo>
    //         <ogc:PropertyName>apiso:subject</ogc:PropertyName>
    //         <ogc:Literal>inspireidentifiziert</ogc:Literal>
    //     </ogc:PropertyIsEqualTo>
    //
    // removes the "<ogc:Filter>" tag
    private String extractSmallFilter(String filter) throws Exception {
        Document doc = XMLTools.parseXML(filter);
        Node mainFilter = doc.getFirstChild().getFirstChild();
        return writeXML(mainFilter);
    }

    public String getExpectedNumberRecordsXml(String filter) throws Exception {
        // not filter - no need to add anything
        if ((filter == null) || (filter.trim().isEmpty())) {
            String result = GETEXPECTEDNUMBERRECORDS_XML.replace("PUT_FILTER_HERE", "");
            return result;
        }
        // filter - need to add it
        String fullFilter = "<csw:Constraint  version=\"1.1.0\">" + filter + "</csw:Constraint>";
        String result = GETEXPECTEDNUMBERRECORDS_XML.replace("PUT_FILTER_HERE", fullFilter);
        return result;
    }


    protected String writeXML(Node doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;

        transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.getBuffer().toString();
    }


}
