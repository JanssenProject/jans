package io.swagger.client.api;

import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.model.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static junit.framework.Assert.*;

/**
 * @author yuriyz
 * @author Shoeb
 */
public class IntrospectAccessTokenTest extends BaseTestCase {

    @Parameters({"opHost", "redirectUrls"})
    @Test
    public void introspectAccessToken(String opHost, String redirectUrls) throws Exception {
        DevelopersApi client = Tester.api();
        RegisterSiteResponse setupResponse = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        GetClientTokenResponse tokenResponse = getGetClientTokenResponseData(opHost, client, setupResponse);
        assertNotNull(tokenResponse);
        final String accessToken = tokenResponse.getAccessToken();
        Tester.notEmpty(accessToken);
        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getOxdId());
        introspectParams.setAccessToken(accessToken);
        //
        final String authorization = "Bearer " + accessToken;
        final IntrospectAccessTokenResponse iaTokenResponse = client.introspectAccessToken(authorization, introspectParams);
        assertNotNull(iaTokenResponse);
        assertTrue(iaTokenResponse.isActive());
        assertNotNull(iaTokenResponse.getIat());
        assertNotNull(iaTokenResponse.getExp());
        assertTrue(iaTokenResponse.getExp() >= iaTokenResponse.getIat());
        final Long nbf = iaTokenResponse.getNbf();
        if (nbf != null) {
            assertTrue(nbf > iaTokenResponse.getIat());
            assertTrue(nbf < iaTokenResponse.getExp());
        }
    }

    /*
    According to open id spec, introspect access token API, for authorized request with an invalid
    token, should not throw an error but should return the client as inactive.
     */
    @Parameters({"opHost", "redirectUrls"})
    @Test
    @ProtectionAccessTokenRequired
    public void testWithInvalidToken(String opHost, String redirectUrls) throws Exception {
        DevelopersApi client = Tester.api();
        RegisterSiteResponse setupData = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        GetClientTokenResponse tokenResponse = getGetClientTokenResponseData(opHost, client, setupData);
        assertNotNull(tokenResponse);
        final String accessToken = tokenResponse.getAccessToken();
        final String validHeader = "Bearer " + accessToken;
        final String invalidToken = accessToken.concat("BlahBlah");

        IntrospectAccessTokenParams iatParams = new IntrospectAccessTokenParams();
        iatParams.setAccessToken(invalidToken);
        iatParams.setOxdId(setupData.getOxdId());

        try {
            client.introspectAccessToken(validHeader, iatParams);
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
            return;
        }
        throw new AssertionError("Got response while we expect failure because there is not such token on the server.");

        // return back to it after oxd is switched to oxauth 3.1.5 or later, see https://github.com/GluuFederation/oxAuth/issues/929
//        ApiResponse<IntrospectAccessTokenResponse> apiIatResponse = client.introspectAccessTokenWithHttpInfo(validHeader, iatParams);
//        assertEquals(apiIatResponse.getStatusCode(), 200);
//        assertNotNull(apiIatResponse.getData());
//        // verify client is NOT active
//        assertFalse(apiIatResponse.getData().isActive());
    }

    @Parameters({"opHost", "redirectUrls"})
    @Test
    @ProtectionAccessTokenRequired
    public void testWithInvalidAuthorization(String opHost, String redirectUrls) throws Exception {

        DevelopersApi client = Tester.api();
        RegisterSiteResponse setupResponse = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        GetClientTokenResponse tokenResponseData = getGetClientTokenResponseData(opHost, client, setupResponse);
        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getOxdId());
        introspectParams.setAccessToken(tokenResponseData.getAccessToken());

        final String invalidAuthString = "Bearer NotAuthorized";
        final ApiResponse<IntrospectAccessTokenResponse> introApiResponse = client.introspectAccessTokenWithHttpInfo(invalidAuthString, introspectParams);

        assertEquals(403, introApiResponse.getStatusCode());
        assertNotNull(introApiResponse.getData());
        assertNull(introApiResponse.getData().getClientId());
    }

    private static GetClientTokenResponse getGetClientTokenResponseData(String opHost, DevelopersApi client,
                                                                        RegisterSiteResponse setupResponse) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid", "oxd"));
        params.setClientId(setupResponse.getClientId());
        params.setClientSecret(setupResponse.getClientSecret());

        final GetClientTokenResponse clientTokenResponse = client.getClientToken(params);
        assertNotNull(clientTokenResponse);
        return clientTokenResponse;
    }
}
