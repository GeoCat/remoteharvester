package com.geocat.ingester.model.metadata;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "HarvesterSettings")
public class HarvesterSetting {
    private static final String ID_SEQ_NAME = "harvester_setting_id_seq";

    @Id
    @SequenceGenerator(name = ID_SEQ_NAME, initialValue = 100, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQ_NAME)
    private int id;
    @OneToOne(optional = true, fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "parentid")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HarvesterSetting parent;
    private String name;
    private String value;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HarvesterSetting getParent() {
        return parent;
    }

    public void setParent(HarvesterSetting parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
