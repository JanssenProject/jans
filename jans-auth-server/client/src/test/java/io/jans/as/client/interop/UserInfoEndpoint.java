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