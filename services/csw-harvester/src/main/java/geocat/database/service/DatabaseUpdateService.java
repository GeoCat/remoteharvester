package geocat.database.service;

import geocat.csw.CSWMetadata;
import geocat.csw.http.HttpResult;
import geocat.csw.http.IHTTPRetriever;
import geocat.database.entities.EndpointJob;
import geocat.database.entities.HarvestJob;
import geocat.database.repos.EndpointJobRepo;
import geocat.database.repos.HarvestJobRepo;
import geocat.events.EventFactory;
import geocat.events.determinework.CSWEndPointDetectedEvent;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
public class DatabaseUpdateService {

    Logger logger = LoggerFactory.getLogger(DatabaseUpdateService.class);


    @Autowired
    public EndpointJobService endpointJobService;
    @Autowired
    public EventFactory eventFactory;
    @Autowired
    private EndpointJobRepo endpointJobRepo;
    @Autowired
    private HarvestJobRepo harvestJobRepo;
    @Autowired
    @Qualifier("cookieAttachingRetriever")
    IHTTPRetriever retriever;

//    public Object updateDatabase(Object obj) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        Method m  = getClass().getMethod("updateDatabase", obj.getClass());
//        return m.invoke(obj);
//    }

    //idempotent transaction
    public List<CSWEndPointDetectedEvent> updateDatabase(CSWMetadata cswMetadata) {
        EndpointJob endpointJob = endpointJobRepo.findById(cswMetadata.getEndpointId()).get();
        endpointJob.setExpectedNumberOfRecords(cswMetadata.getNumberOfExpectedRecords());
        endpointJob.setUrlGetRecords(cswMetadata.getGetRecordsUrl());

        List<CSWEndPointDetectedEvent> result = createCSWEndPointDetectedEvents(cswMetadata);
        // endpointJob.setState(EndpointJobState.WORK_DETERMINED);
        endpointJobRepo.save(endpointJob);
        return result;
    }

    //pre-check --- check if a url is "good"
    //   the basic problem is that some nested URL return a "401" - but we want the system to continue anyways
    //   So, before we process the url, we do a quick check that it will return.
    //
    // returns 200 -> ok
    // returns anything else -> bad
    public boolean urlIsGood(String url) {
        HttpResult result;
        try {
            result = retriever.retrieveXML("GET", url, null, null, null);
        } catch (Exception e) {
            logger.error("while attempting to get nested url - " + url + ".  END POINT IGNORED.", e);
            return false;
        }
        if (result.getHttpCode() != 200) {
            logger.error("while attempting to get nested url - " + url + ".  END POINT IGNORED.",
                    new Exception("bad server response - expect 200, but got " + result.getHttpCode()));
            return false;
        }
        return true;
    }

    public List<CSWEndPointDetectedEvent> createCSWEndPointDetectedEvents(CSWMetadata metadata) {
        List<CSWEndPointDetectedEvent> result = new ArrayList<>();
        for (List<String> urlSet : metadata.getNestedGetCapUrls()) {
            String url = urlSet.get(0);
            boolean noActionRequired = endpointJobService.areTheseUrlsInDB(metadata.getHarvesterId(), urlSet);
            boolean goodUrl = urlIsGood(url);  //don't process if the URL is bad
            if (!noActionRequired && goodUrl) {
                EndpointJob job = endpointJobService.createInitial(metadata.getHarvesterId(), url, metadata.getFilter(), metadata.isLookForNestedDiscoveryService());
                result.add(eventFactory.create_CSWEndPointDetectedEvent(job.getHarvestJobId(), job.getEndpointJobId(), url, metadata.getFilter(), metadata.isLookForNestedDiscoveryService()));
            }
        }
        return result;
    }

    static Object lockobject = new Object();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    //synchronized so other threads cannot update while we are writing...
    public void errorOccurred(Exchange exchange) {
        synchronized (lockobject) {
            Exception e = (Exception) exchange.getMessage().getHeader("exception");
            if (e == null)
                return;
            String processId = (String) exchange.getMessage().getHeader("processID");
            Optional<HarvestJob> _job = harvestJobRepo.findById(processId);
            if (!_job.isPresent())
                return; // cannot update database.  Likely DB issue or very very early exception
            HarvestJob job = _job.get();
            if (job.getMessages() == null)
                job.setMessages("");
            String thisMessage = "\n--------------------------------------\n";
            thisMessage += "WHEN:" + Instant.now().toString() + "\n\n";
            thisMessage += convertToString(e);
            thisMessage += "\n--------------------------------------\n";
            job.setMessages(job.getMessages() + thisMessage);
            HarvestJob j2 = harvestJobRepo.save(job);
        }
    }


    public static String convertToString(Throwable e) {
        String result = e.getClass().getCanonicalName() + " - " + e.getMessage();

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTraceStr = sw.toString();

        result += stackTraceStr;
        if (e.getCause() != null)
            return result + convertToString(e.getCause());
        return result;
    }

}
