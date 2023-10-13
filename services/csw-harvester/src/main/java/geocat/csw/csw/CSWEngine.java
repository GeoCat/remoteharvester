package geocat.csw.csw;


import geocat.csw.http.HttpResult;
import geocat.csw.http.IHTTPRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component
@Scope("prototype")
public class CSWEngine {

    public static String GETCAP_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<csw:GetCapabilities xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" service=\"CSW\">\n" +
            "<ows:AcceptVersions xmlns:ows=\"http://www.opengis.net/ows\">\n" +
            "<ows:Version>2.0.2</ows:Version>\n" +
            "</ows:AcceptVersions>\n" +
            "<ows:AcceptFormats xmlns:ows=\"http://www.opengis.net/ows\">\n" +
            "<ows:OutputFormat>application/xml</ows:OutputFormat>\n" +
            "</ows:AcceptFormats>\n" +
            "</csw:GetCapabilities>\n";
    public static String GETCAP_KVP = "request=GetCapabilities&service=CSW&acceptVersions=2.0.2&acceptFormats=application%2Fxml";
    @Autowired
    @Qualifier("cookieAttachingRetriever")
    IHTTPRetriever retriever;
    @Autowired
    OGCFilterService ogcFilterService;

    public final static String UTF8_BOM = "\uFEFF";
    private final static Charset UTF8_CHARSET = Charset.forName("UTF-8");


    public static String trim(String s) {
        String result = s.trim();

        if (result.startsWith(UTF8_BOM)) {
            result = result.substring(1).trim();
        }

        return result;
    }

    public static boolean isXML(String doc) {
        try {
            if (!doc.startsWith("<?xml")) {
                // sometimes it doesn't start with the xml declaration
                doc = trim(doc);
                if (!doc.startsWith("<"))
                    return false; //not xml
                if (doc.length() < 4)
                    return false;
                //flaky, is second char a letter?
                return Character.isLetter(doc.charAt(1));
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //----
    public String GetCapabilities(String url) throws Exception {
        String result;

        try {
            result = tryGetCapabilities(url, true);
        } catch (Exception ex) {
            result = tryGetCapabilities(url, false);
        }

        return result;
    }

    private String tryGetCapabilities(String url, boolean usePost) throws Exception {
        String result = usePost?GetCapabilitiesPOST(url):GetCapabilitiesGET(url);
        checkCapabilitiesResponse(result);

        return result;
    }

    private void checkCapabilitiesResponse(String capabilitiesResponse) throws Exception {
        if (!isXML(capabilitiesResponse)) {
            throw new Exception("URL did not return XML!");
        }

        if (capabilitiesResponse.contains("<ows:ExceptionReport") ||
                capabilitiesResponse.contains("<ExceptionReport")) {
            throw new Exception("GetCapabilities exception: " + capabilitiesResponse);
        }

        if (!capabilitiesResponse.contains("<csw:Capabilities") ||
                capabilitiesResponse.contains("<Capabilities")) {
            throw new Exception("GetCapabilities response is not valid: " + capabilitiesResponse);
        }
    }

    protected String GetCapabilitiesPOST(String url) throws Exception {
        HttpResult result = retriever.retrieveXML("POST", url, GETCAP_XML, null, null);
        if (result.getHttpCode() == 500) {
            throw new Exception("attempting to get Cap with POST gives 500");
        }

        if (result.getHttpCode() == 404) {
            throw new Exception("attempting to get Cap with POST gives 404");
        }

        return new String(result.getData());
    }

    protected String GetCapabilitiesGET(String url) throws Exception {
        if (url.endsWith("?"))
            url += GETCAP_KVP;
        else if (!url.contains("?"))
            url += "?" + GETCAP_KVP;
        //else
        //    url += "&" + GETCAP_KVP;

        //otherwise, likely already has the request=GetCapabilities in it!
        HttpResult result = retriever.retrieveXML("GET", url, null, null, null);
        return new String(result.getData());
    }
    //--

    public String GetRecords(String url, String requestXML) throws Exception {
        HttpResult result = retriever.retrieveXML("POST", url, requestXML, null, null);
        return new String(result.getData());
    }


}
