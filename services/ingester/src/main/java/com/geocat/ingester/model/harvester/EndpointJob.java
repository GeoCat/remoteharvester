package com.geocat.ingester.model.harvester;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;


//create table endpoint_job ( endpoint_job_id varchar(40), harvest_job_id varchar(40), state varchar(40), url varchar(255), url_get_records varchar(255), expected_number_of_records int, filter text,look_for_nested_discovery_service bool);
@Entity
@Table(name = "endpoint_job",
        indexes = {
                @Index(
                        name = "harvestJobId_idx",
                        columnList = "harvest_job_id",
                        unique = false
                )
        })

public class EndpointJob {
    @Column(columnDefinition = "timestamp with time zone", name = "create_time_utc")
    ZonedDateTime createTimeUTC;
    @Column(columnDefinition = "timestamp with time zone", name = "last_update_utc")
    ZonedDateTime lastUpdateUTC;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "endpoint_job_id")
    private long endpointJobId;
    @Column(columnDefinition = "varchar(40)", name = "harvest_job_id")
    private String harvestJobId;
    private String url;
    @Column(name = "look_for_nested_discovery_service")
    private boolean lookForNestedDiscoveryService;
    @Column(columnDefinition = "text")
    private String filter;
    @Column(name = "expected_number_of_records")
    private Integer expectedNumberOfRecords;
    @Column(name = "url_get_records")
    private String urlGetRecords;
    @Enumerated(EnumType.STRING)
    private EndpointJobState state;

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

    public Integer getExpectedNumberOfRecords() {
        return expectedNumberOfRecords;
    }

    public void setExpectedNumberOfRecords(int expectedNumberOfRecords) {
        this.expectedNumberOfRecords = expectedNumberOfRecords;
    }

    public String getUrlGetRecords() {
        return urlGetRecords;
    }

    public void setUrlGetRecords(String urlGetRecords) {
        this.urlGetRecords = urlGetRecords;
    }

    public EndpointJobState getState() {
        return state;
    }

    public void setState(EndpointJobState state) {
        this.state = state;
    }

    public long getEndpointJobId() {
        return endpointJobId;
    }

    public void setEndpointJobId(long endpointJobId) {
        this.endpointJobId = endpointJobId;
    }

    public String getHarvestJobId() {
        return harvestJobId;
    }

    public void setHarvestJobId(String harvestJobId) {
        this.harvestJobId = harvestJobId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
