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

package net.geocat.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class LinkCheckRunConfig {

    String longTermTag;
    String harvestJobId;

    Boolean useOtherJobsHTTPCache;
    Boolean deleteHTTPCacheWhenComplete;

    Integer maxDataLinksToFollow;
    Integer maxAtomEntriesToAttempt;
    Integer maxAtomSectionLinksToFollow;

    // GUID for the harvest (used as JMS Correlation ID).  Provided by server (do not specify)
    private String processID;
    Integer storeAtMostNHistoricalRuns;


    //--


    public Boolean getUseOtherJobsHTTPCache() {
        return useOtherJobsHTTPCache;
    }

    public void setUseOtherJobsHTTPCache(Boolean useOtherJobsHTTPCache) {
        this.useOtherJobsHTTPCache = useOtherJobsHTTPCache;
    }

    public Boolean getDeleteHTTPCacheWhenComplete() {
        return deleteHTTPCacheWhenComplete;
    }

    public void setDeleteHTTPCacheWhenComplete(Boolean deleteHTTPCacheWhenComplete) {
        this.deleteHTTPCacheWhenComplete = deleteHTTPCacheWhenComplete;
    }

    public void setMaxDataLinksToFollow(Integer maxDataLinksToFollow) {
        this.maxDataLinksToFollow = maxDataLinksToFollow;
    }

    public void setMaxAtomEntriesToAttempt(Integer maxAtomEntriesToAttempt) {
        this.maxAtomEntriesToAttempt = maxAtomEntriesToAttempt;
    }

    public void setMaxAtomSectionLinksToFollow(Integer maxAtomSectionLinksToFollow) {
        this.maxAtomSectionLinksToFollow = maxAtomSectionLinksToFollow;
    }

    public Integer getStoreAtMostNHistoricalRuns() {
        return storeAtMostNHistoricalRuns;
    }

    public void setStoreAtMostNHistoricalRuns(Integer storeAtMostNHistoricalRuns) {
        this.storeAtMostNHistoricalRuns = storeAtMostNHistoricalRuns;
    }

    public Integer getMaxDataLinksToFollow() {
        return maxDataLinksToFollow;
    }

    public void setMaxDataLinksToFollow(int maxDataLinksToFollow) {
        this.maxDataLinksToFollow = maxDataLinksToFollow;
    }

    public Integer getMaxAtomEntriesToAttempt() {
        return maxAtomEntriesToAttempt;
    }

    public void setMaxAtomEntriesToAttempt(int maxAtomEntriesToAttempt) {
        this.maxAtomEntriesToAttempt = maxAtomEntriesToAttempt;
    }

    public Integer getMaxAtomSectionLinksToFollow() {
        return maxAtomSectionLinksToFollow;
    }

    public void setMaxAtomSectionLinksToFollow(int maxAtomSectionLinksToFollow) {
        this.maxAtomSectionLinksToFollow = maxAtomSectionLinksToFollow;
    }

    public Boolean isUseOtherJobsHTTPCache() {
        return useOtherJobsHTTPCache;
    }

    public void setUseOtherJobsHTTPCache(boolean useOtherJobsHTTPCache) {
        this.useOtherJobsHTTPCache = useOtherJobsHTTPCache;
    }

    public Boolean isDeleteHTTPCacheWhenComplete() {
        return deleteHTTPCacheWhenComplete;
    }

    public void setDeleteHTTPCacheWhenComplete(boolean deleteHTTPCacheWhenComplete) {
        this.deleteHTTPCacheWhenComplete = deleteHTTPCacheWhenComplete;
    }

    public String getLongTermTag() {
        return longTermTag;
    }

    public void setLongTermTag(String longTermTag) {
        this.longTermTag = longTermTag;
    }

    public String getHarvestJobId() {
        return harvestJobId;
    }

    public void setHarvestJobId(String harvestJobId) {
        this.harvestJobId = harvestJobId;
    }

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }
}
