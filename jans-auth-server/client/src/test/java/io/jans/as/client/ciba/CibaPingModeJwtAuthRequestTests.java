/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.client.ws.rs.Tester;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Responsible to validate many cases using JWT Requests for Ciba Ping flows.
 */
public class CibaPingModeJwtAuthRequestTests extends BaseTest {

    private RegisterResponse registerResponse;
    private String idTokenHintRS384;

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void pingFlowPS256HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("pingFlowPS256HappyFlow");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.PS256,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.PS256);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS384_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void pingFlowPS384HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("pingFlowPS384HappyFlow");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.PS384,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.PS384);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS512_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void pingFlowPS512HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("pingFlowPS512HappyFlow");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.PS512,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.PS512);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"ES256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void pingFlowES256HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("pingFlowES256HappyFlow");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.ES256,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.ES256);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"ES384_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void pingFlowES384HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("pingFlowES384HappyFlow");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.ES384,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.ES384);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"ES512_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void pingFlowES512HappyFlow(final String keyId, final String userId, final String dnName,
                                       final String keyStoreFile, final String keyStoreSecret,
                                       final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("pingFlowES512HappyFlow");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.ES512,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName,
                userId, keyId, SignatureAlgorithm.ES512);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void cibaPingJWTRequestDataValidations(final String keyId, final String userId, final String dnName,
                                                  final String keyStoreFile, final String keyStoreSecret,
                                                  final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("cibaPingJWTRequestDataValidations");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.PS256,
                backchannelClientNotificationEndpoint);

        String clientId = registerResponse.getClientId();

        // 1. Request doesn't include Aud
        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setAud(null);

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_REQUEST);

        // 2. Request doesn't include any hint
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setLoginHint(null);

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID);

        // 3. Request has a wrong Binding Message
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setBindingMessage("(/)=&/(%&/(%$/&($%/&)");

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_BINDING_MESSAGE);

        // 4. Request has wrong Client Id
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setClientId("abcabcabcabcabcabcabcabcabcabc");

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), "abcabcabcabcabcabcabcabcabcabc",
                registerResponse.getClientSecret(), 401, BackchannelAuthenticationErrorResponseType.INVALID_CLIENT);

        // 5. Request has wrong Client Id
        jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setClientNotificationToken(null);

        processCibaAuthorizationEndpointFailCall(jwtAuthorizationRequest.getEncodedJwt(), clientId,
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_REQUEST);
    }

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test(dependsOnMethods = "idTokenHintRS384")
    public void cibaPingJWTRequestIdTokenHint(final String keyId, final String userId, final String dnName,
                                              final String keyStoreFile, final String keyStoreSecret,
                                              final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("cibaPingJWTRequestIdTokenHint");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.PS256,
                backchannelClientNotificationEndpoint);

        // 1. Request doesn't include Aud
        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);
        jwtAuthorizationRequest.setLoginHint(null);
        jwtAuthorizationRequest.setIdTokenHint(idTokenHintRS384);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(),
                registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri",
            "backchannelClientNotificationEndpoint"})
    @Test
    public void cibaPingJWTRequestWrongSigning(final String keyId, final String userId, final String dnName,
                                               final String keyStoreFile, final String keyStoreSecret,
                                               final String clientJwksUri, final String backchannelClientNotificationEndpoint) throws Exception {
        showTitle("cibaPingJWTRequestWrongSigning");
        registerPingClient(clientJwksUri, BackchannelTokenDeliveryMode.PING, AsymmetricSignatureAlgorithm.PS256,
                backchannelClientNotificationEndpoint);

        JwtAuthorizationRequest jwtAuthorizationRequest = createJwtRequest(keyStoreFile, keyStoreSecret, dnName, userId, keyId, SignatureAlgorithm.PS256);

        String jwt = jwtAuthorizationRequest.getEncodedJwt();
        String[] jwtParts = jwt.split("\\.");
        String jwtWithWrongSigning = jwtParts[0] + "." + jwtParts[1] + ".WRONG-SIGNING";

        processCibaAuthorizationEndpointFailCall(jwtWithWrongSigning, registerResponse.getClientId(),
                registerResponse.getClientSecret(), 400, BackchannelAuthenticationErrorResponseType.INVALID_REQUEST);
    }

    /**
     * Registers a client using CIBA configuration for Ping flow and PS256
     *
     * @param clientJwksUri
     */
    private void registerPingClient(final String clientJwksUri, final BackchannelTokenDeliveryMode mode,
                                    final AsymmetricSignatureAlgorithm algorithm, final String backchannelClientNotificationEndpoint) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));
        registerRequest.setScope(Tester.standardScopes);

        registerRequest.setBackchannelTokenDeliveryMode(mode);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(algorithm);
        registerRequest.setBackchannelUserCodeParameter(false);
        registerRequest.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerResponse = registerClient.exec();

        showClient(registerClient);

        AssertBuilder.registerResponse(registerResponse).created()
                .backchannelTokenDeliveryMode(mode)
                .backchannelRequestSigningAlgorithm(algorithm)
                .backchannelUserCodeParameter(false)
                .check();
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
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse).ok()
                        .check();
    }

    /**
     * Process a Ciba call to the OP using JWT Request object and validate HTTP status and error type.
     *
     * @param jwtRequest   JWT in plain String.
     * @param clientId     Client identifier.
     * @param clientSecret Client secret.
     * @param httpStatus   Param used to validate response from the server.
     * @param errorType    Error used to validate error response from the server.
     */
    private void processCibaAuthorizationEndpointFailCall(String jwtRequest, String clientId, String clientSecret, int httpStatus, BackchannelAuthenticationErrorResponseType errorType) {
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setRequest(jwtRequest);
        backchannelAuthenticationRequest.setClientId(clientId);
        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        showClient(backchannelAuthenticationClient);
        AssertBuilder.backchannelAuthenticationResponse(backchannelAuthenticationResponse)
                .status(httpStatus)
                .errorResponseType(errorType)
                .nullAuthReqId()
                .nullExpiresIn()
                .nullInterval()
                .check();
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
        jwtAuthorizationRequest.setClientNotificationToken("notification-token-123");
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
        List<String> scopes = Collections.singletonList("openid");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setScope(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS384)
                .notNullAccesTokenHash()
                .notNullAuthenticationTime()
                .check();

        idTokenHintRS384 = idToken;
    }

}