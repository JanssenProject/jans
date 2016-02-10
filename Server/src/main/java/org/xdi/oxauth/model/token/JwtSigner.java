package org.xdi.oxauth.model.token;

import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jws.ECDSASigner;
import org.xdi.oxauth.model.jws.HMACSigner;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.util.security.StringEncrypter;

import java.security.SignatureException;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/01/2016
 */

public class JwtSigner {

    private final JSONWebKeySet jwks = ConfigurationFactory.instance().getWebKeys();

    private final Client client;
    private SignatureAlgorithm signatureAlgorithm;
    private Jwt jwt;

    public JwtSigner(Client client) {
        this.client = client;

        signatureAlgorithm = SignatureAlgorithm.fromName(ConfigurationFactory.instance().getConfiguration().getDefaultSignatureAlgorithm());
        if (client != null && client.getIdTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromName(client.getIdTokenSignedResponseAlg());
        }
    }

    public Jwt newJwt() {
        jwt = new Jwt();

        // Header
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);
        List<JSONWebKey> jsonWebKeys = jwks.getKeys(signatureAlgorithm);
        if (jsonWebKeys.size() > 0) {
            jwt.getHeader().setKeyId(jsonWebKeys.get(0).getKeyId());
        }

        // Claims
        jwt.getClaims().setIssuer(ConfigurationFactory.instance().getConfiguration().getIssuer());
        jwt.getClaims().setAudience(client.getClientId());
        return jwt;
    }

    public Jwt sign() throws SignatureException, InvalidJwtException, StringEncrypter.EncryptionException {
         // Signature
        JSONWebKey jwk;
        switch (signatureAlgorithm) {
            case HS256:
            case HS384:
            case HS512:
                HMACSigner hmacSigner = new HMACSigner(signatureAlgorithm, client.getClientSecret());
                jwt = hmacSigner.sign(jwt);
                break;
            case RS256:
            case RS384:
            case RS512:
                jwk = jwks.getKey(jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
                RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(
                        jwk.getPrivateKey().getModulus(),
                        jwk.getPrivateKey().getPrivateExponent());
                RSASigner rsaSigner = new RSASigner(signatureAlgorithm, rsaPrivateKey);
                jwt = rsaSigner.sign(jwt);
                break;
            case ES256:
            case ES384:
            case ES512:
                jwk = jwks.getKey(jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
                ECDSAPrivateKey ecdsaPrivateKey = new ECDSAPrivateKey(jwk.getPrivateKey().getD());
                ECDSASigner ecdsaSigner = new ECDSASigner(signatureAlgorithm, ecdsaPrivateKey);
                jwt = ecdsaSigner.sign(jwt);
                break;
            case NONE:
                break;
            default:
                break;
        }

        return jwt;
    }

    public Client getClient() {
        return client;
    }

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}
