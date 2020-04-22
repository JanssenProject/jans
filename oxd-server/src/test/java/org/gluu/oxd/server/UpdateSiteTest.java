package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.gluu.oxd.common.Jackson2;
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UpdateSiteTest {

    @Parameters({"host", "opHost", "redirectUrls"})
    @BeforeClass
    public static void beforeClass(String host, String opHost, String redirectUrls) {
        SetUpTest.beforeSuite(host, opHost, redirectUrls);
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

        Rp fetchedRp = fetchRp(host, registerResponse);

        assertEquals(authorizationRedirectUri, fetchedRp.getRedirectUri());
        assertEquals(Lists.newArrayList("acrBefore"), fetchedRp.getAcrValues());

        final UpdateSiteParams updateParams = new UpdateSiteParams();
        updateParams.setOxdId(oxdId);
        updateParams.setRedirectUris(Lists.newArrayList(anotherRedirectUri));
        updateParams.setScope(Lists.newArrayList("profile"));
        updateParams.setAcrValues(Lists.newArrayList("acrAfter"));

        UpdateSiteResponse updateResponse = Tester.newClient(host).updateSite(Tester.getAuthorization(registerResponse), null, updateParams);
        assertNotNull(updateResponse);

        fetchedRp = fetchRp(host, registerResponse);

        assertEquals(anotherRedirectUri, fetchedRp.getRedirectUri());
        assertEquals(Lists.newArrayList("acrAfter"), fetchedRp.getAcrValues());
    }

    private static Rp fetchRp(String host, RegisterSiteResponse site) throws IOException {
        final String rpAsJson = Tester.newClient(host).getRp(Tester.getAuthorization(site), null, new GetRpParams(site.getOxdId()));
        GetRpResponse resp = Jackson2.createJsonMapper().readValue(rpAsJson, GetRpResponse.class);
        return Jackson2.createJsonMapper().readValue(resp.getNode().toString(), Rp.class);
    }
}
