package com.geocat.ingester.model.harvester;


import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "metadata_record",
        indexes = {
                @Index(
                        name = "metadata_record_endpointJobId_idx",
                        columnList = "endpoint_job_id",
                        unique = false
                )
        })
public class MetadataRecord {
    @Column(columnDefinition = "timestamp with time zone", name = "create_time_utc")
    ZonedDateTime createTimeUTC;
    @Id
    // @Column(columnDefinition = "varchar(40)")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metadata_record_id")
    private long metadataRecordId;
    // @Column(columnDefinition = "varchar(40)")
    @Column(name = "endpoint_job_id")
    private long endpointJobId;
    @Column(name = "record_number")
    private int recordNumber;
    @Column(columnDefinition = "varchar(64)")
    private String sha2;
    @Column(columnDefinition = "text", name = "record_identifier")
    private String recordIdentifier;

    @PrePersist
    private void onInsert() {
        this.createTimeUTC = ZonedDateTime.now(ZoneId.of("UTC"));
    }


    public ZonedDateTime getCreateTimeUTC() {
        return createTimeUTC;
    }


    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }


    public long getMetadataRecordId() {
        return metadataRecordId;
    }

    public void setMetadataRecordId(long metadataRecordId) {
        this.metadataRecordId = metadataRecordId;
    }

    public long getEndpointJobId() {
        return endpointJobId;
    }

    public void setEndpointJobId(long endpointJobId) {
        this.endpointJobId = endpointJobId;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(int recordNumber) {
        this.recordNumber = recordNumber;
    }

    public String getSha2() {
        return sha2;
    }

    public void setSha2(String sha2) {
        this.sha2 = sha2;
    }
}
