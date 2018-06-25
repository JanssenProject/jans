package io.swagger.client.api;

import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.model.SetupClientParams;
import io.swagger.client.model.SetupClientResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.common.GrantType;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


/**
 * @author yuriyz
 */
public class SetupClientTestGen {

    private static final DevelopersApi api = new DevelopersApi();

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

        Tester.notEmpty(resp.getData().getClientId());
        Tester.notEmpty(resp.getData().getClientSecret());
    }

    public static SetupClientResponse setupClient(String opHost, String redirectUrl) throws ApiException {
        return setupClient(opHost, redirectUrl, redirectUrl, "");
    }

    public static SetupClientResponse setupClient(String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri) throws ApiException {

        final SetupClientParams params = new SetupClientParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        SetupClientResponse resp = api.setupClient(params);

        assertResponse(resp);
        return resp;
    }

}
