package org.gluu.oxauth.ciba;

import org.apache.commons.lang.time.DateUtils;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.gluu.oxauth.model.register.RegisterRequestParam.*;
import static org.gluu.oxauth.model.register.RegisterRequestParam.BACKCHANNEL_USER_CODE_PARAMETER;
import static org.testng.Assert.*;
import static org.testng.Assert.assertTrue;

/**
 * Responsible to validate many cases using JWT Requests for Ciba Authorization flows.
 */
public class CibaJwtAuthenticationRequestTests extends BaseTest {

    private RegisterResponse registerResponse;

    @Parameters({"PS256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowPS256HappyFlow1(final String keyId, final String userId, final String dnName,
                                        final String keyStoreFile, final String keyStoreSecret,
                                        final String clientJwksUri) throws Exception {
        showTitle("pollFlowPS256HappyFlow1");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS256);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int)(System.currentTimeMillis() / 1000);
        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, SignatureAlgorithm.PS256, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int)(DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(), clientId, registerResponse.getClientSecret());
    }

    @Parameters({"PS384_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowPS384HappyFlow1(final String keyId, final String userId, final String dnName,
                                        final String keyStoreFile, final String keyStoreSecret,
                                        final String clientJwksUri) throws Exception {
        showTitle("pollFlowPS384HappyFlow1");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS384);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int)(System.currentTimeMillis() / 1000);
        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, SignatureAlgorithm.PS384, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int)(DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(), clientId, registerResponse.getClientSecret());
    }

    @Parameters({"PS512_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowPS512HappyFlow1(final String keyId, final String userId, final String dnName,
                                        final String keyStoreFile, final String keyStoreSecret,
                                        final String clientJwksUri) throws Exception {
        showTitle("pollFlowPS512HappyFlow1");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.PS512);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int)(System.currentTimeMillis() / 1000);
        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, SignatureAlgorithm.PS512, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int)(DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(), clientId, registerResponse.getClientSecret());
    }

    @Parameters({"ES256_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowES256HappyFlow1(final String keyId, final String userId, final String dnName,
                                        final String keyStoreFile, final String keyStoreSecret,
                                        final String clientJwksUri) throws Exception {
        showTitle("pollFlowES256HappyFlow1");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.ES256);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int)(System.currentTimeMillis() / 1000);
        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, SignatureAlgorithm.ES256, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int)(DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(), clientId, registerResponse.getClientSecret());
    }

    @Parameters({"ES384_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowES384HappyFlow1(final String keyId, final String userId, final String dnName,
                                        final String keyStoreFile, final String keyStoreSecret,
                                        final String clientJwksUri) throws Exception {
        showTitle("pollFlowES384HappyFlow1");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.ES384);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int)(System.currentTimeMillis() / 1000);
        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, SignatureAlgorithm.ES384, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int)(DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(), clientId, registerResponse.getClientSecret());
    }

    @Parameters({"ES512_keyId", "userId", "dnName", "keyStoreFile", "keyStoreSecret", "clientJwksUri"})
    @Test
    public void pollFlowES512HappyFlow1(final String keyId, final String userId, final String dnName,
                                        final String keyStoreFile, final String keyStoreSecret,
                                        final String clientJwksUri) throws Exception {
        showTitle("pollFlowES384HappyFlow1");
        registerPollClient(clientJwksUri, BackchannelTokenDeliveryMode.POLL, AsymmetricSignatureAlgorithm.ES512);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String clientId = registerResponse.getClientId();

        int now = (int)(System.currentTimeMillis() / 1000);
        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                null, SignatureAlgorithm.ES512, cryptoProvider);
        jwtAuthorizationRequest.setAud(issuer);
        jwtAuthorizationRequest.setLoginHint(userId);
        jwtAuthorizationRequest.setNbf(now);
        jwtAuthorizationRequest.setScopes(Collections.singletonList("openid"));
        jwtAuthorizationRequest.setIss(clientId);
        jwtAuthorizationRequest.setBindingMessage("1234");
        jwtAuthorizationRequest.setExp((int)(DateUtils.addMinutes(new Date(), 5).getTime() / 1000));
        jwtAuthorizationRequest.setIat(now);
        jwtAuthorizationRequest.setJti(UUID.randomUUID().toString());
        jwtAuthorizationRequest.setKeyId(keyId);

        processCibaAuthorizationEndpointSuccessfulCall(jwtAuthorizationRequest.getEncodedJwt(), clientId, registerResponse.getClientSecret());
    }

    /**
     * Registers a client using CIBA configuration for Poll flow and PS256
     * @param clientJwksUri
     */
    private void registerPollClient(final String clientJwksUri, BackchannelTokenDeliveryMode mode, AsymmetricSignatureAlgorithm algorithm) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.CIBA));

        registerRequest.setBackchannelTokenDeliveryMode(mode);
        registerRequest.setBackchannelAuthenticationRequestSigningAlg(algorithm);
        registerRequest.setBackchannelUserCodeParameter(false);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerResponse = registerClient.exec();

        showClient(registerClient);

        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
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
     * @param jwtRequest JWT in plain String.
     * @param clientId Client identifier.
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

}
