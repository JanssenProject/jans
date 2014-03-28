package org.xdi.oxauth.interop;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

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