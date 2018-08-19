package io.swagger.client.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.ApiClient;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponseData;
import io.swagger.client.model.RegisterSiteResponseData;
import org.testng.AssertJUnit;

import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;


/**
 * @author yuriyz
 */
public class Tester {

    private static String AUTHORIZATION = "";
    private static String HOST;
    private static String OP_HOST;
    private static RegisterSiteResponseData clientData;

    private Tester() {
    }

    public static DevelopersApi api() {
        HOST = "https://localhost:8443";//TODO: remove when the module become part of parent test suite
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(HOST);

        apiClient.setVerifyingSsl(false);
        apiClient.setDebugging(true);

        return new DevelopersApi(apiClient);
    }

    public static void notEmpty(String str) {
        assertTrue(str != null && !str.isEmpty());
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && str.get(0) != null && !str.get(0).isEmpty());
    }

    public static String getAuthorization() throws Exception {
        Preconditions.checkNotNull(clientData);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(OP_HOST);
            params.setScope(Lists.newArrayList("openid"));
            params.setClientId(clientData.getClientId());
            params.setClientSecret(clientData.getClientSecret());

            GetClientTokenResponseData resp = api().getClientToken(params).getData();
            assertNotNull(resp);
            AssertJUnit.assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));
            AUTHORIZATION = "Bearer " + resp.getAccessToken();
        }
        return AUTHORIZATION;
    }


    public static void setupHosts(String host, String opHost) {
        HOST = host;
        OP_HOST = opHost;

    }

    public static void setClientInfo(RegisterSiteResponseData clientInfo) {
        clientData = clientInfo;
    }
}
