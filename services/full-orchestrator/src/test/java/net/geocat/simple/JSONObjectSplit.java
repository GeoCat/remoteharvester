package net.geocat.simple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.geocat.model.HarvesterConfig;
import net.geocat.model.LinkCheckRunConfig;
import org.junit.Test;

import static org.junit.Assert.*;

public class JSONObjectSplit {


    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void t1() throws JsonProcessingException {
        String json = "{\"url\":\"https://rpi.gov.sk/rpi_csw/service.svc/get\",\"longTermTag\":\"SK-Slovakia\",\"lookForNestedDiscoveryService\":false," +
                "\"maxDataLinksToFollow\":111 " +
                "}";

        HarvesterConfig objHarvesterConfig = objectMapper.readValue(json, HarvesterConfig.class);
        assertNotNull(objHarvesterConfig);
        assertEquals("https://rpi.gov.sk/rpi_csw/service.svc/get", objHarvesterConfig.getUrl());
        assertEquals("SK-Slovakia", objHarvesterConfig.getLongTermTag());
        assertFalse(objHarvesterConfig.isLookForNestedDiscoveryService());

        LinkCheckRunConfig objLinkCheckRunConfig = objectMapper.readValue(json, LinkCheckRunConfig.class);
        assertNotNull(objLinkCheckRunConfig);
        assertEquals(111, objLinkCheckRunConfig.getMaxDataLinksToFollow().intValue());
        assertNull(objLinkCheckRunConfig.getMaxAtomEntriesToAttempt());
        assertNull(objLinkCheckRunConfig.isDeleteHTTPCacheWhenComplete());

        int t = 0;
    }
}
