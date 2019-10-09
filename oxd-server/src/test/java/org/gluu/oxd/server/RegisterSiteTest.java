package org.gluu.oxd.server;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;
import org.gluu.oxd.server.mapper.RegisterRequestMapper;
import org.gluu.oxd.server.service.Rp;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class RegisterSiteTest {

    private String oxdId = null;

    @Parameters({"host", "opHost", "redirectUrls", "logoutUrl", "postLogoutRedirectUrls"})
    @Test
    public void register(String host, String opHost, String redirectUrls,  String logoutUrl, String postLogoutRedirectUrls) {
        RegisterSiteResponse resp = registerSite(Tester.newClient(host), opHost, redirectUrls, postLogoutRedirectUrls, logoutUrl);
        assertNotNull(resp);

        notEmpty(resp.getOxdId());

        // more specific site registration
        final RegisterSiteParams params = new RegisterSiteParams();
        //commandParams.setProtectionAccessToken(setupClient.getClientRegistrationAccessToken());
        params.setOpHost(opHost);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setAcrValues(new ArrayList<String>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

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

        resp = Tester.newClient(host).registerSite(params);
        assertNotNull(resp);
        assertNotNull(resp.getOxdId());
        oxdId = resp.getOxdId();
    }

    @Parameters({"host"})
    @Test(dependsOnMethods = {"register"})
    public void update(String host) {
        notEmpty(oxdId);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // more specific site registration
        final UpdateSiteParams params = new UpdateSiteParams();
        params.setOxdId(oxdId);
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

        UpdateSiteResponse resp = Tester.newClient(host).updateSite(Tester.getAuthorization(), params);
        assertNotNull(resp);
    }

    public Rp createRp() {
        Rp rp = new Rp();

        rp.setClientName("clientName");
        rp.setApplicationType("web");
        rp.setTokenEndpointAuthSigningAlg("HS256");
        rp.setGrantType(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        rp.setFrontChannelLogoutUris(Lists.newArrayList("https://client.example.org/logout"));
        rp.setTokenEndpointAuthMethod("client_secret_basic");
        rp.setClientRequestUris(Lists.newArrayList("https://client.example.org/requestUri"));
        rp.setClientSectorIdentifierUri("https://client.example.org/identifierUri");
        rp.setRedirectUris(Lists.newArrayList("https://client.example.org/redirectUri"));
        rp.setRedirectUri("https://client.example.org/redirectUri");
        rp.setAccessTokenAsJwt(true);
        rp.setAccessTokenSigningAlg("HS256");
        rp.setRptAsJwt(true);
        rp.setResponseTypes(Lists.newArrayList("code"));
        rp.setAcrValues(Lists.newArrayList("basic"));
        rp.setContacts(Lists.newArrayList("contact"));
        rp.setPostLogoutRedirectUris(Lists.newArrayList("https://client.example.org/postLogoutUri"));
        rp.setScope(Lists.newArrayList("openid"));
        rp.setLogoUri("https://client.example.org/logoutUri");
        rp.setClientUri("https://client.example.org/clientUri");
        rp.setPolicyUri("https://client.example.org/policyUri");
        rp.setFrontChannelLogoutSessionRequired(true);
        rp.setTosUri("https://client.example.org/tosUri");
        rp.setJwks("{\"key1\": \"value1\", \"key2\": \"value2\"}");
        rp.setIdTokenBindingCnf("4NRB1-0XZABZI9E6-5SM3R");
        rp.setTlsClientAuthSubjectDn("www.test.com");
        rp.setSubjectType("pairwise");
        rp.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(true);
        rp.setIdTokenSignedResponseAlg("HS256");
        rp.setIdTokenEncryptedResponseAlg("RSA1_5");
        rp.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256.toString());
        rp.setUserInfoSignedResponseAlg("HS256");
        rp.setUserInfoEncryptedResponseAlg("RSA1_5");
        rp.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256.toString());
        rp.setRequestObjectSigningAlg("HS256");
        rp.setRequestObjectEncryptionAlg("RSA1_5");
        rp.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256.toString());
        rp.setDefaultMaxAge(1000);
        rp.setRequireAuthTime(true);
        rp.setInitiateLoginUri("https://client.example.org/identifierUri");
        rp.setAuthorizedOrigins(Lists.newArrayList("https://client.example.org/requestUri"));
        rp.setAccessTokenLifetime(1000);
        rp.setSoftwareId("4NRB1-0XZABZI9E6-5SM3R");
        rp.setSoftwareVersion("2.0");
        rp.setSoftwareStatement("software name");
        rp.setClientJwksUri("https://client.example.org/jwksUri");
        rp.setClaimsRedirectUri(Lists.newArrayList("https://client.example.org/requestUri"));

        return rp;
    }
    @Test
    public void testRegisterRequestMapper() throws IOException {
        RegisterRequestMapper registerRequestMapper = new RegisterRequestMapper();
        //check createRegisterRequest
        Rp rp = createRp();

        RegisterRequest request = registerRequestMapper.createRegisterRequest(rp);

        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getClaimsRedirectUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getClaimsRedirectUri())));
        assertEquals(request.getIdTokenSignedResponseAlg(), SignatureAlgorithm.HS256);
        assertEquals(request.getIdTokenEncryptedResponseAlg(), KeyEncryptionAlgorithm.RSA1_5);
        assertEquals(request.getUserInfoEncryptedResponseEnc(), BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);

        assertEquals(request.getClientName(), rp.getClientName());
        assertEquals(request.getApplicationType().toString(), rp.getApplicationType());
        assertEquals(request.getTokenEndpointAuthMethod(), AuthenticationMethod.CLIENT_SECRET_BASIC);
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getGrantTypes())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getGrantType().stream().map(item -> GrantType.fromString(item)).collect(Collectors.toList()))));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getFrontChannelLogoutUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getFrontChannelLogoutUris())));
        assertEquals(request.getTokenEndpointAuthMethod().toString(), rp.getTokenEndpointAuthMethod());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getRequestUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getClientRequestUris())));
        assertEquals(request.getSectorIdentifierUri().toString(), rp.getClientSectorIdentifierUri());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getRedirectUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getRedirectUris())));
        assertEquals(request.getAccessTokenAsJwt(), rp.getAccessTokenAsJwt());
        assertEquals(request.getAccessTokenSigningAlg().toString(), rp.getAccessTokenSigningAlg());
        assertEquals(request.getRptAsJwt(), rp.getRptAsJwt());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getResponseTypes())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getResponseTypes())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getDefaultAcrValues())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getAcrValues())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getContacts())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getContacts())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getPostLogoutRedirectUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getPostLogoutRedirectUris())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getScope())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getScope())));
        assertEquals(request.getLogoUri(), rp.getLogoUri());
        assertEquals(request.getClientUri(), rp.getClientUri());
        assertEquals(request.getPolicyUri(), rp.getPolicyUri());
        assertEquals(request.getFrontChannelLogoutSessionRequired(), rp.getFrontChannelLogoutSessionRequired());
        assertEquals(request.getTosUri(), rp.getTosUri());
        assertEquals(request.getJwks(), rp.getJwks());
        assertEquals(request.getIdTokenTokenBindingCnf(), rp.getIdTokenBindingCnf());
        assertEquals(request.getTlsClientAuthSubjectDn(), rp.getTlsClientAuthSubjectDn());
        assertEquals(request.getSubjectType().toString(), rp.getSubjectType());
        assertEquals(request.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(), rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        assertEquals(request.getIdTokenSignedResponseAlg().toString(), rp.getIdTokenSignedResponseAlg());
        assertEquals(request.getIdTokenEncryptedResponseAlg().toString(), rp.getIdTokenEncryptedResponseAlg());
        assertEquals(request.getIdTokenEncryptedResponseEnc().toString(), rp.getIdTokenEncryptedResponseEnc());
        assertEquals(request.getUserInfoSignedResponseAlg().toString(), rp.getUserInfoSignedResponseAlg());
        assertEquals(request.getUserInfoEncryptedResponseAlg().toString(), rp.getUserInfoEncryptedResponseAlg());
        assertEquals(request.getUserInfoEncryptedResponseEnc().toString(), rp.getUserInfoEncryptedResponseEnc());
        assertEquals(request.getRequestObjectSigningAlg().toString(), rp.getRequestObjectSigningAlg());
        assertEquals(request.getRequestObjectEncryptionAlg().toString(), rp.getRequestObjectEncryptionAlg());
        assertEquals(request.getRequestObjectEncryptionEnc().toString(), rp.getRequestObjectEncryptionEnc());
        assertEquals(request.getDefaultMaxAge(), rp.getDefaultMaxAge());
        assertEquals(request.getRequireAuthTime(), rp.getRequireAuthTime());
        assertEquals(request.getInitiateLoginUri(), rp.getInitiateLoginUri());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getAuthorizedOrigins())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getAuthorizedOrigins())));
        assertEquals(request.getAccessTokenLifetime(), rp.getAccessTokenLifetime());
        assertEquals(request.getSoftwareId(), rp.getSoftwareId());
        assertEquals(request.getSoftwareVersion(), rp.getSoftwareVersion());
        assertEquals(request.getSoftwareStatement(), rp.getSoftwareStatement());
        assertEquals(request.getJwksUri(), rp.getClientJwksUri());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(request.getClaimsRedirectUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getClaimsRedirectUri())));

        //check fillRp
        Rp newRp = new Rp();

        registerRequestMapper.fillRp(newRp, request);

        assertEquals(newRp.getClientName(), rp.getClientName());
        assertEquals(newRp.getApplicationType(), rp.getApplicationType());
        assertEquals(newRp.getTokenEndpointAuthMethod(), rp.getTokenEndpointAuthMethod());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getGrantType())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getGrantType())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getFrontChannelLogoutUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getFrontChannelLogoutUris())));
        assertEquals(newRp.getTokenEndpointAuthMethod().toString(), rp.getTokenEndpointAuthMethod());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getClientRequestUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getClientRequestUris())));
        assertEquals(newRp.getClientSectorIdentifierUri(), rp.getClientSectorIdentifierUri());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getRedirectUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getRedirectUris())));
        assertEquals(newRp.getAccessTokenAsJwt(), rp.getAccessTokenAsJwt());
        assertEquals(newRp.getAccessTokenSigningAlg(), rp.getAccessTokenSigningAlg());
        assertEquals(newRp.getRptAsJwt(), rp.getRptAsJwt());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getResponseTypes())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getResponseTypes())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getAcrValues())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getAcrValues())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getContacts())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getContacts())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getPostLogoutRedirectUris())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getPostLogoutRedirectUris())));
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getScope())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getScope())));
        assertEquals(newRp.getLogoUri(), rp.getLogoUri());
        assertEquals(newRp.getClientUri(), rp.getClientUri());
        assertEquals(newRp.getPolicyUri(), rp.getPolicyUri());
        assertEquals(newRp.getFrontChannelLogoutSessionRequired(), rp.getFrontChannelLogoutSessionRequired());
        assertEquals(newRp.getTosUri(), rp.getTosUri());
        assertEquals(newRp.getJwks(), rp.getJwks());
        assertEquals(newRp.getIdTokenBindingCnf(), rp.getIdTokenBindingCnf());
        assertEquals(newRp.getTlsClientAuthSubjectDn(), rp.getTlsClientAuthSubjectDn());
        assertEquals(newRp.getSubjectType(), rp.getSubjectType());
        assertEquals(newRp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(), rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        assertEquals(newRp.getIdTokenSignedResponseAlg(), rp.getIdTokenSignedResponseAlg());
        assertEquals(newRp.getIdTokenEncryptedResponseAlg(), rp.getIdTokenEncryptedResponseAlg());
        assertEquals(newRp.getIdTokenEncryptedResponseEnc(), rp.getIdTokenEncryptedResponseEnc());
        assertEquals(newRp.getUserInfoSignedResponseAlg(), rp.getUserInfoSignedResponseAlg());
        assertEquals(newRp.getUserInfoEncryptedResponseAlg(), rp.getUserInfoEncryptedResponseAlg());
        assertEquals(newRp.getUserInfoEncryptedResponseEnc(), rp.getUserInfoEncryptedResponseEnc());
        assertEquals(newRp.getRequestObjectSigningAlg(), rp.getRequestObjectSigningAlg());
        assertEquals(newRp.getRequestObjectEncryptionAlg(), rp.getRequestObjectEncryptionAlg());
        assertEquals(newRp.getRequestObjectEncryptionEnc(), rp.getRequestObjectEncryptionEnc());
        assertEquals(newRp.getDefaultMaxAge(), rp.getDefaultMaxAge());
        assertEquals(newRp.getRequireAuthTime(), rp.getRequireAuthTime());
        assertEquals(newRp.getInitiateLoginUri(), rp.getInitiateLoginUri());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getAuthorizedOrigins())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getAuthorizedOrigins())));
        assertEquals(newRp.getAccessTokenLifetime(), rp.getAccessTokenLifetime());
        assertEquals(newRp.getSoftwareId(), rp.getSoftwareId());
        assertEquals(newRp.getSoftwareVersion(), rp.getSoftwareVersion());
        assertEquals(newRp.getSoftwareStatement(), rp.getSoftwareStatement());
        assertEquals(newRp.getClientJwksUri(), rp.getClientJwksUri());
        assertEquals(Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(newRp.getClaimsRedirectUri())),
                Jackson2.createRpMapper().readTree(Jackson2.serializeWithoutNulls(rp.getClaimsRedirectUri())));

    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrls) {
        return registerSite(client, opHost, redirectUrls, redirectUrls, "");
    }

    public static RegisterSiteResponse registerSite(ClientInterface client, String opHost, String redirectUrls, String postLogoutRedirectUrls, String logoutUri) {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri));
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile"));
        params.setResponseTypes(Lists.newArrayList("code", "id_token", "token"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }
}
