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
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.client.model.SoftwareStatement;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.oxauth.model.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version December 4, 2018
 */
public class RegistrationWithSoftwareStatement extends BaseTest {

    private String registrationAccessToken1;
    private String registrationClientUri1;
    private String registrationAccessToken2;
    private String registrationClientUri2;

    /**
     * Verify signature with JWKS_URI
     */
    @Parameters({"redirectUris", "sectorIdentifierUri", "logoutUri", "keyStoreFile", "keyStoreSecret", "dnName",
            "RS256_keyId", "clientJwksUri"})
    @Test
    public void requestClientAssociate1(final String redirectUris, final String sectorIdentifierUri,
                                        final String logoutUri, final String keyStoreFile, final String keyStoreSecret,
                                        final String dnName, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("requestClientAssociate1");

        String softwareId = UUID.randomUUID().toString();
        String softwareVersion = "version_3.1.5";

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        SoftwareStatement softwareStatement = new SoftwareStatement(SignatureAlgorithm.RS256, cryptoProvider);
        softwareStatement.setKeyId(keyId);
        softwareStatement.getClaims().put(APPLICATION_TYPE.toString(), ApplicationType.WEB);
        softwareStatement.getClaims().put(CLIENT_NAME.toString(), "oxAuth test app");
        softwareStatement.getClaims().put(REDIRECT_URIS.toString(), StringUtils.spaceSeparatedToList(redirectUris));
        softwareStatement.getClaims().put(CONTACTS.toString(), Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        softwareStatement.getClaims().put(SCOPE.toString(), Util.listAsString(Arrays.asList("openid", "address", "profile", "email", "phone", "clientinfo", "invalid_scope")));
        softwareStatement.getClaims().put(LOGO_URI.toString(), "http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
        softwareStatement.getClaims().put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), AuthenticationMethod.CLIENT_SECRET_JWT);
        softwareStatement.getClaims().put(POLICY_URI.toString(), "http://www.gluu.org/policy");
        softwareStatement.getClaims().put(JWKS_URI.toString(), clientJwksUri);
        softwareStatement.getClaims().put(SECTOR_IDENTIFIER_URI.toString(), sectorIdentifierUri);
        softwareStatement.getClaims().put(SUBJECT_TYPE.toString(), SubjectType.PAIRWISE);
        softwareStatement.getClaims().put(REQUEST_URIS.toString(), Arrays.asList("http://www.gluu.org/request"));
        softwareStatement.getClaims().put(FRONT_CHANNEL_LOGOUT_URI.toString(), Lists.newArrayList(logoutUri));
        softwareStatement.getClaims().put(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString(), true);
        softwareStatement.getClaims().put(ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), SignatureAlgorithm.RS512);
        softwareStatement.getClaims().put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), KeyEncryptionAlgorithm.RSA1_5);
        softwareStatement.getClaims().put(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        softwareStatement.getClaims().put(USERINFO_SIGNED_RESPONSE_ALG.toString(), SignatureAlgorithm.RS384);
        softwareStatement.getClaims().put(USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), KeyEncryptionAlgorithm.A128KW);
        softwareStatement.getClaims().put(USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), BlockEncryptionAlgorithm.A128GCM);
        softwareStatement.getClaims().put(REQUEST_OBJECT_SIGNING_ALG.toString(), SignatureAlgorithm.RS256);
        softwareStatement.getClaims().put(REQUEST_OBJECT_ENCRYPTION_ALG.toString(), KeyEncryptionAlgorithm.A256KW);
        softwareStatement.getClaims().put(REQUEST_OBJECT_ENCRYPTION_ENC.toString(), BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        softwareStatement.getClaims().put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), AuthenticationMethod.CLIENT_SECRET_JWT);
        softwareStatement.getClaims().put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString(), SignatureAlgorithm.ES256);
        softwareStatement.getClaims().put(SOFTWARE_ID.toString(), softwareId);
        softwareStatement.getClaims().put(SOFTWARE_VERSION.toString(), softwareVersion);
        String encodedSoftwareStatement = softwareStatement.getEncodedJwt();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setSoftwareStatement(encodedSoftwareStatement);

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
        assertTrue(response.getClaims().containsKey(SOFTWARE_ID.toString()));
        assertEquals(response.getClaims().get(SOFTWARE_ID.toString()), softwareId);
        assertTrue(response.getClaims().containsKey(SOFTWARE_VERSION.toString()));
        assertEquals(response.getClaims().get(SOFTWARE_VERSION.toString()), softwareVersion);
        assertTrue(response.getClaims().containsKey(SOFTWARE_STATEMENT.toString()));

        registrationAccessToken1 = response.getRegistrationAccessToken();
        registrationClientUri1 = response.getRegistrationClientUri();
    }

    /**
     * Verify signature with JWKS
     */
    @Parameters({"redirectUris", "sectorIdentifierUri", "logoutUri", "keyStoreFile", "keyStoreSecret", "dnName",
            "RS256_keyId", "clientJwksUri"})
    @Test
    public void requestClientAssociate2(final String redirectUris, final String sectorIdentifierUri,
                                        final String logoutUri, final String keyStoreFile, final String keyStoreSecret,
                                        final String dnName, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("requestClientAssociate2");

        String softwareId = UUID.randomUUID().toString();
        String softwareVersion = "version_3.1.5";

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        SoftwareStatement softwareStatement = new SoftwareStatement(SignatureAlgorithm.RS256, cryptoProvider);
        softwareStatement.setKeyId(keyId);
        softwareStatement.getClaims().put(APPLICATION_TYPE.toString(), ApplicationType.WEB);
        softwareStatement.getClaims().put(CLIENT_NAME.toString(), "oxAuth test app");
        softwareStatement.getClaims().put(REDIRECT_URIS.toString(), StringUtils.spaceSeparatedToList(redirectUris));
        softwareStatement.getClaims().put(CONTACTS.toString(), Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        softwareStatement.getClaims().put(SCOPE.toString(), Util.listAsString(Arrays.asList("openid", "address", "profile", "email", "phone", "clientinfo", "invalid_scope")));
        softwareStatement.getClaims().put(LOGO_URI.toString(), "http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
        softwareStatement.getClaims().put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), AuthenticationMethod.CLIENT_SECRET_JWT);
        softwareStatement.getClaims().put(POLICY_URI.toString(), "http://www.gluu.org/policy");
        softwareStatement.getClaims().put(JWKS.toString(), jwkResponse.getJwks().toString());
        softwareStatement.getClaims().put(SECTOR_IDENTIFIER_URI.toString(), sectorIdentifierUri);
        softwareStatement.getClaims().put(SUBJECT_TYPE.toString(), SubjectType.PAIRWISE);
        softwareStatement.getClaims().put(REQUEST_URIS.toString(), Arrays.asList("http://www.gluu.org/request"));
        softwareStatement.getClaims().put(FRONT_CHANNEL_LOGOUT_URI.toString(), Lists.newArrayList(logoutUri));
        softwareStatement.getClaims().put(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString(), true);
        softwareStatement.getClaims().put(ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), SignatureAlgorithm.RS512);
        softwareStatement.getClaims().put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), KeyEncryptionAlgorithm.RSA1_5);
        softwareStatement.getClaims().put(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
        softwareStatement.getClaims().put(USERINFO_SIGNED_RESPONSE_ALG.toString(), SignatureAlgorithm.RS384);
        softwareStatement.getClaims().put(USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), KeyEncryptionAlgorithm.A128KW);
        softwareStatement.getClaims().put(USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), BlockEncryptionAlgorithm.A128GCM);
        softwareStatement.getClaims().put(REQUEST_OBJECT_SIGNING_ALG.toString(), SignatureAlgorithm.RS256);
        softwareStatement.getClaims().put(REQUEST_OBJECT_ENCRYPTION_ALG.toString(), KeyEncryptionAlgorithm.A256KW);
        softwareStatement.getClaims().put(REQUEST_OBJECT_ENCRYPTION_ENC.toString(), BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
        softwareStatement.getClaims().put(TOKEN_ENDPOINT_AUTH_METHOD.toString(), AuthenticationMethod.CLIENT_SECRET_JWT);
        softwareStatement.getClaims().put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString(), SignatureAlgorithm.ES256);
        softwareStatement.getClaims().put(SOFTWARE_ID.toString(), softwareId);
        softwareStatement.getClaims().put(SOFTWARE_VERSION.toString(), softwareVersion);
        String encodedSoftwareStatement = softwareStatement.getEncodedJwt();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setSoftwareStatement(encodedSoftwareStatement);

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
        assertTrue(response.getClaims().containsKey(SOFTWARE_ID.toString()));
        assertEquals(response.getClaims().get(SOFTWARE_ID.toString()), softwareId);
        assertTrue(response.getClaims().containsKey(SOFTWARE_VERSION.toString()));
        assertEquals(response.getClaims().get(SOFTWARE_VERSION.toString()), softwareVersion);
        assertTrue(response.getClaims().containsKey(SOFTWARE_STATEMENT.toString()));

        registrationAccessToken2 = response.getRegistrationAccessToken();
        registrationClientUri2 = response.getRegistrationClientUri();
    }

    @Test(dependsOnMethods = "requestClientAssociate1")
    public void requestClientRead1() throws Exception {
        showTitle("requestClientRead1");

        RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken1);

        RegisterClient registerClient = new RegisterClient(registrationClientUri1);
        registerClient.setRequest(registerRequest);
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
        assertTrue(response.getClaims().containsKey(SOFTWARE_ID.toString()));
        assertTrue(response.getClaims().containsKey(SOFTWARE_VERSION.toString()));
        assertTrue(response.getClaims().containsKey(SOFTWARE_STATEMENT.toString()));
    }

    @Test(dependsOnMethods = "requestClientAssociate2")
    public void requestClientRead2() throws Exception {
        showTitle("requestClientRead2");

        RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken2);

        RegisterClient registerClient = new RegisterClient(registrationClientUri2);
        registerClient.setRequest(registerRequest);
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
        assertTrue(response.getClaims().containsKey(SOFTWARE_ID.toString()));
        assertTrue(response.getClaims().containsKey(SOFTWARE_VERSION.toString()));
        assertTrue(response.getClaims().containsKey(SOFTWARE_STATEMENT.toString()));
    }

    @Test
    public void requestClientRegistrationFail1() throws Exception {
        showTitle("requestClientRegistrationFail1");

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setSoftwareStatement("INVALID_SOFTWARE_STATEMENT");

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

    @Test
    public void requestClientRegistrationFail2() throws Exception {
        showTitle("requestClientRegistrationFail2");

        RegisterRequest registerRequest = new RegisterRequest();
        // Test with invalid signature
        registerRequest.setSoftwareStatement("eyJhbGciOiJSUzI1NiIsImtpZCI6IjQ4YmZhOGE0LWM4YTctNGEwOS1hZTk4LWJmMzI1ZDc0OTExOSJ9.eyJhcHBsaWNhdGlvbl90eXBlIjoid2ViIiwiY2xpZW50X25hbWUiOiJveEF1dGggdGVzdCBhcHAiLCJyZWRpcmVjdF91cmlzIjpbImh0dHBzOlwvXC9jZS5nbHV1LmluZm86ODQ0M1wvb3hhdXRoLXJwXC9ob21lLmh0bSIsImh0dHBzOlwvXC9jbGllbnQuZXhhbXBsZS5jb21cL2NiIiwiaHR0cHM6XC9cL2NsaWVudC5leGFtcGxlLmNvbVwvY2IxIiwiaHR0cHM6XC9cL2NsaWVudC5leGFtcGxlLmNvbVwvY2IyIl0sImNvbnRhY3RzIjpbImphdmllckBnbHV1Lm9yZyIsImphdmllci5yb2phcy5ibHVtQGdtYWlsLmNvbSJdLCJzY29wZSI6Im9wZW5pZCBhZGRyZXNzIHByb2ZpbGUgZW1haWwgcGhvbmUgY2xpZW50aW5mbyBpbnZhbGlkX3Njb3BlIiwibG9nb191cmkiOiJodHRwOlwvXC93d3cuZ2x1dS5vcmdcL3dwLWNvbnRlbnRcL3RoZW1lc1wvZ2x1dXJzblwvaW1hZ2VzXC9sb2dvLnBuZyIsInRva2VuX2VuZHBvaW50X2F1dGhfbWV0aG9kIjoiY2xpZW50X3NlY3JldF9qd3QiLCJwb2xpY3lfdXJpIjoiaHR0cDpcL1wvd3d3LmdsdXUub3JnXC9wb2xpY3kiLCJqd2tzX3VyaSI6Imh0dHA6XC9cL2xvY2FsaG9zdFwvb3hhdXRoLWNsaWVudFwvdGVzdFwvcmVzb3VyY2VzXC9qd2tzLmpzb24iLCJzZWN0b3JfaWRlbnRpZmllcl91cmkiOiJodHRwczpcL1wvY2UuZ2x1dS5pbmZvOjg0NDNcL3NlY3RvcmlkZW50aWZpZXJcL2E1NWVkZTI5LThmNWEtNDYxZC1iMDZlLTc2Y2FlZThkNDBiNSIsInN1YmplY3RfdHlwZSI6InBhaXJ3aXNlIiwicmVxdWVzdF91cmlzIjpbImh0dHA6XC9cL3d3dy5nbHV1Lm9yZ1wvcmVxdWVzdCJdLCJmcm9udGNoYW5uZWxfbG9nb3V0X3VyaSI6WyJodHRwczpcL1wvY2UuZ2x1dS5pbmZvOjg0NDNcL294YXV0aC1ycFwvaG9tZS5odG0iXSwiZnJvbnRjaGFubmVsX2xvZ291dF9zZXNzaW9uX3JlcXVpcmVkIjp0cnVlLCJpZF90b2tlbl9zaWduZWRfcmVzcG9uc2VfYWxnIjoiUlM1MTIiLCJpZF90b2tlbl9lbmNyeXB0ZWRfcmVzcG9uc2VfYWxnIjoiUlNBMV81IiwiaWRfdG9rZW5fZW5jcnlwdGVkX3Jlc3BvbnNlX2VuYyI6IkExMjhDQkMrSFMyNTYiLCJ1c2VyaW5mb19zaWduZWRfcmVzcG9uc2VfYWxnIjoiUlMzODQiLCJ1c2VyaW5mb19lbmNyeXB0ZWRfcmVzcG9uc2VfYWxnIjoiQTEyOEtXIiwidXNlcmluZm9fZW5jcnlwdGVkX3Jlc3BvbnNlX2VuYyI6IkExMjhHQ00iLCJyZXF1ZXN0X29iamVjdF9zaWduaW5nX2FsZyI6IlJTMjU2IiwicmVxdWVzdF9vYmplY3RfZW5jcnlwdGlvbl9hbGciOiJBMjU2S1ciLCJyZXF1ZXN0X29iamVjdF9lbmNyeXB0aW9uX2VuYyI6IkEyNTZDQkMrSFM1MTIiLCJ0b2tlbl9lbmRwb2ludF9hdXRoX3NpZ25pbmdfYWxnIjoiRVMyNTYiLCJzb2Z0d2FyZV9pZCI6Ijk0MDdlMWY2LTdkMmUtNDg0YS05NTg1LTY2NWI5MmY1NzgyNSIsInNvZnR3YXJlX3ZlcnNpb24iOiJ2ZXJzaW9uXzMuMS41In0.LtrjFNPRXHArcZbAv0vcYMcOdsQG8jZ0qkNPkmAQlHwyoJN1F3jv6OI8-rdu-55osStX39_NPYjpjHwzakhi3XN0pO_b1HL6sXAkhJ-UfQ7jNgtElfJ39b0maONdEJl4nblNhEho2-SbfO_OIOIFJha-OcsTS9-DUJ6umRNfaIoNhioFzrVj8rDK-MWNcXQNCKvj4IPgH2hW7adAuj6Du1k7BdtH-IeIVb1ZCjnOl9IETbq7wyc4xL6tILw40oelgVyyHCFbIWZOJJI8n59U8DlqIBqYx0lCOjIY-BH6DLxZ1PxGrXxqMRJx1h64Oh9QxuzK-GzUY4bFInnvv3Gf3g");

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
}