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

import net.geocat.database.linkchecker.entities.DatasetDocumentLink;
import net.geocat.database.linkchecker.entities.LocalServiceMetadataRecord;
import net.geocat.database.linkchecker.entities.ServiceDocumentLink;
import net.geocat.database.linkchecker.entities.helper.DatasetMetadataRecord;
import net.geocat.database.linkchecker.entities.helper.LinkState;
import net.geocat.xml.helpers.OnlineResource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ServiceDocumentLinkService {


    public ServiceDocumentLink create(LocalServiceMetadataRecord localServiceMetadataRecord, OnlineResource onlineResource) {
        ServiceDocumentLink result = new ServiceDocumentLink();


        result.setLinkState(LinkState.Created);
        result.setServiceMetadataRecord(localServiceMetadataRecord);
        result.setFunction(onlineResource.getFunction());
        result.setOperationName(onlineResource.getOperationName());
        result.setRawURL(onlineResource.getRawURL());
        result.setProtocol(onlineResource.getProtocol());
        result.setApplicationProfile(onlineResource.getApplicationProfile());
        result.setLinkCheckJobId(localServiceMetadataRecord.getLinkCheckJobId());

        return result;
    }


    public DatasetDocumentLink create(DatasetMetadataRecord datasetMetadataRecord, OnlineResource onlineResource) {
        DatasetDocumentLink result = new DatasetDocumentLink();

        result.setLinkState(LinkState.Created);
        result.setDatasetMetadataRecord(datasetMetadataRecord);
        result.setFunction(onlineResource.getFunction());
        result.setOperationName(onlineResource.getOperationName());
        result.setRawURL(onlineResource.getRawURL());

        String protocolFromUrl = inferProtocolFromUrl(onlineResource.getRawURL());

        if ((onlineResource.getProtocol() == null) && (protocolFromUrl != null)) {
            // If no protocol defined, try to infer the protocol from the URL
            result.setProtocol(protocolFromUrl);
        } else {
            result.setProtocol(onlineResource.getProtocol());

            // if the XML document's protocol isn't compatible with the actual URL
            // then use the inferred URL protocol.
            // Example;
            // xml protocol is WMS (view)
            // but, url is "...?service=WFS" (inferred url protocol is download and not view)
            // then, set the protocol to Download (ignore the XML)
            if (protocolFromUrl != null) {
                boolean isDownloadProtocol = ServiceDocumentLink.validDownloadProtocols.contains(onlineResource.getProtocol().toLowerCase());
                boolean isDownloadUrlProtocol = ServiceDocumentLink.validDownloadProtocols.contains(protocolFromUrl.toLowerCase());
                boolean isViewProtocol = ServiceDocumentLink.validViewProtocols.contains(onlineResource.getProtocol().toLowerCase());
                boolean isViewUrlProtocol = ServiceDocumentLink.validViewProtocols.contains(protocolFromUrl);

                if (isDownloadProtocol) {
                   if (!isDownloadUrlProtocol && isViewUrlProtocol) {
                       result.setProtocol(protocolFromUrl);
                   }
                } else if (isViewProtocol) {
                    if (!isViewUrlProtocol && isDownloadUrlProtocol) {
                        result.setProtocol(protocolFromUrl);
                    }
                }
            }
        }

        result.setApplicationProfile(onlineResource.getApplicationProfile());

        result.setLinkCheckJobId(datasetMetadataRecord.getLinkCheckJobId());

        return result;
    }


    private String inferProtocolFromUrl(String url) {
        String normalizedUrl = url.toLowerCase();

        if (normalizedUrl.indexOf("wms") > -1) {
            return "wms";
        } else if (normalizedUrl.indexOf("wmts") > -1) {
            return "wmts";
        } else if (normalizedUrl.indexOf("wfs") > -1) {
            return "wfs";
        } else if (normalizedUrl.indexOf("atom") > -1) {
            return "atom";
        } else if (normalizedUrl.indexOf("wcs") > -1) {
            return "wcs";
        } else if (normalizedUrl.indexOf("sos") > -1) {
            return "sos";
        } else if (normalizedUrl.indexOf("api features") > -1) {
            return "api features";
        } else if (normalizedUrl.indexOf("sensorthings") > -1) {
            return "sensorthings";
        }

        return null;
    }
}
