package org.gluu.oxd.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.OxdClient;
import org.gluu.oxd.common.params.GetClientTokenParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;

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
        return OxdClient.newClient(getTargetHost(targetHost));
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

    public static RegisterSiteResponse getSetupClient() {
        return SETUP_CLIENT;
    }

    public static void setSetupClient(RegisterSiteResponse setupClient, String host, String opHost) {
        SETUP_CLIENT = setupClient;
        HOST = host;
        OP_HOST = opHost;
    }
}
