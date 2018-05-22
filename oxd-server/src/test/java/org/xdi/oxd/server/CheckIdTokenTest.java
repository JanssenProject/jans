package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.CheckIdTokenParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

public class CheckIdTokenTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            String nonce = CoreUtils.secureRandomString();
            GetTokensByCodeResponse response = GetTokensByCodeTest.tokenByCode(client, site, userId, userSecret, nonce);

            final CheckIdTokenParams params = new CheckIdTokenParams();
            params.setOxdId(site.getOxdId());
            params.setIdToken(response.getIdToken());
            params.setNonce(nonce);

            final Command checkIdTokenCommand = new Command(CommandType.CHECK_ID_TOKEN);
            checkIdTokenCommand.setParamsObject(params);
            final CommandResponse r = client.send(checkIdTokenCommand);
            assertNotNull(r);

            final CheckIdTokenResponse checkR = r.dataAsResponse(CheckIdTokenResponse.class);
            assertNotNull(checkR);
            assertTrue(checkR.isActive());
            assertNotNull(checkR.getExpiresAt());
            assertNotNull(checkR.getIssuedAt());
            assertNotNull(checkR.getClaims());

            final Map<String, List<String>> claims = checkR.getClaims();
            assertClaim(claims, "aud");
            assertClaim(claims, "iss");
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static void assertClaim(Map<String, List<String>> p_claims, String p_claimName) {
        final List<String> claimValueList = p_claims.get(p_claimName);
        assertTrue(claimValueList != null && !claimValueList.isEmpty());
    }
}
