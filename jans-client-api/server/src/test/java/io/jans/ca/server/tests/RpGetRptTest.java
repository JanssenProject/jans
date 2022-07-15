package io.jans.ca.server.tests;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;

import static org.testng.AssertJUnit.*;

/**
 * @author Yuriy Zabrovarnyy
 */

public class RpGetRptTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void simple(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {

        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final RpGetRptResponse response = requestRpt(client, site, rsProtect);

        assertNotNull(response);
    }

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void rptAsJwt(String host, String opHost, String redirectUrls, String rsProtect) throws IOException, InvalidJwtException {

        ClientInterface client = getClientInterface(url);

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUri(redirectUrls.split(" ")[0]);
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "jans_client_api"));
        params.setRptAsJwt(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse site = client.registerSite(params);
        assertNotNull(site);
        assertTrue(!Strings.isNullOrEmpty(site.getRpId()));

        final RpGetRptResponse response = requestRpt(client, site, rsProtect);
        assertNotNull(response);

        Jwt jwt = Jwt.parse(response.getRpt());
        assertNotNull(jwt);
        assertEquals(site.getClientId(), jwt.getClaims().getClaimAsString("client_id"));
        assertTrue(jwt.getClaims().getClaimAsString("permissions").contains("resource_id"));
    }

    public static RpGetRptResponse requestRpt(ClientInterface client, RegisterSiteResponse site, String rsProtect) throws IOException {
        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetRptParams params = new RpGetRptParams();
        params.setRpId(site.getRpId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(client.getApitargetURL(), site), params.getRpId(), params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRpt()));
        assertTrue(StringUtils.isNotBlank(response.getPct()));
        return response;
    }
}
