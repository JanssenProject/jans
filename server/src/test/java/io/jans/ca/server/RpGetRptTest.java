package io.jans.ca.server;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;

import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
/**
 * @author Yuriy Zabrovarnyy
 */

public class RpGetRptTest {

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void simple(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final RpGetRptResponse response = requestRpt(client, site, rsProtect);

        assertNotNull(response);
    }

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void rptAsJwt(String host, String opHost, String redirectUrls, String rsProtect) throws IOException, InvalidJwtException {

        ClientInterface client = Tester.newClient(host);

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUri(redirectUrls.split(" ")[0]);
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setRptAsJwt(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse site = client.registerSite(params);
        assertNotNull(site);
        assertTrue(!Strings.isNullOrEmpty(site.getOxdId()));

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
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(site), null, params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRpt()));
        assertTrue(StringUtils.isNotBlank(response.getPct()));
        return response;
    }
}
