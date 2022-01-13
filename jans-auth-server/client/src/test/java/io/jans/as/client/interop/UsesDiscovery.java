/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.BaseTest;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

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