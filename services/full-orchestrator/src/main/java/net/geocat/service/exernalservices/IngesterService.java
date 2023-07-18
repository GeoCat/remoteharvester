package net.geocat.service.exernalservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.geocat.database.linkchecker.entities.helper.HttpResult;
import net.geocat.http.BasicHTTPRetriever;
import net.geocat.model.HarvestStartResponse;
import net.geocat.model.IngestStatus;
import net.geocat.model.LinkCheckRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class IngesterService {

    Logger logger = LoggerFactory.getLogger(IngesterService.class);

    @Autowired
    BasicHTTPRetriever basicHTTPRetriever;

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ingester.url}")
    String ingesterAPIURL;


    public String abortIngest(String ingestProcessID) throws Exception {
        String url = ingesterAPIURL + "/abort/" + ingestProcessID;
        HttpResult httpResponse = sendJSON("GET", url, null);
        return new String(httpResponse.getData());
    }


    // call the harvest remote service and return the processID
    public HarvestStartResponse startIngest(String harvesterId) throws Exception {

        String url = ingesterAPIURL + "/startIngest";
        LinkCheckRunConfig linkCheckRunConfig = new LinkCheckRunConfig();
        linkCheckRunConfig.setHarvestJobId(harvesterId);
        String requestJSON = objectMapper.writeValueAsString(linkCheckRunConfig);

        HttpResult httpResponse = sendJSON("POST", url, requestJSON);
        String result = httpResponse.getData() == null ? "" : new String(httpResponse.getData());
        if (httpResponse.isErrorOccurred() || (httpResponse.getHttpCode() != 200))
            throw new Exception("couldn't start ingest process - " + result);

        HarvestStartResponse _result = objectMapper.readValue(result, HarvestStartResponse.class);
        logger.debug("started ingest with ProcessId=" + _result.getProcessID());

        return _result;
    }

    public IngestStatus getIngestState(String ingesterProcessID, boolean quick) throws Exception {
        String url = ingesterAPIURL + "/getstatus/" + ingesterProcessID + "?quick=" + quick;
        HttpResult httpResponse = sendJSON("GET", url, null);
        String result = httpResponse.getData() == null ? "" : new String(httpResponse.getData());
        if (httpResponse.isErrorOccurred() || (httpResponse.getHttpCode() != 200))
            throw new Exception("couldn't get ingest process state - " + result);

        IngestStatus _result = objectMapper.readValue(result, IngestStatus.class);
        logger.debug("ingest state=" + computeIngesterState(_result));

        return _result;
    }


    public String computeIngesterState(IngestStatus status) {
        String result = "Ingest " + status.getProcessID() + " is in State:" + status.getState();
        if (status.totalRecords > 0) {
            result += ", total records:" + status.totalRecords;
            int percent_ingested = (int) (((double) status.numberOfRecordsIngested / (double) status.totalRecords) * 100.0);
            int percent_indexed = (int) (((double) status.numberOfRecordsIndexed / (double) status.totalRecords) * 100.0);

            result += "\n    + " + percent_ingested + "% ingested";
            result += "\n    + " + percent_indexed + "% indexed";
        }

        return result;
    }

    public HttpResult sendJSON(String verb, String url, String json) throws Exception {
        HttpResult result = basicHTTPRetriever.retrieveJSON(verb, url, json, null, null);
        return result;
    }
}
