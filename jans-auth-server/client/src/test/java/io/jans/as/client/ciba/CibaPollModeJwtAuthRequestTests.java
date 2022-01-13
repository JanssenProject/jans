/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BackchannelAuthenticationClient;
import io.jans.as.client.BackchannelAuthenticationRequest;
import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_TOKEN_DELIVERY_MODE;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_USER_CODE_PARAMETER;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Responsible to validate many cases using JWT Requests for Ciba Poll flows.
 */
public class CibaPollModeJwtAuthRequestTests extends BaseTest {

    private RegisterResponse registerResponse;
    private String idTokenHintRS384;

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowPS256HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri) throws Exception {
        showTitle("pollFlowPS256HappyFlow");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS256);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.PS256);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS384_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowPS384HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri) throws Exception {
        showTitle("pollFlowPS384HappyFlow");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS384);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.PS384);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS512_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowPS512HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri) throws Exception {
        showTitle("pollFlowPS512HappyFlow");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS512);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.PS512);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"ES256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowES256HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri) throws Exception {
        showTitle("pollFlowES256HappyFlow");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.ES256);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.ES256);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"ES384_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowES384HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri) throws Exception {
        showTitle("pollFlowES384HappyFlow");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.ES384);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.ES384);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"ES512_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowES512HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri) throws Exception {
        showTitle("pollFlowES512HappyFlow");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.ES512);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.ES512);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void cibaPollJWTRequestDataValidations(final String keyId, final String userId, final String dnName,
                                                  final String keyStoreFile, final String keyStoreSecret,
                                                  final String clientJwksUri) throws Exception {
        showTitle("cibaPollJWTRequestDataValidations");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS256);

        String clientId = registerResponse.getClientId();

        // 1. Request doesn't include Aud
        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setAud(null);

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_REQUEST.getParameter());

        // 2. Request doesn't include any hint
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setLoginHint(null);

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID.getParameter());

        // 3. Request has a wrong Binding Message
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setBindingMessage("(/)=&/(%&/(%$/&($%/&)");

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_BINDING_MESSAGE.getParameter());

        // 4. Request has wrong Client Id
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setClientId("abcabcabcabcabcabcabcabcabcabc");

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), "abcabcabcabcabcabcabcabcabcabc",
                registerResponse.getClientSecret(), 401, BackchannelAuthenticationErrorResponseType.INVALID_CLIENT.getParameter());
    }

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test(dependsOnMethods = "idTokenHintRS384")
    public void cibaPollJWTRequestIdTokenHint(final String keyId, final String userId, final String dnName,
                                              final String keyStoreFile, final String keyStoreSecret,
                                              final String clientJwksUri) throws Exception {
        showTitle("cibaPollJWTRequestIdTokenHint");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS256);

        // 1. Request doesn't include Aud
        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setLoginHint(null);
        jwtAuthorizationRequest.setIdTokenHint(idTokenHintRS384);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void cibaPollJWTRequestWrongSigning(final String keyId, final String userId, final String dnName,
                                               final String keyStoreFile, final String keyStoreSecret,
                                               final String clientJwksUri) throws Exception {
        showTitle("cibaPollJWTRequestWrongSigning");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS256);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);

        String jwt = jwtAuthorizationRequest.getEncodedJwt();
        String[] jwtParts = jwt.split("\\.");
        String jwtWithWrongSigning = jwtParts[0] + "." + jwtParts[1] + ".WRONG-SIGNING";

        processCibaAuthorizationEndpointFailCall(jwtWithWrongSigning, registerResponse.getClientId(),
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_REQUEST.getParameter());
    }

    /**
     * Registers a client using CIBA configuration for Poll flow and PS256
     *
     * @param clientJwksUri
     */
    private void registerPollClient(final String clientJwksUri, BackchannelTokenDeliveryMode mode, AsymmetricSignatureAlgorithm algorithm) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(mode);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(algorithm);
        registerRequest.setBackchannelUserCodeParameter(false);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerResponse = registerClient.exec();

        showClient(registerClient);

        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), mode.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), algorithm.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), "false");
    }

    /**
     * Process a Ciba call to the OP using JWT Request object.
     *
     * @param jwtRequest   JWT in plain String.
     * @param clientId     Client identifier.
     * @param clientSecret Client secret.
     */
    private void processCibaAuthorizationEndpointSuccessfulCall(String jwtRequest, String clientId, String clientSecret) {
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setRequest(jwtRequest);
        backchannelAuthenticationRequest.setClientId(clientId);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);

        assertEquals(backchannelAuthenticationResponse.getStatus(), 200, "Unexpected response code: " + backchannelAuthenticationResponse.getEntity());
        assertNotNull(backchannelAuthenticationResponse.getAuthReqId());
        assertNotNull(backchannelAuthenticationResponse.getExpiresIn());
        assertNotNull(backchannelAuthenticationResponse.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
    }

    /**
     * Process a Ciba call to the OP using JWT Request object and validate HTTP status and error type.
     *
     * @param jwtRequest   JWT in plain String.
     * @param clientId     Client identifier.
     * @param clientSecret Client secret.
     * @param httpStatus   Param used to validate response from the server.
     * @param error        Error used to validate error response from the server.
     */
    private void processCibaAuthorizationEndpointFailCall(String jwtRequest, String clientId, String clientSecret, int httpStatus, String error) {
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setRequest(jwtRequest);
        backchannelAuthenticationRequest.setClientId(clientId);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);

        assertEquals(backchannelAuthenticationResponse.getStatus(), httpStatus, "Unexpected response code: " + backchannelAuthenticationResponse.getEntity());
        assertNotNull(backchannelAuthenticationResponse.getErrorType());
        assertNotNull(backchannelAuthenticationResponse.getErrorDescription());
        assertEquals(error, backchannelAuthenticationResponse.getErrorType().getParameter());
        assertNull(backchannelAuthenticationResponse.getAuthReqId());
        assertNull(backchannelAuthenticationResponse.getExpiresIn());
        assertNull(backchannelAuthenticationResponse.getInterval());
    }

    /**
     * Creates a new JwtAuthorizationRequest using default configuration and params.
     */
    private JwtAuthorizationRequest createJwtRequest(String keyStoreFile, String keyStoreSecret, String dnName,
                                                     String userId, String keyId, SignatureAlgorithm signatureAlgorithm) throws Exception {
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int) (System.currentTimeMillis() / 1000);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, signatureAlgorithm, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int) (DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        return jwtAuthorizationRequest;
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void idTokenHintRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String sectorIdentifierUri) throws Exception {
        showTitle("idTokenHintRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Collections.singletonList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS384, publicKey);

        assertTrue(rsaSigner.validate(jwt));

        idTokenHintRS384 = idToken;
    }

}