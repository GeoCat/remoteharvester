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

package net.geocat.routes.rest;

import net.geocat.events.EventService;
import net.geocat.model.OrchestratorJobConfig;
import net.geocat.routes.queuebased.MainOrchestrator;
import org.apache.camel.BeanScope;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class StartHarvesting extends RouteBuilder {


    @Value("${geocat.jettyHost}")
    public String jettyHost;

    @Value("${geocat.jettyPort}")
    public Integer jettyPort;


    // This route takes an incoming request for starting a Harvesting job.
    //  - a uuid is setup for this harvest job
    //  - a message is put on the startHarvestRequest queue (includes the harvester config and the job uuid)
    //  - the UUID is returned to the user (for further tracking)
    //
    // NOTE: when this returns, the harvest job has NOT been started - its just been scheduled for starting.
    //       Immediately requesting a status of the job after this may result in an error (wait a few seconds)
    @Override
    public void configure() throws Exception {

        restConfiguration().component("jetty").host(jettyHost).port(jettyPort);

        JacksonDataFormat jsonDefHarvesterConfig = new JacksonDataFormat(OrchestratorJobConfig.class);

        //--- incoming start process request (HTTP)
        rest("/api/startHarvest")
                .post()
                .route()
                .unmarshal(jsonDefHarvesterConfig)  // parse the json input (in post body)
                .routeId("rest.rest.startHarvest")
                .bean(EventService.class, "validateHarvesterConfig", BeanScope.Request)  // have config validate itself
                .bean(EventService.class, "addGUID", BeanScope.Request)  // strip off header and add the process ID guid

                .multicast()
                // send event to start process
                .to(ExchangePattern.InOnly, "direct:addToQueue") // add to queue (see below)

                //return answer (guid) via http
                .setHeader("content-type", constant("application/json"))
                .bean(EventService.class, "resultJSON", BeanScope.Request)
        ;

        // mini-route to send to the message queue
        from("direct:addToQueue") // from above route
                .routeId("rest.HarvestRequestedEvent")

                .log("REST - generating HarvestRequestedEvent")
                .bean(EventService.class, "createHarvestRequestedEvent(${body},${headers.processID})", BeanScope.Request)
                .marshal().json() // convert HarvesterConfig back to json
                .to("activemq:" + MainOrchestrator.myJMSQueueName) //send to message queue
        ;
    }

}
