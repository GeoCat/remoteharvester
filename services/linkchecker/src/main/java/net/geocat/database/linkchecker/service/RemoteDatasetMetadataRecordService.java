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

package net.geocat.database.linkchecker.service;

import net.geocat.database.linkchecker.entities.LocalDatasetMetadataRecord;
import net.geocat.database.linkchecker.entities.helper.ServiceMetadataDocumentState;
import net.geocat.xml.XmlDatasetMetadataDocument;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class RemoteDatasetMetadataRecordService {
//
//    public OperatesOnRemoteDatasetMetadataRecord createRemoteDatasetMetadataRecord(OperatesOnLink link) {
//        OperatesOnRemoteDatasetMetadataRecord result = new OperatesOnRemoteDatasetMetadataRecord();
//        return result;
//    }

    public LocalDatasetMetadataRecord createLocalDatasetMetadataRecord(XmlDatasetMetadataDocument doc,
                                                                       Long underlyingHarvestMetadataRecordId,
                                                                       String linkCheckJobId,
                                                                       String sha2) {
        LocalDatasetMetadataRecord result = new LocalDatasetMetadataRecord();

        result.setTitle(doc.getTitle());


        result.setHarvesterMetadataRecordId(underlyingHarvestMetadataRecordId);
        result.setLinkCheckJobId(linkCheckJobId);
        result.setSha2(sha2);

        //metadataDocumentFactory.augment(result,doc);

        result.setFileIdentifier(doc.getFileIdentifier());
        result.setParentIdentifier(doc.getParentIdentifier());
        result.setMetadataRecordType(doc.getMetadataDocumentType()); // ds

        result.setState(ServiceMetadataDocumentState.CREATED);
        return result;
    }


//    public CapabilitiesRemoteDatasetMetadataDocument createCapabilitiesRemoteDatasetMetadataDocument(CapabilitiesDatasetMetadataLink link,String jobid) {
//
//        CapabilitiesRemoteDatasetMetadataDocument result = new CapabilitiesRemoteDatasetMetadataDocument();
//        result.setLinkCheckJobId(jobid);
//        return result;
//    }

    public LocalDatasetMetadataRecord createLocalServiceMetadataRecord(XmlDatasetMetadataDocument doc, Long underlyingHarvestMetadataRecordId, String linkCheckJobId, String sha2) {
        LocalDatasetMetadataRecord result = createLocalDatasetMetadataRecord(doc, underlyingHarvestMetadataRecordId, linkCheckJobId, sha2);
        result.setHarvesterMetadataRecordId(underlyingHarvestMetadataRecordId);
        result.setLinkCheckJobId(linkCheckJobId);
        result.setSha2(sha2);


        result.setFileIdentifier(doc.getFileIdentifier());
        result.setMetadataRecordType(doc.getMetadataDocumentType()); // dataset

        result.setState(ServiceMetadataDocumentState.CREATED);

        return result;
    }
}
