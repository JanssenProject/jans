/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import java.net.URI;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/03/2013
 */

public class UmaConfigurationWSTest extends BaseTest {

	@ArquillianResource
    private URI url;

    @Parameters({"umaConfigurationPath"})
    @Test
    public void configurationPresence(final String umaConfigurationPath) throws Exception {
        final UmaConfiguration c = TUma.requestConfiguration(url, umaConfigurationPath);
        UmaTestUtil.assert_(c);
    }
}
