package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.GetTokensByCodeParams;
import io.swagger.client.model.GetTokensByCodeResponse;
import io.swagger.client.model.GetUserInfoParams;
import io.swagger.client.model.RegisterSiteResponse;
import io.jans.ca.common.CoreUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Map;

import static io.swagger.client.api.Tester.*;
import static org.testng.Assert.*;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 * @version 10/25/2018
 */

public class GetUserInfoTest {

    @Parameters({"opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void test(String opHost, String redirectUrls, String userId, String userSecret) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final GetTokensByCodeResponse tokens = requestTokens(client, opHost, site, userId, userSecret, site.getClientId(), redirectUrls);

        final GetUserInfoParams params = new GetUserInfoParams();
        params.setRpId(site.getRpId());
        params.setAccessToken(tokens.getAccessToken());
        params.setIdToken(tokens.getIdToken());

        final Map<String, Object> resp = client.getUserInfo(params, getAuthorization(site), null);
        assertNotNull(resp);
        assertFalse(resp.isEmpty());
        assertNotNull(resp.get("sub"));
    }

    @Parameters({"opHost", "redirectUrls"})
    @Test(enabled = false)
    public void testWithInvalidToken(String opHost, String redirectUrls) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        final GetUserInfoParams params = new GetUserInfoParams();
        params.setRpId(site.getRpId());
        params.setAccessToken("blahBlah"); // invalid token

        final ApiResponse<Map<String, Object>> apiResponse = client.getUserInfoWithHttpInfo(params, getAuthorization(site), null);
        assertEquals(apiResponse.getStatusCode(), 200); // fixme should be 401

        assertNotNull(apiResponse.getData());
        assertNull(apiResponse.getData().get("sub"));
    }

    private GetTokensByCodeResponse requestTokens(DevelopersApi client, String opHost, RegisterSiteResponse site, String userId, String userSecret, String clientId, String redirectUrls) throws Exception {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setRpId(site.getRpId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, opHost, site.getRpId(), userId, userSecret, clientId, redirectUrls, state, nonce, getAuthorization(site)));
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokensByCode(params, getAuthorization(site), null);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}
