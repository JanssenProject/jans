package io.swagger.client.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import io.swagger.client.model.*;
import org.gluu.oxd.common.SeleniumTestUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.common.CoreUtils;

import static io.swagger.client.api.Tester.notEmpty;
import static org.testng.Assert.*;

/**
 * Test class to test refresh token and related end points
 *
 * @author Yuriy Z
 * @author Shoeb
 * @version 5, Oct, 2018
 */
public class GetTokensByCodeTest {

    private static final String AUTH_CODE_ENDPOINT = "/get-authorization-code";

    @Parameters({"opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void test(String opHost, String redirectUrls, String userId, String userSecret) throws Exception {

        DevelopersApi client = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        GetTokensByCodeResponse tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());

        refreshToken(tokensResponse, client, site);
    }

    private static void refreshToken(GetTokensByCodeResponse resp, DevelopersApi client, RegisterSiteResponse site) throws Exception {
        notEmpty(resp.getRefreshToken());

        final String authorization = Tester.getAuthorization(site);

        // refresh token
        final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
        refreshParams.setOxdId(site.getOxdId());
        refreshParams.setScope(Lists.newArrayList("openid"));
        refreshParams.setRefreshToken(resp.getRefreshToken());

        GetAccessTokenByRefreshTokenResponse refreshResponse = client.getAccessTokenByRefreshToken(refreshParams, authorization, null);

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
    }

    private static GetTokensByCodeResponse tokenByCode(DevelopersApi client, RegisterSiteResponse site, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String nonce) throws Exception {

        final String state = CoreUtils.secureRandomString();

        final String authorizationStr = Tester.getAuthorization(site);

        final String code = codeRequest(client, opHost, site.getOxdId(), userId, userSecret, clientId, redirectUrls, state, nonce, authorizationStr);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokensByCode(params, authorizationStr, null);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }

    public static String codeRequest(DevelopersApi client, String opHost, String oxdId, String userId, String userSecret, String clientId, String redirectUrls, String state,
                               String nonce, String authorization) throws Exception {
        SeleniumTestUtils.authorizeClient(opHost, userId, userSecret, clientId, redirectUrls, state, nonce, null, null);

        final Request request = buildRequest(authorization, oxdId, userId, userSecret, state, nonce, client);

        final Response response = client.getApiClient().getHttpClient().newCall(request).execute();

        final JsonElement jsonResponse = new JsonParser().parse(response.body().string());

        return jsonResponse.getAsJsonObject().get("code").getAsString();

    }

    private static Request buildRequest(String authorization, String oxdId, String userId, String userSecret, String state, String nonce, DevelopersApi client) {

        final String json = "{\"oxd_id\":\"" + oxdId + "\",\"username\":\"" + userId + "\",\"password\":\"" + userSecret
                + "\",\"state\":\"" + state + "\",\"nonce\":\"" + nonce + "\"}";

        final RequestBody reqBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        return new Request.Builder()
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .method("POST", reqBody)
                .url(client.getApiClient().getBasePath() + AUTH_CODE_ENDPOINT).build();

    }

}
