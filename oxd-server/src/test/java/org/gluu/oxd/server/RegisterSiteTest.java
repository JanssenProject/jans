package org.gluu.oxd.server;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.*;

import static org.gluu.oxd.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class RegisterSiteTest {

    private RegisterSiteResponse site = null;

    @Parameters({"host", "opHost", "redirectUrls", "logoutUrl", "postLogoutRedirectUrls"})
    @Test
    public void register(String host, String opHost, String redirectUrls, String logoutUrl, String postLogoutRedirectUrls) {
        RegisterSiteResponse resp = registerSite(Tester.newClient(host), opHost, redirectUrls, postLogoutRedirectUrls, logoutUrl, false);
        assertNotNull(resp);

        notEmpty(resp.getOxdId());
        site = resp;
    }

    @Parameters({"host", "opConfigurationEndpoint", "redirectUrls", "logoutUrl", "postLogoutRedirectUrls"})
    @Test
    public static void register_withOpConfigurationEndpoint(String host, String opConfigurationEndpoint, String redirectUrls, String logoutUrl, String postLogoutRedirectUrls) {

        // more specific site registration
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpConfigurationEndpoint(opConfigurationEndpoint);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setAcrValues(new ArrayList<String>());
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setResponseTypes(Lists.newArrayList("code", "id_token", "token"));

        params.setClientName("oxd-client-extension-up" + System.currentTimeMillis());
        params.setClientTokenEndpointAuthMethod("client_secret_basic");
        params.setClientTokenEndpointAuthSigningAlg("HS256");
        params.setClaimsRedirectUri(Lists.newArrayList("https://client.example.org"));

        params.setAccessTokenSigningAlg("HS256");
        params.setRptAsJwt(true);
        params.setAccessTokenAsJwt(true);
        params.setFrontChannelLogoutSessionRequired(true);
        params.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(true);
        params.setRequireAuthTime(true);

        params.setLogoUri("https://client.example.org/authorization/page3");
        params.setClientUri("https://client.example.org/authorization/page3");
        params.setPolicyUri("https://client.example.org/authorization/page3");

        params.setTosUri("https://client.example.org/authorization/page3");
        params.setJwks("{\"key1\": \"value1\", \"key2\": \"value2\"}");
        params.setIdTokenBindingCnf("4NRB1-0XZABZI9E6-5SM3R");
        params.setTlsClientAuthSubjectDn("www.test-updated.com");
        params.setSubjectType("pairwise");

        params.setIdTokenSignedResponseAlg("HS256");
        params.setIdTokenEncryptedResponseAlg("RSA1_5");
        params.setIdTokenEncryptedResponseEnc("A128CBC+HS256");
        params.setUserInfoSignedResponseAlg("HS256");
        params.setUserInfoEncryptedResponseAlg("RSA1_5");
        params.setUserInfoEncryptedResponseEnc("A128CBC+HS256");
        params.setRequestObjectSigningAlg("HS256");
        params.setRequestObjectEncryptionAlg("RSA1_5");
        params.setRequestObjectEncryptionEnc("A128CBC+HS256");
        params.setDefaultMaxAge(100000000);

        params.setInitiateLoginUri("https://client.example.org/authorization/page2");
        params.setAuthorizedOrigins(Lists.newArrayList("beem://www.test.com", "fb://app.local.url"));
        params.setAccessTokenLifetime(100000000);
        params.setSoftwareId("4NRB1-0XZABZI9E6-5SM3R");
        params.setSoftwareVersion("2.0");

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("k1", "v1");
        customAttributes.put("k2", "v2");
        params.setCustomAttributes(customAttributes);

        RegisterSiteResponse resp = Tester.newClient(host).registerSite(params);
        assertNotNull(resp);
        assertNotNull(resp.getOxdId());
    }

    @Parameters({"host"})
    @Test(dependsOnMethods = {"register"})
    public void update(String host) {
        notEmpty(site.getOxdId());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // more specific site registration
        final UpdateSiteParams params = new UpdateSiteParams();
        params.setOxdId(site.getOxdId());
        params.setScope(Lists.newArrayList("profile"));

        params.setClientName("oxd-client-updated-test");
        params.setClientTokenEndpointAuthMethod("client_secret_basic");
        params.setClientTokenEndpointAuthSigningAlg("HS256");
        params.setClaimsRedirectUri(Lists.newArrayList("https://client.example.org/update"));

        params.setAccessTokenSigningAlg("RS256");
        params.setAccessTokenAsJwt(false);
        params.setRptAsJwt(true);
        params.setFrontChannelLogoutSessionRequired(false);
        params.setRequireAuthTime(false);
        params.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(true);

        params.setLogoUri("https://client.example.org/authorization//update1");
        params.setClientUri("https://client.example.org/authorization/update2");
        params.setPolicyUri("https://client.example.org/authorization/update3");

        params.setTosUri("https://client.example.org/authorization/update4");
        params.setJwks("{\"key1\": \"value1\", \"key2\": \"value2\"}");
        params.setIdTokenBindingCnf("4NRB1-0XZABZI9E6-5SM3R");
        params.setTlsClientAuthSubjectDn("www.test.com");
        params.setSubjectType("pairwise");

        params.setIdTokenSignedResponseAlg("PS256");
        params.setIdTokenEncryptedResponseAlg("A128KW");
        params.setIdTokenEncryptedResponseEnc("A128CBC+HS256");
        params.setUserInfoSignedResponseAlg("HS256");
        params.setUserInfoEncryptedResponseAlg("RSA1_5");
        params.setUserInfoEncryptedResponseEnc("A128CBC+HS256");
        params.setRequestObjectSigningAlg("HS256");
        params.setRequestObjectEncryptionAlg("RSA1_5");
        params.setRequestObjectEncryptionEnc("A128CBC+HS256");
        params.setDefaultMaxAge(200000000);

        params.setInitiateLoginUri("https://client.example.org/authorization/page2");
        params.setAuthorizedOrigins(Lists.newArrayList("beem://www.test-updated.com", "fb://updated.local.url"));
        params.setAccessTokenLifetime(200000000);
        params.setSoftwareId("4NRB1-0XZABZI9E6-5SM3R");
        params.setSoftwareVersion("3.0");

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("key1", "v1");
        customAttributes.put("key2", "v2");
        params.setCustomAttributes(customAttributes);

        UpdateSiteResponse resp = Tester.newClient(host).updateSite(Tester.getAuthorization(site), null, params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrls) {
        return registerSite(client, opHost, redirectUrls, redirectUrls, "", false);
    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrls, String postLogoutRedirectUrls, String logoutUri, boolean syncClientFromOp) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setResponseTypes(Lists.newArrayList("code", "id_token", "token"));
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setSyncClientFromOp(syncClientFromOp);
        params.setSyncClientPeriodInSeconds(0);

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrls, String idTokenSignedResponseAlg) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setResponseTypes(Lists.newArrayList("code", "id_token", "token"));
        params.setIdTokenSignedResponseAlg(idTokenSignedResponseAlg);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

    public static RegisterSiteResponse registerSite(
            ClientInterface client, String opHost, String redirectUrls, List<String> scopes, List<String> responseTypes,
            boolean allowSpontaneousScopes, List<String> spontaneousScopes) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setScope(scopes);
        params.setResponseTypes(responseTypes);
        params.setAllowSpontaneousScopes(true);
        params.setSpontaneousScopes(spontaneousScopes);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

    public static RegisterSiteResponse registerSite_withAuthenticationMethod(ClientInterface client, String opHost, String redirectUrls, String algorithm, String authenticationMethod) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setResponseTypes(Lists.newArrayList("code", "id_token", "token"));
        params.setIdTokenSignedResponseAlg(algorithm);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setClientTokenEndpointAuthSigningAlg(algorithm);
        params.setClientTokenEndpointAuthMethod(authenticationMethod);
        params.setJwks(client.getRpJwks().toString());

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
