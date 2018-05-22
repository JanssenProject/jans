package org.xdi.oxd.server;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang.ArrayUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.GetAccessTokenByRefreshTokenParams;
import org.xdi.oxd.common.params.GetAuthorizationCodeParams;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.response.GetAuthorizationCodeResponse;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetTokensByCodeTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
            GetTokensByCodeResponse tokensResponse = tokenByCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
            refreshToken(tokensResponse, client, site.getOxdId());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public GetClientTokenResponse refreshToken(GetTokensByCodeResponse resp, CommandClient client, String oxdId) {
        notEmpty(resp.getRefreshToken());

        // refresh token
        final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
        refreshParams.setOxdId(oxdId);
        refreshParams.setScope(Lists.newArrayList("openid"));
        refreshParams.setRefreshToken(resp.getRefreshToken());

        GetClientTokenResponse refreshResponse = client.send(new Command(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN).setParamsObject(refreshParams)).dataAsResponse(GetClientTokenResponse.class);

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
        return refreshResponse;
    }

    public static GetTokensByCodeResponse tokenByCode(CommandClient client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();

        String code = codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce);

        notEmpty(code);

        final GetTokensByCodeParams commandParams = new GetTokensByCodeParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setCode(code);
        commandParams.setState(state);

        final Command command = new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(commandParams);

        final GetTokensByCodeResponse resp = client.send(command).dataAsResponse(GetTokensByCodeResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }

    public static String codeRequest(CommandClient client, String siteId, String userId, String userSecret, String state, String nonce) {
        GetAuthorizationCodeParams params = new GetAuthorizationCodeParams();
        params.setOxdId(siteId);
        params.setUsername(userId);
        params.setPassword(userSecret);
        params.setState(state);
        params.setNonce(nonce);

        final Command command = new Command(CommandType.GET_AUTHORIZATION_CODE).setParamsObject(params);
        return client.send(command).dataAsResponse(GetAuthorizationCodeResponse.class).getCode();
    }

    public static void main(String[] args) {
        long[] ids = new long[] {123};

        System.out.println(Joiner.on(",").join(ArrayUtils.toObject(ids)));
    }


}
