package com.geocat.ingester.model.harvester;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

// create table harvest_job (long_term_tag text, job_id varchar(40), state varchar(40), look_for_nested_discovery_service bool, filter text, initial_url text, messages text);
@Entity
@Table(name = "harvest_job")
public class HarvestJob {
    @Column(name = "initial_url")
    public String initialUrl;
    @Column(name = "nrecords_per_request")
    int nrecordsPerRequest;
    @Column(name = "get_record_queue_hint")
    String getRecordQueueHint;
    @Column(name = "create_time_utc", columnDefinition = "timestamp with time zone")
    ZonedDateTime createTimeUTC;
    @Column(name = "last_update_utc", columnDefinition = "timestamp with time zone")
    ZonedDateTime lastUpdateUTC;
    @Id
    @Column(name = "job_id", columnDefinition = "varchar(40)")
    private String jobId;
    @Column(name = "message", columnDefinition = "text")
    private String messages;
    @Column(name = "long_term_tag")
    private String longTermTag;
    @Column(name = "look_for_nested_discovery_service")
    private boolean lookForNestedDiscoveryService;
    @Column(columnDefinition = "text")
    private String filter;
    @Enumerated(EnumType.STRING)
    private HarvestJobState state;
    @Column(name = "problematic_results_configuration_json", columnDefinition = "text")
    private String problematicResultsConfigurationJSON;

    @PrePersist
    private void onInsert() {
        this.createTimeUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        this.lastUpdateUTC = this.createTimeUTC;
    }

    @PreUpdate
    private void onUpdate() {
        this.lastUpdateUTC = ZonedDateTime.now(ZoneId.of("UTC"));
    }

    public ZonedDateTime getCreateTimeUTC() {
        return createTimeUTC;
    }

    public ZonedDateTime getLastUpdateUTC() {
        return lastUpdateUTC;
    }

    public String getGetRecordQueueHint() {
        return getRecordQueueHint;
    }

    public void setGetRecordQueueHint(String getRecordQueueHint) {
        this.getRecordQueueHint = getRecordQueueHint;
    }


    public String getProblematicResultsConfigurationJSON() {
        return problematicResultsConfigurationJSON;
    }

    public void setProblematicResultsConfigurationJSON(String problematicResultsConfigurationJSON) {
        this.problematicResultsConfigurationJSON = problematicResultsConfigurationJSON;
    }


    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public String getInitialUrl() {
        return initialUrl;
    }

    public void setInitialUrl(String initalUrl) {
        this.initialUrl = initalUrl;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
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

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public HarvestJobState getState() {
        return state;
    }

    public void setState(HarvestJobState state) {
        this.state = state;
    }

    public int getNrecordsPerRequest() {
        return nrecordsPerRequest;
    }

    public void setNrecordsPerRequest(int nrecordsPerRequest) {
        this.nrecordsPerRequest = nrecordsPerRequest;
    }


}
