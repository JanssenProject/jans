package org.xdi.oxauth.ws.rs.uma;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.uma.ScopeService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.ScopeDescription;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public class ScopeHttpTest {

    @Test
    @Parameters({"umaMetaDataUrl"})
    public void scopePresence(final String umaMetaDataUrl) {
        final MetadataConfiguration conf = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
        final ScopeService scopeService = UmaClientFactory.instance().createScopeService(conf);
        final ScopeDescription modifyScope = scopeService.getScope("modify");
        UmaTestUtil.assert_(modifyScope);
    }
}
