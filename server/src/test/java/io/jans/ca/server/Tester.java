package io.jans.ca.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.RpClient;
import io.jans.ca.common.params.GetClientTokenParams;
import io.jans.ca.common.response.GetClientTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import org.apache.commons.lang.StringUtils;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class Tester {

    private Tester() {
    }

    private static String AUTHORIZATION = "";
    private static RegisterSiteResponse SETUP_CLIENT;
    private static String HOST;
    private static String OP_HOST;

    public static ClientInterface newClient(String targetHost) {
        return RpClient.newClient(getTargetHost(targetHost));
    }

    public static String getTargetHost(String targetHost) {
        if (StringUtils.countMatches(targetHost, ":") < 2 && "http://localhost".equalsIgnoreCase(targetHost) || "http://127.0.0.1".equalsIgnoreCase(targetHost) ) {
            targetHost = targetHost + ":" + SetUpTest.SUPPORT.getLocalPort();
        }
        if ("localhost".equalsIgnoreCase(targetHost)) {
            targetHost = "http://localhost:" + SetUpTest.SUPPORT.getLocalPort();
        }
        return targetHost;
    }

    public static String getAuthorization() {
        Preconditions.checkNotNull(SETUP_CLIENT);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(OP_HOST);
            params.setScope(Lists.newArrayList("openid"));
            params.setClientId(Tester.getSetupClient().getClientId());
            params.setClientSecret(Tester.getSetupClient().getClientSecret());

            GetClientTokenResponse resp = Tester.newClient(HOST).getClientToken(params);
            assertNotNull(resp);
            assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

            AUTHORIZATION = "Bearer " + resp.getAccessToken();
        }
        return AUTHORIZATION;
    }

    public static String getAuthorization(RegisterSiteResponse site) {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setScope(Lists.newArrayList("openid", "jans_client_api"));
        params.setOpHost(site.getOpHost());
        params.setClientId(site.getClientId());
        params.setClientSecret(site.getClientSecret());

        GetClientTokenResponse resp = Tester.newClient(HOST).getClientToken(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

        return "Bearer " + resp.getAccessToken();
    }

    public static RegisterSiteResponse getSetupClient() {
        return SETUP_CLIENT;
    }

    public static void setSetupClient(RegisterSiteResponse setupClient, String host, String opHost) {
        SETUP_CLIENT = setupClient;
        HOST = host;
        OP_HOST = opHost;
    }
}
