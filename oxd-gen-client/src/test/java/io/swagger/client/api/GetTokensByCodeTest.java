package io.swagger.client.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import io.swagger.client.model.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.common.CoreUtils;

import static io.swagger.client.api.Tester.notEmpty;
import static org.junit.Assert.assertNotNull;

/**
 * Test class to test refresh token and related end points
 *
 * @author Yuriy Z
 * @author Shoeb
 * @version 5, Oct, 2018
 */
public class GetTokensByCodeTest {

    private static final String AUTH_CODE_ENDPOINT = "/get-authorization-code";

    @Parameters({"opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String opHost, String redirectUrl, String userId, String userSecret) throws Exception {

        DevelopersApi client = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        GetTokensByCodeResponse tokensResponse = tokenByCode(client, site, userId, userSecret, CoreUtils.secureRandomString());

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

        GetAccessTokenByRefreshTokenResponse refreshResponse = client.getAccessTokenByRefreshToken(authorization, refreshParams);

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
    }

    private static GetTokensByCodeResponse tokenByCode(DevelopersApi client, RegisterSiteResponse site, String userId, String userSecret, String nonce) throws Exception {

        final String state = CoreUtils.secureRandomString();

        final String authorizationStr = Tester.getAuthorization(site);

        final String code = codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce, authorizationStr);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokensByCode(authorizationStr, params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }

    public static String codeRequest(DevelopersApi client, String oxdId, String userId, String userSecret, String state,
                               String nonce, String authorization) throws Exception {

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
