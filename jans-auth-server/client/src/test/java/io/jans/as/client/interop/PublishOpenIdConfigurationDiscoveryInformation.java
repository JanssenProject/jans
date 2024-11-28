/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.BaseTest;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * OC5:FeatureTest-Publish openid-configuration Discovery Information
 *
 * @author Javier Rojas Blum
 * @version 0.9, 06/09/2014
 */
public class PublishOpenIdConfigurationDiscoveryInformation extends BaseTest {

    @Test
    public void publishOpenIdConfigurationDiscoveryInformation() {
        showTitle("OC5:FeatureTest-Publish openid-configuration Discovery Information");

        assertTrue(StringUtils.isNotBlank(authorizationEndpoint));
        assertTrue(StringUtils.isNotBlank(tokenEndpoint));
        assertTrue(StringUtils.isNotBlank(userInfoEndpoint));
        assertTrue(StringUtils.isNotBlank(checkSessionIFrame));
        assertTrue(StringUtils.isNotBlank(endSessionEndpoint));
        assertTrue(StringUtils.isNotBlank(jwksUri));
        assertTrue(StringUtils.isNotBlank(registrationEndpoint));
    }
}