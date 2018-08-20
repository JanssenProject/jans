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
    private static RegisterSiteResponseData setupData;

    private Tester() {
    }

    public static DevelopersApi api() {
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
        Preconditions.checkNotNull(setupData);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(OP_HOST);
            params.setScope(Lists.newArrayList("openid"));
            params.setClientId(setupData.getClientId());
            params.setClientSecret(setupData.getClientSecret());

            GetClientTokenResponseData resp = api().getClientToken(params).getData();
            assertNotNull(resp);
            AssertJUnit.assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));
            AUTHORIZATION = "Bearer " + resp.getAccessToken();
        }
        return AUTHORIZATION;
    }


    public static void setHost(String host) {
        HOST = host;
    }

    public static void setOpHost(String opHost) {
        OP_HOST = opHost;
    }

    public static void setSetupData(RegisterSiteResponseData setupData) {
        Tester.setupData = setupData;
    }
}
