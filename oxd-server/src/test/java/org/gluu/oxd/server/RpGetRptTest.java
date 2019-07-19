package org.gluu.oxd.server;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.params.RpGetRptParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RpGetRptResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;

import java.io.IOException;

import static org.testng.Assert.*;

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
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setRptAsJwt(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse site = client.registerSite(params);
        Assert.assertNotNull(site);
        Assert.assertTrue(!Strings.isNullOrEmpty(site.getOxdId()));

        final RpGetRptResponse response = requestRpt(client, site, rsProtect);
        assertNotNull(response);

        Jwt jwt = Jwt.parse(response.getRpt());
        assertNotNull(jwt);
        assertEquals(site.getClientId(), jwt.getClaims().getClaimAsString("client_id"));
        assertTrue(jwt.getClaims().getClaimAsString("permissions").contains("resource_id"));
    }

    public static RpGetRptResponse requestRpt(ClientInterface client, RegisterSiteResponse site, String rsProtect) throws IOException {
        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final RpGetRptParams params = new RpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(), params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRpt()));
        assertTrue(StringUtils.isNotBlank(response.getPct()));
        return response;
    }
}
