package geocat.csw.csw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.regex.Pattern;

@Component
public class CSWGetRecordsHandler {

    Logger logger = LoggerFactory.getLogger(CSWGetRecordsHandler.class);
    Pattern SearchResultsTagPattern = Pattern.compile("<[^:<>]+:SearchResults [^>]+numberOfRecordsReturned=\"(\\d+)\"[^>]+>");


    public int extractTotalNumberOfRecords(String getRecordsResponseXML) throws Exception {
        Document doc = XMLTools.parseXML(getRecordsResponseXML);
        return extractTotalNumberOfRecords(doc);
    }

    public int extractTotalNumberOfRecords(Document getRecordsResponseXML) throws Exception {
        String nrecordsString = XMLTools.xpath_attribute(getRecordsResponseXML,
                "/GetRecordsResponse/SearchResults", "numberOfRecordsMatched");
        return Integer.parseInt(nrecordsString);
    }

    public Integer extractNextRecordNumber(String getRecordsResponseXML) throws Exception {
        Document doc = XMLTools.parseXML(getRecordsResponseXML);
        return extractNextRecordNumber(doc);
    }

    public Integer extractNextRecordNumber(Document getRecordsResponseXML) throws Exception {
        try {
            String nextRecordString = XMLTools.xpath_attribute(getRecordsResponseXML, "/GetRecordsResponse/SearchResults", "nextRecord");
            return Integer.parseInt(nextRecordString);
        } catch (Exception e) {
            return null; // this can happen if there's no "nextRecord" in the response...
        }
    }

//    //this is MUCH faster than parsing the xml
//    // parse XML = 14seconds
//    //   this = <0.1 second
//    // NOTE: if the response is an error, this can be very slow.
//    //<csw:SearchResults numberOfRecordsMatched="138" numberOfRecordsReturned="10" elementSet="full" nextRecord="41">
//    public int extractActualNumberOfRecordsReturned(String getRecordsResponseXML) throws Exception {
//        if (!getRecordsResponseXML.contains("SearchResults"))
//            throw new Exception("XML doesn't contain SearchResults");
//        Matcher matcher = SearchResultsTagPattern.matcher(getRecordsResponseXML);
//        if (!matcher.find())
//            throw new Exception("couldn't parse xml");
//        String nreturned_string = matcher.group(1); //i.e. numberOfRecordsMatched="138" numberOfRecordsReturned="10" elementSet="full" nextRecord="41"
//        return Integer.parseInt(nreturned_string);
//    }

    public int extractActualNumberOfRecordsReturned(Document getRecordsResponseXML) throws Exception {
        String numberOfRecordsMatched = XMLTools.xpath_attribute(getRecordsResponseXML,
                "/GetRecordsResponse/SearchResults", "numberOfRecordsReturned");
        return Integer.parseInt(numberOfRecordsMatched);
    }
}
