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

package net.geocat.eventprocessor.processors.findlinks;


import net.geocat.database.harvester.repos.MetadataRecordRepo;
import net.geocat.database.linkchecker.entities.*;
import net.geocat.database.linkchecker.entities.helper.ServiceMetadataDocumentState;
import net.geocat.database.linkchecker.repos.LocalDatasetMetadataRecordRepo;
import net.geocat.database.linkchecker.repos.LocalNotProcessedMetadataRecordRepo;
import net.geocat.database.linkchecker.repos.LocalServiceMetadataRecordRepo;
import net.geocat.database.linkchecker.service.MetadataDocumentFactory;
import net.geocat.database.linkchecker.service.MetadataDocumentService;
import net.geocat.eventprocessor.BaseEventProcessor;
import net.geocat.events.Event;
import net.geocat.events.EventFactory;
import net.geocat.events.findlinks.LinksFoundInAllDocuments;
import net.geocat.events.findlinks.ProcessLocalMetadataDocumentEvent;
import net.geocat.service.BlobStorageService;
import net.geocat.service.ServiceDocLinkExtractor;
import net.geocat.service.helper.ShouldTransitionOutOfLinkFinding;
import net.geocat.xml.XmlDatasetMetadataDocument;
import net.geocat.xml.XmlDocumentFactory;
import net.geocat.xml.XmlMetadataDocument;
import net.geocat.xml.XmlServiceRecordDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Scope("prototype")
public class EventProcessor_ProcessLocalMetadataDocumentEvent extends BaseEventProcessor<ProcessLocalMetadataDocumentEvent> {

    Logger logger = LoggerFactory.getLogger(ProcessLocalMetadataDocumentEvent.class);


    @Autowired
    EventFactory eventFactory;

//    @Autowired
//    LinkFactory linkFactory;

    @Autowired
    BlobStorageService blobStorageService;

    @Autowired
    MetadataDocumentService metadataDocumentService;
//
//    @Autowired
//    MetadataDocumentRepo metadataDocumentRepo;

    @Autowired
    XmlDocumentFactory xmlDocumentFactory;

    @Autowired
    ServiceDocLinkExtractor serviceDocLinkExtractor;

    @Autowired
    MetadataRecordRepo metadataRecordRepo;

    @Autowired
    MetadataDocumentFactory metadataDocumentFactory;

    @Autowired
    LocalDatasetMetadataRecordRepo localDatasetMetadataRecordRepo;

    @Autowired
    LocalServiceMetadataRecordRepo localServiceMetadataRecordRepo;

    @Autowired
    LocalNotProcessedMetadataRecordRepo localNotProcessedMetadataRecordRepo;

    @Autowired
    ShouldTransitionOutOfLinkFinding shouldTransitionOutOfLinkFinding;

    String xml;
    XmlMetadataDocument doc;
    net.geocat.database.linkchecker.entities.helper.MetadataRecord metadataRecord;

    @Override
    public EventProcessor_ProcessLocalMetadataDocumentEvent externalProcessing() throws Exception {
        String sha2 = getInitiatingEvent().getSha2();
        // long endpointJobId = getInitiatingEvent().getEndpointJobId();
//        if (true)
//         throw new Exception("blah");
        xml = blobStorageService.findXML(sha2);
        doc = (XmlMetadataDocument) xmlDocumentFactory.create(xml);
        return this;
    }


    @Override
    public EventProcessor_ProcessLocalMetadataDocumentEvent internalProcessing() throws Exception {
        String sha2 = getInitiatingEvent().getSha2();
        //  String harvestJobId = getInitiatingEvent().getHarvestJobId();
        String linkCheckJob = getInitiatingEvent().getLinkCheckJobId();
        Long underlyingHarvestMetadataRecordId = getInitiatingEvent().getUnderlyingHarvestMetadataRecordId();

        metadataRecord = getDoc(sha2, underlyingHarvestMetadataRecordId, linkCheckJob);

        if (metadataRecord instanceof LocalServiceMetadataRecord) {
            handleService((LocalServiceMetadataRecord) metadataRecord);
        } else if (metadataRecord instanceof LocalDatasetMetadataRecord) {
            handleDataset((LocalDatasetMetadataRecord) metadataRecord);
        } else if (metadataRecord instanceof LocalNotProcessedMetadataRecord) {
            handleWillNotProcess((LocalNotProcessedMetadataRecord) metadataRecord);
        } else {
            int t = 0;
        }

        return this;
    }

    private void handleWillNotProcess(LocalNotProcessedMetadataRecord metadataRecord) {

        metadataDocumentService.setState(metadataRecord, ServiceMetadataDocumentState.NOT_APPLICABLE);
        logger.debug("not a processable record type:" + metadataRecord.getMetadataRecordType() + ", fileIdentifier:" + metadataRecord.getFileIdentifier());
    }

