package io.jans.ca.server.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import io.jans.as.model.common.GrantType;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.params.GetTokensByCodeParams;
import io.jans.ca.common.params.GetUserInfoParams;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

//Set `protect_commands_with_access_token` field to true in client-api-server.yml file
public class DifferentAuthServerTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "otherAuthServer", "redirectUrls", "clientId", "clientSecret", "userId", "userSecret", "opConfigurationEndpoint"})
    @Test
    public void getUserInfo_withDifferentAuthServer(String host, String opHost, String otherAuthServer, String redirectUrls, String clientId, String clientSecret, String userId, String userSecret, String opConfigurationEndpoint) {

        ClientInterface client = getClientInterface(url);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        RegisterSiteResponse authServerResp = RegisterSiteTest.registerSite(client, otherAuthServer, redirectUrls, redirectUrls, "", opConfigurationEndpoint);

        final GetTokensByCodeResponse2 tokens = requestTokens(client, opHost, site, authServerResp, userId, userSecret, site.getClientId(), redirectUrls);

        GetUserInfoParams params = new GetUserInfoParams();
        params.setRpId(site.getRpId());
        params.setAccessToken(tokens.getAccessToken());
        params.setIdToken(tokens.getIdToken());

        final JsonNode resp = client.getUserInfo(Tester.getAuthorization(client.getApitargetURL(), authServerResp), authServerResp.getRpId(), params);
        assertNotNull(resp);
        assertNotNull(resp.get("sub"));
    }

    @Parameters({"host", "otherAuthServer", "redirectUrls", "opHost", "rsProtect", "opConfigurationEndpoint"})
    @Test
    public void umaFullTest_withDifferentAuthServer(String host, String otherAuthServer, String redirectUrls, String opHost, String rsProtect, String opConfigurationEndpoint) throws Exception {

        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        RegisterSiteResponse authServerResp = RegisterSiteTest.registerSite(client, otherAuthServer, redirectUrls, redirectUrls, "", opConfigurationEndpoint);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetRptParams params = new RpGetRptParams();
        params.setRpId(site.getRpId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(client.getApitargetURL(), authServerResp), authServerResp.getRpId(), params);

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

        final GetTokensByCodeResponse2 resp = client.getTokenByCode(Tester.getAuthorization(client.getApitargetURL(), authServer), authServer.getRpId(), params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}
