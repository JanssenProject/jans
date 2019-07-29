package io.swagger.client.api;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.model.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxauth.model.common.GrantType;
import org.testng.collections.Maps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import static io.swagger.client.api.Tester.*;
import static org.testng.Assert.*;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb Khan
 * @version 11/07/2018
 */

@Test
public class RegisterSiteTest {

    private String oxdId = null;

    @Parameters({"opHost", "redirectUrls", "logoutUrl", "postLogoutRedirectUrls", "clientJwksUri", "accessTokenSigningAlg"})
    @Test
    public void register(String opHost, String redirectUrls, String logoutUrl, String postLogoutRedirectUrls,  String clientJwksUri, String accessTokenSigningAlg) throws Exception {
        DevelopersApi client = api();

        registerSite(client, opHost, redirectUrls, logoutUrl, postLogoutRedirectUrls, clientJwksUri, accessTokenSigningAlg);

        // more specific site registration
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUrl));
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setAcrValues(new ArrayList<>());
        params.setScope(Lists.newArrayList("openid", "profile"));
        params.setGrantTypes(Lists.newArrayList("authorization_code"));
        params.setResponseTypes(Lists.newArrayList("code"));

        params.setLogoUri("https://client.example.org/logo.png");
        params.setClientUri("https://client.example.org/authorization/page3");
        params.setPolicyUri("https://client.example.org/authorization/page3");
        params.setFrontChannelLogoutSessionRequired(true);
        params.setTosUri("https://localhost:5053/authorization/page3");
        params.setJwks("{\"key1\": \"value1\", \"key2\": \"value2\"}");
        params.setIdTokenBindingCnf("4NRB1-0XZABZI9E6-5SM3R");
        params.setTlsClientAuthSubjectDn("www.test.com");
        params.setDefaultMaxAge(100000000);
        params.setRequireAuthTime(true);
        params.setInitiateLoginUri("https://client.example.org/authorization/page2");
        params.setAuthorizedOrigins(Lists.newArrayList("beem://www.test.com", "fb://app.local.url"));
        params.setAccessTokenLifetime(100000000);
        params.setSoftwareId("4NRB1-0XZABZI9E6-5SM3R");
        params.setSoftwareVersion("2.0");

        Map<String, String> customAttributes = Maps.newHashMap();
        customAttributes.put("attr1", "val1");
        customAttributes.put("attr2", "val2");

        params.setCustomAttributes(customAttributes);

        params.setIdTokenSignedResponseAlg("HS256");
        params.setIdTokenEncryptedResponseAlg("RSA1_5");
        params.setIdTokenEncryptedResponseEnc("A128CBC+HS256");
        params.setUserInfoSignedResponseAlg("HS256");
        params.setUserInfoEncryptedResponseAlg("RSA1_5");
        params.setUserInfoEncryptedResponseEnc("A128CBC+HS256");
        params.setRequestObjectSigningAlg("HS256");
        params.setRequestObjectEncryptionAlg("RSA1_5");
        params.setRequestObjectEncryptionEnc("A128CBC+HS256");

        final RegisterSiteResponse resp = client.registerSite(params);
        assertNotNull(resp);
        assertNotNull(resp.getOxdId());
        oxdId = resp.getOxdId();
    }

    @Test(dependsOnMethods = {"register"})
    public void update() throws Exception {
        notEmpty(oxdId);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // more specific site registration
        final UpdateSiteParams params = new UpdateSiteParams();
        params.setOxdId(oxdId);
        params.setScope(Lists.newArrayList("profile", "oxd"));

        UpdateSiteResponse resp = api().updateSite(getAuthorization(), params);
        assertNotNull(resp);
    }

    public static RegisterSiteResponse registerSite(DevelopersApi apiClient, String opHost, String redirectUrls) throws Exception {
        return registerSite(apiClient, opHost, redirectUrls, redirectUrls, "", "", "");
    }

    public static RegisterSiteResponse registerSite(DevelopersApi apiClient, String opHost, String redirectUrls, String logoutUri, String postLogoutRedirectUrls, String clientJwksUri, String accessTokenSigningAlg) throws Exception {

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(logoutUri.split(" ")));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setClientJwksUri(clientJwksUri);
        params.setAccessTokenSigningAlg(accessTokenSigningAlg);
        final RegisterSiteResponse resp = apiClient.registerSite(params);
        assertNotNull(resp);
        assertTrue(!Strings.isNullOrEmpty(resp.getOxdId()));
        return resp;
    }

    @Parameters({"opHost", "redirectUrls", "postLogoutRedirectUrls", "clientJwksUri"})
    @Test
    public void registerWithInvalidAlgorithm(String opHost, String redirectUrls, String postLogoutRedirectUrls, String clientJwksUri) {

        final DevelopersApi client = api();

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(opHost);
        params.setRedirectUris(Lists.newArrayList(redirectUrls.split(" ")));
        params.setPostLogoutRedirectUris(Lists.newArrayList(postLogoutRedirectUrls.split(" ")));
        params.setClientFrontchannelLogoutUris(Lists.newArrayList(""));
        params.setScope(Lists.newArrayList("openid", "uma_protection", "profile", "oxd"));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(
                GrantType.AUTHORIZATION_CODE.getValue(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                GrantType.CLIENT_CREDENTIALS.getValue()));
        params.setClientJwksUri(clientJwksUri);
        params.setAccessTokenSigningAlg("blahBlah");

        try {
            client.registerSite(params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);  //BAD Request
        }

    }

}
