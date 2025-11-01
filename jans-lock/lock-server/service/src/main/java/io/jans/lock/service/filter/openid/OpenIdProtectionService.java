package io.jans.lock.service.filter.openid;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.lock.service.OpenIdService;
import io.jans.lock.service.filter.OpenIdProtection;
import io.jans.service.security.api.ProtectedApi;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class OpenIdProtectionService implements OpenIdProtection {

    @Inject
    private Logger log;

    @Inject
    private OpenIdService openIdService;
    
    private IntrospectionService introspectionService;
    
    private OpenIdConfigurationResponse oidcConfig;
    
    private ObjectMapper mapper;

    @PostConstruct
    private void init() {
        try {
            mapper = new ObjectMapper();
            oidcConfig = openIdService.getOpenIdConfiguration();
            
            String introspectionEndpoint = oidcConfig.getIntrospectionEndpoint();
            introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, ClientFactory.instance().createEngine());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Validates the incoming request's access token for the resource and returns an HTTP error response when authorization fails.
     *
     * <p>Accepts either a JWT or an opaque token. For opaque tokens, performs token introspection. For JWTs, validates issuer, expiration,
     * cryptographic signature (HMAC-signed tokens are rejected), and required scopes for the target resource.</p>
     *
     * @param headers      HTTP headers containing the Authorization header
     * @param resourceInfo information about the target resource used to determine required scopes
     * @return a Response describing the authorization failure (UNAUTHORIZED, FORBIDDEN, or INTERNAL_SERVER_ERROR) or `null` if authorization succeeds
     */
    public Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo) {
        try {
            String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            boolean authFound = StringUtils.isNotEmpty(token);
            log.debug("Authorization header{} found", authFound ? "" : " not");
            
            if (!authFound) {
                log.debug("Request is missing authorization header");
                // See section 3.12 RFC 7644
                return simpleResponse(UNAUTHORIZED, "No authorization header found");
            }
            
            token = token.replaceFirst("Bearer\\s+","");
            log.debug("Validating bearer token");

            List<String> scopes = getRequestedScopes(resourceInfo);
            log.debug("Call requires scopes: {}", scopes);

            Jwt jwt = tokenAsJwt(token);
            if (jwt == null) {
                // Do standard token validation
                IntrospectionResponse iresp = null;
                try {
                    iresp = introspectionService.introspectToken("Bearer " + token, token);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

                return processIntrospectionResponse(iresp, scopes);
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

            List<String> tokenScopes = claims.getClaimAsStringList("scope");    //tokenScopes is never null
            if (valid && tokenScopes.containsAll(scopes)) {
            	return null;
            }
 
            String msg = "Invalid token signature or insufficient scopes";
            log.error("{}. Token scopes: {}", msg, tokenScopes);
            
            // See section 3.12 RFC 7644
            return simpleResponse(FORBIDDEN, msg);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return simpleResponse(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Response processIntrospectionResponse(IntrospectionResponse iresponse, List<String> scopes) {
        Response response = null;
        List<String> tokenScopes = Optional.ofNullable(iresponse).map(IntrospectionResponse::getScope)
                .orElse(null);

        if (tokenScopes == null || !iresponse.isActive() || !tokenScopes.containsAll(scopes)) {
            String msg = "Invalid token or insufficient scopes";
            log.error("{}. Token scopes: {}", msg, tokenScopes);
            // See section 3.12 RFC 7644
            response = simpleResponse(Response.Status.FORBIDDEN, msg);
        }

        return response;
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

    private List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        List<String> scopes = new ArrayList<>();
        scopes.addAll(getScopesFromAnnotation(resourceInfo.getResourceClass()));
        scopes.addAll(getScopesFromAnnotation(resourceInfo.getResourceMethod()));

        Method baseMethod = resourceInfo.getResourceMethod();
        for (Class<?> interfaces : resourceInfo.getResourceClass().getInterfaces()) {
            scopes.addAll(getScopesFromAnnotation(interfaces));
            
            Method method = null;
			try {
				method = interfaces.getDeclaredMethod(baseMethod.getName(), baseMethod.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
				// It's expected behavior
			}
            if (method != null) {
                scopes.addAll(getScopesFromAnnotation(method));
            }

        }

        return scopes;
    }

    private List<String> getScopesFromAnnotation(AnnotatedElement elem) {		
        return optAnnnotation(elem, ProtectedApi.class).map(ProtectedApi::scopes)
            .map(Arrays::asList).orElse(Collections.emptyList());
    }	

    private static <T extends Annotation> Optional<T> optAnnnotation(AnnotatedElement elem, Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }

    public Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

}