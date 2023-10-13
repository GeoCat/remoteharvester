package geocat.csw;


import java.util.List;

public class CSWMetadata {
    public String getRecordsUrl;
    public List<List<String>> nestedGetCapUrls;
    private String harvesterId;
    private long endpointId;
    private int numberOfExpectedRecords;
    private boolean lookForNestedDiscoveryService;
    private String filter;

    private String identifierFieldName;

    public CSWMetadata(String harvesterId,
                       long endpointId,
                       int numberOfExpectedRecords,
                       String getRecordsUrl,
                       List<List<String>> nestedGetCapUrls,
                       String filter,
                       boolean lookForNestedDiscoveryService,
                       String identifierFieldName) {
        this.harvesterId = harvesterId;
        this.endpointId = endpointId;
        this.numberOfExpectedRecords = numberOfExpectedRecords;
        this.getRecordsUrl = getRecordsUrl;
        this.nestedGetCapUrls = nestedGetCapUrls;
        this.filter = filter;
        this.lookForNestedDiscoveryService = lookForNestedDiscoveryService;
        this.identifierFieldName = identifierFieldName;
    }

    public boolean isLookForNestedDiscoveryService() {
        return lookForNestedDiscoveryService;
    }

    public void setLookForNestedDiscoveryService(boolean lookForNestedDiscoveryService) {
        this.lookForNestedDiscoveryService = lookForNestedDiscoveryService;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int getNumberOfExpectedRecords() {
        return numberOfExpectedRecords;
    }

    public void setNumberOfExpectedRecords(int numberOfExpectedRecords) {
        this.numberOfExpectedRecords = numberOfExpectedRecords;
    }

    public String getGetRecordsUrl() {
        return getRecordsUrl;
    }

    public void setGetRecordsUrl(String getRecordsUrl) {
        this.getRecordsUrl = getRecordsUrl;
    }

    public List<List<String>> getNestedGetCapUrls() {
        return nestedGetCapUrls;
    }

    public void setNestedGetCapUrls(List<List<String>> nestedGetCapUrls) {
        this.nestedGetCapUrls = nestedGetCapUrls;
    }

    public String getHarvesterId() {
        return harvesterId;
    }

    public void setHarvesterId(String harvesterId) {
        this.harvesterId = harvesterId;
    }

    public long getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(long endpointId) {
        this.endpointId = endpointId;
    }

    public String getIdentifierFieldName() {
        return identifierFieldName;
    }

    public void setIdentifierFieldName(String identifierFieldName) {
        this.identifierFieldName = identifierFieldName;
    }
}
