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