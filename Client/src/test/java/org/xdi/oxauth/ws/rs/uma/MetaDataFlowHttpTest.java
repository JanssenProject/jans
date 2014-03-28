package org.xdi.oxauth.ws.rs.uma;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.MetaDataConfigurationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
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

        MetaDataConfigurationService metaDataConfigurationService = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl);

        // Get meta data configuration
        MetadataConfiguration c = null;
        try {
            c = metaDataConfigurationService.getMetadataConfiguration();
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(c);
    }

}