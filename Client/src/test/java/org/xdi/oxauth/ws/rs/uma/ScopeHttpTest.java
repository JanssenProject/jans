/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.uma.UmaScopeService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaScopeDescription;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public class ScopeHttpTest {

    @Test
    @Parameters({"umaMetaDataUrl"})
    public void scopePresence(final String umaMetaDataUrl) {
        final UmaConfiguration conf = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
        final UmaScopeService scopeService = UmaClientFactory.instance().createScopeService(conf.getScopeEndpoint());
        final UmaScopeDescription modifyScope = scopeService.getScope("modify");
        UmaTestUtil.assert_(modifyScope);
    }
}
