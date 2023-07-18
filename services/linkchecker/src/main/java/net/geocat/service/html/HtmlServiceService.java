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

package net.geocat.service.html;

import net.geocat.database.linkchecker.entities.LinkCheckJob;
import net.geocat.database.linkchecker.entities.LocalServiceMetadataRecord;
import net.geocat.database.linkchecker.entities.OperatesOnLink;
import net.geocat.database.linkchecker.entities.ServiceDocumentLink;
import net.geocat.database.linkchecker.entities.helper.DatasetIdentifier;
import net.geocat.database.linkchecker.repos.LinkCheckJobRepo;
import net.geocat.database.linkchecker.repos.LocalServiceMetadataRecordRepo;
import net.geocat.service.BlobStorageService;
import net.geocat.xml.XmlDoc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;

import static net.geocat.service.html.HtmlDatasetService.showDownloadableLink;

@Component
public class HtmlServiceService {

    @Autowired
    LocalServiceMetadataRecordRepo localServiceMetadataRecordRepo;

    @Autowired
    LinkCheckJobRepo linkCheckJobRepo;

    @Autowired
    BlobStorageService blobStorageService;


    public String lastLinkCheckJob(String country) {
        if (country == null)
            return lastLinkCheckJob();

        LinkCheckJob lastJob = null;
        for (LinkCheckJob job : linkCheckJobRepo.findAll()) {
            if (!job.getLongTermTag().toLowerCase().startsWith(country.toLowerCase()))
                continue;
            if (lastJob == null)
                lastJob = job;
            if (lastJob.getCreateTimeUTC().compareTo(job.getCreateTimeUTC()) < 1)
                lastJob = job;
        }
        return lastJob.getJobId();
    }


    public String lastLinkCheckJob() {
        LinkCheckJob lastJob = null;
        for (LinkCheckJob job : linkCheckJobRepo.findAll()) {
            if (lastJob == null)
                lastJob = job;
            if (lastJob.getCreateTimeUTC().compareTo(job.getCreateTimeUTC()) < 1)
                lastJob = job;
        }
        return lastJob.getJobId();
    }

    public String getHtml(String processID, String fileId) throws Exception {
        if ((processID == null) || (processID.trim().isEmpty()))
            processID = lastLinkCheckJob();
        processID = processID.trim();
        LocalServiceMetadataRecord record = localServiceMetadataRecordRepo.findFirstByFileIdentifierAndLinkCheckJobId(fileId, processID);
        if (record == null)
            return "<h1> Couldnt find service record </h1>";

        String result = "<head><meta charset=\"UTF-8\"></head>\n";

        result += "<h1> Service Record</h1> \n";

        result += "title: " + record.getTitle() + "<br>\n";
        result += "Metadata Record Type: " + record.getMetadataRecordType() + "<br>\n";
        result += "Service Type: " + record.getMetadataServiceType() + "<br>\n";

//        result += "<xmp>"+record.toString()  + "</xmp><br>\n<br>\n";

        result += "<h2>Successful links to Capabilities Documents</h2> \n";
        int idx = 0;
        for (ServiceDocumentLink link : record.getServiceDocumentLinks()) {
            if ((link.getUrlFullyRead() != null) && (link.getUrlFullyRead())) {
                result += " <a href='" + "/api/html/capabilities/" + link.getLinkCheckJobId() + "/" + link.getSha2() + "'>" + link.getXmlDocInfo() + "</a>" + "<br>\n";
                idx++;
            }
        }
        if (idx == 0)
            result += "NO SUCCESSFUL LINKS TO CAPABILITIES DOCUMENTS<br>\n";

        result += "<h2>All Service Document Links - " + record.getServiceDocumentLinks().size() + "</h2>\n";
        idx = 0;
        for (ServiceDocumentLink link : record.getServiceDocumentLinks()) {
            result += " <h3>Document Link #" + idx + "</h3>";//+" - <a href='"+ link.getFixedURL() +"'>"+link.getFixedURL()+"</a>" +"</h3>\n";
            result += showDownloadableLink(link, true);

//            result += "fully downloaed: "+link.getUrlFullyRead()+"<br>\n";
//            result += "<xmp>"+link.toString()  + "</xmp><br>\n<br>\n";
//            result +="Initial Data:<br>\n";
//            if (link.getLinkContentHead() !=null)
//                result += "<xmp>"+new String(link.getLinkContentHead())+"</xmp>";
            idx++;
        }


        result += "<br><h2>Operates On Links - " + record.getOperatesOnLinks().size() + "</h2><br> \n";
        idx = 0;
        for (OperatesOnLink link : record.getOperatesOnLinks()) {
            result += "<h3>OperatesOn Link #" + idx + "<br></h3>";//+" - <a href='"+ link.getFixedURL() +"'>"+link.getFixedURL()+"</a>" +"</h3>\n";
            if (!link.getDatasetIdentifiers().isEmpty()) {
                result += "Remote Operates On Dataset Identifiers:<bR>\n";
                for (DatasetIdentifier identifier : link.getDatasetIdentifiers()) {

                    String codeLink = "<a href='/api/html/identifier?code=" + URLEncoder.encode(identifier.getCode()) + "&linkcheckjobid" + link.getLinkCheckJobId() + "'>" + identifier.toString() + "</a>";

                    result += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + codeLink + "<br>\n";
                }
                result += "<br>";
            }
            result += showDownloadableLink(link, true);

//            result += "fully downloaded: "+link.getUrlFullyRead()+"<br>\n";
//            result += "<xmp>"+link.toString()  + "</xmp><br>\n<br>\n";
////            result +="Initial Data:<br>\n";
////            if (link.getLinkContentHead() !=null)
////                result += "<xmp>"+new String(link.getLinkContentHead())+"</xmp>";
//            result += "Dataset Identfiers:<br>\n";
//            for(DatasetIdentifier identifier:link.getDatasetIdentifiers()) {
//                result += identifier.toString() +"<br>\n";
//            }
            idx++;
        }


        result += "<br><br><br><hr><br><br><h1>Actual Service Text</h1><br><hr><xmp>" + text(record) + "</xmp><br><br>";
        return result;
    }

    private String text(LocalServiceMetadataRecord record) throws Exception {
        String xml = blobStorageService.findXML(record.getSha2());
        xml = XmlDoc.writeXMLPretty(xml);
        return xml;
    }
}
