package com.geocat.ingester.service;

import com.geocat.ingester.dao.metadata.HarvestingSettingRepository;
import com.geocat.ingester.dao.metadata.MetadataRepository;
import com.geocat.ingester.dao.metadata.OperationAllowedRepository;
import com.geocat.ingester.model.harvester.MetadataRecordXml;
import com.geocat.ingester.model.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
//@Transactional("metadataTransactionManager")
public class CatalogueService {
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    Logger log = LoggerFactory.getLogger(CatalogueService.class);

    @Autowired
    private MetadataRepository metadataRepo;

    @Autowired
    private OperationAllowedRepository operationAllowedRepo;

    @Autowired
    private HarvestingSettingRepository harvestingSettingRepo;


    public boolean indicatorsChanged(Map<String, String> indicators_ingesting, Set<MetadataIndicator> indicators_existing_set) {
        if (indicators_existing_set == null)
            indicators_existing_set = new HashSet<>();
        if (indicators_ingesting == null)
            indicators_ingesting = new HashMap<>();

        if (indicators_ingesting.size() != indicators_existing_set.size())
            return true; //obviously not the same!

        for (MetadataIndicator existing_indicator : indicators_existing_set) {
            String ingestingVal = indicators_ingesting.get(existing_indicator.getName());
            String existingVal = existing_indicator.getValue();
            if ((ingestingVal == null) && (existingVal != null))
                return true; // null and not-null
            if ((ingestingVal != null) && (existingVal == null))
                return true; // null and not-null
            if ((ingestingVal == null) && (existingVal == null))
                continue; //both null
            if (!ingestingVal.equals(existingVal))
                return true;
        }
        return false;
    }

