/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import static org.testng.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

/**
 * OC5:FeatureTest-Uses Discovery
 *
 * @author Javier Rojas Blum Date: 09.02.2013
 */
public class UsesDiscovery extends BaseTest {

    @Test
    public void usesDiscovery() {
        showTitle("OC5:FeatureTest-Uses Discovery");

        assertTrue(StringUtils.isNotBlank(authorizationEndpoint));
        assertTrue(StringUtils.isNotBlank(tokenEndpoint));
        assertTrue(StringUtils.isNotBlank(userInfoEndpoint));
        assertTrue(StringUtils.isNotBlank(checkSessionIFrame));
        assertTrue(StringUtils.isNotBlank(endSessionEndpoint));
        assertTrue(StringUtils.isNotBlank(jwksUri));
        assertTrue(StringUtils.isNotBlank(registrationEndpoint));
    }
}