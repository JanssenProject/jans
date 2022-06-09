package io.jans.ca.server.tests;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.params.GetTokensByCodeParams;
import io.jans.ca.common.params.GetUserInfoParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2015
 */

public class GetUserInfoTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final GetTokensByCodeResponse2 tokens = requestTokens(client, opHost, site, userId, userSecret, site.getClientId(), redirectUrls);

        GetUserInfoParams params = new GetUserInfoParams();
        params.setRpId(site.getRpId());
        params.setAccessToken(tokens.getAccessToken());
        params.setIdToken(tokens.getIdToken());

        final JsonNode resp = client.getUserInfo(Tester.getAuthorization(client.getApitargetURL(), site), null, params);
        assertNotNull(resp);
        assertNotNull(resp.get("sub"));
    }

    private GetTokensByCodeResponse2 requestTokens(ClientInterface client, String opHost, RegisterSiteResponse site, String userId, String userSecret, String clientId, String redirectUrls) {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();
        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setRpId(site.getRpId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, opHost, site, userId, userSecret, clientId, redirectUrls, state, nonce));
        params.setState(state);

        final GetTokensByCodeResponse2 resp = client.getTokenByCode(Tester.getAuthorization(client.getApitargetURL(), site), null, params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}
