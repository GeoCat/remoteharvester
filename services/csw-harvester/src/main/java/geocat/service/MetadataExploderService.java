package geocat.service;


import geocat.csw.csw.XMLTools;
import geocat.database.entities.MetadataRecord;
import geocat.database.entities.RecordSet;
import geocat.database.service.BlobStorageService;
import geocat.database.service.MetadataRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class MetadataExploderService {

    @Autowired
    MetadataRecordService metadataRecordService;

    @Autowired
    BlobStorageService blobStorageService;

    //again, to be consistent
    public static Document parseXML(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);


        DocumentBuilder builder = factory.newDocumentBuilder();

        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        return doc;
    }

    //record individual records in the database, given a bunch of results.
    public void explode(RecordSet recordSet, String xml_getRecords) throws Exception {
        Document xml = XMLTools.parseXML(xml_getRecords);
        explode(recordSet, xml);
    }

    //record individual records in the database, given a bunch of results.
    public void explode(RecordSet recordSet, Document xml_getRecords) throws Exception {
        NodeList metadataRecords = XMLTools.xpath_nodeset(xml_getRecords, "/GetRecordsResponse/SearchResults/MD_Metadata");
        Map<String, String> extraNamespaces = extractNamespace(xml_getRecords);
        for (int t = 0; t < metadataRecords.getLength(); t++) {
            Node metadataRecord = metadataRecords.item(t);
            stripEmptyElements(metadataRecord);
            addNamespaces(metadataRecord, extraNamespaces);
            String xmlStr = writeXML(metadataRecord);
            String identifier = extractIdentifier(metadataRecord);
            String sha2 = blobStorageService.computeSHA2(xmlStr);
            blobStorageService.ensureBlobExists(xmlStr, sha2);  //ok to do multiple times
            MetadataRecord record = metadataRecordService.create(recordSet, t, sha2, identifier);
        }
    }

    private String extractIdentifier(Node xml) throws XPathExpressionException {
        return XMLTools.xpath_node(xml, "fileIdentifier/CharacterString").getTextContent();
    }

    private void addNamespaces(Node metadataRecord, Map<String, String> extraNamespaces) {
        for (Map.Entry<String, String> ns : extraNamespaces.entrySet()) {
            ((Element) metadataRecord).setAttribute(ns.getKey(), ns.getValue());
        }
    }

    public Map<String, String> extractNamespace(Document d) throws XPathExpressionException {
        Map<String, String> result = new HashMap<>();
        NamedNodeMap response = XMLTools.xpath_node(d, "/GetRecordsResponse").getAttributes();
        NamedNodeMap searchResults = XMLTools.xpath_node(d, "/GetRecordsResponse/SearchResults").getAttributes();

        extractNamespaces(result, response);
        extractNamespaces(result, searchResults);

        return result;
    }

    private void extractNamespaces(Map<String, String> namespaces, NamedNodeMap attributes) {
        for (int t = 0; t < attributes.getLength(); t++) {
            Node node = attributes.item(t);
            String name = attributes.item(t).getNodeName();
            if (name.startsWith("xmlns:")) {
                namespaces.put(name, node.getNodeValue());
            }
        }
    }

    //this is here so we can be consistent with the XML writing (otherwise the SHA2 will change)
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

    public static void stripEmptyElements(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getTextContent().trim().length() == 0) {
                    child.getParentNode().removeChild(child);
                    i--;
                }
            }
            stripEmptyElements(child);
        }
    }

}
