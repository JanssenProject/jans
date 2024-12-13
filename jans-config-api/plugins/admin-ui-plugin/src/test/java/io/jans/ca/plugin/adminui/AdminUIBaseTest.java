/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui;

import io.jans.as.model.common.GrantType;
import io.jans.configapi.core.test.BaseTest;

import org.testng.annotations.BeforeMethod;

public class AdminUIBaseTest extends BaseTest{

    //AdminUI specific code
    
    @BeforeMethod
    @Override
    public void getAccessToken() throws Exception {
        log.info("AdminUI - getAccessToken - propertiesMap:{}", propertiesMap);

        String tokenUrl = propertiesMap.get("test.authzurl");
        String strGrantType = propertiesMap.get("test.grant.type");
        String clientId = propertiesMap.get("test.client.id");
        String clientSecret = propertiesMap.get("test.client.secret");
        String scopes = propertiesMap.get("test.scopes");

        GrantType grantType = GrantType.fromString(strGrantType);
        this.accessToken = getToken(tokenUrl, clientId, clientSecret, grantType, scopes);
        log.info("\n\n\n\n AdminUI- accessToken:{}", accessToken);
    }
	
    
}
