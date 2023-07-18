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

package net.geocat.eventprocessor.processors.main;

import net.geocat.database.linkchecker.entities.LinkCheckJobState;
import net.geocat.database.linkchecker.service.LinkCheckJobService;
import net.geocat.eventprocessor.BaseEventProcessor;
import net.geocat.events.Event;
import net.geocat.events.EventFactory;
import net.geocat.events.postprocess.AllPostProcessingCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Scope("prototype")
public class EventProcessor_AllPostProcessingCompleteEvent extends BaseEventProcessor<AllPostProcessingCompleteEvent> {

    Logger logger = LoggerFactory.getLogger(net.geocat.eventprocessor.processors.processlinks.EventProcessor_ProcessServiceDocLinksEvent.class);

    @Autowired
    LinkCheckJobService linkCheckJobService;

    @Autowired
    EventFactory eventFactory;

    @Override
    public EventProcessor_AllPostProcessingCompleteEvent externalProcessing() {
        return this;
    }


    @Override
    public EventProcessor_AllPostProcessingCompleteEvent internalProcessing() {

        linkCheckJobService.updateLinkCheckJobStateInDB(getInitiatingEvent().getLinkCheckJobId(), LinkCheckJobState.DATADOWNLOADING);

        return this;
    }


    @Override
    public List<Event> newEventProcessing() {
        logger.debug("AllPostProcessingCompleteEvent - all documents were postprocessed, linkcheckjobid=" + getInitiatingEvent().getLinkCheckJobId());
//        logger.debug("LinkCheckJob COMPLETE - "+ getInitiatingEvent().getLinkCheckJobId());

        List<Event> result = new ArrayList<>();
        Event e = eventFactory.createStartDataDownloadEvent(this.getInitiatingEvent().getLinkCheckJobId());
        result.add(e);
        return result;
    }

}

