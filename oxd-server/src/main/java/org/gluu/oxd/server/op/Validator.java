package org.gluu.oxd.server.op;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxauth.model.crypto.signature.AlgorithmFamily;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jws.AbstractJwsSigner;
import org.gluu.oxauth.model.jws.ECDSASigner;
import org.gluu.oxauth.model.jws.HMACSigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.service.PublicOpKeyService;
import org.gluu.oxd.server.service.Rp;
import org.gluu.oxd.server.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SignatureException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2017
 */

public class Validator {

    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    private final OpenIdConfigurationResponse discoveryResponse;
    private OxdServerConfiguration configuration;
    private AbstractJwsSigner jwsSigner;
    private final Jwt idToken;
    private OpClientFactory opClientFactory;
    private final PublicOpKeyService keyService;
    private final Rp rp;

    public OpenIdConfigurationResponse getDiscoveryResponse() {
        return discoveryResponse;
    }

    public OxdServerConfiguration getOxdServerConfiguration() {
        return configuration;
    }

    public AbstractJwsSigner getJwsSigner() {
        return jwsSigner;
    }

    public OpClientFactory getOpClientFactory() {
        return opClientFactory;
    }

    public PublicOpKeyService getKeyService() {
        return keyService;
    }

    public Rp getRp() {
        return rp;
    }

    private Validator(Builder builder) {
        this.discoveryResponse = builder.discoveryResponse;
        this.configuration = builder.configuration;
        this.idToken = builder.idToken;
        this.opClientFactory = builder.opClientFactory;
        this.keyService = builder.keyService;
        this.rp = builder.rp;
        this.jwsSigner = createJwsSigner(idToken, discoveryResponse, keyService, opClientFactory, rp, configuration);
    }

    //Builder Class
    public static class Builder {

        // required parameters
        private OpenIdConfigurationResponse discoveryResponse;
        private OxdServerConfiguration configuration;
        private Jwt idToken;
        private OpClientFactory opClientFactory;
        private PublicOpKeyService keyService;
        private Rp rp;

        public Builder() {
        }

        public Builder discoveryResponse(OpenIdConfigurationResponse discoveryResponse) {
            this.discoveryResponse = discoveryResponse;
            return this;
        }

        public Builder oxdServerConfiguration(OxdServerConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder idToken(Jwt idToken) {
            this.idToken = idToken;
            return this;
        }

        public Builder opClientFactory(OpClientFactory opClientFactory) {
            this.opClientFactory = opClientFactory;
            return this;
        }

        public Builder keyService(PublicOpKeyService keyService) {
            this.keyService = keyService;
            return this;
        }

        public Builder rp(Rp rp) {
            this.rp = rp;
            return this;
        }

        public Validator build() {
            Preconditions.checkNotNull(this.idToken);
            Preconditions.checkNotNull(this.discoveryResponse);

            return new Validator(this);
        }
    }

    public void validateAccessToken(String accessToken) {
        validateAccessToken(accessToken, false);
    }

    // `at_hash` in ID_TOKEN is mandatory for response_type='id_token token' for Implicit Flow or response_type='code id_token token' for Hybrid Flow.
    public void validateAccessToken(String accessToken, boolean atHashRequired) {

        if (!configuration.getIdTokenValidationAtHashRequired()) {
            return;
        }

        if (Strings.isNullOrEmpty(accessToken)) {
            return;
        }

        String atHash = idToken.getClaims().getClaimAsString("at_hash");
        if (Strings.isNullOrEmpty(atHash)) {
            if (atHashRequired) {
                LOG.error("`at_hash` is missing in `ID_TOKEN`.");
                throw new HttpException(ErrorResponseCode.AT_HASH_NOT_FOUND);
            } else {
                LOG.warn("Skip access_token validation because corresponding id_token does not have at_hash claim. access_token: " + accessToken + ", id_token: " + idToken);
                return;
            }
        }
        if (!jwsSigner.validateAccessToken(accessToken, idToken)) {
            LOG.error("Hash from id_token does not match hash of the access_token (at_hash). access_token:" + accessToken + ", idToken: " + idToken + ", at_hash:" + atHash);
            throw new HttpException(ErrorResponseCode.INVALID_ACCESS_TOKEN_BAD_HASH);
        }
    }

    public void validateState(String state) {

        if (!configuration.getIdTokenValidationSHashRequired()) {
            return;
        }

        if (Strings.isNullOrEmpty(state)) {
            return;
        }

        String sHash = idToken.getClaims().getClaimAsString("s_hash");
        if (Strings.isNullOrEmpty(sHash)) {
            LOG.error("`s_hash` is missing in `ID_TOKEN`.");
            throw new HttpException(ErrorResponseCode.S_HASH_NOT_FOUND);
        }
        if (!jwsSigner.validateState(state, idToken)) {
            LOG.error("Hash from id_token does not match hash of the state (s_hash). state:" + state + ", idToken: " + idToken + ", sHash:" + sHash);
            throw new HttpException(ErrorResponseCode.INVALID_STATE_BAD_HASH);
        }
    }