    private void handleService(LocalServiceMetadataRecord metadataDocument) {
        String sha2 = getInitiatingEvent().getSha2();


        XmlMetadataDocument xmlMetadataDocument = (XmlMetadataDocument) doc;
        XmlServiceRecordDoc xmlServiceRecordDoc = (XmlServiceRecordDoc) doc;
        String serviceType = metadataDocument.getMetadataServiceType();

        if (serviceType == null) {
            metadataDocument.setState(ServiceMetadataDocumentState.NOT_APPLICABLE);
            localServiceMetadataRecordRepo.updateState(metadataDocument.getServiceMetadataDocumentId(), ServiceMetadataDocumentState.NOT_APPLICABLE);
            //  metadataDocumentService.setState(metadataDocument , ServiceMetadataDocumentState.NOT_APPLICABLE);
            logger.debug("service record has no service type - ignored, fileIdentifier:" + xmlMetadataDocument.getFileIdentifier());
            return;
        }

        if (!serviceType.equalsIgnoreCase("view")
                && !serviceType.equalsIgnoreCase("download")
                && !serviceType.equalsIgnoreCase("discovery")) {
            metadataDocument.setState(ServiceMetadataDocumentState.NOT_APPLICABLE);
            localServiceMetadataRecordRepo.updateState(metadataDocument.getServiceMetadataDocumentId(), ServiceMetadataDocumentState.NOT_APPLICABLE);
            logger.debug("service record not an appropriate type - ignored, fileIdentifier:" + xmlMetadataDocument.getFileIdentifier() + ", type:" + serviceType);
            return;
        }

        Set<ServiceDocumentLink> serviceLinks = metadataDocument.getServiceDocumentLinks();
        Set<OperatesOnLink> operatesOnsLinks = metadataDocument.getOperatesOnLinks();

        metadataDocument.setState(ServiceMetadataDocumentState.LINKS_EXTRACTED);
        localServiceMetadataRecordRepo.updateState(metadataDocument.getServiceMetadataDocumentId(), ServiceMetadataDocumentState.LINKS_EXTRACTED);
        // metadataDocumentService.setState(metadataDocument , ServiceMetadataDocumentState.LINKS_EXTRACTED);
    }

    private void handleDataset(LocalDatasetMetadataRecord metadataRecord) {
        // metadataRecord.setState(metadataRecord , ServiceMetadataDocumentState.LINKS_EXTRACTED);
        metadataRecord.setState(ServiceMetadataDocumentState.LINKS_EXTRACTED);
        localDatasetMetadataRecordRepo.updateState(metadataRecord.getDatasetMetadataDocumentId(), ServiceMetadataDocumentState.LINKS_EXTRACTED);
    }


    public net.geocat.database.linkchecker.entities.helper.MetadataRecord getDoc(String sha2, Long underlyingHarvestMetadataRecordId, String linkCheckJobId)
            throws Exception {

        if (doc == null)
            return null;
//        List<MetadataRecord> metadataRecord = metadataRecordRepo.findBySha2(sha2);
//        if (metadataRecord.isEmpty())
//            return null;
//        String xml = blobStorageService.findXML(sha2);
//
//
//        XmlDoc doc = xmlDocumentFactory.create(xml);

        if (doc instanceof XmlServiceRecordDoc) {
            XmlServiceRecordDoc xmlServiceRecordDoc = (XmlServiceRecordDoc) doc;
            LocalServiceMetadataRecord localServiceMetadataRecord =
                    metadataDocumentFactory.createLocalServiceMetadataRecord(xmlServiceRecordDoc, underlyingHarvestMetadataRecordId, linkCheckJobId, sha2);
            LocalServiceMetadataRecord existing = localServiceMetadataRecordRepo.findFirstByLinkCheckJobIdAndSha2(linkCheckJobId, sha2);
            if (existing != null)
                return existing;
            else
                return localServiceMetadataRecordRepo.save(localServiceMetadataRecord);
        } else if (doc instanceof XmlDatasetMetadataDocument) {
            XmlDatasetMetadataDocument xmlDatasetMetadataDocument = (XmlDatasetMetadataDocument) doc;
            LocalDatasetMetadataRecord localDatasetMetadataRecord =
                    metadataDocumentFactory.createLocalDatasetMetadataRecord(xmlDatasetMetadataDocument, underlyingHarvestMetadataRecordId, linkCheckJobId, sha2);

            LocalDatasetMetadataRecord existing = localDatasetMetadataRecordRepo.findFirstByLinkCheckJobIdAndSha2(linkCheckJobId, sha2);
            if (existing == null)
                return localDatasetMetadataRecordRepo.save(localDatasetMetadataRecord);
            else
                return existing;
        } else if (doc instanceof XmlMetadataDocument) {
            XmlMetadataDocument xmlMetadataDocument = (XmlMetadataDocument) doc;
            LocalNotProcessedMetadataRecord localNotProcessedMetadataRecord =
                    metadataDocumentFactory.createLocalNotProcessedMetadataRecord(xmlMetadataDocument, underlyingHarvestMetadataRecordId, linkCheckJobId, sha2);

            LocalNotProcessedMetadataRecord existing = localNotProcessedMetadataRecordRepo.findFirstByLinkCheckJobIdAndSha2(linkCheckJobId, sha2);
            if (existing == null)
                return localNotProcessedMetadataRecordRepo.save(localNotProcessedMetadataRecord);
            else
                return existing;
        } else {
            return null; //should never happen
        }

    }

    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        if (doc instanceof XmlServiceRecordDoc)
            logger.debug("Parsed SERVICE Metadata Record with fileIdentifier:" + doc.getFileIdentifier() + ", title=" + doc.getTitle());
        else
            logger.debug("Parsed DATASET Metadata Record with fileIdentifier:" + doc.getFileIdentifier() + ", title=" + doc.getTitle());

        String linkCheckJob = getInitiatingEvent().getLinkCheckJobId();

//        if (metadataDocumentService.completeLinkExtract(linkCheckJob)) {
//            LinksFoundInAllDocuments e = eventFactory.createLinksFoundInAllDocuments(initiatingEvent);
//            result.add(e);
//        }
        if (shouldTransitionOutOfLinkFinding.shouldSendMessage(linkCheckJob, getInitiatingEvent().getUnderlyingHarvestMetadataRecordId())) {
            LinksFoundInAllDocuments e = eventFactory.createLinksFoundInAllDocuments(initiatingEvent);
            result.add(e);
        }

        return result;
    }
}
