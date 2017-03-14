package org.xdi.oxd.server.op;

import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.JwkResponse;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxd.common.CoreUtils;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2017
 */

public class Validator {

    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    private Validator() {
    }

    public static RSASigner createRSASigner(Jwt jwt, OpenIdConfigurationResponse discoveryResponse) {
        final String jwkUrl = discoveryResponse.getJwksUri();
        final String kid = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        final String algorithm = jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm);

        final RSAPublicKey publicKey = getRSAPublicKey(jwkUrl, kid);
        return new RSASigner(signatureAlgorithm, publicKey);
    }

    public static RSAPublicKey getRSAPublicKey(String jwkSetUri, String keyId) {
        try {
            RSAPublicKey publicKey = null;

            JwkClient jwkClient = new JwkClient(jwkSetUri);
            jwkClient.setExecutor(new ApacheHttpClient4Executor(CoreUtils.createHttpClientTrustAll()));
            JwkResponse jwkResponse = jwkClient.exec();
            if (jwkResponse != null && jwkResponse.getStatus() == 200) {
                PublicKey pk = jwkResponse.getPublicKey(keyId);
                if (pk instanceof RSAPublicKey) {
                    publicKey = (RSAPublicKey) pk;
                }
            }

            return publicKey;
        } catch (Exception e) {
            LOG.error("Failed to obtain public key.", e);
            throw new RuntimeException("Failed to obtain public key.", e);
        }
    }

    public static boolean isIdTokenValid(Jwt idToken, OpenIdConfigurationResponse discoveryResponse, String nonce, String clientId) {
        return isIdTokenValid(idToken, discoveryResponse, nonce, clientId, createRSASigner(idToken, discoveryResponse));
    }

    public static boolean isIdTokenValid(Jwt idToken, OpenIdConfigurationResponse discoveryResponse, String nonce, String clientId, RSASigner rsaSigner) {
        try {
            final String issuer = idToken.getClaims().getClaimAsString(JwtClaimName.ISSUER);
            final String nonceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.NONCE);
            final String audienceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUDIENCE);

            if (!nonceFromToken.endsWith(nonce)) {
                LOG.error("ID Token has invalid nonce. Expected nonce: " + nonce + ", nonce from token is: " + nonceFromToken);
                return false;
            }

            if (!clientId.equalsIgnoreCase(audienceFromToken)) {
                LOG.error("ID Token has invalid audience. Expected audience: " + clientId + ", audience from token is: " + audienceFromToken);
                return false;
            }

            final Date expiresAt = idToken.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            final Date now = new Date();
            if (now.after(expiresAt)) {
                LOG.error("ID Token is expired. (It is after " + now + ").");
                return false;
            }

            // 1. validate issuer
            if (!issuer.equals(discoveryResponse.getIssuer())) {
                LOG.error("ID Token issuer is invalid. Token issuer: " + issuer + ", discovery issuer: " + discoveryResponse.getIssuer());
                return false;
            }

            // 2. validate signature
            final boolean signature = rsaSigner.validate(idToken);
            if (!signature) {
                LOG.error("ID Token signature is invalid.");
            }
            return signature;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }
}
