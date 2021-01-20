package io.jans.scim.auth;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.scim.service.OpenIdService;

@ApplicationScoped
@Named
@BindingUrls({"/"})
public class OAuthTokenProtectionService extends ProtectionService implements Serializable {

	private static final long serialVersionUID = -5447131971095468865L;

    @Inject
    private Logger log;

    @Inject
    private OpenIdService openIdService;
    
    private IntrospectionService introspectionService;

    /**
     * This method checks whether the authorization header is present and valid before scim service methods can be actually
     * called.
     * @param headers An object holding HTTP headers
     * @param resourceInfo An object that allows access to request URI information
     * @return A null value if the authorization was successful, otherwise a Response object is returned signaling an
     * authorization error
     */
	public Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo) {

        Response authorizationResponse = null;
        String token = headers.getHeaderString("Authorization");
        log.info("==== SCIM Service call intercepted ====");
        boolean authFound = StringUtils.isNotEmpty(token);

        try {
            log.info("Authorization header {} found", authFound ? "" : "not");   
			if (authFound) {
				token = token.replaceFirst("Bearer\\s+","");
				log.debug("Validating token {}", token);
								
                IntrospectionResponse response = null;
                try {
                	response = introspectionService.introspectToken("Bearer " + token, token);
                } catch (Exception e) {
                	log.error(e.getMessage());
                }
                List<String> tokenScopes = Optional.ofNullable(response).map(IntrospectionResponse::getScope).orElse(null);

                if (tokenScopes == null || !response.isActive() || !tokenScopes.containsAll(getRequestedScopes(resourceInfo))) {
                	String msg = "Invalid token or insufficient scopes";
                    log.error("{}. Token scopes: {}", msg, tokenScopes);
				    //see section 3.12 RFC 7644
                    authorizationResponse = getErrorResponse(Status.FORBIDDEN, msg);
                }
			} else {
				log.info("Request is missing authorization header");
				//see section 3.12 RFC 7644
				authorizationResponse = getErrorResponse(Status.UNAUTHORIZED, "No authorization header found");
			}    
        } catch (Exception e){
            log.error(e.getMessage(), e);
            authorizationResponse = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return authorizationResponse;

    }
    
    @PostConstruct
    private void init() {

        try {
        	String introspectionEndpoint = openIdService.getOpenIdConfiguration().getIntrospectionEndpoint();
            introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
    }
    
}
