/*
 * =============================================================================
 * ===	Copyright (C) 2019 Food and Agriculture Organization of the
 * ===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * ===	and United Nations Environment Programme (UNEP)
 * ===
 * ===	This program is free software; you can redistribute it and/or modify
 * ===	it under the terms of the GNU General Public License as published by
 * ===	the Free Software Foundation; either version 2 of the License, or (at
 * ===	your option) any later version.
 * ===
 * ===	This program is distributed in the hope that it will be useful, but
 * ===	WITHOUT ANY WARRANTY; without even the implied warranty of
 * ===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * ===	General Public License for more details.
 * ===
 * ===	You should have received a copy of the GNU General Public License
 * ===	along with this program; if not, write to the Free Software
 * ===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * ===
 * ===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * ===	Rome - Italy. email: geonetwork@osgeo.org
 * ===
 * ===  Development of this program was financed by the European Union within
 * ===  Service Contract NUMBER – 941143 – IPR – 2021 with subject matter
 * ===  "Facilitating a sustainable evolution and maintenance of the INSPIRE
 * ===  Geoportal", performed in the period 2021-2023.
 * ===
 * ===  Contact: JRC Unit B.6 Digital Economy, Via Enrico Fermi 2749,
 * ===  21027 Ispra, Italy. email: JRC-INSPIRE-SUPPORT@ec.europa.eu
 * ==============================================================================
 */
package geocat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

@SpringBootApplication()
public class MySpringApp {

    @Autowired
    DataSource dataSource;


    public static void main(String[] args) throws Exception {

        Logger logger = LoggerFactory.getLogger(MySpringApp.class);
//        logger.debug("hi");
//        logger.debug("hi2");
        SpringApplication app = new SpringApplication(MySpringApp.class);
        ApplicationContext ctx = app.run(args);
//        SpringCamelContext camel = (SpringCamelContext) ctx.getBean(SpringCamelContext.class);
//        camel.start();
//        logger.debug("hi");
//        logger.debug("hi");
    }


}