package io.swagger.client.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.ApiClient;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponse;
import io.swagger.client.model.SetupClientParams;
import io.swagger.client.model.SetupClientResponse;
import io.swagger.client.model.SetupClientResponseData;
import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;


/**
 * @author yuriyz
 */
public class Tester {

    private static String AUTHORIZATION = "";
    private static SetupClientResponse SETUP_CLIENT;
    private static String HOST ;
    private static String OP_HOST;

    private Tester() {
    }

    public static DevelopersApi api(String targetHost) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(targetHost );
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

    public static String getAuthorization() throws Exception{
        Preconditions.checkNotNull(SETUP_CLIENT);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(OP_HOST);
            params.setScope(Lists.newArrayList("openid"));
            final SetupClientResponseData clientResponseData = SETUP_CLIENT.getData();
            params.setClientId(clientResponseData.getClientId());
            params.setClientSecret(clientResponseData.getClientSecret());

            GetClientTokenResponse resp = api(HOST).getClientToken(params);
            assertNotNull(resp);
            assertTrue(!Strings.isNullOrEmpty(resp.getData().getAccessToken()));

            AUTHORIZATION = "Bearer " + resp.getData().getAccessToken();
        }
        return AUTHORIZATION;
    }

    public static void setSetupClient(SetupClientResponse setupClient, String host, String opHost) {
        SETUP_CLIENT = setupClient;
        HOST = host;
        OP_HOST = opHost;
    }
}
