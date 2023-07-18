/*
 *  =============================================================================
 *  ===  Copyright (C) 2021 Food and Agriculture Organization of the
 *  ===  United Nations (FAO-UN), United Nations World Food Programme (WFP)
 *  ===  and United Nations Environment Programme (UNEP)
 *  ===
 *  ===  This program is free software; you can redistribute it and/or modify
 *  ===  it under the terms of the GNU General Public License as published by
 *  ===  the Free Software Foundation; either version 2 of the License, or (at
 *  ===  your option) any later version.
 *  ===
 *  ===  This program is distributed in the hope that it will be useful, but
 *  ===  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  ===  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  ===  General Public License for more details.
 *  ===
 *  ===  You should have received a copy of the GNU General Public License
 *  ===  along with this program; if not, write to the Free Software
 *  ===  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *  ===
 *  ===  Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 *  ===  Rome - Italy. email: geonetwork@osgeo.org
 *  ===
 *  ===  Development of this program was financed by the European Union within
 *  ===  Service Contract NUMBER – 941143 – IPR – 2021 with subject matter
 *  ===  "Facilitating a sustainable evolution and maintenance of the INSPIRE
 *  ===  Geoportal", performed in the period 2021-2023.
 *  ===
 *  ===  Contact: JRC Unit B.6 Digital Economy, Via Enrico Fermi 2749,
 *  ===  21027 Ispra, Italy. email: JRC-INSPIRE-SUPPORT@ec.europa.eu
 *  ==============================================================================
 */

package net.geocat.database.linkchecker.entities;

import javax.persistence.*;

@Entity
@Table(
        indexes = {
                @Index(
                        name = "inspireSpatialDatasetIdentifier_code_idx",
                        columnList = "code",
                        unique = false
                ),
                @Index(
                        name = "inspireSpatialDatasetIdentifier_ap_idx",
                        columnList = "cap_sha2,cap_jobid",
                        unique = false
                )
        }
)
public class InspireSpatialDatasetIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long InspireSpatialDatasetIdentifierId;

    @Column(columnDefinition = "text")
    private String metadataURL;

    @Column(columnDefinition = "text")
    private String code;

    @Column(columnDefinition = "text")
    private String namespace;

    //    //which capabilities record does this link belong to?
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns(
            {
                    @JoinColumn(name = "cap_sha2", referencedColumnName = "sha2"),
                    @JoinColumn(name = "cap_jobId", referencedColumnName = "linkcheckjobid")
            }
    )
    private CapabilitiesDocument capabilitiesDocument;

    public InspireSpatialDatasetIdentifier() {

    }


    public InspireSpatialDatasetIdentifier(String metadataURL, String code, String namespace) {
        this.metadataURL = metadataURL;
        this.code = code;
        this.namespace = namespace;
    }

    public CapabilitiesDocument getCapabilitiesDocument() {
        return capabilitiesDocument;
    }

    public void setCapabilitiesDocument(CapabilitiesDocument capabilitiesDocument) {
        this.capabilitiesDocument = capabilitiesDocument;
    }

    public String getMetadataURL() {
        return metadataURL;
    }

    public void setMetadataURL(String metadataURL) {
        this.metadataURL = metadataURL;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public long getInspireSpatialDatasetIdentifierId() {
        return InspireSpatialDatasetIdentifierId;
    }

    public void setInspireSpatialDatasetIdentifierId(long inspireSpatialDatasetIdentifierId) {
        InspireSpatialDatasetIdentifierId = inspireSpatialDatasetIdentifierId;
    }

//    public CapabilitiesDocument getCapabilitiesDocument() {
//        return capabilitiesDocument;
//    }
//
//    public void setCapabilitiesDocument(CapabilitiesDocument capabilitiesDocument) {
//        this.capabilitiesDocument = capabilitiesDocument;
//    }

    @Override
    public String toString() {
        return "InspireSpatialDatasetIdentifier: " +
                "metadataURL='" + metadataURL + '\'' +
                ", code='" + code + '\'' +
                ", namespace='" + namespace + '\'' +
                ' ';
    }
}
