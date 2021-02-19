package io.jans.configapi.auth.util;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.jws.RSASigner;
import io.jans.configapi.auth.client.AuthClientFactory;
import io.jans.configapi.service.ConfigurationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;

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

	public void validateToken(String token, List<String> resourceScopes) throws InvalidJwtException,Exception {
		log.info("JwtUtil::validateToken() - token = " + token + " , resourceScopes =" + resourceScopes + "\n");
		// 1. Parse Jwt token
		// 2. Validate Token
		// 3. Validate Issuer
		// 4. Retrieve Auth Server JSON Web Keys - jwks_uri"
		// :"https://jenkins-config-api.gluu.org/jans-auth/restv1/jwks",
		// 5. Verify the signature used to sign the access token
		// 6. Verify the scopes

		try {
			// Parse Token
			Jwt idToken = this.parse(token);
			log.trace("JwtUtil::validateToken() -JWT details : idToken.toString() - " + idToken.toString()
					+ " , idToken.getClaims() " + idToken.getClaims() + " , idToken.getHeader() = "
					+ idToken.getHeader());

			final Date expiresAt = idToken.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
			String issuer = idToken.getClaims().getClaimAsString(JwtClaimName.ISSUER);
			List<String> scopes = idToken.getClaims().getClaimAsStringList("scope");

			log.debug("\n\n JwtUtil::validateToken() - expiresAt = " + expiresAt + " , issuer =" + issuer
					+ " , scopes = " + scopes + "\n");

			// Validate token is not expired
			final Date now = new Date();
			if (now.after(expiresAt)) {
				log.error("ID Token is expired. (It is after " + now + ").");
				throw new WebApplicationException("ID Token is expired",
						Response.status(Response.Status.UNAUTHORIZED).build());
			}

			// Validate issuer
			if (!authUtil.isValidIssuer(issuer)) {
				throw new WebApplicationException("Jwt Issuer is Invalid.",
						Response.status(Response.Status.UNAUTHORIZED).build());
			}

			// Retrieve JSON Web Key Set Uri
			String jwksUri = this.getJwksUri(issuer);
			log.trace("\n\n JwtUtil::validateToken() - jwksUri = " + jwksUri);

			// Retrieve JSON Web Key Set
			JSONWebKeySet jSONWebKeySet = this.getJSONWebKeys(jwksUri);
			log.trace("\n\n JwtUtil::validateToken() - jSONWebKeySet = " + jSONWebKeySet);

			// Verify the signature used to sign the access token
			boolean isJwtSignatureValid = this.validateSignature(idToken, jSONWebKeySet);
			log.info("\n\n JwtUtil::validateToken() - isJwtSignatureValid = " + isJwtSignatureValid);
			
			if (!isJwtSignatureValid) {
				throw new WebApplicationException("Jwt Signature is Invalid.",
						Response.status(Response.Status.UNAUTHORIZED).build());

			}

			// Validate Scopes
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
	
	public boolean validateSignature(Jwt idToken, JSONWebKeySet jSONWebKeySet) {
		log.trace(
				"\n\n JwtUtil::validateSignature() - idToken = " + idToken + " , jSONWebKeySet =" + jSONWebKeySet + "\n");
		try {
			/*
			 * // Verify the signature used to sign the access token AuthCryptoProvider
			 * cryptoProvider = new AuthCryptoProvider(); boolean isValidJwt =
			 * cryptoProvider.verifySignature(idToken.getSigningInput(),
			 * idToken.getEncodedSignature(), null, null, null, SignatureAlgorithm.HS256);
			 */
			final String kid = idToken.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
			final String algorithm = idToken.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
			log.trace(
					"\n\n JwtUtil::validateSignature() - kid = " + kid + " , algorithm =" + algorithm + "\n");
			RSAPublicKey publicKey = getPublicKey(kid, jSONWebKeySet);
			
			log.trace(
					"\n\n JwtUtil::validateSignature() - publicKey = " + publicKey + "\n");
			
			if (publicKey != null) {
				RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.fromString(algorithm), publicKey);
				boolean signature = rsaSigner.validate(idToken);
				if (signature) {
					log.debug("ID Token is successfully validated.");
					return true;
				}
				log.error("ID Token signature is invalid.");
			} else {
				log.error("Failed to get RSA public key.");
			}
			return false;
		} catch (Exception e) {
			log.error("Failed to validate id_token. Message: " + e.getMessage(), e);
			return false;
		}
	}

	private RSAPublicKey getPublicKey(String kid, JSONWebKeySet jSONWebKeySet) {
		log.trace("\n\n JwtUtil::getPublicKey() - kid = " + kid + " , jSONWebKeySet =" + jSONWebKeySet + "\n");
		JSONWebKey key = jSONWebKeySet.getKey(kid);
		if (key != null) {
			switch (key.getKty()) {
			case RSA:
				return new RSAPublicKey(key.getN(), key.getE());
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
		log.info("JwtUtil::getJSONWebKeys() - issuer = " + issuer);
		if (StringHelper.isNotEmpty(issuer) && issuer.equals(configurationService.find().getIssuer())) {
			return configurationService.find().getJwksUri();
		}
		return AuthClientFactory.getJwksUri(issuer);

	}

	public JSONWebKeySet getJSONWebKeys(String jwksUri) throws Exception {
		log.info("\n\n JwtUtil::getJSONWebKeys() - jwksUri = " + jwksUri + " \n");
		JSONWebKeySet jSONWebKeySet = AuthClientFactory.getJSONWebKeys(jwksUri);
		log.trace("\n\n JwtUtil::getJSONWebKeys() - jSONWebKeySet = " + jSONWebKeySet + " \n");
		return jSONWebKeySet;
	}
	
}
