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

package net.geocat.eventprocessor.processors.processlinks;

import net.geocat.database.linkchecker.entities.DatasetDocumentLink;
import net.geocat.database.linkchecker.entities.LocalDatasetMetadataRecord;
import net.geocat.database.linkchecker.entities.helper.ServiceMetadataDocumentState;
import net.geocat.database.linkchecker.repos.CapabilitiesDocumentRepo;
import net.geocat.database.linkchecker.repos.LocalDatasetMetadataRecordRepo;
import net.geocat.eventprocessor.BaseEventProcessor;
import net.geocat.eventprocessor.processors.processlinks.postprocessing.DatasetToLayerIndicators;
import net.geocat.events.Event;
import net.geocat.events.EventFactory;
import net.geocat.events.processlinks.ProcessDatasetDocLinksEvent;
import net.geocat.service.*;
import net.geocat.service.helper.ShouldTransitionOutOfLinkProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static net.geocat.database.linkchecker.service.DatabaseUpdateService.convertToString;

@Component
@Scope("prototype")
public class EventProcessor_ProcessDatasetDocLinksEvent extends BaseEventProcessor<ProcessDatasetDocLinksEvent> {

    Logger logger = LoggerFactory.getLogger(EventProcessor_ProcessServiceDocLinksEvent.class);

    @Autowired
    DocumentLinkToCapabilitiesProcessor documentLinkToCapabilitiesProcessor;

    @Autowired
    LocalDatasetMetadataRecordRepo localDatasetMetadataRecordRepo;

    @Autowired
    MetadataService metadataService;

    @Autowired
    EventFactory eventFactory;

    @Autowired
    CapabilitiesDocumentRepo capabilitiesDocumentRepo;

    @Autowired
    RetrieveCapabilitiesDatasetMetadataLink retrieveCapabilitiesDatasetMetadataLink;

    @Autowired
    RemoteServiceMetadataRecordLinkRetriever remoteServiceMetadataRecordLinkRetriever;

    @Autowired
    RetrieveServiceDocumentLink retrieveServiceDocumentLink;
//
//    @Autowired
//    CapabilitiesResolvesIndicators capabilitiesResolvesIndicators;

    @Autowired
    DatasetToLayerIndicators datasetToLayerIndicators;

    @Autowired
    ShouldTransitionOutOfLinkProcessing shouldTransitionOutOfLinkProcessing;

    LocalDatasetMetadataRecord localDatasetMetadataRecord;


    @Override
    public EventProcessor_ProcessDatasetDocLinksEvent internalProcessing() throws Exception {

        localDatasetMetadataRecord = localDatasetMetadataRecordRepo.findById(getInitiatingEvent().getDatasetDocumentId()).get();// make sure we re-load
        // Dataset metadata record uses FetchMode.JOIN with DatasetIdentifier and DatasetDocumentLink
        // If the metadata has more than 1 dataset identifier retrieves DatasetDocumentLink duplicated
        // TODO: Check if possible to use instead FetchMode.SUBSELECT
        List<DatasetDocumentLink> uniqueDatasetDocumentLinks = localDatasetMetadataRecord.getDocumentLinks().stream()
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(DatasetDocumentLink::getDatasetMetadataLinkId))),
                        ArrayList::new));

        localDatasetMetadataRecord.setDocumentLinks(uniqueDatasetDocumentLinks);

        if (localDatasetMetadataRecord.getState() == ServiceMetadataDocumentState.NOT_APPLICABLE)
            return this; //nothing to do

        try {
            int nlinksCap = localDatasetMetadataRecord.getDocumentLinks().size();
            logger.debug("processing links DATASET documentid=" + getInitiatingEvent().getDatasetDocumentId() + ", with fileID=" + localDatasetMetadataRecord.getFileIdentifier() + " that has " + nlinksCap + " document links");

            documentLinkToCapabilitiesProcessor.processDocumentLinks(localDatasetMetadataRecord);
            localDatasetMetadataRecord.setState(ServiceMetadataDocumentState.LINKS_PROCESSED);

            save();
            logger.trace("finished  processing links for dataset documentid=" + getInitiatingEvent().getDatasetDocumentId());

        } catch (Exception e) {
            logger.error("exception for datasetMetadataRecordId=" + getInitiatingEvent().getDatasetDocumentId(), e);
            localDatasetMetadataRecord.setState(ServiceMetadataDocumentState.ERROR);
            localDatasetMetadataRecord.setErrorMessage(convertToString(e));
            save();
        }


        return this;
    }


    public void save() {
        localDatasetMetadataRecord = localDatasetMetadataRecordRepo.save(localDatasetMetadataRecord);
    }


    @Override
    public EventProcessor_ProcessDatasetDocLinksEvent externalProcessing() throws Exception {
        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        List<Event> result = new ArrayList<>();
        String linkCheckJobId = getInitiatingEvent().getLinkCheckJobId();

        if (shouldTransitionOutOfLinkProcessing.shouldSendMessage(linkCheckJobId, getInitiatingEvent().getDatasetDocumentId())) {
            //done
            Event e = eventFactory.createAllLinksCheckedEvent(linkCheckJobId);
            result.add(e);
        }
        return result;
    }


}
