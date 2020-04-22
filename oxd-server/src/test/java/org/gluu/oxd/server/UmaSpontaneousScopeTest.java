package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.common.params.IntrospectRptParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RpGetRptResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import static org.testng.AssertJUnit.*;

public class UmaSpontaneousScopeTest {

    public static final String USER_2_SCOPE = "/user/2";

    @Parameters({"host", "opHost", "paramRedirectUrl", "userId", "userSecret", "rsProtectWithSpontaneousScope"})
    @Test
    public void init(String host, String opHost, String paramRedirectUrl, String userId, String userSecret, String rsProtectWithSpontaneousScope) throws Exception {
        List<String> scopes = Lists.newArrayList("openid", "uma_protection", "profile", "address", "email", "phone", "user_name", "oxd");
        List<String> responseTypes = Lists.newArrayList("code", "id_token", "token");
        //register client
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse registerResponse = RegisterSiteTest.registerSite(client, opHost, paramRedirectUrl, scopes, responseTypes, true, null);

        //UMA RP - Get RPT
        //Spontaneous Scope Regress: ^/user/.+$
        RpGetRptResponse response = RpGetRptTest.requestRpt(client, registerResponse, rsProtectWithSpontaneousScope);
        //UMA Introspect RPT
        IntrospectRptParams params = new IntrospectRptParams();
        params.setOxdId(registerResponse.getOxdId());
        params.setRpt(response.getRpt());

        final CorrectRptIntrospectionResponse rptIntrospectionResponse = client.introspectRpt(Tester.getAuthorization(registerResponse), null, params);

        rptIntrospectionResponse.getPermissions().forEach( permission -> {
            assertTrue(permission.getScopes().contains(USER_2_SCOPE));
        });
    }

}
