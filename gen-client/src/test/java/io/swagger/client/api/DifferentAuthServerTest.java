package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.*;
import org.apache.commons.lang.StringUtils;
import io.jans.ca.common.CoreUtils;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.swagger.client.api.Tester.api;
import static io.swagger.client.api.Tester.getAuthorization;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertTrue;

//Set `protect_commands_with_access_token` field to true in oxd-server.yml file
public class DifferentAuthServerTest {

    @Parameters({"opHost", "redirectUrls", "authServer", "userId", "userSecret"})
    @Test
    public void getUserInfo_withDifferentAuthServer(String opHost, String redirectUrls, String authServer, String userId, String userSecret) throws Exception {
        final DevelopersApi client = api();

        final io.swagger.client.model.RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final io.swagger.client.model.RegisterSiteResponse authServerResp = RegisterSiteTest.registerSite(client, authServer, redirectUrls);
        final GetTokensByCodeResponse tokens = requestTokens(client, opHost, site, authServerResp, userId, userSecret, site.getClientId(), redirectUrls);

        final io.swagger.client.model.GetUserInfoParams params = new GetUserInfoParams();
        params.setOxdId(site.getOxdId());
        params.setAccessToken(tokens.getAccessToken());
        params.setIdToken(tokens.getIdToken());

        final Map<String, Object> resp = client.getUserInfo(params, getAuthorization(authServerResp), authServerResp.getOxdId());
        Assert.assertNotNull(resp);
        assertFalse(resp.isEmpty());
        Assert.assertNotNull(resp.get("sub"));
    }

    @Parameters({"redirectUrls", "opHost", "authServer", "rsProtect"})
    @Test
    public void umaFullTest_withDifferentAuthServer(String redirectUrls, String opHost, String authServer, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final RegisterSiteResponse authServerResp = RegisterSiteTest.registerSite(client, authServer, redirectUrls);

        protectResources(client, site, authServerResp, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = checkAccess(client, site, authServerResp, null);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final UmaRpGetRptResponse response = client.umaRpGetRpt(params, getAuthorization(authServerResp), authServerResp.getOxdId());

        Assert.assertNotNull(response);

        assertTrue(isNotBlank(response.getAccessToken()));
        assertTrue(isNotBlank(response.getPct()));
    }

    public static UmaRsProtectResponse protectResources(DevelopersApi client, RegisterSiteResponse site, RegisterSiteResponse authServerResp, List<RsResource> resources) throws Exception {
        final UmaRsProtectParams params = new UmaRsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);

        final UmaRsProtectResponse resp = client.umaRsProtect(params, getAuthorization(authServerResp), authServerResp.getOxdId());
        assertNotNull(resp);
        return resp;
    }

    public static UmaRsCheckAccessResponse checkAccess(DevelopersApi client, RegisterSiteResponse site, RegisterSiteResponse authServer, List<String> scopeList) throws Exception {
        final UmaRsCheckAccessParams params = new UmaRsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");
        params.setScopes(scopeList);

        final ApiResponse<UmaRsCheckAccessResponse> apiResp = client.umaRsCheckAccessWithHttpInfo(params, getAuthorization(authServer), authServer.getOxdId());

        assertEquals(apiResp.getStatusCode(), 200)  ;  //fixme should be 401
        assertNotNull(apiResp.getData());
        assertTrue(StringUtils.isNotBlank(apiResp.getData().getAccess()));

        return apiResp.getData();
    }

    private GetTokensByCodeResponse requestTokens(DevelopersApi client, String opHost, io.swagger.client.model.RegisterSiteResponse site, io.swagger.client.model.RegisterSiteResponse authServer, String userId, String userSecret, String clientId, String redirectUrls) throws Exception {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();

        final io.swagger.client.model.GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, opHost, site.getOxdId(), userId, userSecret, clientId, redirectUrls, state, nonce, getAuthorization(site)));
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokensByCode(params, getAuthorization(authServer), authServer.getOxdId());
        Assert.assertNotNull(resp);
        Tester.notEmpty(resp.getAccessToken());
        Tester.notEmpty(resp.getIdToken());
        return resp;
    }
}
