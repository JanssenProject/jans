package io.jans.lock.service;

import static io.jans.core.cedarling.service.CedarlingProtection.simpleResponse;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.core.cedarling.model.CedarlingPermission;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.util.exception.MissingResourceException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * @author Yuriy Movchan Date: 10/08/2022
 */
@ApplicationScoped
public class CedarlingProtectionService extends io.jans.core.cedarling.service.CedarlingProtectionService {

    public static final int JWKS_CACHE_LIFETIME = 60;

    private OpenIdConfigurationResponse oidcConfig;

    private ObjectMapper mapper;

    private Cache<String, Map<?, ?>> jwksCache;

    @PostConstruct
    private void init() {
        try {
            mapper = new ObjectMapper();
            jwksCache = CacheBuilder.newBuilder().expireAfterWrite(JWKS_CACHE_LIFETIME, TimeUnit.MINUTES).build();
            oidcConfig = getOpenIdConfiguration();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Response processAuthorization(String bearerToken, ResourceInfo resourceInfo) {
        try {
            if (oidcConfig == null) {
                return simpleResponse(FORBIDDEN, "Loaded OpenID configuration is invalid!");
            }

            boolean authFound = StringUtils.isNotEmpty(bearerToken);
            log.info("Authorization header {} found", authFound ? "" : "not");
            
            if (!authFound) {
                log.info("Request is missing authorization header");
                // See section 3.12 RFC 7644
                return simpleResponse(UNAUTHORIZED, "No authorization header found");
            }
            
            bearerToken = bearerToken.replaceFirst("Bearer\\s+","");
            log.debug("Validating token {}", bearerToken);

            List<CedarlingPermission> requestedPermissions = getRequestedOperations(resourceInfo);
            if (requestedPermissions.isEmpty()) {
                return simpleResponse(INTERNAL_SERVER_ERROR, "Access to operation is not correct");
            }

            Jwt jwt = tokenAsJwt(bearerToken);
            if (jwt == null) {
                return simpleResponse(FORBIDDEN, "Provided token isn't JWT encoded");
            }

            // Process the JWT: validate isuer, expiration and signature
            JwtClaims claims = jwt.getClaims();

            if (!oidcConfig.getIssuer().equals(claims.getClaimAsString(JwtClaimName.ISSUER))) {
                return simpleResponse(FORBIDDEN, "Invalid token issuer");
            }

            int exp = Optional.ofNullable(claims.getClaimAsInteger(JwtClaimName.EXPIRATION_TIME)).orElse(0);
            if (1000L * exp < System.currentTimeMillis()) {
                return simpleResponse(FORBIDDEN, "Expired token");
            }

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(null, null, null, true);
            SignatureAlgorithm signatureAlg = jwt.getHeader().getSignatureAlgorithm();
            
            if (AlgorithmFamily.HMAC.equals(signatureAlg.getFamily())) {
                // It is "expensive" to get the associated client secret
                return simpleResponse(INTERNAL_SERVER_ERROR,
                        "HMAC algorithm not allowed for token signature. Please use an algorithm in the EC, ED, or RSA family for signing");
            }
                
            String jwksUri = oidcConfig.getJwksUri();
            Map<?, ?> jwks = getJwks(jwksUri);
            boolean valid = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(),
                    jwt.getHeader().getKeyId(), new JSONObject(jwks), null, signatureAlg);

            if (!valid) {
                // Cached JWKS can be stale after key rotation, refresh once and retry
                jwksCache.invalidate(jwksUri);
                jwks = getJwks(jwksUri);
                valid = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(),
                        jwt.getHeader().getKeyId(), new JSONObject(jwks), null, signatureAlg);
            }

            if (valid) {
                return isValid(bearerToken, resourceInfo);
            }

            // See section 3.12 RFC 7644
            return simpleResponse(FORBIDDEN, "Invalid token signature");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return simpleResponse(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Map<?, ?> getJwks(String jwksUri) throws Exception {
        return jwksCache.get(jwksUri, () -> {
            log.debug("Loading JWKS from {}", jwksUri);
            return mapper.readValue(new URL(jwksUri), Map.class);
        });
    }

    private Jwt tokenAsJwt(String token) {
        Jwt jwt = null;
        try {
            jwt = Jwt.parse(token);
            if (log.isTraceEnabled()) {
            	log.trace("This looks like a JWT token");
            }
        } catch (InvalidJwtException e) {
            log.trace("Not a JWT token");
        }
        
        return jwt;
    }

	private OpenIdConfigurationResponse getOpenIdConfiguration() {
		String openIdIssuer = openIDConnectConfig.getIssuer();
		if (StringHelper.isEmpty(openIdIssuer)) {
			throw new InvalidConfigurationException("OpenIdIssuer Url is invalid");
		}

		String openIdIssuerEndpoint = openIdIssuer + "/.well-known/openid-configuration";

		OpenIdConfigurationClient client = new OpenIdConfigurationClient(openIdIssuerEndpoint);
		OpenIdConfigurationResponse openIdConfigurationResponse = client.execOpenIdConfiguration();

		if ((openIdConfigurationResponse == null) || (openIdConfigurationResponse.getStatus() != HttpStatus.SC_OK)) {
			throw new MissingResourceException("Failed to load OpenID configuration!");
		}

		log.info("Successfully loaded OpenID configuration");

		return openIdConfigurationResponse;
	}
}
