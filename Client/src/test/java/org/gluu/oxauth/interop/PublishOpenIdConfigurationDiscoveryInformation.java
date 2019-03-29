/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.interop;

import static org.testng.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.BaseTest;
import org.testng.annotations.Test;

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