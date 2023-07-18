package com.geocat.ingester.events;

import com.geocat.ingester.dao.harvester.HarvestJobRepo;
import com.geocat.ingester.dao.ingester.IngestJobRepo;
import com.geocat.ingester.events.ingest.AbortCommand;
import com.geocat.ingester.model.IngesterConfig;
import com.geocat.ingester.model.harvester.HarvestJob;
import com.geocat.ingester.model.harvester.HarvestJobState;
import com.geocat.ingester.model.ingester.IngestJob;
import com.geocat.ingester.model.metadata.HarvesterConfiguration;
import com.geocat.ingester.service.CatalogueService;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Component
@Scope("prototype")
//@Slf4j(topic = "com.geocat.ingester.events")
public class IngestEventService {

    Logger log = LoggerFactory.getLogger(IngestEventService.class);

    @Autowired
    IngestJobRepo ingestJobRepo;

    @Autowired
    private HarvestJobRepo harvestJobRepo;

    @Autowired
    public CatalogueService catalogueService;

    public AbortCommand createAbortEvent(String processID) throws Exception {
        Optional<IngestJob> job = ingestJobRepo.findById(processID);
        if (!job.isPresent())
            throw new Exception("could not find processID=" + processID);
        AbortCommand result = new AbortCommand(processID);

        return result;
    }

    //calls validate on the (parsed) input message
    public void validateIngesterConfig(Message message) throws Exception {
        String harvesterUuidOrName = ((IngesterConfig) message.getBody()).getLongTermTag();
        String harvestJobId = ((IngesterConfig) message.getBody()).getHarvestJobId();

        if (StringUtils.isEmpty(harvesterUuidOrName) && StringUtils.isEmpty(harvestJobId)) {
            throw new Exception("Harvester with name/uuid (longTermTag) or harvestJobId should be provided!");
        }

        Optional<HarvestJob> harvestJob;

        if (!StringUtils.isEmpty(harvesterUuidOrName)) {
            Optional<HarvesterConfiguration> harvesterConfigurationOptional = catalogueService.retrieveHarvesterConfiguration(harvesterUuidOrName);

            if (!harvesterConfigurationOptional.isPresent()) {
                log.info("Harvester with name/uuid " + harvesterUuidOrName + " not found.");
                throw new Exception(String.format("Harvester with name/uuid %s not found.", harvesterUuidOrName));
            }

            // Filter most recent
            harvestJob = harvestJobRepo.findMostRecentHarvestJobByLongTermTag(harvesterUuidOrName);
            if (!harvestJob.isPresent()) {
                log.info("No harvester job related found for the harvester with name/uuid " + harvesterUuidOrName + ".");
                throw new Exception(String.format("No harvester job related found for the harvester with name/uuid %s.", harvesterUuidOrName));
            }
        } else {
            harvestJob = harvestJobRepo.findById(harvestJobId);
            if (!harvestJob.isPresent()) {
                throw new Exception("Cannot find previous harvest run harvestJobId: " + harvestJobId);
            }

            if (harvestJob.get().getState() != HarvestJobState.COMPLETE) {
                throw new Exception("Harvest run harvestJobId: " + harvestJobId + ", state '" + harvestJob.get().getState() + "' is not valid.");
            }
        }
    }

    /**
     * remove all headers from the request
     * add processID=GUID  to be used for this ingester
     */
    public void addGUID(Message message) {
        message.getHeaders().clear();
        String guid = createGUID();
        message.getHeaders().put("processID", guid);
        message.getHeaders().put("JMSCorrelationID", guid);
        ((IngesterConfig) message.getBody()).setProcessID(guid);

    }

    //creates a new GUID
    public String createGUID() {
        UUID guid = UUID.randomUUID();
        return guid.toString();
    }

    //we are doing trivial JSON conversion
    //   take the processID from the header, and return it as a json string like;
    //{
    //     "processID":"5fcd5f22-1a40-4712-8d2d-ca88c2d0d472"
    //}
    public void resultJSON(Message message) {
        String uuid = ((IngesterConfig) message.getBody()).getProcessID();
        message.setBody("{\n     \"processID\":\"" + uuid + "\"\n}\n");
    }

    public IngestRequestedEvent createIngestRequestedEvent(IngesterConfig ingesterConfig, String processID) {
        IngestRequestedEvent result = new IngestRequestedEvent();
        result.setJobId(processID);

        if (!StringUtils.isEmpty(ingesterConfig.getLongTermTag())) {
            Optional<HarvestJob> harvestJob = harvestJobRepo.findMostRecentHarvestJobByLongTermTag(ingesterConfig.getLongTermTag());
            result.setHarvestJobId(harvestJob.get().getJobId());
        } else {
            result.setHarvestJobId(ingesterConfig.getHarvestJobId());
        }

        return result;
    }
}
