/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.common.AccessToken;
import org.xdi.oxauth.model.common.AuthorizationCode;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.config.ClaimMappingConfiguration;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwe.JweEncrypter;
import org.xdi.oxauth.model.jwe.JweEncrypterImpl;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.jws.ECDSASigner;
import org.xdi.oxauth.model.jws.HMACSigner;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.util.security.StringEncrypter;

/**
 * JSON Web Token (JWT) is a compact token format intended for space constrained
 * environments such as HTTP Authorization headers and URI query parameters.
 * JWTs encode claims to be transmitted as a JSON object (as defined in RFC
 * 4627) that is base64url encoded and digitally signed. Signing is accomplished
 * using a JSON Web Signature (JWS). JWTs may also be optionally encrypted using
 * JSON Web Encryption (JWE).
 *
 * @author Javier Rojas Blum
 * @version 0.9 December 9, 2014
 */
public class IdTokenFactory {

    public static Jwt generateSignedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                            AuthorizationCode authorizationCode, AccessToken accessToken,
                                            Map<String, String> claims) throws SignatureException, InvalidJwtException, StringEncrypter.EncryptionException {
        Jwt jwt = new Jwt();
        JSONWebKeySet jwks = ConfigurationFactory.getWebKeys();

        // Header
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(ConfigurationFactory.getConfiguration().getDefaultSignatureAlgorithm());
        if (authorizationGrant.getClient() != null
                && authorizationGrant.getClient().getIdTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromName(authorizationGrant.getClient().getIdTokenSignedResponseAlg());
        }
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            jwt.getHeader().setType(JwtType.JWT);
        } else {
            jwt.getHeader().setType(JwtType.JWS);
        }
        jwt.getHeader().setAlgorithm(signatureAlgorithm);
        jwt.getHeader().setKeyId(jwks.getKeys(signatureAlgorithm).get(0).getKeyId());

        // Claims
        jwt.getClaims().setIssuer(ConfigurationFactory.getConfiguration().getIssuer());
        jwt.getClaims().setAudience(authorizationGrant.getClient().getClientId());

        int lifeTime = ConfigurationFactory.getConfiguration().getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwt.getClaims().setExpirationTime(expiration);
        jwt.getClaims().setIssuedAt(issuedAt);

        if (authorizationGrant.getUserDn() != null) {
            ClaimMappingConfiguration subMapping = ClaimMappingConfiguration.getMappingByClaim(JwtClaimName.SUBJECT_IDENTIFIER);

            jwt.getClaims().setClaim(JwtClaimName.SUBJECT_IDENTIFIER, authorizationGrant.getUser().getAttribute(subMapping.getLdap()));
        }
        // TODO: acr
        //if (authenticationContextClassReference != null) {
        //    jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authenticationContextClassReference);
        //}
        if (StringUtils.isNotBlank(nonce)) {
            jwt.getClaims().setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwt.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = authorizationCode.getHash(signatureAlgorithm);
            jwt.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = accessToken.getHash(signatureAlgorithm);
            jwt.getClaims().setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        jwt.getClaims().setClaim("oxValidationURI", ConfigurationFactory.getConfiguration().getCheckSessionIFrame());
        jwt.getClaims().setClaim("oxOpenIDConnectVersion", ConfigurationFactory.getConfiguration().getOxOpenIdConnectVersion());

        if (claims != null) {
            for (Iterator<String> it = claims.keySet().iterator(); it.hasNext(); ) {
                String key = it.next();
                String value = claims.get(key);
                jwt.getClaims().setClaim(key, value);
            }
        }

        // Signature
        JSONWebKey jwk = null;
        switch (signatureAlgorithm) {
            case HS256:
            case HS384:
            case HS512:
                HMACSigner hmacSigner = new HMACSigner(signatureAlgorithm, authorizationGrant.getClient().getClientSecret());
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

    public static Jwe generateEncryptedIdToken(IAuthorizationGrant authorizationGrant, String nonce,
                                               AuthorizationCode authorizationCode, AccessToken accessToken,
                                               Map<String, String> claims) throws InvalidJweException {
        Jwe jwe = new Jwe();

        // Header
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseAlg());
        BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(authorizationGrant.getClient().getIdTokenEncryptedResponseEnc());
        jwe.getHeader().setType(JwtType.JWE);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);

        // Claims
        jwe.getClaims().setIssuer(ConfigurationFactory.getConfiguration().getIssuer());
        jwe.getClaims().setAudience(authorizationGrant.getClient().getClientId());

        int lifeTime = ConfigurationFactory.getConfiguration().getIdTokenLifetime();
        Calendar calendar = Calendar.getInstance();
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.SECOND, lifeTime);
        Date expiration = calendar.getTime();

        jwe.getClaims().setExpirationTime(expiration);
        jwe.getClaims().setIssuedAt(issuedAt);

        if (authorizationGrant.getUserDn() != null) {
            ClaimMappingConfiguration subMapping = ClaimMappingConfiguration.getMappingByClaim(JwtClaimName.SUBJECT_IDENTIFIER);

            jwe.getClaims().setClaim(JwtClaimName.SUBJECT_IDENTIFIER, authorizationGrant.getUser().getAttribute(subMapping.getLdap()));
        }
        // TODO: acr
        //if (authenticationContextClassReference != null) {
        //    jwe.getClaims().setClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, authenticationContextClassReference);
        //}
        if (StringUtils.isNotBlank(nonce)) {
            jwe.getClaims().setClaim(JwtClaimName.NONCE, nonce);
        }
        if (authorizationGrant.getAuthenticationTime() != null) {
            jwe.getClaims().setClaim(JwtClaimName.AUTHENTICATION_TIME, authorizationGrant.getAuthenticationTime());
        }
        if (authorizationCode != null) {
            String codeHash = authorizationCode.getHash(null);
            jwe.getClaims().setClaim(JwtClaimName.CODE_HASH, codeHash);
        }
        if (accessToken != null) {
            String accessTokenHash = accessToken.getHash(null);
            jwe.getClaims().setClaim(JwtClaimName.ACCESS_TOKEN_HASH, accessTokenHash);
        }
        jwe.getClaims().setClaim("oxValidationURI", ConfigurationFactory.getConfiguration().getCheckSessionIFrame());
        jwe.getClaims().setClaim("oxOpenIDConnectVersion", ConfigurationFactory.getConfiguration().getOxOpenIdConnectVersion());

        if (claims != null) {
            for (Iterator<String> it = claims.keySet().iterator(); it.hasNext(); ) {
                String key = it.next();
                String value = claims.get(key);
                jwe.getClaims().setClaim(key, value);
            }
        }

        // Encryption
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5) {
            PublicKey publicKey = JwtUtil.getPublicKey(authorizationGrant.getClient().getJwksUri(), SignatureAlgorithm.RS256, null);
            if (publicKey != null && publicKey instanceof RSAPublicKey) {
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, (RSAPublicKey) publicKey);
                jwe = jweEncrypter.encrypt(jwe);
            } else {
                throw new InvalidJweException("The public key is not valid");
            }
        } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW
                || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            try {
                byte[] sharedSymmetricKey = authorizationGrant.getClient().getClientSecret().getBytes(Util.UTF8_STRING_ENCODING);
                JweEncrypter jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedSymmetricKey);
                jwe = jweEncrypter.encrypt(jwe);
            } catch (UnsupportedEncodingException e) {
                throw new InvalidJweException(e);
            } catch (StringEncrypter.EncryptionException e) {
                throw new InvalidJweException(e);
            } catch (Exception e) {
                throw new InvalidJweException(e);
            }
        }

        return jwe;
    }
}