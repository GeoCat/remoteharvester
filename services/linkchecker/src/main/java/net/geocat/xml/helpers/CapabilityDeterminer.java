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

package net.geocat.xml.helpers;

import net.geocat.xml.XmlDoc;
import org.springframework.stereotype.Component;

@Component
public class CapabilityDeterminer {

    public CapabilitiesType determineCapabilitiesType(XmlDoc doc) throws Exception {

        String tag = doc.getRootTagName();
        String ns = doc.getRootNS();
        if (ns == null)
            ns = "";
        return determineType(ns.toLowerCase(), tag);
    }

    public CapabilitiesType determineType(String ns, String rootTagName) throws Exception {
        if (ns == null)
            ns = "";
        if (rootTagName.equals("WMS_Capabilities") && (ns.equalsIgnoreCase("http://www.opengis.net/wms")))
            return CapabilitiesType.WMS;
        if (rootTagName.equals("Capabilities") && (ns.equalsIgnoreCase("http://www.opengis.net/wmts/1.0")))
            return CapabilitiesType.WMTS;
        if (rootTagName.equals("WMT_MS_Capabilities")) // EE examples dont have a namespace
            return CapabilitiesType.WMS;
        if (rootTagName.equals("WFS_Capabilities") && (ns.equalsIgnoreCase("http://www.opengis.net/wfs/2.0")))
            return CapabilitiesType.WFS;
        if (rootTagName.equals("WFS_Capabilities") && (ns.equalsIgnoreCase("http://www.opengis.net/wfs")))
            return CapabilitiesType.WFS;
//        if (rootTagName.equals("Capabilities")&& (ns.equalsIgnoreCase("http://www.opengis.net/wcs/2.0")) )
//            return CapabilitiesType.WCS;
//        if (rootTagName.equals("Capabilities")&& (ns.equalsIgnoreCase("http://www.opengis.net/sos/2.0")) )
//            return CapabilitiesType.SOS;
        if (rootTagName.equals("feed") && (ns.equalsIgnoreCase("http://www.w3.org/2005/atom")))
            return CapabilitiesType.Atom;
        if (rootTagName.equals("Capabilities") && (ns.equals("http://www.opengis.net/cat/csw/2.0.2")))
            return CapabilitiesType.CSW;

        throw new Exception("not a known capabilities doc");
    }

}
