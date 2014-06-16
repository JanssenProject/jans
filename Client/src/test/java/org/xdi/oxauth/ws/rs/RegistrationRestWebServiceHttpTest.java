package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONArray;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.HttpMethod;
import java.util.Arrays;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;

/**
 * Functional tests for Client Registration Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @version 0.1, 01.17.2012
 */
public class RegistrationRestWebServiceHttpTest extends BaseTest {

    private String clientId1;
    private String registrationAccessToken1;
    private String registrationClientUri1;

    @Parameters({"redirectUris"})
    @Test
    public void requestClientAssociate1(final String redirectUris) throws Exception {
        showTitle("requestClientAssociate1");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse response = registerClient.execRegister(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestClientAssociate2(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestClientAssociate2");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setScopes(Arrays.asList("openid", "clientinfo", "profile", "email", "invalid_scope"));
        registerRequest.setLogoUri("http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setPolicyUri("http://www.gluu.org/policy");
        registerRequest.setJwksUri("http://www.gluu.org/jwks");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setRequestUris(Arrays.asList("http://www.gluu.org/request"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getClaims().get(SCOPES.toString()));
        JSONArray scopesJsonArray = new JSONArray(response.getClaims().get(SCOPES.toString()));
        assertEquals(scopesJsonArray.get(0), "openid");
        assertEquals(scopesJsonArray.get(1), "clientinfo");
        assertEquals(scopesJsonArray.get(2), "profile");
        assertEquals(scopesJsonArray.get(3), "email");

        clientId1 = response.getClientId();
        registrationAccessToken1 = response.getRegistrationAccessToken();
        registrationClientUri1 = response.getRegistrationClientUri();
    }

    @Test(dependsOnMethods = "requestClientAssociate2")
    public void requestClientUpdate() throws Exception {
        showTitle("requestClientUpdate");

        final String logoUriNewValue = "http://www.gluu.org/test/yuriy/logo.png";
        final String contact1NewValue = "yuriy@gluu.org";
        final String contact2NewValue = "yzabrovaniy@gmail.com";

        final RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken1);
        registerRequest.setHttpMethod(HttpMethod.PUT);
        registerRequest.setContacts(Arrays.asList(contact1NewValue, contact2NewValue));
        registerRequest.setLogoUri(logoUriNewValue);

        final RegisterClient registerClient = new RegisterClient(registrationClientUri1);
        registerClient.setRequest(registerRequest);
        final RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());

        // check whether info is really updated
        final String responseContacts = response.getClaims().get(CONTACTS.toString());
        final String responseLogoUri = response.getClaims().get(LOGO_URI.toString());

        assertTrue(responseContacts.contains(contact1NewValue) && responseContacts.contains(contact2NewValue));
        assertNotNull(responseLogoUri.equals(logoUriNewValue));
    }

    @Test(dependsOnMethods = "requestClientAssociate2")
    public void requestClientRead() throws Exception {
        showTitle("requestClientRead");

        RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken1);

        RegisterClient registerClient = new RegisterClient(registrationClientUri1);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getRegistrationClientUri());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(response.getClaims().get(POLICY_URI.toString()));
        assertNotNull(response.getClaims().get(REQUEST_OBJECT_SIGNING_ALG.toString()));
        assertNotNull(response.getClaims().get(CONTACTS.toString()));
        assertNotNull(response.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertNotNull(response.getClaims().get(SUBJECT_TYPE.toString()));
        assertNotNull(response.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(response.getClaims().get(JWKS_URI.toString()));
        assertNotNull(response.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(response.getClaims().get(LOGO_URI.toString()));
        assertNotNull(response.getClaims().get(REQUEST_URIS.toString()));
        assertNotNull(response.getClaims().get("scopes"));
    }

    @Parameters({"redirectUris"})
    @Test
    // ATTENTION : uncomment test annotation only if 112-customAttributes.ldif (located in server test resources)
    // is loaded by ldap server.
    public void requestClientRegistrationWithCustomAttributes(final String redirectUris) throws Exception {
        showTitle("requestClientRegistrationWithCustomAttributes");

        final RegisterRequest request = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        // custom attribute must be declared in oxauth-config.xml in dynamic-registration-custom-attribute tag
        request.addCustomAttribute("myCustomAttr1", "customAttrValue1");
        request.addCustomAttribute("myCustomAttr2", "customAttrValue2");

        final RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(request);
        final RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Test
    public void requestClientRegistrationFail1() throws Exception {
        showTitle("requestClientRegistrationFail1");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse response = registerClient.execRegister(null, null, null);

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Test
    public void requestClientRegistrationFail2() throws Exception {
        showTitle("requestClientRegistrationFail2");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse response = registerClient.execRegister(ApplicationType.WEB, "oxAuth test app", null); // Missing redirect URIs

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Test
    public void requestClientRegistrationFail3() throws Exception {
        showTitle("requestClientRegistrationFail3");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse response = registerClient.execRegister(ApplicationType.WEB, "oxAuth test app",
                Arrays.asList("https://client.example.com/cb#fail_fragment"));

        showClient(registerClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris"})
    @Test
    public void requestClientAssociateWithFederationAttributes(final String redirectUris) throws Exception {
        showTitle("requestClientAssociateWithFederationAttributes");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterRequest request = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        request.setFederationId("1234");
        request.setFederationUrl("http://federationMetadataUrl.org");
        registerClient.setRequest(request);

        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }
}