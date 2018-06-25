package io.swagger.client.api;

import io.swagger.client.model.SetupClientResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


/**
 * @author yuriyz
 */
public class SetupClientTestGen {

    @Parameters({"host", "opHost", "redirectUrl", "logoutUrl", "postLogoutRedirectUrl"})
    @Test
    public void setupClient(String host, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUrl) throws IOException {
//        SetupClientResponse resp = setupClient(Tester.newClient(host), opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl);
//        assertResponse(resp);
//
//        // more specific client setup
//        final SetupClientParams params = new SetupClientParams();
//        params.setOpHost(opHost);
//        params.setAuthorizationRedirectUri(redirectUrl);
//        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
//        params.setClientFrontchannelLogoutUri(new ArrayList<>(Arrays.asList(logoutUrl));
//        params.setRedirectUris(Arrays.asList(redirectUrl));
//        params.setAcrValues(new ArrayList<String>());
//        params.setScope(new ArrayList<>(Arrays.asList("openid", "profile")));
//        params.setGrantType(Lists.newArrayList("authorization_code"));
//        params.setResponseTypes(Lists.newArrayList("code"));
//
//        resp = Tester.newClient(host).setupClient(params).dataAsResponse(SetupClientResponse.class);
//        assertResponse(resp);
    }

    public static void assertResponse(SetupClientResponse resp) {
        assertNotNull(resp);

        notEmpty(resp.getData().getClientId());
        notEmpty(resp.getData().getClientSecret());
    }

    public static SetupClientResponse setupClient(String opHost, String redirectUrl) {
        return setupClient(opHost, redirectUrl, redirectUrl, "");
    }

    public static SetupClientResponse setupClient(String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) {

//        final SetupClientParams params = new SetupClientParams();
//        params.setOpHost(opHost);
//        params.setAuthorizationRedirectUri(redirectUrl);
//        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
//        params.setClientFrontchannelLogoutUri(Lists.newArrayList(logoutUri));
//        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
//        params.setTrustedClient(true);
//        params.setGrantType(Lists.newArrayList(
//                GrantType.AUTHORIZATION_CODE.getValue(),
//                GrantType.CLIENT_CREDENTIALS.getValue()));
//        params.setOxdRpProgrammingLanguage("java");
//
//        final Command command = new Command(CommandType.SETUP_CLIENT);
//        command.setParamsObject(params);
//
//        final SetupClientResponse resp = client.setupClient(params).dataAsResponse(SetupClientResponse.class);
//        assertResponse(resp);
//        return resp;
        return null;
    }

    public static void notEmpty(String str) {
        assertTrue(str != null && !str.isEmpty());
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && str.get(0) != null && !str.get(0).isEmpty());
    }
}
