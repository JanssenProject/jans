package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.params.GetRpParams;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.GetRpResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;
import org.gluu.oxd.server.service.Rp;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UpdateSiteTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @BeforeClass
    public static void beforeClass(String host, String opHost, String redirectUrl) {
        SetUpTest.beforeSuite(host, opHost, redirectUrl);
    }

    @AfterClass
    public static void afterClass() {
        SetUpTest.afterSuite();
    }

    @Parameters({"host", "opHost"})
    @Test
    public void update(String host, String opHost) throws IOException {

        String authorizationRedirectUri = "https://client.example.com/cb";
        String anotherRedirectUri = "https://client.example.com/another";
        String logoutUri = "https://client.example.com/logout";

        final RegisterSiteParams registerParams = new RegisterSiteParams();
        registerParams.setOpHost(opHost);
        registerParams.setAuthorizationRedirectUri(authorizationRedirectUri);
        registerParams.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        registerParams.setRedirectUris(Lists.newArrayList(authorizationRedirectUri, anotherRedirectUri, logoutUri));
        registerParams.setAcrValues(Lists.newArrayList("basic"));
        registerParams.setScope(Lists.newArrayList("openid", "profile"));
        registerParams.setGrantTypes(Lists.newArrayList("authorization_code"));
        registerParams.setResponseTypes(Lists.newArrayList("code"));
        registerParams.setAcrValues(Lists.newArrayList("acrBefore"));

        RegisterSiteResponse registerResponse = Tester.newClient(host).registerSite(registerParams);
        assertNotNull(registerResponse);
        assertNotNull(registerResponse.getOxdId());
        String oxdId = registerResponse.getOxdId();

        Rp fetchedRp = fetchRp(host, oxdId);

        assertEquals("https://client.example.com/cb", fetchedRp.getAuthorizationRedirectUri());
        assertEquals(Lists.newArrayList("acrBefore"), fetchedRp.getAcrValues());

        final UpdateSiteParams updateParams = new UpdateSiteParams();
        updateParams.setOxdId(oxdId);
        updateParams.setAuthorizationRedirectUri(anotherRedirectUri);
        updateParams.setScope(Lists.newArrayList("profile"));
        updateParams.setAcrValues(Lists.newArrayList("acrAfter"));

        UpdateSiteResponse updateResponse = Tester.newClient(host).updateSite(Tester.getAuthorization(), updateParams);
        assertNotNull(updateResponse);

        fetchedRp = fetchRp(host, oxdId);

        assertEquals("https://client.example.com/another", fetchedRp.getAuthorizationRedirectUri());
        assertEquals(Lists.newArrayList("acrAfter"), fetchedRp.getAcrValues());
    }

    private static Rp fetchRp(String host, String oxdId) throws IOException {
        final String rpAsJson = Tester.newClient(host).getRp(Tester.getAuthorization(), new GetRpParams(oxdId));
        GetRpResponse resp = CoreUtils.createJsonMapper().readValue(rpAsJson, GetRpResponse.class);
        return CoreUtils.createJsonMapper().readValue(resp.getNode().toString(), Rp.class);
    }
}
