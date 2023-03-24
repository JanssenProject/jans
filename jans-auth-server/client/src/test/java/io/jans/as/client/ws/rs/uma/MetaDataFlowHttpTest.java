/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.uma;

import io.jans.as.client.BaseTest;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.test.UmaTestUtil;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.ClientErrorException;

/**
 * Test cases for getting meta data configuration flow (HTTP)
 *
 * @author Yuriy Movchan Date: 11/05/2012
 */
public class MetaDataFlowHttpTest extends BaseTest {

    /**
     * Test for getting meta data configuration
     */
    @Test
    @Parameters({"umaMetaDataUrl"})
    public void testGetUmaMetaDataConfiguration(final String umaMetaDataUrl) throws Exception {
        showTitle("testGetUmaMetaDataConfiguration");

        UmaMetadataService metaDataConfigurationService = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientEngine(true));

        // Get meta data
        UmaMetadata c = null;
        try {
            c = metaDataConfigurationService.getMetadata();
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assertIt(c);
    }

}