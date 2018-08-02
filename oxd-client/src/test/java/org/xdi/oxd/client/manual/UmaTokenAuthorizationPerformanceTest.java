package org.xdi.oxd.client.manual;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.UmaMetadataService;
import org.xdi.oxauth.client.uma.UmaTokenService;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.token.ClientAssertionType;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaTokenResponse;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.client.UmaFullTest;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.params.SetupClientParams;
import org.xdi.oxd.common.response.RpGetRptResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.common.response.SetupClientResponse;
import org.xdi.oxd.rs.protect.RsResource;

import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author yuriyz
 */
public class UmaTokenAuthorizationPerformanceTest {

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void authorizeRpt(String host, int port, String redirectUrl, String opHost, String rsProtect) throws Exception {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final SetupClientResponse site = setupClient(client, opHost, redirectUrl, redirectUrl, "", null, null);

            protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
            RsCheckAccessResponse checkAccess = checkAccess(client, site);


            final SetupClientResponse forJwt = setupClientSecretJwt(client, opHost, redirectUrl);

            UmaMetadataService metadataService = UmaClientFactory.instance().createMetadataService("https://ce-dev4.gluu.org/.well-known/uma2-configuration", new ApacheHttpClient4Executor(CoreUtils.createHttpClientTrustAll()));
            UmaMetadata metadata = metadataService.getMetadata();

            final UmaTokenService tokenService = UmaClientFactory.instance().createTokenService(metadata, new ApacheHttpClient4Executor(CoreUtils.createHttpClientTrustAll()));
            UmaTokenResponse umaTokenResponse = tokenService.requestJwtAuthorizationRpt(ClientAssertionType.JWT_BEARER.toString(), getClientAssertion(forJwt, metadata), GrantType.OXAUTH_UMA_TICKET.toString(), checkAccess.getTicket(), null, null, null, null, null);

            System.out.println("TOKEN: " + umaTokenResponse);

            final RpGetRptParams params = new RpGetRptParams();
            params.setOxdId(site.getOxdId());
            params.setTicket(checkAccess.getTicket());

            final RpGetRptResponse response = client
                    .send(new Command(CommandType.RP_GET_RPT).setParamsObject(params))
                    .dataAsResponse(RpGetRptResponse.class);

            assertNotNull(response);
            assertTrue(StringUtils.isNotBlank(response.getRpt()));
            assertTrue(StringUtils.isNotBlank(response.getPct()));
            System.out.println(response);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private static String getClientAssertion(SetupClientResponse forJwt, UmaMetadata metadata) throws Exception {
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider("U:\\own\\project\\git\\oxd\\master\\oxd-client\\src\\test\\resources\\client_keystore.jks", "secret", "CN=oxAuth CA Certificates");

        TokenRequest tokenRequest = new TokenRequest(GrantType.OXAUTH_UMA_TICKET);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(forJwt.getClientId());
        tokenRequest.setAuthPassword(forJwt.getClientSecret());
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(metadata.getTokenEndpoint());
        return tokenRequest.getClientAssertion();
    }

    public static RsProtectResponse protectResources(CommandClient client, SetupClientResponse site, List<RsResource> resources) {
        final RsProtectParams commandParams = new RsProtectParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setResources(resources);

        final RsProtectResponse resp = client
                .send(new Command(CommandType.RS_PROTECT).setParamsObject(commandParams))
                .dataAsResponse(RsProtectResponse.class);
        Assert.assertNotNull(resp);
        return resp;
    }

    public static RsCheckAccessResponse checkAccess(CommandClient client, SetupClientResponse site) {
        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");

        final RsCheckAccessResponse response = client
                .send(new Command(CommandType.RS_CHECK_ACCESS).setParamsObject(params))
                .dataAsResponse(RsCheckAccessResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(StringUtils.isNotBlank(response.getAccess()));
        return response;
    }

    public static SetupClientResponse setupClientSecretJwt(CommandClient client, String opHost, String redirectUrl) {
        return setupClient(client, opHost, redirectUrl, redirectUrl, "", AuthenticationMethod.CLIENT_SECRET_JWT, "https://ce-dev4.gluu.org/oxauth/sectoridentifier/@!38D4.410C.1D43.8932!0001!37F2.B744!0012!D426.70FD");
    }

    public static SetupClientResponse setupClient(CommandClient client, String opHost, String redirectUrl, String postLogoutRedirectUrl, String logoutUri, AuthenticationMethod authenticationMethod, String sectorIdentifier) {

        final SetupClientParams params = new SetupClientParams();
        params.setOpHost(opHost);
        params.setAuthorizationRedirectUri(redirectUrl);
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setClientFrontchannelLogoutUri(Lists.newArrayList(logoutUri));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setTrustedClient(true);
        params.setGrantType(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setOxdRpProgrammingLanguage("java");
        params.setClientTokenEndpointAuthMethod(authenticationMethod != null ? authenticationMethod.toString() : "");
        params.setClientSectorIdentifierUri(sectorIdentifier);

        final Command command = new Command(CommandType.SETUP_CLIENT);
        command.setParamsObject(params);

        final SetupClientResponse resp = client.send(command).dataAsResponse(SetupClientResponse.class);
        Assert.assertNotNull(resp);
        Assert.assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
