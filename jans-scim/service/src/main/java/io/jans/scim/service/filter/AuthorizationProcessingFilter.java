package io.jans.scim.service.filter;

import io.jans.scim.auth.IProtectionService;
import io.jans.scim.auth.JansRestService;
import io.jans.scim.auth.ProtectionServiceSelector;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * A RestEasy filter to centralize protection of APIs based on path pattern
 */
// To protect JAX-RS resources with this filter add the @ProtectedApi annotation
// to them and ensure there is a proper class implementing JansRestService
// that is capable of handling specific protection logic for your particular case
@Provider
@ProtectedApi
@Priority(Priorities.AUTHENTICATION)
@RequestScoped
public class AuthorizationProcessingFilter implements ContainerRequestFilter {

	@Inject
	private Logger log;

	@Context
	private HttpHeaders httpHeaders;

	@Context
	private ResourceInfo resourceInfo;
	
	@Inject
	private ProtectionServiceSelector beanSelector;

	/**
	 * This method performs the protection check of service invocations: it provokes
	 * returning an early error response if the underlying protection logic does not
	 * succeed, otherwise, makes the request flow to its destination service object
	 * 
	 * @param requestContext
	 *            The ContainerRequestContext associated to filter execution
	 * @throws IOException
	 *             In practice no exception is thrown here. It's present to conform
	 *             to interface implemented.
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
            
            Response authorizationResponse = null;
            String path = requestContext.getUriInfo().getPath();
            log.debug("REST call to '{}' intercepted", path);
            JansRestService api = beanSelector.select(path);

            if (api == null) {
                log.warn("No REST service bean associated to this path (resource will be accessed anonymously)");
            } else if (!api.isEnabled()){
                log.warn("Please activate {} API", api.getName());
                authorizationResponse = disabledApiResponse(api.getName());
            } else {
                IProtectionService protectionService = api.getProtectionService();

                if (protectionService == null) {
                    log.warn("No concrete protection mechanism associated to this API. Denying access");
                    authorizationResponse = unprotectedApiResponse(api.getName());

                } else {
                    log.debug("Path is protected, proceeding with authorization processing...");
                    authorizationResponse = protectionService.processAuthorization(httpHeaders, resourceInfo);
                    if (authorizationResponse == null) {
                        // Actual processing of request proceeds
                        log.debug("Authorization passed");
                    }
                }
            }
            if (authorizationResponse != null) {
                requestContext.abortWith(authorizationResponse);
            }

	}

    private Response unprotectedApiResponse(String name) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(name + " API not protected")
                .build();
    }

    private Response disabledApiResponse(String name) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(name + " API is disabled")
                .build();
    }

}
