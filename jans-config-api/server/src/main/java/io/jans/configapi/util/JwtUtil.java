package io.jans.configapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.jws.AbstractJwsSigner;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.RSASigner;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.as.model.crypto.PublicKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.json.JSONObject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class JwtUtil {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthUtil authUtil;

    public boolean isJwt(String token) throws Exception {
        log.trace("\n\n JwtUtil::isJwt()  token = " + token);
        boolean isJwt = false;
        try {
            this.parse(token);
            isJwt = true;
        } catch (Exception ex) {
            log.error("Not jwt token " + token);
        }
        return isJwt;
    }

    public Jwt parse(String encodedJwt) throws InvalidJwtException {
        log.trace("\n\n JwtUtil::parse()  encodedJwt = " + encodedJwt);
        if (StringHelper.isNotEmpty(encodedJwt)) {
            return Jwt.parse(encodedJwt);
        }
        return null;
    }

    public List<String> validateToken(String token) throws InvalidJwtException, Exception {
        // 1. Parse Jwt token
        // 2. Validate Token
        // 3. Validate Issuer
        // 4. Retrieve Auth Server JSON Web Keys - jwks_uri"
        // :"https://jenkins-config-api.gluu.org/jans-auth/restv1/jwks",
        // 5. Verify the signature used to sign the access token
        // 6. Verify the scopes

        try {
            // Parse Token
            Jwt jwt = this.parse(token);
            log.trace("JwtUtil::validateToken() -JWT details : " + " jwt.getSigningInput() = " + jwt.getSigningInput()
                    + " ,jwt.getEncodedSignature() = " + jwt.getEncodedSignature() + " ,jwt.getHeader().getKeyId() = "
                    + jwt.getHeader().getKeyId() + " ,jwt.getHeader().getSignatureAlgorithm() = "
                    + jwt.getHeader().getSignatureAlgorithm()
                    + " ,jwt.getClaims().getClaimAsString(JwtHeaderName.ALGORITHM) = "
                    + jwt.getClaims().getClaimAsString(JwtHeaderName.ALGORITHM)
                    + " ,jwt.getClaims().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD) = "
                    + jwt.getClaims().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD) + ".");

            final Date expiresAt = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
            List<String> scopes = jwt.getClaims().getClaimAsStringList("scope");

            log.debug("\n\n JwtUtil::validateToken() - expiresAt = " + expiresAt + " , issuer =" + issuer
                    + " , scopes = " + scopes + "\n");

            // Validate token is not expired
            log.info("Validate JWT");
            final Date now = new Date();
            if (now.after(expiresAt)) {
                log.error("ID Token is expired. (It is after " + now + ").");
                throw new WebApplicationException("ID Token is expired",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            // Validate issuer
            log.info("Validate JWT Issuer");
            if (!authUtil.isValidIssuer(issuer)) {
                throw new WebApplicationException("Jwt Issuer is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            // Retrieve JSON Web Key Set Uri
            log.info("Retrieve JSON Web Key Set URI");
            String jwksUri = this.getJwksUri(issuer);
            log.trace("\n\n JwtUtil::validateToken() - jwksUri = " + jwksUri);

            // Retrieve JSON Web Key Set
            log.info("Retrieve JSON Web Key Set");
            JSONWebKeySet jsonWebKeySet = this.getJSONWebKeys(jwksUri);
            log.trace("\n\n JwtUtil::validateToken() - jsonWebKeySet = " + jsonWebKeySet);

            // Verify the signature used to sign the access token
            log.info("Verify JWT signature");
            boolean isJwtSignatureValid = this.validateSignature(jwt, jsonWebKeySet);
            log.debug("\n\n JwtUtil::validateToken() - isJwtSignatureValid = " + isJwtSignatureValid + "\n\n");

            if (!isJwtSignatureValid) {
                throw new WebApplicationException("Jwt Signature is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            return scopes;
        } catch (InvalidJwtException exp) {
            log.error("Not a valid Jwt token = " + exp);
            throw exp;
        }

    }

    public void validateToken(String token, List<String> resourceScopes) throws InvalidJwtException, Exception {
        log.trace("Validate Jwt Token - token = " + token + " ,resourceScopes = " + resourceScopes + "\n");
        // 1. Parse Jwt token
        // 2. Validate Token
        // 3. Validate Issuer
        // 4. Retrieve Auth Server JSON Web Keys - jwks_uri"
        // :"https://jenkins-config-api.gluu.org/jans-auth/restv1/jwks",
        // 5. Verify the signature used to sign the access token
        // 6. Verify the scopes

        try {
            // Parse Token
            Jwt jwt = this.parse(token);
            log.trace("JwtUtil::validateToken() -JWT details : " + " jwt.getSigningInput() = " + jwt.getSigningInput()
                    + " ,jwt.getEncodedSignature() = " + jwt.getEncodedSignature() + " ,jwt.getHeader().getKeyId() = "
                    + jwt.getHeader().getKeyId() + " ,jwt.getHeader().getSignatureAlgorithm() = "
                    + jwt.getHeader().getSignatureAlgorithm()
                    + " ,jwt.getClaims().getClaimAsString(JwtHeaderName.ALGORITHM) = "
                    + jwt.getClaims().getClaimAsString(JwtHeaderName.ALGORITHM)
                    + " ,jwt.getClaims().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD) = "
                    + jwt.getClaims().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD) + ".");

            final Date expiresAt = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
            List<String> scopes = jwt.getClaims().getClaimAsStringList("scope");

            log.debug("\n\n JwtUtil::validateToken() - expiresAt = " + expiresAt + " , issuer =" + issuer
                    + " , scopes = " + scopes + "\n");

            // Validate token is not expired
            log.info("Validate JWT");
            final Date now = new Date();
            if (now.after(expiresAt)) {
                log.error("ID Token is expired. (It is after " + now + ").");
                throw new WebApplicationException("ID Token is expired",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            // Validate issuer
            log.info("Validate JWT Issuer");
            if (!authUtil.isValidIssuer(issuer)) {
                throw new WebApplicationException("Jwt Issuer is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            // Retrieve JSON Web Key Set Uri
            log.info("Retrieve JSON Web Key Set URI");
            String jwksUri = this.getJwksUri(issuer);
            log.trace("\n\n JwtUtil::validateToken() - jwksUri = " + jwksUri);

            // Retrieve JSON Web Key Set
            log.info("Retrieve JSON Web Key Set");
            JSONWebKeySet jsonWebKeySet = this.getJSONWebKeys(jwksUri);
            log.trace("\n\n JwtUtil::validateToken() - jsonWebKeySet = " + jsonWebKeySet);

            // Verify the signature used to sign the access token
            log.info("Verify JWT signature");
            boolean isJwtSignatureValid = this.validateSignature(jwt, jsonWebKeySet);
            log.debug("\n\n JwtUtil::validateToken() - isJwtSignatureValid = " + isJwtSignatureValid + "\n\n");

            if (!isJwtSignatureValid) {
                throw new WebApplicationException("Jwt Signature is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            // Validate Scopes
            log.info("Validate token scopes");
            if (!authUtil.validateScope(scopes, resourceScopes)) {
                log.error("Insufficient scopes. Required scope: " + resourceScopes + ", token scopes: " + scopes);
                throw new WebApplicationException("Insufficient scopes. Required scope",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

        } catch (InvalidJwtException exp) {
            log.error("Not a valid Jwt token = " + exp);
            throw exp;
        }

    }

    public boolean validateSignature(Jwt jwt, JSONWebKeySet jsonWebKeySet) {
        log.trace("\n\n JwtUtil::validateSignature() - jwt = " + jwt + " , jsonWebKeySet =" + jsonWebKeySet + "\n");
        try {

            final String kid = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
            final String algorithm = jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
            final SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
            log.trace("\n\n JwtUtil::validateSignature() - kid = " + kid + " , algorithm =" + algorithm
                    + " signatureAlgorithm = " + signatureAlgorithm + "\n");
            PublicKey publicKey = getPublicKey(kid, jsonWebKeySet, signatureAlgorithm);

            log.trace("\n\n JwtUtil::validateSignature() - publicKey = " + publicKey + "\n");
            if (publicKey == null) {
                log.error("Failed to get RSA public key.");
                return false;
            }

            // Validate
            AbstractJwsSigner signer = null;
            if (AlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily())) {
                signer = new RSASigner(SignatureAlgorithm.fromString(algorithm), (RSAPublicKey) publicKey);

            } else if (AlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
                signer = new ECDSASigner(SignatureAlgorithm.fromString(algorithm), (ECDSAPublicKey) publicKey);
            }

            if (signer == null) {
                log.error("ID Token signer is not found!");
                return false;
            }

            boolean signature = signer.validate(jwt);
            if (signature) {
                log.debug("ID Token is successfully validated.");
                return true;
            }

            log.error("ID Token signature invalid.");
            return false;

        } catch (Exception e) {
            log.error("Failed to validate id_token. Message: " + e.getMessage(), e);
            return false;
        }
    }

    public PublicKey getPublicKey(String kid, JSONWebKeySet jsonWebKeySet, SignatureAlgorithm signatureAlgorithm) {
        log.trace("\n\n JwtUtil::getPublicKey() - kid = " + kid + " , jsonWebKeySet =" + jsonWebKeySet
                + " , signatureAlgorithm =  " + signatureAlgorithm + "\n");
        JSONWebKey key = jsonWebKeySet.getKey(kid);
        if (key != null) {
            switch (key.getKty()) {
            case RSA:
                return new RSAPublicKey(key.getN(), key.getE());
            case EC:
                return new ECDSAPublicKey(SignatureAlgorithm.fromString(key.getAlg().getParamName()), key.getX(),
                        key.getY());
            default:
                return null;
            }
        }
        return null;
    }

    public JSONObject fromJson(String json) throws IOException {
        log.trace("\n\n JwtUtil::fromJson() - json = " + json + " \n");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.readValue(json, JSONObject.class);
    }

    public String getJwksUri(String issuer) throws Exception {
        log.debug("JwtUtil::getJSONWebKeys() - issuer = " + issuer);
        if (StringHelper.isNotEmpty(issuer) && issuer.equals(configurationService.find().getIssuer())) {
            return configurationService.find().getJwksUri();
        }
        return AuthClientFactory.getJwksUri(issuer);

    }

    public JSONWebKeySet getJSONWebKeys(String jwksUri) throws Exception {
        log.debug("\n\n JwtUtil::getJSONWebKeys() - jwksUri = " + jwksUri + " \n");
        JSONWebKeySet jsonWebKeySet = AuthClientFactory.getJSONWebKeys(jwksUri);
        log.trace("\n\n JwtUtil::getJSONWebKeys() - jsonWebKeySet = " + jsonWebKeySet + " \n");
        return jsonWebKeySet;
    }

}
