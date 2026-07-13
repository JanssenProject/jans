package io.jans.core.cedarling.service.filter;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.core.cedarling.service.CedarlingProtection;
import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;


/**
 * A RestEasy filter to centralize protection of APIs based on path pattern
 */
// To protect JAX-RS resources with this filter add the @ProtectedApi annotation
@ProtectedCedarlingApi
@Priority(Priorities.AUTHENTICATION + 1)
@Dependent
public abstract class CedarlingAuthorizationProcessingFilter implements ContainerRequestFilter {

	@Inject
	private Logger log;

	@Inject
	private CedarlingProtection protectionService;

	@Context
	private HttpHeaders httpHeaders;

	@Context
	private ResourceInfo resourceInfo;

	@Context
    private HttpServletRequest httpRequest;
	
    protected String extractBearerToken() {
        String authHeader = httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION);
        
        if (StringUtils.isEmpty(authHeader)) {
            return null;
        }

        return authHeader.replaceFirst("(?i)Bearer\\s+", "");
    }

}