    /**
     * Creates/updates a metadata record.
     *
     * @param metadataRecords
     * @param harvesterConfiguration
     * @param jobId
     * @return
     * @throws Exception
     */
    /*public List<Integer> addOrUpdateMetadataRecords(List<MetadataRecordXml> metadataRecords,
                                                    HarvesterConfiguration harvesterConfiguration,
                                                    String jobId) throws Exception {*/
    public Map<String, Boolean> addOrUpdateMetadataRecords(List<MetadataRecordXml> metadataRecords,
                                                           HarvesterConfiguration harvesterConfiguration,
                                                           String jobId) throws Exception {
        List<Integer> metadataIdList = new ArrayList<>();
        Map<String, Boolean> metadataUuidList = new HashMap<>();
        List<Metadata> metadataList = new ArrayList<>();
        List<OperationAllowed> operationAllowedList = new ArrayList<>();

        Set<String> harvestedUuids = new HashSet<>();

        metadataRecords.forEach(r -> {
            harvestedUuids.add(r.getRecordIdentifier());
        });

        // Load in memory the records from the harvester that are in the catalogue database
        List<Metadata> metadataInDb = metadataRepo.findAllByUuidIn(harvestedUuids);

        for (MetadataRecordXml metadataRecord : metadataRecords) {
            String metadataUuid = metadataRecord.getRecordIdentifier();

            LocalDateTime datetime = LocalDateTime.now();
            Metadata metadata;

            Optional<Metadata> metadataOptional = metadataInDb.stream().filter(m -> m.getUuid().equals(metadataUuid)).findFirst();

            if (metadataOptional.isPresent()) {
                metadata = metadataOptional.get();

                String sha2 = computeSHA2(metadata.getData());
                if (sha2.equalsIgnoreCase(metadataRecord.getSha2()) && !indicatorsChanged(metadataRecord.getIndicators(), metadata.getIndicators())) {
                    // Don't process the record, it doesn't have changes
                    metadataUuidList.put(metadataUuid, Boolean.FALSE);
                    continue;
                }
                // TODO: Check about harvester uuid
            } else {
                metadata = new Metadata();
                metadata.setUuid(metadataRecord.getRecordIdentifier());
                metadata.setHarvestUuid(harvesterConfiguration.getUuid());
                metadata.setSource(harvesterConfiguration.getUuid());
                metadata.setCreateDate(datetime.format(DATE_PATTERN));

                metadata.setOwner(Integer.parseInt(harvesterConfiguration.getUserOwner()));
                metadata.setGroupOwner(Integer.parseInt(harvesterConfiguration.getGroupOwner()));
            }

            String metadataXml = metadataRecord.getTextValue();
            metadata.setData(metadataXml);

            // TODO: Get from XML
            metadata.setChangeDate(datetime.format(DATE_PATTERN));

            metadata.getIndicators().clear();

            metadataRecord.getIndicators().forEach((k, v) -> {
                MetadataIndicator metadataIndicator = new MetadataIndicator();
                metadataIndicator.setName(k);
                metadataIndicator.setValue(v);
                metadata.getIndicators().add(metadataIndicator);

            });

            int pos = metadataList.indexOf(metadata);

            if (pos > -1) {
                metadataList.set(pos, metadata);
            } else {
                metadataList.add(metadata);
            }
        }

        metadataRepo.saveAll(metadataList);

        metadataList.forEach(metadata -> {
            metadataIdList.add(metadata.getId());
            metadataUuidList.put(metadata.getUuid(), Boolean.TRUE);
        });

        List<OperationAllowed> operationAllowedInDb =
                operationAllowedRepo.findOperationAllowedByMetadataIds(new HashSet<>(metadataIdList));

        metadataList.forEach(metadata -> {
            Map<Integer, List<Integer>> metadataPrivileges = harvesterConfiguration.getPrivileges();

            for (Map.Entry<Integer, List<Integer>> entry : metadataPrivileges.entrySet()) {

                for (Integer op : entry.getValue()) {
                    OperationAllowedId operationAllowedId = new OperationAllowedId();
                    operationAllowedId.setMetadataId(metadata.getId());
                    // Group
                    operationAllowedId.setGroupId(entry.getKey());
                    // Operation
                    operationAllowedId.setOperationId(op);

                    boolean addOperation = false;

                    Optional<OperationAllowed> operationAllowedOptional = operationAllowedInDb.stream().filter(oa -> (oa.getId().equals(operationAllowedId))).findFirst();

                    if (!operationAllowedOptional.isPresent()) {
                        addOperation = true;
                    }

                    if (addOperation) {
                        OperationAllowed operationAllowed = new OperationAllowed();
                        operationAllowed.setId(operationAllowedId);
                        operationAllowedList.add(operationAllowed);
                    }
                }

            }
        });

        operationAllowedRepo.saveAll(operationAllowedList);

        metadataRepo.flush();

        return metadataUuidList;
    }


