package io.jans.lock.cedarling.service.filter;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.lock.cedarling.service.CedarlingProtection;
import io.jans.lock.cedarling.service.app.audit.ApplicationCedarlingAuditLogger;
import io.jans.lock.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.app.audit.AuditActionType;
import io.jans.lock.model.app.audit.AuditLogEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.net.InetAddressUtility;
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
import jakarta.ws.rs.core.Response;

/**
 * A RestEasy filter to centralize protection of APIs based on path pattern
 */
// To protect JAX-RS resources with this filter add the @ProtectedApi annotation
@ProtectedCedarlingApi
@Priority(Priorities.AUTHENTICATION + 1)
@Dependent
public class CedarlingAuthorizationProcessingFilter implements ContainerRequestFilter {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private CedarlingProtection protectionService;
	
	@Inject
	private ApplicationCedarlingAuditLogger applicationCedarlingAuditLogger;

	@Context
	private HttpHeaders httpHeaders;

	@Context
	private ResourceInfo resourceInfo;

	@Context
    private HttpServletRequest httpRequest;

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
		String path = requestContext.getUriInfo().getPath();
		log.debug("REST call to '{}' intercepted", path);
		
		if (LockProtectionMode.CEDARLING.equals(appConfiguration.getProtectionMode())) {
            Response authorizationResponse = protectionService.processAuthorization(extractBearerToken(), resourceInfo);
			boolean success = authorizationResponse == null;

	        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(httpRequest), AuditActionType.CEDARLING_AUTHZ_FILTER);
			applicationCedarlingAuditLogger.log(auditLogEntry, success);

	        if (success) {
				// Actual processing of request proceeds
				log.debug("Authorization passed");
			} else {
				requestContext.abortWith(authorizationResponse);
			}
		}
	}

    private String extractBearerToken() {
        String authHeader = httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION);
        
        if (StringUtils.isEmpty(authHeader)) {
            return null;
        }

        return authHeader.replaceFirst("(?i)Bearer\\s+", "");
    }

}
