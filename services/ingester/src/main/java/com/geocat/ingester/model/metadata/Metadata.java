/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package com.geocat.ingester.model.metadata;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "Metadata")
@Data
public class Metadata implements Serializable {
    private static final String ID_SEQ_NAME = "metadata_id_seq";

    @Id
    @SequenceGenerator(name = ID_SEQ_NAME, initialValue = 100, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQ_NAME)
    private int id;
    @Column(nullable = false, unique = true)
    private String uuid;
    private String data;
    private String changeDate;
    private String createDate;
    private int popularity = -1;
    private int rating = -1;
    private String schemaId = "iso19139";
    private String isTemplate = "n";
    private String isHarvested = "y";
    private String harvestUuid;
    private String source;
    private int owner;
    private int groupOwner;

    @OneToMany(mappedBy = "id.metadataId", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<OperationAllowed> privileges = new HashSet<OperationAllowed>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "metadata_id", nullable = false)
    private Set<MetadataIndicator> indicators = new HashSet<MetadataIndicator>();

    public Metadata() {
        super();
    }
}
