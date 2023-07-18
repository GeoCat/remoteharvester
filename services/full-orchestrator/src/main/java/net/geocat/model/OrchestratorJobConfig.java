package net.geocat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestratorJobConfig {

    // STUFF FOR HARVEST - CF. HarvesterConfig
    String longTermTag;
    boolean lookForNestedDiscoveryService;
    String filter;
    String url;
    Integer numberOfRecordsPerRequest;
    String getRecordQueueHint;
    ProblematicResultsConfiguration problematicResultsConfiguration;
    Boolean doNotSort;

    //STUFF FOR LINKCHECK - CF. LinkCheckRunConfig
    Boolean useOtherJobsHTTPCache;
    Boolean deleteHTTPCacheWhenComplete;
    Integer maxDataLinksToFollow;
    Integer maxAtomEntriesToAttempt;
    Integer maxAtomSectionLinksToFollow;

    //STUFF FOR ORCHESTRATION
    Boolean executeLinkChecker;
    Boolean skipHarvesting;
    String processID;

    //SHARED - used by LinkChecker and Harvester
    Integer storeAtMostNHistoricalRuns;


    //--

    @Override
    public String toString() {
        try {
            return asJSON();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "--- couldnt create JSON --- ";
        }
    }

    public String asJSON() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public LinkCheckRunConfig asLinkCheckRunConfig() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        LinkCheckRunConfig result = objectMapper.readValue(asJSON(), LinkCheckRunConfig.class);
        return result;
    }

    public HarvesterConfig asHarvesterConfig() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        HarvesterConfig result = objectMapper.readValue(asJSON(), HarvesterConfig.class);
        return result;
    }

    //---


    public void setNumberOfRecordsPerRequest(Integer numberOfRecordsPerRequest) {
        this.numberOfRecordsPerRequest = numberOfRecordsPerRequest;
    }

    public Integer getStoreAtMostNHistoricalRuns() {
        return storeAtMostNHistoricalRuns;
    }

    public void setStoreAtMostNHistoricalRuns(Integer storeAtMostNHistoricalRuns) {
        this.storeAtMostNHistoricalRuns = storeAtMostNHistoricalRuns;
    }

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }

    public String getLongTermTag() {
        return longTermTag;
    }

    public void setLongTermTag(String longTermTag) {
        this.longTermTag = longTermTag;
    }

    public boolean isLookForNestedDiscoveryService() {
        return lookForNestedDiscoveryService;
    }

    public void setLookForNestedDiscoveryService(boolean lookForNestedDiscoveryService) {
        this.lookForNestedDiscoveryService = lookForNestedDiscoveryService;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getNumberOfRecordsPerRequest() {
        return numberOfRecordsPerRequest;
    }

    public void setNumberOfRecordsPerRequest(int numberOfRecordsPerRequest) {
        this.numberOfRecordsPerRequest = numberOfRecordsPerRequest;
    }

    public String getGetRecordQueueHint() {
        return getRecordQueueHint;
    }

    public void setGetRecordQueueHint(String getRecordQueueHint) {
        this.getRecordQueueHint = getRecordQueueHint;
    }

    public ProblematicResultsConfiguration getProblematicResultsConfiguration() {
        return problematicResultsConfiguration;
    }

    public void setProblematicResultsConfiguration(ProblematicResultsConfiguration problematicResultsConfiguration) {
        this.problematicResultsConfiguration = problematicResultsConfiguration;
    }

    public Boolean getDoNotSort() {
        return doNotSort;
    }

    public void setDoNotSort(Boolean doNotSort) {
        this.doNotSort = doNotSort;
    }

    public Boolean getUseOtherJobsHTTPCache() {
        return useOtherJobsHTTPCache;
    }

    public void setUseOtherJobsHTTPCache(Boolean useOtherJobsHTTPCache) {
        this.useOtherJobsHTTPCache = useOtherJobsHTTPCache;
    }

    public Boolean getDeleteHTTPCacheWhenComplete() {
        return deleteHTTPCacheWhenComplete;
    }

    public void setDeleteHTTPCacheWhenComplete(Boolean deleteHTTPCacheWhenComplete) {
        this.deleteHTTPCacheWhenComplete = deleteHTTPCacheWhenComplete;
    }

    public Integer getMaxDataLinksToFollow() {
        return maxDataLinksToFollow;
    }

    public void setMaxDataLinksToFollow(Integer maxDataLinksToFollow) {
        this.maxDataLinksToFollow = maxDataLinksToFollow;
    }

    public Integer getMaxAtomEntriesToAttempt() {
        return maxAtomEntriesToAttempt;
    }

    public void setMaxAtomEntriesToAttempt(Integer maxAtomEntriesToAttempt) {
        this.maxAtomEntriesToAttempt = maxAtomEntriesToAttempt;
    }

    public Integer getMaxAtomSectionLinksToFollow() {
        return maxAtomSectionLinksToFollow;
    }

    public void setMaxAtomSectionLinksToFollow(Integer maxAtomSectionLinksToFollow) {
        this.maxAtomSectionLinksToFollow = maxAtomSectionLinksToFollow;
    }

    public Boolean getExecuteLinkChecker() {
        return executeLinkChecker;
    }

    public void setExecuteLinkChecker(Boolean executeLinkChecker) {
        this.executeLinkChecker = executeLinkChecker;
    }

    public Boolean getSkipHarvesting() {
        return skipHarvesting;
    }

    public void setSkipHarvesting(Boolean skipHarvesting) {
        this.skipHarvesting = skipHarvesting;
    }
}
