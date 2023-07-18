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

package net.geocat.events;

import net.geocat.database.orchestrator.repos.OrchestratedHarvestProcessRepo;
import net.geocat.model.OrchestratorJobConfig;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Scope("prototype")
public class EventService {

    @Autowired
    OrchestratedHarvestProcessRepo orchestratedHarvestProcessRepo;

    //creates a new GUID
    public String createGUID() {
        UUID guid = java.util.UUID.randomUUID();
        return guid.toString();
    }

    /**
     * remove all headers from the request
     * add processID=GUID  to be used for this harvest
     */
    public void addGUID(Message message) {
        message.getHeaders().clear();
        String guid = createGUID();
        message.getHeaders().put("processID", guid);
        message.getHeaders().put("JMSCorrelationID", guid);
        ((OrchestratorJobConfig) message.getBody()).setProcessID(guid);
    }


    //we are doing trivial JSON conversion
    //   take the processID from the header, and return it as a json string like;
    //{
    //     "processID":"5fcd5f22-1a40-4712-8d2d-ca88c2d0d472"
    //}
    public void resultJSON(Message message) {
        String uuid = ((OrchestratorJobConfig) message.getBody()).getProcessID();
        message.setBody("{\n     \"processID\":\"" + uuid + "\"\n}\n");
    }


    //calls validate on the (parsed) input message
    public void validateHarvesterConfig(Message message) throws Exception {
        ((OrchestratorJobConfig) message.getBody()).asHarvesterConfig().validate();
    }


    public OrchestratedHarvestRequestedEvent createHarvestRequestedEvent(OrchestratorJobConfig orchestratorJobConfig, String processID) {
//        Optional<OrchestratedHarvestProcess> harvestJob = orchestratedHarvestProcessRepo.findById(processID);

        OrchestratedHarvestRequestedEvent result = new OrchestratedHarvestRequestedEvent(processID, orchestratorJobConfig);
        return result;
    }

}
