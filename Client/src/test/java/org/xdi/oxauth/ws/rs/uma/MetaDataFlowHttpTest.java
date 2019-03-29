/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaMetadataService;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.UmaTestUtil;

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

        UmaMetadataService metaDataConfigurationService = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientExecutor(true));

        // Get meta data
        UmaMetadata c = null;
        try {
            c = metaDataConfigurationService.getMetadata();
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(c);
    }

}