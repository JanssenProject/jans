package io.swagger.client.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponseData;
import io.swagger.client.model.RegisterSiteResponseData;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponse;

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
    private static RegisterSiteResponseData setupData;
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

    public static String getAuthorization() throws Exception {
        Preconditions.checkNotNull(setupData);
        if (Strings.isNullOrEmpty(AUTHORIZATION)) {
            AUTHORIZATION = "Bearer " + getAuthorization(setupData);
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
    public static String getAuthorization(RegisterSiteResponseData siteResponseData) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(OP_HOST);
        params.setScope(Lists.newArrayList("openid", "oxd"));
        params.setClientId(siteResponseData.getClientId());
        params.setClientSecret(siteResponseData.getClientSecret());

        GetClientTokenResponseData resp = api().getClientToken(params).getData();
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

    public static void setSetupData(RegisterSiteResponseData setupData) {
        Tester.setupData = setupData;
    }

    public static void setTokenProtectionEnabled(Boolean isTokenProtectionEnabled) {
        Tester.isTokenProtectionEnabled = isTokenProtectionEnabled;
    }

    public static RegisterSiteResponseData getSetupData() {
        return setupData;
    }

    public static Boolean isTokenProtectionEnabled() {
        return isTokenProtectionEnabled;
    }

    public static ErrorResponse asError(String entity) throws IOException {
        return CoreUtils.createJsonMapper().readValue(entity, ErrorResponse.class);
    }

    public static ErrorResponse asError(ApiException e) throws IOException {
        return asError(e.getResponseBody());
    }
}
