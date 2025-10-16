package io.jans.lock.cedarling.service.filter;

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
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.lock.cedarling.model.CedarlingPermission;
import io.jans.lock.cedarling.service.CedarlingAuthorizationService;
import io.jans.lock.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.util.exception.MissingResourceException;
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
    private AppConfiguration appConfiguration;
    
    @Inject
    private CedarlingAuthorizationService authorizationService;

    private OpenIdConfigurationResponse oidcConfig;
    
    private ObjectMapper mapper;
    
    @PostConstruct
    private void init() {
        try {
            mapper = new ObjectMapper();
            oidcConfig = getOpenIdConfiguration();
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

            List<CedarlingPermission> requestedPermissions = getRequestedOperations(resourceInfo);
            log.info("Check access to requested opearations: {}", requestedPermissions);
            if (requestedPermissions.size() == 0) {
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
	            for (CedarlingPermission requestedPermission : requestedPermissions) {
	            	authorized &= authorizationService.authorize(tokens, requestedPermission.getAction(),
	            			getCedarlingResource(requestedPermission), getCedarlingContext());
	            	if (!authorized) {
	            		log.error("Insufficient permissions to access '{}'", requestedPermission);
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
            if (log.isTraceEnabled()) {
            	log.trace("This looks like a JWT token");
            }
        } catch (InvalidJwtException e) {
            log.trace("Not a JWT token");
        }
        
        return jwt;
    }

	private Map<String, String> getCedarlingTokens(String accessToken) {
		return Map.of("access_token", accessToken);
	}

	private Map<String, Object> getCedarlingResource(CedarlingPermission requestedPermission) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		int id = requestedPermission.hashCode();
		id = id > 0 ? id : -id;
		map.putAll(
				Map.of("cedar_entity_mapping",
						Map.of("entity_type", requestedPermission.getResource(), "id", requestedPermission.getId())
					)
		);
		map.putAll(
				Map.of("url",
						Map.of("host", "", "path", requestedPermission.getPath(), "protocol", "")
					)
		);
		map.putAll(
				Map.of("header",
						Map.of()
					)
		);

		return map;
	}
	
	private Map<String, Object> getCedarlingContext() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		return map;
	}

    private List<CedarlingPermission> getRequestedOperations(ResourceInfo resourceInfo) {
        List<CedarlingPermission> cedarlingPermissions = new ArrayList<>();
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

	private void addCedarlingPermission(List<CedarlingPermission> cedarlingPermissions, CedarlingPermission permission) {
		if (permission != null) {
			cedarlingPermissions.add(permission);
		}
	}

	private CedarlingPermission getOperationFromAnnotation(AnnotatedElement elem) {
		Optional<ProtectedCedarlingApi> annotation = optAnnnotation(elem, ProtectedCedarlingApi.class);
		if (annotation.isPresent()) {
			ProtectedCedarlingApi cedarlingPermission = annotation.get();
			return new CedarlingPermission(cedarlingPermission.action(), cedarlingPermission.resource(), cedarlingPermission.id(), cedarlingPermission.path());
		} else {
			return null;
		}
	}

    private static <T extends Annotation> Optional<T> optAnnnotation(AnnotatedElement elem, Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }

	private OpenIdConfigurationResponse getOpenIdConfiguration() {
		String openIdIssuer = appConfiguration.getOpenIdIssuer();
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

    public Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

}
