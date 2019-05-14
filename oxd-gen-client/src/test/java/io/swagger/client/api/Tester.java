package io.swagger.client.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.model.ErrorResponse;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponse;
import io.swagger.client.model.RegisterSiteResponse;
import org.gluu.oxd.common.CoreUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;


/**
 * @author yuriyz
 */
public class Tester {

    private static String AUTHORIZATION = "";
    private static String HOST;
    private static String OP_HOST;
    private static RegisterSiteResponse setupData;
    private static boolean isTokenProtectionEnabled = false;

    private Tester() {
    }

    public static DevelopersApi api() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(HOST);

        apiClient.setVerifyingSsl(false);
        apiClient.setDebugging(true);
        apiClient.getHttpClient().setConnectTimeout(10, TimeUnit.SECONDS);

        return new DevelopersApi(apiClient);
    }

    public static void notEmpty(String str) {
        assertTrue(str != null && !str.isEmpty());
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && str.get(0) != null && !str.get(0).isEmpty());
    }

    public static String getAuthorization() throws ApiException {
        Preconditions.checkNotNull(setupData);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            AUTHORIZATION = getAuthorization(setupData);
        }
        return AUTHORIZATION;
    }

    /**
     * Tests requiring register site operation to be performed before their execution better call this method instead of the overloaded no-arg method.
     * This ensures, a new access token is generated for the new client site.
     *
     * Note:- This IS a requirement in case value of protect_commands_with_access_token is set to true in server configuration.
     *
     * @param siteResponseData
     * @return access token for the provided site's client id
     * @throws ApiException
     */
    public static String getAuthorization(RegisterSiteResponse siteResponseData) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(OP_HOST);
        params.setScope(Lists.newArrayList("openid", "oxd"));
        params.setClientId(siteResponseData.getClientId());
        params.setClientSecret(siteResponseData.getClientSecret());

        GetClientTokenResponse resp = api().getClientToken(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getAccessToken()));

        return "Bearer " + resp.getAccessToken();
    }


    public static void setHost(String host) {
        HOST = host;
    }

    public static void setOpHost(String opHost) {
        OP_HOST = opHost;
    }

    public static void setSetupData(RegisterSiteResponse setupData) {
        Tester.setupData = setupData;
    }

    public static void setTokenProtectionEnabled(Boolean isTokenProtectionEnabled) {
        Tester.isTokenProtectionEnabled = isTokenProtectionEnabled;
    }

    public static RegisterSiteResponse getSetupData() {
        return setupData;
    }

    public static Boolean isTokenProtectionEnabled() {
        return isTokenProtectionEnabled;
    }

    public static ErrorResponse asError(String entity) throws IOException {
        return CoreUtils.createJsonMapper().readValue(entity, ErrorResponse.class);
    }

    public static ErrorResponse asError(ApiException e) throws IOException {
        return e.getResponseBody();
    }
}
