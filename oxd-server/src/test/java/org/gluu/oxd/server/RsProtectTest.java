package org.gluu.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.RsProtectParams2;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.RsCheckAccessParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;
import org.gluu.oxd.common.response.RsProtectResponse;
import org.gluu.oxd.rs.protect.RsResource;
import org.gluu.oxd.server.op.RsProtectOperation;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/06/2016
 */

public class RsProtectTest {

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void protect(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
        RsCheckAccessTest.checkAccess(client, site, null);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void overwriteFalse(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
        protectResources(client, site, resources);

        final RsProtectParams2 params = new RsProtectParams2();
        params.setOxdId(site.getOxdId());
        params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));

        try {
            client.umaRsProtect(Tester.getAuthorization(), params);
        } catch (BadRequestException e) {
            assertEquals("uma_protection_exists", TestUtils.asError(e).getError());
            return;
        }

        throw new AssertionError("Expected 400 (bad request) but got successful result.");
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void overwriteTrue(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
        protectResources(client, site, resources);

        final RsProtectParams2 params = new RsProtectParams2();
        params.setOxdId(site.getOxdId());
        params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));
        params.setOverwrite(true); // force overwrite

        RsProtectResponse response = client.umaRsProtect(Tester.getAuthorization(), params);
        assertNotNull(response);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectScopeExpression"})
    @Test
    public void protectWithScopeExpression(String host, String redirectUrls, String opHost, String rsProtectScopeExpression) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources());
        RsCheckAccessTest.checkAccess(client, site, null);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectScopeExpressionSecond"})
    @Test
    public void protectWithScopeExpressionSeconds(String host, String redirectUrls, String opHost, String rsProtectScopeExpressionSecond) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpressionSecond).getResources());

        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/GetAll");
        params.setRpt("");

        final RsCheckAccessResponse response = client.umaRsCheckAccess(Tester.getAuthorization(), params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getAccess()));
    }

    public static RsProtectResponse protectResources(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources) {
        final RsProtectParams2 params = new RsProtectParams2();
        params.setOxdId(site.getOxdId());
        try {
            params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        final RsProtectResponse resp = client.umaRsProtect(Tester.getAuthorization(), params);
        assertNotNull(resp);
        return resp;
    }

    @Parameters({"correctScopeExpression"})
    @Test
    public void testCorrectScopeExpression(String correctScopeExpression) {
        RsProtectOperation.validateScopeExpression(correctScopeExpression.replaceAll("'", "\""));
    }

    @Parameters({"incorrectScopeExpression"})
    @Test(expectedExceptions = HttpException.class)
    public void testIncorrectScopeExpression(String incorrectScopeExpression) {
        RsProtectOperation.validateScopeExpression(incorrectScopeExpression.replaceAll("'", "\""));
    }

}
