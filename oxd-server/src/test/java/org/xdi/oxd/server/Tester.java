package org.xdi.oxd.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.client.OxdClient;
import org.xdi.oxd.common.params.GetClientTokenParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.SetupClientResponse;

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
    private static SetupClientResponse SETUP_CLIENT;
    private static String HOST;
    private static String OP_HOST;

    public static ClientInterface newClient(String targetHost) {
        if (StringUtils.countMatches(targetHost, ":") < 2 && "http://localhost".equalsIgnoreCase(targetHost) || "http://127.0.0.1".equalsIgnoreCase(targetHost) ) {
            targetHost = targetHost + ":" + SetUpTest.SUPPORT.getLocalPort();
        }
        return OxdClient.newClient(targetHost);
    }

    public static String getAuthorization() {
        Preconditions.checkNotNull(SETUP_CLIENT);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(OP_HOST);
            params.setScope(Lists.newArrayList("openid"));
            params.setClientId(Tester.getSetupClient().getClientId());
            params.setClientSecret(Tester.getSetupClient().getClientSecret());

            GetClientTokenResponse resp = Tester.newClient(HOST).getClientToken(params).dataAsResponse(GetClientTokenResponse.class);
            assertNotNull(resp);
            assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

            AUTHORIZATION = resp.getAccessToken();
        }
        return AUTHORIZATION;
    }

    public static SetupClientResponse getSetupClient() {
        return SETUP_CLIENT;
    }

    public static void setSetupClient(SetupClientResponse setupClient, String host, String opHost) {
        SETUP_CLIENT = setupClient;
        HOST = host;
        OP_HOST = opHost;
    }
}
