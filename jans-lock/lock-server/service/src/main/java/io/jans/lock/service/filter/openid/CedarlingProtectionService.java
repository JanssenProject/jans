package io.jans.lock.service.filter.openid;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.lock.cedarling.service.CedarlingAuthorizationService;
import io.jans.lock.cedarling.service.filter.CedarlingProtection;
import io.jans.lock.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.OpenIdService;
import io.jans.util.Pair;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class CedarlingProtectionService implements CedarlingProtection {

    @Inject
    private Logger log;

    @Inject
    private OpenIdService openIdService;
    
    @Inject
    private CedarlingAuthorizationService authorizationService;
    
    private OpenIdConfigurationResponse oidcConfig;
    
    private ObjectMapper mapper;
    
    @PostConstruct
    private void init() {
        try {
            mapper = new ObjectMapper();
            oidcConfig = openIdService.getOpenIdConfiguration();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Response processAuthorization(ContainerRequestContext requestContext, HttpHeaders headers, ResourceInfo resourceInfo) {
        try {
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            boolean authFound = StringUtils.isNotEmpty(token);
            log.info("Authorization header {} found", authFound ? "" : "not");
            
            if (!authFound) {
                log.info("Request is missing authorization header");
                // See section 3.12 RFC 7644
                return simpleResponse(UNAUTHORIZED, "No authorization header found");
            }
            
            token = token.replaceFirst("Bearer\\s+","");
            log.debug("Validating token {}", token);

            List<Pair<String, String>> requestedOperations = getRequestedOperations(resourceInfo);
            log.info("Check access to requested opearations: {}", requestedOperations);
            if (requestedOperations.size() == 0) {
	            return simpleResponse(INTERNAL_SERVER_ERROR, "Access to operation is not correct");
            }

            Jwt jwt = tokenAsJwt(token);
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
                
            Map<?, ?> jwks = mapper.readValue(new URL(oidcConfig.getJwksUri()), Map.class);
            boolean valid = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), 
                    jwt.getHeader().getKeyId(), new JSONObject(jwks), null, signatureAlg);

            if (valid) {
	            boolean authorized = true;
	            Map<String, String> tokens = getCedarlingTokens(token);
	            for (Pair<String, String> requestedOperation : requestedOperations) {
	            	authorized &= authorizationService.authorize(tokens, requestedOperation.getFirst(),
	            			getCedarlingResource(requestedOperation.getSecond()), getCedarlingCpntext());
	            	if (!authorized) {
	            		log.error("Insufficient permissions to access '{}' resource '{}'", requestedOperation.getSecond(), requestedOperation.getFirst());
	            		break;
	            	}
	            }
	            
	            if (authorized) {
	            	return null;
	            }
            }
 
            // See section 3.12 RFC 7644
            return simpleResponse(FORBIDDEN, "Invalid token signature or insufficient scopes");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return simpleResponse(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Jwt tokenAsJwt(String token) {
        Jwt jwt = null;
        try {
            jwt = Jwt.parse(token);
            log.trace("This looks like a JWT token");
        } catch (InvalidJwtException e) {
            log.trace("Not a JWT token");
        }
        
        return jwt;
    }

	private Map<String, String> getCedarlingTokens(String accessToken) {
		return Map.of("access_token", accessToken);
	}

	private Map<String, Object> getCedarlingResource(String entityType) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		int id = entityType.hashCode();
		id = id > 0 ? id : -id;
		map.putAll(
				Map.of("cedar_entity_mapping",
						Map.of("entity_type", entityType, "id", String.valueOf(id))
					)
			);
		return map;
	}

	private Map<String, Object> getCedarlingCpntext() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		return map;
	}

    private List<Pair<String, String>> getRequestedOperations(ResourceInfo resourceInfo) {
        List<Pair<String, String>> cedarlingPermissions = new ArrayList<>();
        addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(resourceInfo.getResourceClass()));
        addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(resourceInfo.getResourceMethod()));

        Method baseMethod = resourceInfo.getResourceMethod();
        for (Class<?> interfaces : resourceInfo.getResourceClass().getInterfaces()) {
        	addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(interfaces));
            
            Method method = null;
			try {
				method = interfaces.getDeclaredMethod(baseMethod.getName(), baseMethod.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
				// It's expected behavior
			}
            if (method != null) {
            	addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(method));
            }

        }

        return cedarlingPermissions;
    }

	private void addCedarlingPermission(List<Pair<String, String>> cedarlingPermissions, Pair<String, String> permission) {
		if (permission != null) {
			cedarlingPermissions.add(permission);
		}
	}

	private Pair<String, String> getOperationFromAnnotation(AnnotatedElement elem) {
		Optional<ProtectedCedarlingApi> annotation = optAnnnotation(elem, ProtectedCedarlingApi.class);
		if (annotation.isPresent()) {
			ProtectedCedarlingApi cedarlingPermission = annotation.get();
			return new Pair(cedarlingPermission.action(), cedarlingPermission.resource());
		} else {
			return null;
		}
	}

    private static <T extends Annotation> Optional<T> optAnnnotation(AnnotatedElement elem, Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }

    public Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

}
