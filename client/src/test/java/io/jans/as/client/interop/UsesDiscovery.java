/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import static org.testng.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import io.jans.as.client.BaseTest;
import org.testng.annotations.Test;

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