    public Optional<HarvesterConfiguration> retrieveHarvesterConfiguration(String harvesterUuidOrName) {
        Optional<HarvesterSetting> harvesterSettingOptional = harvestingSettingRepo.findByNameAndValue("uuid", harvesterUuidOrName);

        if (!harvesterSettingOptional.isPresent()) {
            harvesterSettingOptional = harvestingSettingRepo.findByNameAndValue("name", harvesterUuidOrName);
        }

        HarvesterConfiguration harvesterConfiguration = null;

        if (harvesterSettingOptional.isPresent()) {
            harvesterConfiguration = new HarvesterConfiguration();

            HarvesterSetting harvesterSetting = harvesterSettingOptional.get();
            HarvesterSetting harvesterSettingParent = harvesterSetting.getParent();

            List<HarvesterSetting> harvesterSettingList = harvestingSettingRepo.findAllByParent(harvesterSettingParent);

            Optional<HarvesterSetting> harvestingSettingUuid = retrieveHarvesterSetting(harvesterSettingList, "uuid");
            harvesterConfiguration.setUuid(harvestingSettingUuid.get().getValue());

            Optional<HarvesterSetting> harvestingSettingName = retrieveHarvesterSetting(harvesterSettingList, "name");
            harvesterConfiguration.setName(harvestingSettingName.get().getValue());

            Optional<HarvesterSetting> harvestingSettingUrl = retrieveHarvesterSetting(harvesterSettingList, "capabUrl");
            harvesterConfiguration.setUrl(harvestingSettingUrl.get().getValue());

            Optional<HarvesterSetting> harvestingSettingUserId = retrieveHarvesterSetting(harvesterSettingList, "ownerUser");
            harvesterConfiguration.setUserOwner(harvestingSettingUserId.get().getValue());

            Optional<HarvesterSetting> harvestingSettingGroupId = retrieveHarvesterSetting(harvesterSettingList, "ownerGroup");
            harvesterConfiguration.setGroupOwner(harvestingSettingGroupId.get().getValue());

            // Retrieve the harvester privileges settings
            HarvesterSetting harvesterSettingParentParent = harvesterSettingParent.getParent();
            List<HarvesterSetting> harvesterSettingParentList = harvestingSettingRepo.findAllByParent(harvesterSettingParentParent);


            Optional<HarvesterSetting> harvesterSettingPrivileges = retrieveHarvesterSetting(harvesterSettingParentList, "privileges");

            if (harvesterSettingPrivileges.isPresent()) {
                // Get the groups in the privileges list
                List<HarvesterSetting> harvesterSettingPrivilegesList = harvestingSettingRepo.findAllByParent(harvesterSettingPrivileges.get());

                Map<Integer, List<Integer>> privileges = new HashMap<>();

                List<HarvesterSetting> privilegesGroups = retrieveHarvesterSettingsByParentIdAndName(harvesterSettingPrivilegesList, harvesterSettingPrivileges.get().getId(), "group");

                // For each group in the privileges list process the operations associated
                for (HarvesterSetting g : privilegesGroups) {
                    List<HarvesterSetting> privilegesGroupsList = harvestingSettingRepo.findAllByParent(g);

                    List<HarvesterSetting> operationsGroupOperations = retrieveHarvesterSettingsByParentIdAndName(privilegesGroupsList, g.getId(), "operation");
                    List<Integer> ops = new ArrayList<>();

                    for (HarvesterSetting op : operationsGroupOperations) {
                        ops.add(Integer.parseInt(op.getValue()));
                    }

                    Integer groupId = Integer.parseInt(g.getValue());
                    privileges.put(groupId, ops);

                }

                harvesterConfiguration.setPrivileges(privileges);
            }

            // TODO: Process CSW filter
            harvesterConfiguration.setCswFilter("");
        }

        return Optional.ofNullable(harvesterConfiguration);
    }


    public List<String> retrieveLocalUuidsForHarvester(String harvesterUuid) {
        return metadataRepo.findAllUuidsBySource(harvesterUuid);
    }

    public void deleteMetadata(Set<Integer> metadataIds) {
        metadataRepo.deleteAllByIdIn(metadataIds);
    }

    public void deleteMetadataByUuids(Set<String> metadataUuids) {
        for (String metadataUuid : metadataUuids) {

            Optional<Metadata> metadata = metadataRepo.findMetadataByUuid(metadataUuid);

            if (metadata.isPresent()) {
                List<OperationAllowed> operationAllowedList = operationAllowedRepo.findAllByMetadataId(metadata.get().getId());
                operationAllowedRepo.deleteAll(operationAllowedList);

                metadataRepo.delete(metadata.get());
            }
        }
    }

    private Optional<HarvesterSetting> retrieveHarvesterSetting(List<HarvesterSetting> harvesterSettingList, String name) {
        return harvesterSettingList.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    private List<HarvesterSetting> retrieveHarvesterSettingsByParentIdAndName(List<HarvesterSetting> harvesterSettingList, Integer id, String name) {
        return harvesterSettingList.stream().filter(s -> s.getParent().getId() == id && s.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
    }


    private String computeSHA2(String xml) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(xml.getBytes(StandardCharsets.UTF_8));
        String hexHash = javax.xml.bind.DatatypeConverter.printHexBinary(hash);
        return hexHash;
    }
}
