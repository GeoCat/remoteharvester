package net.geocat.service.exernalservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.geocat.database.linkchecker.entities.helper.HttpResult;
import net.geocat.http.BasicHTTPRetriever;
import net.geocat.model.EndpointStatus;
import net.geocat.model.HarvestStartResponse;
import net.geocat.model.HarvestStatus;
import net.geocat.model.HarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class HarvesterService {

    Logger logger = LoggerFactory.getLogger(HarvesterService.class);

    @Autowired
    BasicHTTPRetriever basicHTTPRetriever;

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${harvester.url}")
    String harvesterAPIURL;


    public String abortHarvest(String harvesterProcessID) throws Exception {
        String url = harvesterAPIURL + "/abortharvest/" + harvesterProcessID;
        HttpResult httpResponse = sendJSON("GET", url, null);
        return new String(httpResponse.getData());
    }

    // call the harvest remote service and return the processID
    public HarvestStartResponse startHarvest(HarvesterConfig harvestConfig) throws Exception {

        String url = harvesterAPIURL + "/startHarvest";
        String requestJSON = objectMapper.writeValueAsString(harvestConfig);

        HttpResult httpResponse = sendJSON("POST", url, requestJSON);
        String result = httpResponse.getData() == null ? "" : new String(httpResponse.getData());
        if (httpResponse.isErrorOccurred() || (httpResponse.getHttpCode() != 200))
            throw new Exception("couldnt start harvest process - " + result);

        HarvestStartResponse _result = objectMapper.readValue(result, HarvestStartResponse.class);
        logger.debug("started harvest with ProcessId=" + _result.getProcessID());

        return _result;
    }

    public HarvestStatus getHarvestState(String harvesterProcessID, boolean quick) throws Exception {
        String url = harvesterAPIURL + "/getstatus/" + harvesterProcessID + "?quick=" + quick;
        HttpResult httpResponse = sendJSON("GET", url, null);
        String result = httpResponse.getData() == null ? "" : new String(httpResponse.getData());
        if (httpResponse.isErrorOccurred() || (httpResponse.getHttpCode() != 200))
            throw new Exception("couldnt get harvest process state for harvesterProcessID=" + harvesterProcessID + " - " + result);

        HarvestStatus _result = objectMapper.readValue(result, HarvestStatus.class);
        logger.debug("harvest state=" + computeHarvestState(_result));

        return _result;
    }

    public String computeHarvestState(HarvestStatus status) {
        String result = "Harvest " + status.processID + " is in State:" + status.state;
        for (EndpointStatus endpoint : status.endpoints) {
            if (endpoint.expectedNumberOfRecords > 0) {
                int percent = (int) (((double) endpoint.numberOfRecordsReceived / (double) endpoint.expectedNumberOfRecords) * 100.0);
                result += ", endpoint: " + percent + "% complete (" + endpoint.numberOfRecordsReceived + " of " + endpoint.expectedNumberOfRecords + ")";
            }
        }
        return result;
    }

    // call the harvest remote service and return the processID
    public String getLastCompletedHarvestJobIdByLongTermTag(HarvesterConfig harvestConfig) throws Exception {

        String url = harvesterAPIURL + "/getLastCompletedHarvestJobIdByLongTermTag/" + harvestConfig.getLongTermTag();
        HttpResult httpResponse = sendJSON("GET", url, null);
        String result = httpResponse.getData() == null ? "" : new String(httpResponse.getData());
        if (httpResponse.isErrorOccurred() || (httpResponse.getHttpCode() != 200))
            throw new Exception("couldnt query the harvest job id process - " + result);

        // The identifier is returned between quotes
        return result.replace("\"", "");
    }


    public HttpResult sendJSON(String verb, String url, String json) throws Exception {
        HttpResult result = basicHTTPRetriever.retrieveJSON(verb, url, json, null, null);
        return result;
    }
}
