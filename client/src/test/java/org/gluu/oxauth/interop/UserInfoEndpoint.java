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
 * OC5:FeatureTest-UserInfo Endpoint
 *
 * @author Javier Rojas Blum Date: 07.15.2013
 */
public class UserInfoEndpoint extends BaseTest {

    @Test
    public void userInfoEndpoint() {
        showTitle("OC5:FeatureTest-UserInfo Endpoint");

        assertTrue(StringUtils.isNotBlank(userInfoEndpoint));
    }
}