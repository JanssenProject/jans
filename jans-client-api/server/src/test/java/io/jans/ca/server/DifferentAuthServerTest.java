package io.jans.ca.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.params.GetTokensByCodeParams;
import io.jans.ca.common.params.GetUserInfoParams;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

//Set `protect_commands_with_access_token` field to true in client-api-server.yml file
public class DifferentAuthServerTest {

    @Parameters({"host", "opHost", "authServer", "redirectUrls", "clientId", "clientSecret", "userId", "userSecret"})
    @Test
    public void getUserInfo_withDifferentAuthServer(String host, String opHost, String authServer, String redirectUrls, String clientId, String clientSecret, String userId, String userSecret) {

        ClientInterface client = Tester.newClient(host);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        RegisterSiteResponse authServerResp = RegisterSiteTest.registerSite(client, authServer, redirectUrls);

        final GetTokensByCodeResponse2 tokens = requestTokens(client, opHost, site, authServerResp, userId, userSecret, site.getClientId(), redirectUrls);

        GetUserInfoParams params = new GetUserInfoParams();
        params.setRpId(site.getRpId());
        params.setAccessToken(tokens.getAccessToken());
        params.setIdToken(tokens.getIdToken());

        final JsonNode resp = client.getUserInfo(Tester.getAuthorization(authServerResp), authServerResp.getRpId(), params);
        assertNotNull(resp);
        assertNotNull(resp.get("sub"));
    }

    @Parameters({"host", "authServer", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void umaFullTest_withDifferentAuthServer(String host, String authServer, String redirectUrls, String opHost, String rsProtect) throws Exception {

        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        RegisterSiteResponse authServerResp = RegisterSiteTest.registerSite(client, authServer, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetRptParams params = new RpGetRptParams();
        params.setRpId(site.getRpId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(authServerResp), authServerResp.getRpId(), params);

        Assert.assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRpt()));
        assertTrue(StringUtils.isNotBlank(response.getPct()));
    }

    private GetTokensByCodeResponse2 requestTokens(ClientInterface client, String opHost, RegisterSiteResponse site, RegisterSiteResponse authServer, String userId, String userSecret, String clientId, String redirectUrls) {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();
        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setRpId(site.getRpId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, opHost, site, userId, userSecret, clientId, redirectUrls, state, nonce));
        params.setState(state);

        final GetTokensByCodeResponse2 resp = client.getTokenByCode(Tester.getAuthorization(authServer), authServer.getRpId(), params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}
