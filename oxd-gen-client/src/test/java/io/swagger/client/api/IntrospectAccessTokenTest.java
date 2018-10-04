package io.swagger.client.api;

import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponse;
import io.swagger.client.model.GetClientTokenResponseData;
import io.swagger.client.model.IntrospectAccessTokenParams;
import io.swagger.client.model.IntrospectAccessTokenResponse;
import io.swagger.client.model.IntrospectAccessTokenResponseData;
import io.swagger.client.model.RegisterSiteResponseData;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static junit.framework.Assert.*;

/**
 * @author yuriyz
 * @author Shoeb
 */
public class IntrospectAccessTokenTest extends BaseTestCase {

    @Parameters({"opHost", "redirectUrl"})
    @Test
    public void introspectAccessToken(String opHost, String redirectUrl) throws Exception {
        DevelopersApi client = Tester.api();
        RegisterSiteResponseData setupResponse = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        GetClientTokenResponseData tokenResponse = getGetClientTokenResponseData(opHost, client, setupResponse);
        assertNotNull(tokenResponse);
        final String accessToken = tokenResponse.getAccessToken();
        Tester.notEmpty(accessToken);
        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getOxdId());
        introspectParams.setAccessToken(accessToken);
        //
        final String authorization = "Bearer " + accessToken;
        final IntrospectAccessTokenResponse iaTokenResponse = client.introspectAccessToken(authorization, introspectParams);
        assertNotNull(iaTokenResponse.getData());
        assertTrue(iaTokenResponse.getData().isActive());
        assertNotNull(iaTokenResponse.getData().getIat());
        assertNotNull(iaTokenResponse.getData().getExp());
        assertTrue(iaTokenResponse.getData().getExp() >= iaTokenResponse.getData().getIat());
        final Long nbf = iaTokenResponse.getData().getNbf();
        if (nbf != null) {
            assertTrue(nbf > iaTokenResponse.getData().getIat());
            assertTrue(nbf < iaTokenResponse.getData().getExp());
        }
    }

    /*
    According to open id spec, introspect access token API, for authorized request with an invalid
    token, should not throw an error but should return the client as inactive.
     */
    @Parameters({"opHost", "redirectUrl"})
    @Test
    public void testWithInvalidToken(String opHost, String redirectUrl) throws Exception {
        DevelopersApi client = Tester.api();
        RegisterSiteResponseData setupData = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        GetClientTokenResponseData tokenResponse = getGetClientTokenResponseData(opHost, client, setupData);
        assertNotNull(tokenResponse);
        final String accessToken = tokenResponse.getAccessToken();
        final String validHeader = "Bearer " + accessToken;
        final String invalidToken = accessToken.concat("BlahBlah");

        IntrospectAccessTokenParams iatParams = new IntrospectAccessTokenParams();
        iatParams.setAccessToken(invalidToken);
        iatParams.setOxdId(setupData.getOxdId());

        ApiResponse<IntrospectAccessTokenResponse>
                apiIatResponse = client.introspectAccessTokenWithHttpInfo(validHeader, iatParams);
        assertEquals(apiIatResponse.getStatusCode(), 200);
        assertNotNull(apiIatResponse.getData());

        IntrospectAccessTokenResponseData responseData = apiIatResponse.getData().getData();
        assertNotNull(responseData);
        assertFalse(responseData.isActive());
    }

    @Parameters({"opHost", "redirectUrl"})
    @Test
    @ProtectionAccessTokenRequired
    public void testWithInvalidAuthorization(String opHost, String redirectUrl) throws Exception {

        DevelopersApi client = Tester.api();
        RegisterSiteResponseData setupResponse = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        GetClientTokenResponseData tokenResponseData = this.getGetClientTokenResponseData(opHost, client, setupResponse);
        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setOxdId(setupResponse.getOxdId());
        introspectParams.setAccessToken(tokenResponseData.getAccessToken());

        final String invalidAuthString = "Bearer NotAuthorized";
        final ApiResponse<IntrospectAccessTokenResponse> introApiResponse = client.introspectAccessTokenWithHttpInfo(invalidAuthString, introspectParams);

        assertEquals(403,introApiResponse.getStatusCode());

        IntrospectAccessTokenResponseData responseData = introApiResponse.getData().getData();
        assertNotNull(responseData);
        assertNull(responseData.getClientId());

    }

    private static GetClientTokenResponseData getGetClientTokenResponseData(String opHost, DevelopersApi client,
                                                                            RegisterSiteResponseData setupResponse) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid","oxd"));
        params.setClientId(setupResponse.getClientId());
        params.setClientSecret(setupResponse.getClientSecret());

        final GetClientTokenResponse clientTokenResponse = client.getClientToken(params);
        assertNotNull(clientTokenResponse);
        return clientTokenResponse.getData();
    }
}
