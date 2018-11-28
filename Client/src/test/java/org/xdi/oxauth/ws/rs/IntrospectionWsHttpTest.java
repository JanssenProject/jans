/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.BaseRequest;
import org.xdi.oxauth.client.service.ClientFactory;
import org.xdi.oxauth.client.service.IntrospectionService;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxauth.model.uma.wrapper.Token;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */

public class IntrospectionWsHttpTest extends BaseTest {

    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void bearer(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token authorization = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        final Token tokenToIntrospect = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint);
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Bearer " + authorization.getAccessToken(), tokenToIntrospect.getAccessToken());
        Assert.assertTrue(introspectionResponse != null && introspectionResponse.isActive());
    }

    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void basicAuthentication(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token tokenToIntrospect = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientExecutor(true));

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, clientExecutor(true));
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Basic " + BaseRequest.getEncodedCredentials(umaPatClientId, umaPatClientSecret), tokenToIntrospect.getAccessToken());
        Assert.assertTrue(introspectionResponse != null && introspectionResponse.isActive());
    }

    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void introspectWithValidAuthorizationButInvalidTokenShouldReturnActiveFalse(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token authorization = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientExecutor(true));

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, clientExecutor(true));
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Bearer " + authorization.getAccessToken(), "invalid_token");
        assertNotNull(introspectionResponse);
        assertFalse(introspectionResponse.isActive());
    }

}
