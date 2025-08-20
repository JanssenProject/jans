/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.server.service.ClientService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.Pair;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Z
 * @version February 12, 2019
 */
public class ClientAssertion {

    private final AppConfiguration appConfiguration;
    private final AbstractCryptoProvider cryptoProvider;
    private final String clientId;
    private final ClientAssertionType clientAssertionType;
    private final String encodedAssertion;
    private boolean verified;

    private Jwt jwt;
    private String clientSecret;

    public ClientAssertion(AppConfiguration appConfiguration, AbstractCryptoProvider cryptoProvider, String clientId, io.jans.as.model.token.ClientAssertionType clientAssertionType, String encodedAssertion) {
        this.appConfiguration = appConfiguration;
        this.cryptoProvider = cryptoProvider;
        this.clientId = clientId;
        this.clientAssertionType = clientAssertionType;
        this.encodedAssertion = encodedAssertion;
        this.verified = false;
    }

    public String getSubjectIdentifier() throws InvalidJwtException {
        assertVerified();
        return jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
    }

    public String getJti() throws InvalidJwtException {
        assertVerified();
        return jwt.getClaims().getClaimAsString(JwtClaimName.JWT_ID);
    }

    public String getClientSecret() throws InvalidJwtException {
        assertVerified();
        return clientSecret;
    }

    public void assertVerified() throws InvalidJwtException {
        if (!verified) {
            throw new InvalidJwtException("Client Assertion is not verified");
        }
    }

    public void verifyClientAssertionType() throws InvalidJwtException {
        if (clientAssertionType != ClientAssertionType.JWT_BEARER) {
            throw new InvalidJwtException("Invalid Client Assertion Type");
        }
    }

    public void initAndVerify() throws InvalidJwtException {
        try {
            initAndVerifyInternally();
            verified = true;
        } catch (StringEncrypter.EncryptionException e) {
            throw new InvalidJwtException(e.getMessage(), e);
        } catch (Exception e) {
            throw new InvalidJwtException("Cannot verify the JWT", e);
        }
    }

    public void verifyEncodedAssertion() throws InvalidJwtException {
        if (StringUtils.isBlank(encodedAssertion)) {
            throw new InvalidJwtException("The Client Assertion is null or empty");
        }
    }

    public void verifyAudience(List<String> audience) throws InvalidJwtException {
        if (audience == null || audience.isEmpty()) {
            throw new InvalidJwtException("Audience is null or blank.");
        }

        // relax strict 'aud' check against server issuer. By default this value is false which means server makes strict check.
        if (appConfiguration.getAllowClientAssertionAudWithoutStrictIssuerMatch()) {
            return;
        }

        final String serverIssuer = appConfiguration.getIssuer();

        // aud must be equals to server's issuer or otherwise start with it (e.g. point to particular endpoint)
        if (audience.stream().anyMatch(aud -> aud.equals(serverIssuer) || aud.startsWith(serverIssuer))) {
            return;
        }

        throw new InvalidJwtException("Invalid Audience. It must contain server issuer or server. Aud: " + audience);
    }

    public void verifyExpiration(Date expirationTime) throws InvalidJwtException {
        if (expirationTime != null && expirationTime.after(new Date())) {
            return;
        }
        throw new InvalidJwtException("JWT has expired");
    }

    public Pair<Client, ClientService> verifyClient(String subject) throws InvalidJwtException {
        ClientService clientService = CdiUtil.bean(ClientService.class);
        Client client = clientService.getClient(subject);
        if (client != null) {
            return new Pair<>(client, clientService);
        }
        throw new InvalidJwtException("Invalid client");
    }

    public void verifyAuthenticationMethod(Client client) throws InvalidJwtException {
        JwtType jwtType = JwtType.fromString(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        Set<AuthenticationMethod> authenticationMethods = client.getAllAuthenticationMethods();
        SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();

        if (jwtType == null && signatureAlgorithm != null) {
            jwtType = signatureAlgorithm.getJwtType();
        }

        if (jwtType != null && signatureAlgorithm != null && signatureAlgorithm.getFamily() != null &&
                ((authenticationMethods.contains(AuthenticationMethod.CLIENT_SECRET_JWT) && AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily()))
                        || (authenticationMethods.contains(AuthenticationMethod.PRIVATE_KEY_JWT) && (AlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily()) || AlgorithmFamily.EC.equals(signatureAlgorithm.getFamily()))))) {
            return;
        }

        throw new InvalidJwtException("Invalid authentication method");
    }

    private void initAndVerifyInternally() throws InvalidJwtException, StringEncrypter.EncryptionException, CryptoProviderException {

        verifyClientAssertionType();
        verifyEncodedAssertion();

        jwt = Jwt.parse(encodedAssertion);

        // Validate clientId
        String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
        String subject = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
        List<String> audience = jwt.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
        Date expirationTime = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);

        final boolean issuerEqualsToSubject = StringUtils.isNotBlank(issuer) && StringUtils.isNotBlank(subject) && issuer.equals(subject);
        final boolean noClientAndIssuerEqualsToSubject = clientId == null && issuerEqualsToSubject;
        final boolean clientEqualsToSubjectAndToIssuer = StringUtils.isNotBlank(clientId) && clientId.equals(issuer) && issuerEqualsToSubject;

        if (!noClientAndIssuerEqualsToSubject && !clientEqualsToSubjectAndToIssuer) {
            throw new InvalidJwtException("Invalid clientId");
        }

        verifyAudience(audience);
        verifyExpiration(expirationTime);

        final Pair<Client, ClientService> verifiedClientPair = verifyClient(subject);
        Client client = verifiedClientPair.getFirst();

        verifyAuthenticationMethod(client);
        verifySignatureAlgorithm(client);
        verifySignature(verifiedClientPair.getSecond(), verifiedClientPair.getFirst());
    }

    public void verifySignature(ClientService clientService, Client client) throws StringEncrypter.EncryptionException, InvalidJwtException, CryptoProviderException {
        SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
        clientSecret = clientService.decryptSecret(client.getClientSecret());

        // Validate the crypto segment
        String keyId = jwt.getHeader().getKeyId();
        JSONObject jwks = CommonUtils.getJwks(client);
        boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(),
                keyId, jwks, clientSecret, signatureAlgorithm);

        if (!validSignature) {
            throw new InvalidJwtException("Invalid cryptographic segment");
        }
    }

    public void verifySignatureAlgorithm(Client client) throws InvalidJwtException {
        SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
        if (client.getTokenEndpointAuthSigningAlg() == null || SignatureAlgorithm.fromString(client.getTokenEndpointAuthSigningAlg()).equals(signatureAlgorithm)) {
            return;
        }

        throw new InvalidJwtException("Invalid signing algorithm");
    }
}