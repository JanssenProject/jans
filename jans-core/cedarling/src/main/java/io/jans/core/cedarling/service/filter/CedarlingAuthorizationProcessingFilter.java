package io.jans.core.cedarling.service.filter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.core.cedarling.service.CedarlingProtection;
import io.jans.core.cedarling.service.app.audit.CedarlingApplicationAuditLogger;
import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
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
	protected Logger log;
	
	@Inject
	protected CedarlingApplicationAuditLogger cedarlingApplicationAuditLogger;

	@Inject
	protected CedarlingProtection protectionService;

	@Context
	protected HttpHeaders httpHeaders;

	@Context
	protected ResourceInfo resourceInfo;

	@Context
	protected HttpServletRequest httpRequest;
	
    protected String extractBearerToken() {
        String authHeader = httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION);
        
        if (StringUtils.isEmpty(authHeader)) {
            return null;
        }

        return authHeader.replaceFirst("(?i)Bearer\\s+", "");
    }

}
