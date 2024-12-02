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

package net.geocat.database.linkchecker.entities.helper;

import net.geocat.database.linkchecker.entities.CapabilitiesDocument;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Represents a link in a document
@MappedSuperclass
public abstract class DocumentLink extends RetrievableSimpleLink {

    //if the operation name was attached to the link, its recorded here (see XML XSL)
    @Column(columnDefinition = "text")
    String operationName;

    //if the protocol was attached to the link, its recorded here (see XML XSL)
    @Column(columnDefinition = "text")
    String protocol;

    //if the function was attached to the link, its recorded here (see XML XSL)
    @Column(columnDefinition = "text")
    String function;

    //applicationProfile from XML
    @Column(columnDefinition = "text")
    String applicationProfile;

    //not saved - if this resolved to a capabilities document, temporarily store it here.
    @Transient
    private CapabilitiesDocument capabilitiesDocument;


    //---------------------------------------------------------------


    public CapabilitiesDocument getCapabilitiesDocument() {
        return capabilitiesDocument;
    }

    public void setCapabilitiesDocument(CapabilitiesDocument capabilitiesDocument) {
        this.capabilitiesDocument = capabilitiesDocument;
    }

    public DocumentLink() {
        super();
    }

    //--

    public static List<String> validViewProtocols = Arrays.asList(new String[]{
            "wms",
            "http://www.opengis.net/def/serviceType/ogc/wms".toLowerCase(),
            "OGC Web Map Service".toLowerCase(),
            "Web Map Service (WMS)".toLowerCase(),
            "OGC:WMS".toLowerCase(),
            "http://www.opengeospatial.org/standards/wms",
            "wmts",
            "http://www.opengis.net/def/serviceType/ogc/wmts".toLowerCase(),
            "OGC Web Map Tile Service".toLowerCase(),
            "OGC:WMTS".toLowerCase(),
            "http://www.opengeospatial.org/standards/wmts"
    });

    public static List<String> validDownloadProtocols = Arrays.asList(new String[]{
            "wfs",
            "http://www.opengis.net/def/serviceType/ogc/wfs".toLowerCase(),
            "OGC Web Feature Service".toLowerCase(),
            "Web Feature Service (WFS)".toLowerCase(),
            "OGC:WFS".toLowerCase(),
            "http://www.opengeospatial.org/standards/wfs",
            "atom",
            "https://tools.ietf.org/html/rfc4287".toLowerCase(),
            "ATOM Syndication Format".toLowerCase(),
            "INSPIRE Atom".toLowerCase(),
            "wcs",
            "OGC:WCS".toLowerCase(),
            "http://www.opengis.net/def/serviceType/ogc/wcs".toLowerCase(),
            "api features",
            "OGC - API Features".toLowerCase(),
            "OGC:OGC-API-Features-items".toLowerCase(),
            "HTTP:OGC:API-Features".toLowerCase(),
            "http://www.opengis.net/def/interface/ogcapi-features".toLowerCase(),
            "SensorThings".toLowerCase(),
            "sos",
            "OGC:SOS".toLowerCase(),
            "http://www.opengis.net/def/serviceType/ogc/sos".toLowerCase()
    });

    public static List<String> validProtocols = Stream.concat(validViewProtocols.stream(),
                    validDownloadProtocols.stream()).collect(Collectors.toList());

    public static List<String> validAtomProtocols = Arrays.asList(new String[]{
            "https://tools.ietf.org/html/rfc4287".toLowerCase(),
            "ATOM Syndication Format".toLowerCase(),
            "atom",
            "INSPIRE Atom".toLowerCase()
    });

    public static List<String> validAppProfiles = Arrays.asList(new String[]{
            "Download Service".toLowerCase(),
            "View Service".toLowerCase(),
            "http://inspire.ec.europa.eu/metadata-codelist/SpatialDataServiceType/download".toLowerCase(),
            "http://inspire.ec.europa.eu/metadata-codelist/SpatialDataServiceType/view".toLowerCase()
    });

    public static final String VALID_PROTOCOLS_VIEW_REGEX = "(.*wms.*|.*wmts.*|.*web map service.*)";

    public static final String VALID_PROTOCOLS_DOWNLOAD_REGEX = "(.*wfs.*|.*atom.*|.*wcs.*|.*sos.*|.*api.*feature.*|.*sensorthings.*|.*web feature service.*)";

    public static final String VALID_PROTOCOLS_REGEX = "(.*wfs.*|.*atom.*|.*wcs.*|.*sos.*|.*api.*feature.*|.*sensorthings.*|.*wms.*|.*wmts.*|.*web map service.*|.*web feature service.*)";

    public boolean isInspireSimplifiedLink() {
        // Relax the check to process links with the applicationProfile information
        if ((rawURL == null) || (protocol == null))
            return false;
        if (rawURL.isEmpty() || protocol.isEmpty())
            return false;

        if (!validProtocols.contains(protocol.toLowerCase())) {
            // Check protocol match "simple" values instead of exact match
            if (!protocol.toLowerCase().matches(VALID_PROTOCOLS_REGEX)) {
                return false;
            }
        }


        return true;
    }


    /*public boolean isInspireSimplifiedLink() {
        if ((rawURL == null) || (protocol == null) || (applicationProfile == null))
            if ((rawURL == null) || (protocol == null))
                return false;
        if (rawURL.isEmpty() || protocol.isEmpty() || applicationProfile.isEmpty())
            if (rawURL.isEmpty() || protocol.isEmpty())
                return false;

        if (!validProtocols.contains(protocol.toLowerCase()))
            return false;

        if (!validAppProfiles.contains(applicationProfile.toLowerCase()))
            return false;

        return true;
    }*/



    //--

    public String getApplicationProfile() {
        return applicationProfile;
    }

    public void setApplicationProfile(String applicationProfile) {
        this.applicationProfile = applicationProfile;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }


    protected void onUpdate() {
        super.onUpdate();
    }


    protected void onInsert() {
        super.onInsert();
    }

    @Override
    public String toString() {
        String result = "";
        if ((operationName != null) && (!operationName.isEmpty()))
            result += "      operationName: " + operationName + "\n";

        if ((protocol != null) && (!protocol.isEmpty()))
            result += "      protocol: " + protocol + "\n";
        if ((function != null) && (!function.isEmpty()))
            result += "      function: " + function + "\n";

        result += super.toString();
        return result;
    }
}
