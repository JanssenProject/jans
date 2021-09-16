/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseRequest;
import io.jans.as.client.BaseTest;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.uma.wrapper.Token;
import org.jboss.resteasy.client.ClientExecutor;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
        assertTrue(introspectionResponse != null && introspectionResponse.isActive());
    }

    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void bearerWithResponseAsJwt(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final ClientExecutor clientExecutor = clientExecutor(true);
        final Token authorization = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientExecutor);
        final Token tokenToIntrospect = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientExecutor);

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, clientExecutor);
        final String jwtAsString = introspectionService.introspectTokenWithResponseAsJwt("Bearer " + authorization.getAccessToken(), tokenToIntrospect.getAccessToken(), true);
        final Jwt jwt = Jwt.parse(jwtAsString);
        assertTrue(Boolean.parseBoolean(jwt.getClaims().getClaimAsString("active")));
    }

    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void basicAuthentication(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        final Token tokenToIntrospect = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientExecutor(true));

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, clientExecutor(true));
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Basic " + BaseRequest.getEncodedCredentials(umaPatClientId, umaPatClientSecret), tokenToIntrospect.getAccessToken());
        assertTrue(introspectionResponse != null && introspectionResponse.isActive());
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
