package com.geocat.ingester.model.harvester;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "blob_storage")
public class BlobStorage {
    @Id
    @Column(columnDefinition = "varchar(64)")
    private String sha2;

    @Column(columnDefinition = "text", name = "text_value")
    private String textValue;

    public String getSha2() {
        return sha2;
    }

    public void setSha2(String sha2) {
        this.sha2 = sha2;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }
}
