package geocat.csw;

import geocat.csw.csw.*;
import geocat.events.determinework.CSWEndPointDetectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class CSWService {

    Logger logger = LoggerFactory.getLogger(CSWService.class);

    @Autowired
    CSWGetRecordsHandler cswGetRecordsHandler;

    @Autowired
    CSWGetCapHandler cswGetCapHandler;

    @Autowired
    CSWNestedDiscoveryHandler cswNestedDiscoveryHandler;

    @Autowired
    CSWEngine cswEngine;

    @Autowired
    OGCFilterService ogcFilterService;


    //this will setup an endpoint for read harvesting
    // output;
    //    * update the corresponding endpoint_job in the database
    //    * detected endpoints;
    //          * add record to DB (if needed)
    //          * emit event
    //
    // 1. GetCapabilities
    //      * annotate endpoint with urls of the service endpoints
    // 2. Do a filter request (hits-only)
    //      * annotate endpoint with how many to harvest
    // 3. Find Discovery records
    //      * add record to DB (if needed)
    //      * emit CSWEndPointDetectedEvent events
    public CSWMetadata getMetadata(CSWEndPointDetectedEvent endPointDetectedEvent) throws Exception {
        String harvestId = endPointDetectedEvent.getHarvesterId();
        long endpointId = endPointDetectedEvent.getEndPointId();
        String filter = endPointDetectedEvent.getFilter();

        logger.debug("getMetadata called on URL=" + endPointDetectedEvent.getUrl());
        String getRecordsUrl = endpoint_extractGetRecordsURL(endPointDetectedEvent);
        int expectedNumberOfRecords = endpoint_numberOfRecordsMatchingFilter(endPointDetectedEvent, getRecordsUrl);

        String identifierFieldName= endpoint_identifierFieldName(endPointDetectedEvent);

        List<List<String>> nestedGetCaps = new ArrayList<>();
        if (endPointDetectedEvent.isLookForNestedDiscoveryService())
            nestedGetCaps = endpoint_nestedDiscovery(endPointDetectedEvent, getRecordsUrl);

        CSWMetadata result = new CSWMetadata(harvestId,
                endpointId,
                expectedNumberOfRecords,
                getRecordsUrl,
                nestedGetCaps,
                filter,
                endPointDetectedEvent.isLookForNestedDiscoveryService(),
                identifierFieldName);
        return result;
    }

    private List<List<String>> endpoint_nestedDiscovery(CSWEndPointDetectedEvent endPointDetectedEvent, String url) throws Exception {
        logger.debug("determining nested discovery services from GETCAP URL=" + endPointDetectedEvent.getUrl() + ", with GetRecord URL=" + url);
        List<List<String>> result = new ArrayList<>();

        String getDiscoveryRecordsXml = cswEngine.GetRecords(url, ogcFilterService.getGetdiscoveryXml(endPointDetectedEvent.getFilter()));

        return cswNestedDiscoveryHandler.extractNestedDiscoveryEndpoints(getDiscoveryRecordsXml);
    }

    private int endpoint_numberOfRecordsMatchingFilter(CSWEndPointDetectedEvent endPointDetectedEvent, String url) throws Exception {
        logger.debug("determining number of records from GETCAP URL=" + endPointDetectedEvent.getUrl() + ", with GetRecord URL=" + url);

        String getRecordSummaryXml = cswEngine.GetRecords(url, ogcFilterService.getExpectedNumberRecordsXml(endPointDetectedEvent.getFilter()));
        return cswGetRecordsHandler.extractTotalNumberOfRecords(getRecordSummaryXml);
    }

    private String endpoint_extractGetRecordsURL(CSWEndPointDetectedEvent endPointDetectedEvent) throws Exception {
        logger.debug("requesting GetCapabilities from URL=" + endPointDetectedEvent.getUrl());

        String getcapXmlString = cswEngine.GetCapabilities(endPointDetectedEvent.getUrl());
        return cswGetCapHandler.extractGetRecordsURL(getcapXmlString);
    }

    private String endpoint_identifierFieldName(CSWEndPointDetectedEvent endPointDetectedEvent) throws Exception {
        logger.debug("requesting GetCapabilities from URL=" + endPointDetectedEvent.getUrl());

        String getcapXmlString = cswEngine.GetCapabilities(endPointDetectedEvent.getUrl());
        return cswGetCapHandler.extractIdentifierFieldName(getcapXmlString);
    }

    public String GetRecords(String url, String filter, int startRecord, int endRecord, Boolean doNotSort, String identifierFieldName) throws Exception {
        boolean _doNotSort = doNotSort != null && doNotSort;
        return cswEngine.GetRecords(url, ogcFilterService.getRecordsXML(filter, startRecord, endRecord, _doNotSort, identifierFieldName));
    }
}
