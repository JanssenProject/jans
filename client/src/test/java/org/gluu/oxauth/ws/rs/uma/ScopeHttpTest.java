/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package org.gluu.oxauth.ws.rs.uma;

import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaScopeService;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaScopeDescription;
import org.gluu.oxauth.model.uma.UmaTestUtil;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public class ScopeHttpTest {

    @Test
    @Parameters({"umaMetaDataUrl"})
    public void scopePresence(final String umaMetaDataUrl) {
        final UmaMetadata metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl).getMetadata();
        final UmaScopeService scopeService = UmaClientFactory.instance().createScopeService(metadata.getScopeEndpoint());
        final UmaScopeDescription modifyScope = scopeService.getScope("modify");
        UmaTestUtil.assert_(modifyScope);
    }
}
