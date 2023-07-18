package com.geocat.ingester.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geocat.ingester.dao.harvester.EndpointJobRepo;
import com.geocat.ingester.dao.harvester.HarvestJobRepo;
import com.geocat.ingester.dao.harvester.MetadataRecordRepo;
import com.geocat.ingester.dao.ingester.IngestJobRepo;
import com.geocat.ingester.dao.linkchecker.*;
import com.geocat.ingester.exception.GeoNetworkClientException;
import com.geocat.ingester.geonetwork.client.GeoNetworkClient;
import com.geocat.ingester.model.harvester.EndpointJob;
import com.geocat.ingester.model.harvester.HarvestJob;
import com.geocat.ingester.model.harvester.MetadataRecordXml;
import com.geocat.ingester.model.ingester.IngestJob;
import com.geocat.ingester.model.ingester.IngestJobState;
import com.geocat.ingester.model.linkchecker.*;
import com.geocat.ingester.model.linkchecker.helper.*;
import com.geocat.ingester.model.linkedresources.LinkedResourceIndicator;
import com.geocat.ingester.model.metadata.HarvesterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
//@Transactional("metadataTransactionManager")
//@Slf4j(topic = "com.geocat.ingester.service")
public class IngesterService {

    Logger log = LoggerFactory.getLogger(IngesterService.class);

    @Autowired
    private HarvestJobRepo harvestJobRepo;

    @Autowired
    private EndpointJobRepo endpointJobRepo;

    @Autowired
    private MetadataRecordRepo metadataRecordRepo;

    @Autowired
    private CatalogueService catalogueService;

    @Autowired
    private GeoNetworkClient geoNetworkClient;

    @Autowired
    private IngestJobService ingestJobService;

    @Autowired
    private LinkCheckJobRepo linkCheckJobRepo;

//    @Autowired
//   Autowired private LocalServiceMetadataRecordRepo localServiceMetadataRecordRepo;

//    @Autowired
//    private LocalDatasetMetadataRecordRepo localDatasetMetadataRecordRepo;

    @Autowired
    LazyLocalServiceMetadataRecordRepo lazyLocalServiceMetadataRecordRepo;

    @Autowired
    LazyLocalDatsetMetadataRecordRepo lazyLocalDatsetMetadataRecordRepo;

    @Autowired
    ServiceDocumentLinkRepo serviceDocumentLinkRepo;

    @Autowired
    DatasetDocumentLinkRepo datasetDocumentLinkRepo;

    @Autowired
    LinkToDataRepo linkToDataRepo;

    @Autowired
    private IngestJobRepo ingestJobRepo;

    public boolean continueProcessing(String jobid) {
        IngestJob ingestJob = ingestJobRepo.findById(jobid).get();
        return (ingestJob.getState() != IngestJobState.ERROR) && (ingestJob.getState() != IngestJobState.USERABORT);
    }

