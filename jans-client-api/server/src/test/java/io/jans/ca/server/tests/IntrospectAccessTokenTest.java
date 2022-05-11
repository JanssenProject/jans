package io.jans.ca.server.tests;

import com.google.common.collect.Lists;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetClientTokenParams;
import io.jans.ca.common.params.IntrospectAccessTokenParams;
import io.jans.ca.common.response.GetClientTokenResponse;
import io.jans.ca.common.response.IntrospectAccessTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls"})
    @Test
    public void introspectAccessToken(String host, String opHost, String redirectUrls) {

        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse setupResponse = SetupClientTest.setupClient(client, opHost, redirectUrls);

        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid", "jans_client_api"));
        params.setClientId(setupResponse.getClientId());
        params.setClientSecret(setupResponse.getClientSecret());

        GetClientTokenResponse tokenResponse = client.getClientToken(params);

        assertNotNull(tokenResponse);
        notEmpty(tokenResponse.getAccessToken());

        IntrospectAccessTokenParams introspectParams = new IntrospectAccessTokenParams();
        introspectParams.setRpId(setupResponse.getRpId());
        introspectParams.setAccessToken(tokenResponse.getAccessToken());

        IntrospectAccessTokenResponse introspectionResponse = client.introspectAccessToken("Bearer " + tokenResponse.getAccessToken(), null, introspectParams);

        assertNotNull(introspectionResponse);
        assertTrue(introspectionResponse.isActive());
        assertNotNull(introspectionResponse.getIssuedAt());
        assertNotNull(introspectionResponse.getExpiresAt());
        assertTrue(introspectionResponse.getExpiresAt() >= introspectionResponse.getIssuedAt());
    }
}
