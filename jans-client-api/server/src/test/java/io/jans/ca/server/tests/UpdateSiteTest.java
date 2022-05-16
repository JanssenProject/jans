package io.jans.ca.server.tests;

import com.google.common.collect.Lists;
import io.jans.as.model.common.GrantType;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetRpParams;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.params.UpdateSiteParams;
import io.jans.ca.common.response.GetRpResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.UpdateSiteResponse;
import io.jans.ca.server.SetUpTest;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import io.jans.ca.server.configuration.model.Rp;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UpdateSiteTest extends BaseTest {

    @ArquillianResource
    private static URI url;

    @Parameters({"host", "opHost", "redirectUrls"})
    @Test
    public void update(String host, String opHost, String redirectUrls) throws IOException {

//        SetUpTest.beforeSuite(getApiTagetURL(url), host, opHost, redirectUrls);

        String authorizationRedirectUri = "https://client.example.com/cb";
        String anotherRedirectUri = "https://client.example.com/another";
        String logoutUri = "https://client.example.com/logout";

        final RegisterSiteParams registerParams = new RegisterSiteParams();
        registerParams.setOpHost(opHost);
        registerParams.setClientFrontchannelLogoutUri(logoutUri);
        registerParams.setRedirectUris(Lists.newArrayList(authorizationRedirectUri, anotherRedirectUri, logoutUri));
        registerParams.setAcrValues(Lists.newArrayList("basic"));
        registerParams.setScope(Lists.newArrayList("openid", "profile"));
        registerParams.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        registerParams.setResponseTypes(Lists.newArrayList("code", "id_token", "token"));
        registerParams.setAcrValues(Lists.newArrayList("acrBefore"));

        RegisterSiteResponse registerResponse = getClientInterface(url).registerSite(registerParams);
        assertNotNull(registerResponse);
        assertNotNull(registerResponse.getRpId());
        String rpId = registerResponse.getRpId();

        String strAuthorization1 = Tester.getAuthorization(getApiTagetURL(url), registerResponse, registerParams.getScope());
        Rp fetchedRp = fetchRp(getApiTagetURL(url), strAuthorization1, registerResponse.getRpId());

        assertEquals(authorizationRedirectUri, fetchedRp.getRedirectUri());
        assertEquals(Lists.newArrayList("acrBefore"), fetchedRp.getAcrValues());

        final UpdateSiteParams updateParams = new UpdateSiteParams();
        updateParams.setRpId(rpId);
        updateParams.setRedirectUris(Lists.newArrayList(anotherRedirectUri));
        updateParams.setScope(Lists.newArrayList("profile"));
        updateParams.setAcrValues(Lists.newArrayList("acrAfter"));

        UpdateSiteResponse updateResponse = getClientInterface(url).updateSite(Tester.getAuthorization(getApiTagetURL(url), registerResponse), null, updateParams);
        assertNotNull(updateResponse);

        String strAuthorization2 = Tester.getAuthorization(getApiTagetURL(url), registerResponse, updateParams.getScope());
        fetchedRp = fetchRp(getApiTagetURL(url), strAuthorization2, registerResponse.getRpId());

        assertEquals(anotherRedirectUri, fetchedRp.getRedirectUri());
        assertEquals(Lists.newArrayList("acrAfter"), fetchedRp.getAcrValues());
    }

    public static Rp fetchRp(String apiTargetUrl, RegisterSiteResponse site) throws IOException {
        String strAuthorization = Tester.getAuthorization(apiTargetUrl, site);
        final String rpAsJson = Tester.newClient(apiTargetUrl).getRp(strAuthorization, null, new GetRpParams(site.getRpId()));
        GetRpResponse resp = Jackson2.createJsonMapper().readValue(rpAsJson, GetRpResponse.class);
        return Jackson2.createJsonMapper().readValue(resp.getNode().toString(), Rp.class);
    }

    public static Rp fetchRp(String apiTargetUrl, String strAuthorization, String rpId) throws IOException {
        final String rpAsJson = Tester.newClient(apiTargetUrl).getRp(strAuthorization, null, new GetRpParams(rpId));
        GetRpResponse resp = Jackson2.createJsonMapper().readValue(rpAsJson, GetRpResponse.class);
        return Jackson2.createJsonMapper().readValue(resp.getNode().toString(), Rp.class);
    }
}
