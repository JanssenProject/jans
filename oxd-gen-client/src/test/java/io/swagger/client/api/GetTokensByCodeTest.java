package io.swagger.client.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import io.swagger.client.model.GetAccessTokenByRefreshTokenParams;
import io.swagger.client.model.GetAccessTokenByRefreshTokenResponseData;
import io.swagger.client.model.GetTokensByCodeParams;
import io.swagger.client.model.GetTokensByCodeResponseData;
import io.swagger.client.model.RegisterSiteResponseData;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.CoreUtils;

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

        final RegisterSiteResponseData site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        GetTokensByCodeResponseData tokensResponse = tokenByCode(client, site, userId, userSecret, CoreUtils.secureRandomString());

        refreshToken(tokensResponse, client, site.getOxdId());
    }

    private static void refreshToken(GetTokensByCodeResponseData resp, DevelopersApi client, String oxdId) throws Exception {
        notEmpty(resp.getRefreshToken());

        // refresh token
        final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
        refreshParams.setOxdId(oxdId);
        refreshParams.setScope(Lists.newArrayList("openid"));
        refreshParams.setRefreshToken(resp.getRefreshToken());
        refreshParams.setProtectionToken(Tester.getAuthorization());

        GetAccessTokenByRefreshTokenResponseData refreshResponse = client.getAccessTokenByRefreshToken(Tester.getAuthorization(), refreshParams).getData();

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
    }

    private GetTokensByCodeResponseData tokenByCode(DevelopersApi client, RegisterSiteResponseData site, String userId, String userSecret, String nonce) throws Exception {

        final String state = CoreUtils.secureRandomString();

        final String authorizationStr = Tester.getAuthorization(site); //getAuthorization(opHost, client, site);

        final String code = codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce, authorizationStr);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);
        params.setState(state);

        final GetTokensByCodeResponseData resp = client.getTokensByCode(authorizationStr, params).getData();
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }

    private String codeRequest(DevelopersApi client, String oxdId, String userId, String userSecret, String state,
                               String nonce, String authorization) throws Exception {

        final Request request = buildRequest(authorization, oxdId, userId, userSecret, state, nonce, client);

        final Response response = client.getApiClient().getHttpClient().newCall(request).execute();

        final JsonElement jsonResponse = new JsonParser().parse(response.body().string());

        return jsonResponse.getAsJsonObject().getAsJsonObject("data").get("code").getAsString();

    }

    private Request buildRequest(String authorization, String oxdId, String userId, String userSecret, String state, String nonce, DevelopersApi client) {

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
