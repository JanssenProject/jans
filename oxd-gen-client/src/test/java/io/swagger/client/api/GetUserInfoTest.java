package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.GetTokensByCodeParams;
import io.swagger.client.model.GetTokensByCodeResponse;
import io.swagger.client.model.GetUserInfoParams;
import io.swagger.client.model.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.CoreUtils;

import java.util.Map;

import static io.swagger.client.api.Tester.*;
import static org.testng.Assert.*;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 *
 * @version 10/25/2018
 */

public class GetUserInfoTest {

    @Parameters({"opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String opHost, String redirectUrl, String userId, String userSecret) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        final GetTokensByCodeResponse tokens = requestTokens(client, site, userId, userSecret);

        final GetUserInfoParams params = new GetUserInfoParams();
        params.setOxdId(site.getOxdId());
        params.setAccessToken(tokens.getAccessToken());

        final Map<String, Object> resp = client.getUserInfo(getAuthorization(site), params);
        assertNotNull(resp);
        assertFalse(resp.isEmpty());
        assertNotNull(resp.get("sub"));
    }

    @Parameters({"opHost", "redirectUrl"})
    @Test
    public void testWithInvalidToken(String opHost, String redirectUrl) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        final GetUserInfoParams params = new GetUserInfoParams();
        params.setOxdId(site.getOxdId());
        params.setAccessToken("blahBlah"); // invalid token

        final ApiResponse<Map<String, Object>> apiResponse = client.getUserInfoWithHttpInfo(getAuthorization(site), params);
        assertEquals(apiResponse.getStatusCode() , 200); // fixme should be 401

        assertNotNull(apiResponse.getData());
        assertNull(apiResponse.getData().get("sub"));
    }


    private GetTokensByCodeResponse requestTokens(DevelopersApi client, RegisterSiteResponse site, String userId, String userSecret) throws Exception {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce, getAuthorization(site)));
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokensByCode(getAuthorization(site), params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}
