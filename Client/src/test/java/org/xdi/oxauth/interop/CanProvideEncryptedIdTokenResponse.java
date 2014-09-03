package org.xdi.oxauth.interop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.oxauth.model.util.Util;

/**
 * OC5:FeatureTest-Can Provide Encrypted ID Token Response
 *
 * @author Javier Rojas Blum Date: 08.28.2013
 */
public class CanProvideEncryptedIdTokenResponse extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response A128KW A128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Read Encrypted ID Token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response A256KW A256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Read Encrypted ID Token
        Jwe jwe = Jwe.parse(idToken, null, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String modulus, final String privateExponent) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response RSA1_5 A128CBC_PLUS_HS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Read Encrypted ID Token
        RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(modulus, privateExponent);

        Jwe jwe = Jwe.parse(idToken, rsaPrivateKey, null);
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String modulus, final String privateExponent) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response RSA1_5 A256CBC_PLUS_HS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Read Encrypted ID Token
        RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(modulus, privateExponent);

        Jwe jwe = Jwe.parse(idToken, rsaPrivateKey, null);
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void canProvideEncryptedIdTokenResponseAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String modulus, final String privateExponent) throws Exception {
        showTitle("OC5:FeatureTest-Can Provide Encrypted ID Token Response RSA_OAEP A256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
        registerRequest.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Read Encrypted ID Token
        RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(modulus, privateExponent);

        Jwe jwe = Jwe.parse(idToken, rsaPrivateKey, null);
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }
}