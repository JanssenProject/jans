package org.xdi.oxd.server.manual;

import com.google.common.base.Strings;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.server.Tester;

import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/08/2016
 */

public class GoogleTest {

    private static final String HOST = "http://localhost:8084";

    private static final String OP_HOST = "https://accounts.google.com";
    private static final String REDIRECT_URI = "https://mytestproduct.com";

    private static final String CLIENT_ID = "470707636610-pr4nn6dr82the6kuktqfr7lq4g7n9rb1.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "vJVYPI4Ybwc9a2YOesq2bRCa";

    public static void main(String[] args) throws IOException {
        ClientInterface client = Tester.newClient(HOST);

        final RegisterSiteResponse site = registerSite(client);
        final String authorizationUrl = getAuthorizationUrl(client, site.getOxdId());

        // after successful login get redirect with code
        // https://mytestproduct.com/?state=af0ifjsldkj&code=4/2I5U130cxs7MObKVniVseBQkHQ0JQS0p5JfZm7NgZ-M&authuser=0&session_state=02bd0461002924877bee444d9dfd9f1279e44335..ae63&prompt=consent#
//            String code = "4/2I5U130cxs7MObKVniVseBQkHQ0JQS0p5JfZm7NgZ-M";
//            String code = "4/e8WrOQDMNRllHvMGAEKAovwWMnIy7k7oxRN4Xn9JPrg";
        String code = "4/nWqDxBS-c7MDN_1Gq2WtW2L6B9KnA5rpOOYzGEdg7lM";

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);

        final GetTokensByCodeResponse resp = client.getTokenByCode(Tester.getAuthorization(), params).dataAsResponse(GetTokensByCodeResponse.class);
        System.out.println(resp);
    }

    private static String getAuthorizationUrl(ClientInterface client, String oxdId) {
        final GetAuthorizationUrlParams params = new GetAuthorizationUrlParams();
        params.setOxdId(oxdId);

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(), params).dataAsResponse(GetAuthorizationUrlResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        System.out.println("Authorization url: " + resp.getAuthorizationUrl());
        return resp.getAuthorizationUrl();
    }

    public static RegisterSiteResponse registerSite(ClientInterface client) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(OP_HOST);
        params.setAuthorizationRedirectUri(REDIRECT_URI);
        params.setClientId(CLIENT_ID);
        params.setClientSecret(CLIENT_SECRET);

        final RegisterSiteResponse resp = client.registerSite(Tester.getAuthorization(), params).dataAsResponse(RegisterSiteResponse.class);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
