package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.RsProtectParams2;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.RsCheckAccessParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import io.jans.ca.common.response.RsProtectResponse;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.TestUtils;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.op.RsProtectOperation;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.testng.AssertJUnit.*;

public class RsProtectTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void protect(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {

        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
        RsCheckAccessTest.checkAccess(client, site, null);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectWithCreationExpiration"})
    @Test
    public void protect_withResourceCreationExpiration(String host, String redirectUrls, String opHost, String rsProtectWithCreationExpiration) throws IOException {

        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectWithCreationExpiration).getResources());

        Rp rp = UpdateSiteTest.fetchRp(client.getApitargetURL(), site);
        rp.getUmaProtectedResources().forEach(ele -> {
            assertEquals(1582890956L, ele.getIat().longValue());
            assertEquals(2079299799L, ele.getExp().longValue());
        });
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void overwriteFalse(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
        protectResources(client, site, resources);

        final RsProtectParams2 params = new RsProtectParams2();
        params.setRpId(site.getRpId());
        params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));
        RsProtectResponse r = client.umaRsProtect(Tester.getAuthorization(getApiTagetURL(url), site), params.getRpId(), params);
        assertNotNull(r);
        assertEquals(r.getError(), "uma_protection_exists");
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void overwriteTrue(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
        protectResources(client, site, resources);

        final RsProtectParams2 params = new RsProtectParams2();
        params.setRpId(site.getRpId());
        params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));
        params.setOverwrite(true); // force overwrite

        RsProtectResponse response = client.umaRsProtect(Tester.getAuthorization(getApiTagetURL(url), site), params.getRpId(), params);
        assertNotNull(response);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectScopeExpression"})
    @Test
    public void protectWithScopeExpression(String host, String redirectUrls, String opHost, String rsProtectScopeExpression) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources());
        RsCheckAccessTest.checkAccess(client, site, null);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectScopeExpressionSecond"})
    @Test
    public void protectWithScopeExpressionSeconds(String host, String redirectUrls, String opHost, String rsProtectScopeExpressionSecond) throws IOException {
        ClientInterface client = getClientInterface(url);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpressionSecond).getResources());

        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setRpId(site.getRpId());
        params.setHttpMethod("GET");
        params.setPath("/GetAll");
        params.setRpt("");

        final RsCheckAccessResponse response = client.umaRsCheckAccess(Tester.getAuthorization(getApiTagetURL(url), site), params.getRpId(), params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getAccess()));
    }

    public static RsProtectResponse protectResources(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources) {
        final RsProtectParams2 params = new RsProtectParams2();
        params.setRpId(site.getRpId());
        try {
            params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        final RsProtectResponse resp = client.umaRsProtect(Tester.getAuthorization(client.getApitargetURL(), site), params.getRpId(), params);
        assertNotNull(resp);
        return resp;
    }

    @Parameters({"correctScopeExpression"})
    @Test
    public void testCorrectScopeExpression(String correctScopeExpression) {
        RsProtectOperation.validateScopeExpression(correctScopeExpression.replaceAll("'", "\""));
    }

    @Parameters({"incorrectScopeExpression"})
    @Test(expectedExceptions = HttpException.class, enabled = false)
    public void testIncorrectScopeExpression(String incorrectScopeExpression) {
        RsProtectOperation.validateScopeExpression(incorrectScopeExpression.replaceAll("'", "\""));
    }

}
