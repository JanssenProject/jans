package io.jans.ca.server.tests;

import com.google.common.collect.Lists;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.IntrospectRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.List;

import static org.testng.AssertJUnit.assertTrue;

public class UmaSpontaneousScopeTest extends BaseTest {

    @ArquillianResource
    private URI url;

    public static final String USER_2_SCOPE = "/user/2";

    @Parameters({"host", "opHost", "paramRedirectUrl", "userId", "userSecret", "rsProtectWithSpontaneousScope"})
    @Test
    public void init(String host, String opHost, String paramRedirectUrl, String userId, String userSecret, String rsProtectWithSpontaneousScope) throws Exception {
        List<String> scopes = Lists.newArrayList("openid", "uma_protection", "profile", "address", "email", "phone", "user_name", "jans_client_api");
        List<String> responseTypes = Lists.newArrayList("code", "id_token", "token");
        //register client
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse registerResponse = RegisterSiteTest.registerSite(client, opHost, paramRedirectUrl, scopes, responseTypes, true, null);

        //UMA RP - Get RPT
        //Spontaneous Scope Regress: ^/user/.+$
        RpGetRptResponse response = RpGetRptTest.requestRpt(client, registerResponse, rsProtectWithSpontaneousScope);
        //UMA Introspect RPT
        IntrospectRptParams params = new IntrospectRptParams();
        params.setRpId(registerResponse.getRpId());
        params.setRpt(response.getRpt());

        final CorrectRptIntrospectionResponse rptIntrospectionResponse = client.introspectRpt(Tester.getAuthorization(client.getApitargetURL(), registerResponse), null, params);

        rptIntrospectionResponse.getPermissions().forEach( permission -> {
            assertTrue(permission.getScopes().contains(USER_2_SCOPE));
        });
    }

}
