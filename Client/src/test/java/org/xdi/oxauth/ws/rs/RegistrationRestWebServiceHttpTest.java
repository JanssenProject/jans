/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import com.google.common.collect.Lists;
import org.codehaus.jettison.json.JSONArray;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.common.GrantType.*;
import static org.xdi.oxauth.model.common.ResponseType.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;

/**
 * Functional tests for Client Registration Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @version March 13, 2019
 */
public class RegistrationRestWebServiceHttpTest extends BaseTest {

    private String registrationAccessToken1;
    private String registrationClientUri1;
    private String registrationAccessToken2;
    private String registrationClientUri2;

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestClientAssociate1(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestClientAssociate1");

        // 1. Register Client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(Arrays.asList(
                AUTHORIZATION_CODE,
                IMPLICIT,
                RESOURCE_OWNER_PASSWORD_CREDENTIALS,
                CLIENT_CREDENTIALS,
                REFRESH_TOKEN,
                OXAUTH_UMA_TICKET));
        registerRequest.setResponseTypes(Arrays.asList(
                CODE,
                TOKEN,
                ID_TOKEN
        ));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client Update
        String newClientName = "New Client Name";

        RegisterRequest clientUpdateRequest = new RegisterRequest(registrationAccessToken);
        clientUpdateRequest.setHttpMethod(HttpMethod.PUT);
        clientUpdateRequest.setClientName(newClientName);

        RegisterClient clientUpdateClient = new RegisterClient(registrationClientUri);
        clientUpdateClient.setRequest(clientUpdateRequest);
        RegisterResponse clientUpdateResponse = clientUpdateClient.exec();

        showClient(clientUpdateClient);
        assertEquals(clientUpdateResponse.getStatus(), 200, "Unexpected response code: " + clientUpdateResponse.getEntity());
        assertEquals(clientUpdateResponse.getClaims().get(CLIENT_NAME.toString()), newClientName);
        assertEquals(clientUpdateResponse.getClientId(), registerResponse.getClientId());
        assertEquals(clientUpdateResponse.getClientSecret(), registerResponse.getClientSecret());
        assertEquals(clientUpdateResponse.getRegistrationAccessToken(), registerResponse.getRegistrationAccessToken());
        assertEquals(clientUpdateResponse.getRegistrationClientUri(), registerResponse.getRegistrationClientUri());
        assertEquals(clientUpdateResponse.getClientIdIssuedAt(), registerResponse.getClientIdIssuedAt());
        assertEquals(clientUpdateResponse.getClientSecretExpiresAt(), registerResponse.getClientSecretExpiresAt());
        assertEquals(clientUpdateResponse.getResponseTypes(), registerResponse.getResponseTypes());
        assertEquals(clientUpdateResponse.getGrantTypes(), registerResponse.getGrantTypes());
        assertEquals(clientUpdateResponse.getClaims().get(REDIRECT_URIS.toString()), registerResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(APPLICATION_TYPE.toString()), registerResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()), registerResponse.getClaims().get(SECTOR_IDENTIFIER_URI.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(SUBJECT_TYPE.toString()), registerResponse.getClaims().get(SUBJECT_TYPE.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()), registerResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()), registerResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(REQUIRE_AUTH_TIME.toString()), registerResponse.getClaims().get(REQUIRE_AUTH_TIME.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(RPT_AS_JWT.toString()), registerResponse.getClaims().get(RPT_AS_JWT.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(ACCESS_TOKEN_AS_JWT.toString()), registerResponse.getClaims().get(ACCESS_TOKEN_AS_JWT.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(ACCESS_TOKEN_SIGNING_ALG.toString()), registerResponse.getClaims().get(ACCESS_TOKEN_SIGNING_ALG.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString()), registerResponse.getClaims().get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString()));
        assertEquals(clientUpdateResponse.getClaims().get(SCOPE.toString()), registerResponse.getClaims().get(SCOPE.toString()));
    }

    @Parameters({"redirectUris", "sectorIdentifierUri", "logoutUri"})
    @Test
    public void requestClientAssociate2(final String redirectUris, final String sectorIdentifierUri,
                                        final String logoutUri) throws Exception {
        showTitle("requestClientAssociate2");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setScope(Arrays.asList("openid", "address", "profile", "email", "phone", "clientinfo", "invalid_scope"));
        registerRequest.setLogoUri("http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setPolicyUri("http://www.gluu.org/policy");
        registerRequest.setJwksUri("http://www.gluu.org/jwks");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setRequestUris(Arrays.asList("http://www.gluu.org/request"));
        registerRequest.setFrontChannelLogoutUris(Lists.newArrayList(logoutUri));
        registerRequest.setFrontChannelLogoutSessionRequired(true);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientExecutor(true));
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getClaims().get(SCOPE.toString()));
        assertNotNull(response.getClaims().get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString()));
        assertTrue(Boolean.parseBoolean(response.getClaims().get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString())));
        assertNotNull(response.getClaims().get(FRONT_CHANNEL_LOGOUT_URI.toString()));
        assertTrue(new JSONArray(response.getClaims().get(FRONT_CHANNEL_LOGOUT_URI.toString())).getString(0).equals(logoutUri));
        assertNotNull(response.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertEquals(SignatureAlgorithm.RS512,
                SignatureAlgorithm.fromString(response.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString())));
        assertNotNull(response.getClaims().get(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString()));
        assertEquals(KeyEncryptionAlgorithm.RSA1_5,
                KeyEncryptionAlgorithm.fromName(response.getClaims().get(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString())));
        assertNotNull(response.getClaims().get(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString()));
        assertEquals(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256,
                BlockEncryptionAlgorithm.fromName(response.getClaims().get(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString())));
        assertNotNull(response.getClaims().get(USERINFO_SIGNED_RESPONSE_ALG.toString()));
        assertEquals(SignatureAlgorithm.RS384,
                SignatureAlgorithm.fromString(response.getClaims().get(USERINFO_SIGNED_RESPONSE_ALG.toString())));
        assertNotNull(response.getClaims().get(USERINFO_ENCRYPTED_RESPONSE_ALG.toString()));
        assertEquals(KeyEncryptionAlgorithm.A128KW,
                KeyEncryptionAlgorithm.fromName(response.getClaims().get(USERINFO_ENCRYPTED_RESPONSE_ALG.toString())));
        assertNotNull(response.getClaims().get(USERINFO_ENCRYPTED_RESPONSE_ENC.toString()));
        assertEquals(BlockEncryptionAlgorithm.A128GCM,
                BlockEncryptionAlgorithm.fromName(response.getClaims().get(USERINFO_ENCRYPTED_RESPONSE_ENC.toString())));
        assertNotNull(response.getClaims().get(REQUEST_OBJECT_SIGNING_ALG.toString()));
        assertEquals(SignatureAlgorithm.RS256,
                SignatureAlgorithm.fromString(response.getClaims().get(REQUEST_OBJECT_SIGNING_ALG.toString())));
        assertNotNull(response.getClaims().get(REQUEST_OBJECT_ENCRYPTION_ALG.toString()));
        assertEquals(KeyEncryptionAlgorithm.A256KW,
                KeyEncryptionAlgorithm.fromName(response.getClaims().get(REQUEST_OBJECT_ENCRYPTION_ALG.toString())));
        assertNotNull(response.getClaims().get(REQUEST_OBJECT_ENCRYPTION_ENC.toString()));
        assertEquals(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512,
                BlockEncryptionAlgorithm.fromName(response.getClaims().get(REQUEST_OBJECT_ENCRYPTION_ENC.toString())));
        assertNotNull(response.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(AuthenticationMethod.CLIENT_SECRET_JWT,
                AuthenticationMethod.fromString(response.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString())));
        assertNotNull(response.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(SignatureAlgorithm.ES256,
                SignatureAlgorithm.fromString(response.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString())));
        JSONArray scopesJsonArray = new JSONArray(StringUtils.spaceSeparatedToList(response.getClaims().get(SCOPE.toString())));
        List<String> scopes = new ArrayList<String>();
        for (int i = 0; i < scopesJsonArray.length(); i++) {
            scopes.add(scopesJsonArray.get(i).toString());
        }
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("address"));
        assertTrue(scopes.contains("email"));
        assertTrue(scopes.contains("profile"));
        assertTrue(scopes.contains("phone"));
        assertTrue(scopes.contains("clientinfo"));

        registrationAccessToken1 = response.getRegistrationAccessToken();
        registrationClientUri1 = response.getRegistrationClientUri();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestClientAssociate3(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestClientAssociate3");
        String softwareId = UUID.randomUUID().toString();
        String softwareVersion = "version_3.1.5";
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSoftwareId(softwareId);
        registerRequest.setSoftwareVersion(softwareVersion);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();
        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
        assertTrue(response.getClaims().containsKey(SOFTWARE_ID.toString()));
        assertEquals(response.getClaims().get(SOFTWARE_ID.toString()), softwareId);
        assertTrue(response.getClaims().containsKey(SOFTWARE_VERSION.toString()));
        assertEquals(response.getClaims().get(SOFTWARE_VERSION.toString()), softwareVersion);
    }

    @Test(dependsOnMethods = "requestClientAssociate2")
    public void requestClientUpdate() throws Exception {
        showTitle("requestClientUpdate");

        final String logoUriNewValue = "http://www.gluu.org/test/yuriy/logo.png";
        final String contact1NewValue = "yuriy@gluu.org";
        final String contact2NewValue = "yuriyz@gmail.com";

        final RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken1);
        registerRequest.setHttpMethod(HttpMethod.PUT);
        registerRequest.setContacts(Arrays.asList(contact1NewValue, contact2NewValue));
        registerRequest.setLogoUri(logoUriNewValue);

        final RegisterClient registerClient = new RegisterClient(registrationClientUri1);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientExecutor(true));
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
        assertNotNull(response.getClaims().get(SCOPE.toString()));
    }

    @Parameters({"redirectUris", "sectorIdentifierUri", "logoutUri"})
    @Test
    public void requestClientAssociate3(final String redirectUris, final String sectorIdentifierUri,
                                        final String logoutUri) throws Exception {
        showTitle("requestClientAssociate3");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setPostLogoutRedirectUris(Lists.newArrayList(logoutUri));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri); //
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.IMPLICIT));
        registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
        registerRequest.setScope(Arrays.asList("openid", "profile", "email"));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setFrontChannelLogoutSessionRequired(true);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientExecutor(true));
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getClaims().get(SCOPE.toString()));
        assertNotNull(response.getClaims().get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString()));
        assertTrue(Boolean.parseBoolean(response.getClaims().get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString())));
        assertNotNull(response.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertEquals(SignatureAlgorithm.RS256,
                SignatureAlgorithm.fromString(response.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString())));
        assertEquals(AuthenticationMethod.CLIENT_SECRET_POST,
                AuthenticationMethod.fromString(response.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString())));
        JSONArray scopesJsonArray = new JSONArray(StringUtils.spaceSeparatedToList(response.getClaims().get(SCOPE.toString())));
        List<String> scopes = new ArrayList<String>();
        for (int i = 0; i < scopesJsonArray.length(); i++) {
            scopes.add(scopesJsonArray.get(i).toString());
        }
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("email"));
        assertTrue(scopes.contains("profile"));

        registrationAccessToken2 = response.getRegistrationAccessToken();
        registrationClientUri2 = response.getRegistrationClientUri();
    }

    @Test(dependsOnMethods = "requestClientAssociate3")
    public void requestClientUpdate3() throws Exception {
        showTitle("requestClientUpdate3");

        final String clientName = "Dynamically Registered Client #1 update_1";

        final RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken2);
        registerRequest.setHttpMethod(HttpMethod.PUT);

        registerRequest.setRedirectUris(Arrays.asList("https://localhost:8443/auth"));
        registerRequest.setPostLogoutRedirectUris(Arrays.asList("https://localhost:8443/auth"));
        registerRequest.setApplicationType(ApplicationType.WEB);
        registerRequest.setClientName(clientName);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setGrantTypes(Arrays.asList(GrantType.IMPLICIT));
        registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
        registerRequest.setScope(Arrays.asList("openid", "address", "profile", "email", "phone", "clientinfo", "invalid_scope"));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setFrontChannelLogoutSessionRequired(true);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        final RegisterClient registerClient = new RegisterClient(registrationClientUri2);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientExecutor(true));
        final RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());

        assertTrue(response.getClaims().containsKey(CLIENT_NAME.toString()));
        assertEquals(clientName, response.getClaims().get(CLIENT_NAME.toString()));
        JSONArray scopesJsonArray = new JSONArray(StringUtils.spaceSeparatedToList(response.getClaims().get(SCOPE.toString())));
        List<String> scopes = new ArrayList<String>();
        for (int i = 0; i < scopesJsonArray.length(); i++) {
            scopes.add(scopesJsonArray.get(i).toString());
        }
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("address"));
        assertTrue(scopes.contains("email"));
        assertTrue(scopes.contains("profile"));
        assertTrue(scopes.contains("phone"));
        assertTrue(scopes.contains("clientinfo"));
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    // ATTENTION : uncomment test annotation only if 112-customAttributes.ldif (located in server test resources)
    // is loaded by ldap server.
    public void requestClientRegistrationWithCustomAttributes(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestClientRegistrationWithCustomAttributes");

        final RegisterRequest request = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        // custom attribute must be declared in oxauth-config.xml in dynamic-registration-custom-attribute tag
        request.addCustomAttribute("myCustomAttr1", "customAttrValue1");
        request.addCustomAttribute("myCustomAttr2", "customAttrValue2");
        request.setSectorIdentifierUri(sectorIdentifierUri);

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
    public void requestClientRegistrationFail4(final String redirectUris) throws Exception {
        showTitle("requestClientRegistrationFail4");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.NONE); // id_token signature cannot be none

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientExecutor(true));
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400);
        assertNotNull(response.getEntity());
        assertNotNull(response.getErrorType());
        assertNotNull(response.getErrorDescription());
    }

    @Parameters({"redirectUris"})
    @Test
    public void registerWithCustomURI(final String redirectUris) throws Exception {
        showTitle("registerWithCustomURI");

        List<String> redirectUriList = Lists.newArrayList(StringUtils.spaceSeparatedToList(redirectUris));
        redirectUriList.add("myschema://client.example.com/cb"); // URI with custom schema

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "oxAuth native test app with custom schema in URI",
                redirectUriList);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientExecutor(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void registerWithApplicationTypeNativeAndSubjectTypePairwise(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("registerWithApplicationTypeNativeAndSubjectTypePairwise");

        List<String> redirectUriList = Lists.newArrayList(StringUtils.spaceSeparatedToList(redirectUris));

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "oxAuth native test app",
                redirectUriList);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientExecutor(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Parameters({"redirectUris"})
    @Test
    public void registerWithHttp1(final String redirectUris) throws Exception {
        showTitle("registerWithHttp1");

        List<String> redirectUriList = Lists.newArrayList(StringUtils.spaceSeparatedToList(redirectUris));
        redirectUriList.add("http://localhost/cb"); // URI with HTTP schema

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth web test app with HTTP schema in URI",
                redirectUriList);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientExecutor(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Parameters({"redirectUris"})
    @Test
    public void registerWithHttp2(final String redirectUris) throws Exception {
        showTitle("registerWithHttp2");

        List<String> redirectUriList = Lists.newArrayList(StringUtils.spaceSeparatedToList(redirectUris));
        redirectUriList.add("http://127.0.0.1/cb"); // URI with HTTP schema

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth web test app with HTTP schema in URI",
                redirectUriList);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientExecutor(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Parameters({"redirectUris"})
    @Test
    public void registerWithHttpFail(final String redirectUris) throws Exception {
        showTitle("registerWithHttpFail");

        List<String> redirectUriList = Lists.newArrayList(StringUtils.spaceSeparatedToList(redirectUris));
        redirectUriList.add("http://www.example.com/cb"); // URI with HTTP schema

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth web test app with HTTP schema in URI",
                redirectUriList);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientExecutor(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 400);
        assertNotNull(response.getEntity());
        assertNotNull(response.getErrorType());
        assertNotNull(response.getErrorDescription());
    }
}