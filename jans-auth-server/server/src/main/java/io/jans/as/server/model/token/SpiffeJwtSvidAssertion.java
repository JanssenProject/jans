/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.SpiffeIdUtil;
import io.jans.as.server.service.ClientIdMetadataService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.SpiffeBundleService;
import io.jans.service.cdi.util.CdiUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Validates a SPIFFE JWT-SVID client assertion (client_assertion_type =
 * urn:ietf:params:oauth:client-assertion-type:jwt-spiffe), per draft-ietf-oauth-spiffe-client-auth.
 * <p>
 * Unlike {@link ClientAssertion} (private_key_jwt/client_secret_jwt), the signature here is
 * verified against the SPIFFE trust domain's JWT-SVID signing keys (fetched from the
 * admin-configured SPIFFE Bundle Endpoint), not the client's own registered JWKS.
 *
 * @author Yuriy Zabrovarnyy
 */
public class SpiffeJwtSvidAssertion {

    private final AppConfiguration appConfiguration;
    private final AbstractCryptoProvider cryptoProvider;
    private final String clientId;
    private final String encodedAssertion;
    private boolean verified;

    private Jwt jwt;
    private Client client;

    public SpiffeJwtSvidAssertion(AppConfiguration appConfiguration, AbstractCryptoProvider cryptoProvider, String clientId, String encodedAssertion) {
        this.appConfiguration = appConfiguration;
        this.cryptoProvider = cryptoProvider;
        this.clientId = clientId;
        this.encodedAssertion = encodedAssertion;
        this.verified = false;
    }

    public String getSubjectIdentifier() throws InvalidJwtException {
        assertVerified();
        return jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
    }

    public Client getClient() throws InvalidJwtException {
        assertVerified();
        return client;
    }

    private void assertVerified() throws InvalidJwtException {
        if (!verified) {
            throw new InvalidJwtException("SPIFFE JWT-SVID assertion is not verified");
        }
    }

    public void initAndVerify() throws InvalidJwtException {
        try {
            initAndVerifyInternally();
            verified = true;
        } catch (InvalidJwtException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidJwtException("Cannot verify SPIFFE JWT-SVID", e);
        }
    }

    private void initAndVerifyInternally() throws Exception {
        if (StringUtils.isBlank(encodedAssertion)) {
            throw new InvalidJwtException("The client_assertion is null or empty");
        }
        if (StringUtils.isBlank(clientId)) {
            throw new InvalidJwtException("client_id is required for SPIFFE JWT-SVID client authentication");
        }

        jwt = Jwt.parse(encodedAssertion);

        final String subject = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
        final List<String> audience = jwt.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
        final Date expirationTime = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);

        if (StringUtils.isBlank(subject) || audience == null || audience.isEmpty() || expirationTime == null) {
            throw new InvalidJwtException("SPIFFE JWT-SVID must contain sub, aud and exp claims");
        }
        if (expirationTime.before(new Date())) {
            throw new InvalidJwtException("SPIFFE JWT-SVID has expired");
        }
        if (!SpiffeIdUtil.isValidPresentedSpiffeId(subject)) {
            throw new InvalidJwtException("Invalid SPIFFE ID in sub claim: " + subject);
        }

        // aud MUST contain only the authorization server's issuer identifier as its sole value
        final String issuer = appConfiguration.getIssuer();
        if (audience.size() != 1 || !audience.get(0).equals(issuer)) {
            throw new InvalidJwtException("Invalid audience for SPIFFE JWT-SVID. It must contain only the server issuer as its sole value. Aud: " + audience);
        }

        client = resolveClient(clientId);
        if (client == null) {
            throw new InvalidJwtException("Invalid client");
        }

        final String registeredSpiffeId = client.getAttributes().getSpiffeId();
        if (StringUtils.isBlank(registeredSpiffeId) || !SpiffeIdUtil.matches(registeredSpiffeId, subject)) {
            throw new InvalidJwtException("SPIFFE ID in sub claim does not match client's registered spiffe_id");
        }

        verifySignature(subject);
    }

    private Client resolveClient(String clientId) {
        final ClientIdMetadataService clientIdMetadataService = CdiUtil.bean(ClientIdMetadataService.class);
        if (clientIdMetadataService.isCimdClientId(clientId)) {
            try {
                return clientIdMetadataService.getClient(clientId);
            } catch (RuntimeException e) {
                return null;
            }
        }
        final ClientService clientService = CdiUtil.bean(ClientService.class);
        return clientService.getClient(clientId);
    }

    private void verifySignature(String presentedSpiffeId) throws Exception {
        final SpiffeBundleService spiffeBundleService = CdiUtil.bean(SpiffeBundleService.class);
        final String trustDomain = SpiffeIdUtil.trustDomainOf(presentedSpiffeId);
        final JSONObject jwks = spiffeBundleService.getJwtSvidJwks(trustDomain);
        if (jwks == null) {
            throw new InvalidJwtException("No SPIFFE JWT-SVID signing keys configured/available for trust domain: " + trustDomain);
        }

        final SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
        final String keyId = jwt.getHeader().getKeyId();
        final boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId, jwks, null, signatureAlgorithm);
        if (!validSignature) {
            throw new InvalidJwtException("Invalid SPIFFE JWT-SVID signature");
        }
    }
}
