package org.xdi.oxauth.ws.rs;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.service.ClientFactory;
import org.xdi.oxauth.client.service.IntrospectionService;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */

public class IntrospectionWsHttpTest extends BaseTest {

    @Test
    @Parameters({"umaUserId", "umaUserSecret", "umaAatClientId", "umaAatClientSecret", "umaRedirectUri"})
    public void test(final String umaUserId, final String umaUserSecret,
                     final String umaAatClientId, final String umaAatClientSecret,
                     final String umaRedirectUri) throws Exception {

        final Token authorization = UmaClient.requestAat(authorizationEndpoint, tokenEndpoint, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);
        final Token tokenToIntrospect = UmaClient.requestPat(authorizationEndpoint, tokenEndpoint, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint);
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Bearer " + authorization.getAccessToken(), tokenToIntrospect.getAccessToken());
        Assert.assertTrue(introspectionResponse != null && introspectionResponse.isActive());
    }
}
