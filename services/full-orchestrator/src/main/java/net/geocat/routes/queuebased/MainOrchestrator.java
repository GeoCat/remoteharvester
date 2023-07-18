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

package net.geocat.routes.queuebased;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.geocat.eventprocessor.MainLoopRouteCreator;
import net.geocat.events.CheckProcessEvent;
import net.geocat.events.Event;
import net.geocat.events.OrchestratedHarvestAbortEvent;
import net.geocat.events.OrchestratedHarvestRequestedEvent;
import net.geocat.service.DatabaseUpdateService;
import net.geocat.service.OrchestratedHarvestProcessService;
import org.apache.camel.BeanScope;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MainOrchestrator extends SpringRouteBuilder {

    public static String myJMSQueueName = "Orchestrator.MainOrchestrator";
    @Autowired
    MainLoopRouteCreator mainLoopRouteCreator;

    @Override
    public void configure() throws Exception {


        mainLoopRouteCreator.createEventProcessingLoop(this,
                "activemq:" + myJMSQueueName,
                new Class[]{OrchestratedHarvestAbortEvent.class, OrchestratedHarvestRequestedEvent.class, CheckProcessEvent.class},
                Arrays.asList(

                ),
                Arrays.asList(new Class[0]),
                2
        );

        from("activemq:ActiveMQ.DLQ")
                .routeId("MainOrchestrator.DLQ")
                .onException(Exception.class).onExceptionOccurred(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Exception ex = exchange.getException();
                        exchange.getMessage().setHeader("exception", ex);
                        exchange.getMessage().setHeader("exceptionTxt", DatabaseUpdateService.convertToString(ex));
                    }
                })
                .handled(true)
                .to("activemq:ActiveMQ.DLQ_DLQ")
                .end()

                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String json = new String((byte[]) exchange.getMessage().getBody());
                        ObjectMapper mapper = new ObjectMapper();
                        Event event = mapper.readValue(json, Event.class);
                        exchange.getMessage().setHeader("processID", event.getProcessID());
                        int t = 0;
                    }
                })
                .bean(OrchestratedHarvestProcessService.class, "updateLinkCheckJobStateInDBToError( ${header.processID} )", BeanScope.Request)
        ;
    }
}
