package geocat.csw.csw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;

@Component
public class CSWGetCapHandler {

    Logger logger = LoggerFactory.getLogger(CSWGetCapHandler.class);

    //typically, this is easy, however, it can get more complicated if there are multiple postencodings
    public static String extractGetRecordsLink(Document getCapDoc) throws Exception {
        NodeList nodes = XMLTools.xpath_nodeset(getCapDoc, "/Capabilities/OperationsMetadata/Operation[@name='GetRecords']/DCP/HTTP/Post");
        if (nodes.getLength() == 1) {
            Node node = nodes.item(0);
            return node.getAttributes().getNamedItem("xlink:href").getNodeValue();
        }
        //there's more than one
        //<ows:Post xlink:href="https//...">
        //   <ows:Constraint name="PostEncoding">
        //       <ows:Value>XML</ows:Value>
        //    </ows:Constraint>
        //</ows:Post>
        //<ows:Post xlink:href="https//...">
        //   <ows:Constraint name="PostEncoding">
        //       <ows:Value>SOAP</ows:Value>
        //    </ows:Constraint>
        //</ows:Post>
        nodes = XMLTools.xpath_nodeset(getCapDoc, "/Capabilities/OperationsMetadata/Operation[@name='GetRecords']/DCP/HTTP/Post[Constraint/Value = 'XML']");
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            return node.getAttributes().getNamedItem("xlink:href").getNodeValue();
        }

        //ireland case - only a GET
        nodes = XMLTools.xpath_nodeset(getCapDoc, "/Capabilities/OperationsMetadata/Operation[@name='GetRecords']/DCP/HTTP/Get");
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            return node.getAttributes().getNamedItem("xlink:href").getNodeValue();
        }
        throw new Exception("couldnt extract GetRecords from GetCapabilities document.");
    }

    public String extractGetRecordsURL(String getCapResponseXML) throws Exception {
        Document doc = XMLTools.parseXML(getCapResponseXML);
        String getRecordsURL = extractGetRecordsLink(doc);
        URL u = new URL(getRecordsURL);
        if (!u.getProtocol().equalsIgnoreCase("HTTP") && !u.getProtocol().equalsIgnoreCase("HTTPS"))
            throw new Exception("getRecordsURL URL isn't HTTP or HTTPS - security violation");

        logger.debug("      * GetRecords Endpoint is:" + u.toString());

        return u.toString();
    }

    public String extractIdentifierFieldName(String getCapResponseXML) throws Exception {
        Document getCapDoc = XMLTools.parseXML(getCapResponseXML);

        String identifierFieldName = "";

        NodeList nodes = XMLTools.xpath_nodeset(getCapDoc, "/Capabilities/OperationsMetadata/Operation[@name='GetRecords']/Constraint[@name='SupportedISOQueryables']/Value");
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getTextContent().toLowerCase().equals("identifier") ||
                    nodes.item(i).getTextContent().toLowerCase().contains(":identifier")) {
                identifierFieldName = nodes.item(i).getNodeValue();
                break;
            }
        }
        return identifierFieldName;
    }
}