    public void validateAuthorizationCode(String code) {
        if (!configuration.getIdTokenValidationCHashRequired()) {
            return;
        }

        if (Strings.isNullOrEmpty(code)) {
            return;
        }

        if (Strings.isNullOrEmpty(idToken.getClaims().getClaimAsString("c_hash"))) {
            LOG.error("`c_hash` is missing in `ID_TOKEN`.");
            throw new HttpException(ErrorResponseCode.C_HASH_NOT_FOUND);
        }
        if (!jwsSigner.validateAuthorizationCode(code, idToken)) {
            LOG.error("`Authorization code is invalid. Hash of authorization code does not match hash from id_token (c_hash).");
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_CODE_BAD_HASH);
        }
    }

    public static AbstractJwsSigner createJwsSigner(Jwt idToken, OpenIdConfigurationResponse discoveryResponse, PublicOpKeyService keyService, OpClientFactory opClientFactory, Rp rp, OxdServerConfiguration configuration) {
        final String algorithm = idToken.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm);
        final String jwkUrl = discoveryResponse.getJwksUri();
        String kid = idToken.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);

        if (signatureAlgorithm == null)
            throw new HttpException(ErrorResponseCode.INVALID_ALGORITHM);

        if (Strings.isNullOrEmpty(kid) && (signatureAlgorithm.getFamily() == AlgorithmFamily.RSA || signatureAlgorithm.getFamily() == AlgorithmFamily.EC)) {
            LOG.warn("Warning:`kid` is missing in id_token header. oxd will throw error if RP is unable to determine the key to used for `id_token` validation.");
        }
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {

            if (!configuration.getAcceptIdTokenWithoutSignature()) {
                LOG.error("`ID_TOKEN` without signature is not allowed. To allow `ID_TOKEN` without signature set `accept_id_token_without_signature` field to 'true' in oxd-server.yml.");
                throw new HttpException(ErrorResponseCode.ID_TOKEN_WITHOUT_SIGNATURE_NOT_ALLOWED);
            }

            return new AbstractJwsSigner(signatureAlgorithm) {
                @Override
                public String generateSignature(String signingInput) throws SignatureException {
                    return null;
                }

                @Override
                public boolean validateSignature(String signingInput, String signature) throws SignatureException {
                    return true;
                }
            };
        } else if (signatureAlgorithm.getFamily() == AlgorithmFamily.RSA) {
            final RSAPublicKey publicKey = (RSAPublicKey) keyService.getPublicKey(jwkUrl, kid, signatureAlgorithm, Use.SIGNATURE);
            return opClientFactory.createRSASigner(signatureAlgorithm, publicKey);
        } else if (signatureAlgorithm.getFamily() == AlgorithmFamily.HMAC) {
            return new HMACSigner(signatureAlgorithm, rp.getClientSecret());
        } else if (signatureAlgorithm.getFamily() == AlgorithmFamily.EC) {
            final ECDSAPublicKey publicKey = (ECDSAPublicKey) keyService.getPublicKey(jwkUrl, kid, signatureAlgorithm, Use.SIGNATURE);
            return new ECDSASigner(signatureAlgorithm, publicKey);
        }
        throw new HttpException(ErrorResponseCode.ALGORITHM_NOT_SUPPORTED);
    }

    public void validateNonce(StateService stateService) {
        final String nonceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.NONCE);
        if (!stateService.isExpiredObjectPresent(nonceFromToken)) {
            LOG.error("Nonce value from `id_token` is not registered with oxd.");
            throw new HttpException(ErrorResponseCode.INVALID_NONCE);
        }
    }

    public boolean isIdTokenValid(String nonce) {
        try {
            validateIdToken(nonce);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void validateIdToken() {
        validateIdToken(null);
    }

    public void validateIdToken(String nonce) {
        try {
            final String issuer = idToken.getClaims().getClaimAsString(JwtClaimName.ISSUER);
            final String sub = idToken.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
            final String nonceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.NONCE);
            final String clientId = rp.getClientId();
            //validate nonce
            if(configuration.getFapiEnabled() && Strings.isNullOrEmpty(nonceFromToken)) {
                LOG.error("Nonce is missing from id_token.");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_NO_NONCE);
            }

            if (!Strings.isNullOrEmpty(nonce) && !nonceFromToken.endsWith(nonce)) {
                LOG.error("ID Token has invalid nonce. Expected nonce: " + nonce + ", nonce from token is: " + nonceFromToken);
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_NONCE);
            }
            //validate audience
            validateAudience(idToken, clientId);

            //validate subject identifier
            if (Strings.isNullOrEmpty(sub)) {
                LOG.error("ID Token is missing `sub` value.");
                throw new HttpException(ErrorResponseCode.NO_SUBJECT_IDENTIFIER);
            }

            //validate id_token issued at date
            final Date issuedAt = idToken.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT);
            if (issuedAt == null) {
                LOG.error("`ISSUED_AT` date is either invalid or missing from `ID_TOKEN`.");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_ISSUED_AT);
            }

            final Date now = new Date();
            if (configuration.getFapiEnabled() && TimeUnit.MILLISECONDS.toHours(now.getTime() - issuedAt.getTime()) > configuration.getIatExpirationInHours()) {
                LOG.error("`ISSUED_AT` date too far in the past. iat : " + issuedAt + " now : " + now + ").");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_OLD_ISSUED_AT);
            }

            //validate id_token expire date
            final Date expiresAt = idToken.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);

            if (expiresAt == null) {
                LOG.error("EXPIRATION_TIME (`exp`) is either invalid or missing from `ID_TOKEN`.");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_EXPIRATION_TIME);
            }

            if (now.after(expiresAt)) {
                LOG.error("ID Token is expired. (" + expiresAt + " is before " + now + ").");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_EXPIRED);
            }

            // 1. validate issuer
            if (Strings.isNullOrEmpty(issuer)) {
                LOG.error("Issuer (`iss`) claim is missing from id_token.");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_NO_ISSUER);
            }

            if (!issuer.equals(discoveryResponse.getIssuer())) {
                LOG.error("ID Token issuer is invalid. Token issuer: " + issuer + ", discovery issuer: " + discoveryResponse.getIssuer());
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_ISSUER);
            }

            //validate signature
            final String algorithm = idToken.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
            final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm);
            //validate algorithm
            if (!Strings.isNullOrEmpty(rp.getIdTokenSignedResponseAlg()) &&
                    SignatureAlgorithm.fromString(rp.getIdTokenSignedResponseAlg()) != signatureAlgorithm) {
                LOG.error("The algorithm used to sign the ID Token does not matches with `id_token_signed_response_alg` algorithm set during client registration.Expected: {}, Got: {}", rp.getIdTokenSignedResponseAlg(), algorithm);
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_INVALID_ALGORITHM);
            }
            if (signatureAlgorithm != SignatureAlgorithm.NONE) {
                boolean signature = jwsSigner.validate(idToken);

                if (!signature) {
                    LOG.error("ID Token signature is invalid.");
                    throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_SIGNATURE);
                }
            }
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_UNKNOWN);
        }
    }

    //Added this method so that we can write the test cases
    public static void validateAudience(Jwt idToken, String clientId) {

        final String audienceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUDIENCE);

        if (Strings.isNullOrEmpty(audienceFromToken)) {
            LOG.error("The audience (`aud`) claim is missing from ID Token.");
            throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_NO_AUDIENCE);
        }

        if (!clientId.equalsIgnoreCase(audienceFromToken)) {
            List<String> audAsList = idToken.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);

            if (audAsList != null && !audAsList.isEmpty()) {
                //check for element in list format
                if (hasListAsElement(audAsList)) {
                    audAsList = arrStringToList(audAsList.get(0));
                }

                if (!audAsList.stream().anyMatch(aud -> clientId.equalsIgnoreCase(aud))) {
                    LOG.error("ID Token has invalid audience (string list). Expected audience: " + clientId + ", audience from token is: " + audAsList);
                    throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_AUDIENCE);
                }

                if (audAsList.size() > 1) {
                    String azpFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUTHORIZED_PARTY);
                    //If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
                    if(Strings.isNullOrEmpty(azpFromToken)) {
                        LOG.error("The ID Token has multiple audiences. Authorized party (`azp`) is missing in ID Token.");
                        throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_NO_AUTHORIZED_PARTY);
                    }
                    //If an azp (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value. If present, it MUST contain the OAuth 2.0 Client ID of this party.
                    if (!Strings.isNullOrEmpty(azpFromToken) && !azpFromToken.equalsIgnoreCase(clientId)) {
                        LOG.error("ID Token has invalid authorized party (string list). Expected authorized party: " + clientId + ", authorized party from token is: " + azpFromToken);
                        throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_AUTHORIZED_PARTY);
                    }
                }
            }
        }
    }

    public static boolean hasListAsElement(List<String> audAsList) {
        if (audAsList.size() == 1 && audAsList.get(0).contains("[") && audAsList.get(0).contains("]")) {
            return true;
        }
        return false;
    }

    public static List<String> arrStringToList(String input) {
        if (!Strings.isNullOrEmpty(input)) {
            input = input.replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "");
            return Lists.newArrayList(input.split("\\s*,\\s*"));
        }
        throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_AUDIENCE);
    }

    public Jwt getIdToken() {
        return idToken;
    }
}