    /**
     * Executes the ingester process.
     *
     * @param harvestJobId
     * @return true - completed, false - aborted
     * @throws Exception
     */
    public boolean run(String processId, String harvestJobId) throws Exception {
        Optional<HarvestJob> harvestJob = harvestJobRepo.findById(harvestJobId);
        if (!harvestJob.isPresent()) {
            log.info("No harvester job related found with harvest job id " + harvestJobId + ".");
            // TODO: throw Exception harvester job not found
            return false;
        }

        String harvesterUuidOrName = harvestJob.get().getLongTermTag();

        Optional<HarvesterConfiguration> harvesterConfigurationOptional = catalogueService.retrieveHarvesterConfiguration(harvesterUuidOrName);

        if (!harvesterConfigurationOptional.isPresent()) {
            log.info("Harvester with name/uuid " + harvesterUuidOrName + " not found.");
            // TODO: throw Exception harvester not found
            return false;
        }

        log.info("Start ingestion process for harvester with name/uuid " + harvestJob.get().getLongTermTag() + ".");

        Optional<LinkCheckJob> linkCheckJob = linkCheckJobRepo.findByHarvestJobId(harvestJobId);
        String linkCheckJobId = null;
        if (!linkCheckJob.isPresent()) {
            log.info("No link checker job related found for the harvester with name/uuid " + harvesterUuidOrName + ".");
        } else {
            linkCheckJobId = linkCheckJob.get().getJobId();
        }

        List<EndpointJob> endpointJobList = endpointJobRepo.findByHarvestJobId(harvestJobId);

        long totalMetadataToProcess = 0;
        for (EndpointJob job : endpointJobList) {
            totalMetadataToProcess = totalMetadataToProcess + metadataRecordRepo.countMetadataRecordByEndpointJobId(job.getEndpointJobId());
        }

        if (!continueProcessing(processId)) {
            log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
            return false;
        }
        ingestJobService.updateIngestJobStateInDBIngestedRecords(processId, 0, 0, 0, totalMetadataToProcess);

        Map<String, Boolean> metadataIds = new HashMap<>();

        try {
            for (EndpointJob job : endpointJobList) {
                Boolean pagesAvailable = true;

                int page = 0;
                int size = 200;

                long total = 0;

                while (pagesAvailable) {

                    if (!continueProcessing(processId)) {
                        log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
                        return false;
                    }

                    Pageable pageableRequest = PageRequest.of(page++, size);

                    Page<MetadataRecordXml> metadataRecordList =
                            metadataRecordRepo.findMetadataRecordWithXmlByEndpointJobId(job.getEndpointJobId(), pageableRequest);

                    if (page == 1) {
                        log.info("Total harvested records to process: " + metadataRecordList.getTotalElements());
                    }

                    long from = (size * (page - 1)) + 1;
                    long to = Math.min((size * page) - 1, metadataRecordList.getTotalElements());
                    log.info("Adding harvested metadata records to the catalogue from " + from + " to " + to + " of " + metadataRecordList.getTotalElements());

                    total = total + metadataRecordList.getNumberOfElements();

                    // Process metadata indicators
                    if (!StringUtils.isEmpty(linkCheckJobId)) {
                        final String linkCheckJobIdAux = linkCheckJobId;

                        metadataRecordList.forEach(r -> {
                            fillMetadataIndicators(r, linkCheckJobIdAux);
                        });
                    }

                    if (!continueProcessing(processId)) {
                        log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
                        return false;
                    }
                    metadataIds.putAll(catalogueService.addOrUpdateMetadataRecords(metadataRecordList.toList(), harvesterConfigurationOptional.get(), harvestJobId));
                    if (!continueProcessing(processId)) {
                        log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
                        return false;
                    }
                    ingestJobService.updateIngestJobStateInDBIngestedRecords(processId, total);

                    pagesAvailable = !metadataRecordList.isLast();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!continueProcessing(processId)) {
            log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
            return false;
        }

        // Index added/updated records
        // TODO: Test, commented to be indexed in GeoNetwork to avoid issues with GeoNetwork API and ECAS
        List<String> metadataIdsToIndex = metadataIds.entrySet().stream().filter(a -> a.getValue().equals(Boolean.TRUE))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        geoNetworkClient.init();

        if (!continueProcessing(processId)) {
            log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
            return false;
        }

        ingestJobService.updateIngestJobStateInDB(processId, IngestJobState.INDEXING_RECORDS);

        // Commented: to do remote indexing due to ECAS auth in GeoNetwork the csw-ingester can't use GeoNetwork API
        /*boolean completed=indexRecords(metadataIdsToIndex, processId);
        if (!completed) {
            log.warn(harvestJobId + " indexRecords reported non-complete -- aborting");
            return false;
        }*/
        // Delete old harvested records no longer in the harvester server
        List<String> remoteHarvesterUuids = metadataIds.entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> localHarvesterUuids =
                catalogueService.retrieveLocalUuidsForHarvester(harvesterConfigurationOptional.get().getUuid());

        List<String> metadataIdsToDelete = localHarvesterUuids.stream()
                .distinct()
                .filter(s -> !remoteHarvesterUuids.contains(s))
                .collect(Collectors.toList());

        if (!continueProcessing(processId)) {
            log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
            return false;
        }

        ingestJobService.updateIngestJobStateInDB(processId, IngestJobState.DELETING_RECORDS);
        deleteRecords(metadataIdsToDelete, processId);

        if (!continueProcessing(processId)) {
            log.warn(harvestJobId + " is in USERABORT/ERROR state - aborting");
            return false;
        }

        ingestJobService.updateIngestJobStateInDB(processId, IngestJobState.RECORDS_PROCESSED);

        log.info("IngesterService: run(): Finished ingestion process for harvester with name/uuid " + harvesterUuidOrName + ".");
        return true;
    }


    /**
     * Calls GeoNetwork index process for a set of metadata records.
     *
     * @param metadataIds
     * @param processId
     * @return true - completed, false - aborted
     * @throws GeoNetworkClientException
     */
    private boolean indexRecords(List<String> metadataIds, String processId) {
        int batchSize = 50;

        int totalPages = (int) Math.ceil(metadataIds.size() * 1.0 / batchSize * 1.0);

        int total = 0;

        for (int i = 0; i < totalPages; i++) {
            try {
                if (!continueProcessing(processId)) {
                    log.warn(processId + " is in USERABORT/ERROR state - aborting");
                    return false;
                }

                int from = i * batchSize;
                int to = Math.min(((i + 1) * batchSize), metadataIds.size());

                int toR = (i == totalPages - 1) ? metadataIds.size() : (to - 1);
                log.info("Indexing harvested metadata records from " + Math.max(1, i * batchSize) + " to " + toR + " of " + metadataIds.size());

                geoNetworkClient.index(metadataIds.subList(from, to));

                total = total + (to - from);

                ingestJobService.updateIngestJobStateInDBIndexedRecords(processId, total);

            } catch (GeoNetworkClientException ex) {
                // TODO: Handle exception
                log.error(ex.getMessage(), ex);
                return false;
            }
        }
        return true;
    }

    /**
     * Calls GeoNetwork delete process for a set of metadata records.
     *
     * @param metadataIds
     * @param processId
     * @throws GeoNetworkClientException
     */
    private void deleteRecords(List<String> metadataIds, String processId) {
        int batchSize = 50;

        int totalPages = (int) Math.ceil(metadataIds.size() * 1.0 / batchSize * 1.0);

        int total = 0;

        for (int i = 0; i < totalPages; i++) {
            try {
                int from = i * batchSize;
                int to = Math.min(((i + 1) * batchSize), metadataIds.size());

                int toR = (i == totalPages - 1) ? metadataIds.size() : (to - 1);
                log.info("Deleting old harvested metadata records from " + Math.max(1, i * batchSize) + " to " + toR + " of " + metadataIds.size());

                catalogueService.deleteMetadataByUuids(new HashSet(metadataIds.subList(from, to)));
                // Commented: to do local delete due to ECAS auth in GeoNetwork the csw-ingester can't use GeoNetwork API
                //geoNetworkClient.delete(metadataIds.subList(from , to));

                total = total + (to - from);

                ingestJobService.updateIngestJobStateInDBDeletedRecords(processId, total);

            } catch (Exception ex) {
                // TODO: Handle exception
                log.error(ex.getMessage(), ex);

            }
        }
    }


    /**
     * Fill the indicators associated to a metadata record for a link checker job.
     *
     * @param metadata
     * @param linkCheckJobId
     */
    private void fillMetadataIndicators(MetadataRecordXml metadata, String linkCheckJobId) {
        // List<LocalServiceMetadataRecord> localServiceMetadataRecord = localServiceMetadataRecordRepo.findAllByFileIdentifierAndLinkCheckJobId(metadata.getRecordIdentifier(), linkCheckJobId);
        Optional<LocalServiceMetadataRecord> localServiceMetadataRecord = lazyLocalServiceMetadataRecordRepo.searchFirstByFileIdentifierAndLinkCheckJobId(metadata.getRecordIdentifier(), linkCheckJobId);


        if (localServiceMetadataRecord.isPresent()) {
            addIndicator(metadata, "INDICATOR_ALL_CAPABILITIES_LAYER_RESOLVE", localServiceMetadataRecord.get().getINDICATOR_ALL_CAPABILITIES_LAYER_RESOLVE());
            addIndicator(metadata, "INDICATOR_ALL_OPERATES_ON_RESOLVE", localServiceMetadataRecord.get().getINDICATOR_ALL_OPERATES_ON_RESOLVE());
            addIndicator(metadata, "INDICATOR_ALL_OPERATES_ON_MATCH_CAPABILITIES", localServiceMetadataRecord.get().getINDICATOR_ALL_OPERATES_ON_MATCH_CAPABILITIES());
            addIndicator(metadata, "INDICATOR_CAPABILITIES_TYPE", localServiceMetadataRecord.get().getINDICATOR_CAPABILITIES_TYPE());
            addIndicator(metadata, "INDICATOR_CAPABILITIES_RESOLVES_TO_SERVICE", localServiceMetadataRecord.get().getINDICATOR_CAPABILITIES_RESOLVES_TO_SERVICE());
            addIndicator(metadata, "INDICATOR_CAPABILITIES_SERVICE_FILE_ID_MATCHES", localServiceMetadataRecord.get().getINDICATOR_CAPABILITIES_SERVICE_FILE_ID_MATCHES());
            // addIndicator(metadata, "INDICATOR_CAPABILITIES_SERVICE_FULLY_MATCHES", localServiceMetadataRecord.get(0).getINDICATOR_CAPABILITIES_SERVICE_FULLY_MATCHES());
            addIndicator(metadata, "INDICATOR_RESOLVES_TO_CAPABILITIES", localServiceMetadataRecord.get().getINDICATOR_RESOLVES_TO_CAPABILITIES());

        } else {
            // List<LocalDatasetMetadataRecord> localDatasetMetadataRecord = localDatasetMetadataRecordRepo.findAllByFileIdentifierAndLinkCheckJobId(metadata.getRecordIdentifier(), linkCheckJobId);
            Optional<LocalDatasetMetadataRecord> localDatasetMetadataRecord = lazyLocalDatsetMetadataRecordRepo.searchFirstByFileIdentifierAndLinkCheckJobId(metadata.getRecordIdentifier(), linkCheckJobId);

            if (localDatasetMetadataRecord.isPresent()) {
                addIndicator(metadata, "INDICATOR_CAPABILITIES_TYPE", localDatasetMetadataRecord.get().getINDICATOR_CAPABILITIES_TYPE());
                addIndicator(metadata, "INDICATOR_LAYER_MATCHES", localDatasetMetadataRecord.get().getINDICATOR_LAYER_MATCHES());
                addIndicator(metadata, "INDICATOR_RESOLVES_TO_CAPABILITIES", localDatasetMetadataRecord.get().getINDICATOR_RESOLVES_TO_CAPABILITIES());
                addIndicator(metadata, "INDICATOR_LAYER_MATCHES_VIEW", localDatasetMetadataRecord.get().getINDICATOR_LAYER_MATCHES_VIEW());
                addIndicator(metadata, "INDICATOR_LAYER_MATCHES_DOWNLOAD", localDatasetMetadataRecord.get().getINDICATOR_LAYER_MATCHES_DOWNLOAD());
                addIndicator(metadata, "INDICATOR_VIEW_LINK_TO_DATA", localDatasetMetadataRecord.get().getINDICATOR_VIEW_LINK_TO_DATA());
                addIndicator(metadata, "INDICATOR_DOWNLOAD_LINK_TO_DATA", localDatasetMetadataRecord.get().getINDICATOR_DOWNLOAD_LINK_TO_DATA());

                List<LinkToData> metadataLinks = linkToDataRepo.findAllByLinkCheckJobIdAndDatasetMetadataFileIdentifier(linkCheckJobId, localDatasetMetadataRecord.get().getFileIdentifier());

                processViewLinksIndicators(metadata, metadataLinks, linkCheckJobId);

                processDownloadLinksIndicators(metadata, metadataLinks, linkCheckJobId);
            }
        }
    }

    private void addIndicator(MetadataRecordXml metadata, String indicatorName, String value) {
        if (StringUtils.hasLength(value)) {
            metadata.addIndicator(indicatorName, value);
        }
    }

    private void addOrUpdateIndicator(MetadataRecordXml metadata, String indicatorName, String value) {
        String currentValue = metadata.getIndicatorValue(indicatorName);
        if (StringUtils.hasLength(currentValue)) {
            value = currentValue + "|" + value;
        }

        metadata.addIndicator(indicatorName, Optional.ofNullable(value).orElse(""));
    }

    private void addIndicator(MetadataRecordXml metadata, String indicatorName, IndicatorStatus indicatorStatus) {
        if (indicatorStatus != null) {
            metadata.addIndicator(indicatorName, indicatorStatus.toString());
        }
    }

    private void addIndicator(MetadataRecordXml metadata, String indicatorName, CapabilitiesType capabilitiesType) {
        if (capabilitiesType != null) {
            metadata.addIndicator(indicatorName, capabilitiesType.toString());
        }
    }

    private void addIndicator(MetadataRecordXml metadata, String indicatorName, Integer indicator) {
        if (indicator != null) {
            metadata.addIndicator(indicatorName, indicator.toString());
        }
    }

    private void processViewLinksIndicators(MetadataRecordXml metadata, List<LinkToData> metadataLinks, String linkCheckJobId) {
        // Extract the view links
        List<LinkToData> viewLinks = metadataLinks.stream().filter(l ->
                l.getCapabilitiesDocumentType().equals(CapabilitiesType.WMS) ||
                        l.getCapabilitiesDocumentType().equals(CapabilitiesType.WMTS)
        ).collect(Collectors.toList());


        List<LinkedResourceIndicator> viewServiceLinkedResourceIndicators = new ArrayList<>();

        // Calculate the indicators for the view links
        viewLinks.stream().forEach(vl -> {
            String capabilitiesSha2 = vl.getCapabilitiesSha2();

            if (vl instanceof OGCLinkToData) {
                OGCLinkToData ogcLinkToData = (OGCLinkToData) vl;

                String layerName = ogcLinkToData.getOgcLayerName();
                String layerLink = (ogcLinkToData.getOgcRequest() != null) ? ogcLinkToData.getOgcRequest().getFinalURL() : "";

                List<ServiceDocumentLink> serviceDocumentLinks = serviceDocumentLinkRepo.findByLinkCheckJobIdAndSha2(linkCheckJobId, capabilitiesSha2);

                List<String> associatedServiceIds = new ArrayList<>();

                for (ServiceDocumentLink documentLink : serviceDocumentLinks) {
                    ServiceMetadataRecord serviceMetadataRecord = documentLink.getLocalServiceMetadataRecord();

                    if (serviceMetadataRecord != null) {
                        associatedServiceIds.add(serviceMetadataRecord.getFileIdentifier());
                    }
                }

                // For simplified INSPIRE model - the links can come from the dataset document as well
                List<DatasetDocumentLink> datasetDocumentLinks = datasetDocumentLinkRepo.findByLinkCheckJobIdAndSha2(linkCheckJobId, capabilitiesSha2);

                for (DatasetDocumentLink documentLink : datasetDocumentLinks) {
                    DatasetMetadataRecord datasetMetadataRecord = documentLink.getDatasetMetadataRecord();

                    if (datasetMetadataRecord != null) {
                        associatedServiceIds.add(datasetMetadataRecord.getFileIdentifier());
                    }
                }

                LinkedResourceIndicator indicator = new LinkedResourceIndicator(layerName, layerLink, associatedServiceIds);
                viewServiceLinkedResourceIndicators.add(indicator);
            }
        });

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            addOrUpdateIndicator(metadata, "INDICATOR_VIEW_SERVICE_LINKED_RESOURCES",
                    objectMapper.writeValueAsString(viewServiceLinkedResourceIndicators));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void processDownloadLinksIndicators(MetadataRecordXml metadata, List<LinkToData> metadataLinks, String linkCheckJobId) {
        // Retrieve the service download links
        List<LinkToData> downloadLinks = metadataLinks.stream().filter(l ->
                l.getCapabilitiesDocumentType().equals(CapabilitiesType.Atom) ||
                        l.getCapabilitiesDocumentType().equals(CapabilitiesType.WFS)
        ).collect(Collectors.toList());

        List<LinkedResourceIndicator> downloadServiceLinkedResourceIndicators = new ArrayList<>();

        // Calculate the indicators for the service download links
        downloadLinks.stream().forEach(vl -> {
            String capabilitiesSha2 = vl.getCapabilitiesSha2();

            String layerName = "";
            String layerLink = "";
            List<String> associatedServiceIds = new ArrayList<>();

            if (vl instanceof OGCLinkToData) {
                OGCLinkToData ogcLinkToData = (OGCLinkToData) vl;

                layerName = ogcLinkToData.getOgcLayerName();
                layerLink = (ogcLinkToData.getOgcRequest() != null) ? ogcLinkToData.getOgcRequest().getFinalURL() : "";

            } else if (vl instanceof SimpleStoredQueryDataLink) {
                SimpleStoredQueryDataLink simpleStoredQueryDataLink = (SimpleStoredQueryDataLink) vl;

                layerLink = (simpleStoredQueryDataLink.getOgcRequest() != null) ? simpleStoredQueryDataLink.getOgcRequest().getFinalURL() : "";

            } else if (vl instanceof SimpleAtomLinkToData) {
                SimpleAtomLinkToData simpleAtomLinkToData = (SimpleAtomLinkToData) vl;

                // TODO: Review
                List<AtomActualDataEntry> atomActualDataEntryList = simpleAtomLinkToData.getAtomActualDataEntryList();
                for (AtomActualDataEntry atomActualDataEntry : atomActualDataEntryList) {
                    if ((atomActualDataEntry.getSuccessfullyDownloaded() != null) && atomActualDataEntry.getSuccessfullyDownloaded()) {
                        for (AtomDataRequest atomDataRequest : atomActualDataEntry.getAtomDataRequestList()) {
                            layerLink = atomDataRequest.getFinalURL();
                            break;
                        }
                        break;
                    }
                }

            }

            List<ServiceDocumentLink> serviceDocumentLinks = serviceDocumentLinkRepo.findByLinkCheckJobIdAndSha2(linkCheckJobId, capabilitiesSha2);
            for (ServiceDocumentLink serviceDocumentLink : serviceDocumentLinks) {
                ServiceMetadataRecord serviceMetadataRecord = serviceDocumentLink.getLocalServiceMetadataRecord();

                if (serviceMetadataRecord != null) {
                    associatedServiceIds.add(serviceMetadataRecord.getFileIdentifier());
                }
            }

            LinkedResourceIndicator indicator = new LinkedResourceIndicator(layerName, layerLink, associatedServiceIds);
            downloadServiceLinkedResourceIndicators.add(indicator);
        });

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            addOrUpdateIndicator(metadata, "INDICATOR_DOWNLOAD_SERVICE_LINKED_RESOURCES",
                    objectMapper.writeValueAsString(downloadServiceLinkedResourceIndicators));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
