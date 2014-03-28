package org.xdi.oxauth.interop;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

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