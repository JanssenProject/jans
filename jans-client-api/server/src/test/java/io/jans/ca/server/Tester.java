package io.jans.ca.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.model.common.GrantType;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetClientTokenParams;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.response.GetClientTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.arquillian.ClientIterfaceImpl;
import io.jans.ca.server.arquillian.ConfigurableTest;
import io.jans.ca.server.op.RegisterSiteOperation;
import io.jans.ca.server.tests.SetupClientTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.jans.ca.server.tests.SetupClientTest.assertResponse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class Tester {

    private static final Logger LOG = LoggerFactory.getLogger(Tester.class);

    private Tester() {
    }

    private static String AUTHORIZATION = "";
    private static RegisterSiteResponse SETUP_CLIENT;
    private static String HOST;
    private static String OP_HOST;

    private static Map<String, String> MAP_TEST_PARAMS;

    public static ClientInterface newClient(String targeHosttUrl) {
        return ClientIterfaceImpl.getInstanceClient(targeHosttUrl);
    }

    public static String getSetupAuthorization(String url) {
        Preconditions.checkNotNull(SETUP_CLIENT);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            LOG.info("------------------------- INITIALIZING AUTHORIZATION FOR CLIENT_SETUP --------------------------------");
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(OP_HOST);
            params.setScope(Lists.newArrayList("openid"));
            params.setClientId(Tester.getSetupClient(url).getClientId());
            params.setClientSecret(Tester.getSetupClient(url).getClientSecret());

            GetClientTokenResponse resp = Tester.newClient(url).getClientToken(params);
            assertNotNull(resp);
            assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

            AUTHORIZATION = "Bearer " + resp.getAccessToken();
        }
        return AUTHORIZATION;
    }

    public static String getAuthorization(String url, RegisterSiteResponse site) {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setScope(Lists.newArrayList("openid", "jans_client_api"));
        params.setOpHost(site.getOpHost());
        params.setClientId(site.getClientId());
        params.setClientSecret(site.getClientSecret());

        GetClientTokenResponse resp = Tester.newClient(url).getClientToken(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

        return "Bearer " + resp.getAccessToken();
    }

    public static String getAuthorization(String url, RegisterSiteResponse site, List<String> scopes) {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setScope(scopes);
        params.setOpHost(site.getOpHost());
        params.setClientId(site.getClientId());
        params.setClientSecret(site.getClientSecret());

        GetClientTokenResponse resp = Tester.newClient(url).getClientToken(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

        return "Bearer " + resp.getAccessToken();
    }

    public static RegisterSiteResponse getSetupClient(String url) {
        if (SETUP_CLIENT == null) {
            SETUP_CLIENT = setupClient(url);
        }
        return SETUP_CLIENT;
    }

    public static void setSetupClient(RegisterSiteResponse setupClient, String host, String opHost) {
        SETUP_CLIENT = setupClient;
        HOST = host;
        OP_HOST = opHost;
    }

    public static void setSetupTestParams(Map<String, String> hmTestParams) {
        MAP_TEST_PARAMS = hmTestParams;
    }

    public static String getTestParam(String nameParam) {
        if (MAP_TEST_PARAMS != null) {
            return MAP_TEST_PARAMS.get(nameParam);
        } else {
            return null;
        }
    }

    public static boolean testWithExternalApiUrl() {
        return System.getProperties().containsKey("test.client.api.url");
    }

    public static String readExternalApiUrl() {
        if (testWithExternalApiUrl()) {
            return System.getProperties().getProperty("test.client.api.url");
        }
        return null;
    }

    public static RegisterSiteResponse setupClient(String url) {
        LOG.info("------------------------- INITIALIZING CLIENT_SETUP --------------------------------");
        String opHost = Tester.getTestParam("opHost");
        String redirectUrls = Tester.getTestParam("redirectUrls");
//        String postLogoutRedirectUrls = Tester.getTestParam("postLogoutRedirectUrls");
//        String logoutUri = Tester.getTestParam("logoutUrl");
        RegisterSiteResponse setupClient = SetupClientTest.setupClient(Tester.newClient(url), opHost, redirectUrls);
        Tester.setSetupClient(setupClient, null, opHost);
        LOG.debug("SETUP_CLIENT is set in Tester.");
        return setupClient;
    }
}
