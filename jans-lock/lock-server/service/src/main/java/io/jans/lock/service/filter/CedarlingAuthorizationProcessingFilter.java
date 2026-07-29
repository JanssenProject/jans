package io.jans.lock.service.filter;

import java.io.IOException;

import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.net.InetAddressUtility;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

/**
 * A RestEasy filter to centralize protection of APIs based on path pattern
 */
// To protect JAX-RS resources with this filter add the @ProtectedApi annotation
@ProtectedCedarlingApi
@Priority(Priorities.AUTHENTICATION + 1)
@Dependent
public class CedarlingAuthorizationProcessingFilter extends io.jans.core.cedarling.service.filter.CedarlingAuthorizationProcessingFilter {

	@Inject
	private AppConfiguration appConfiguration;

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
	        cedarlingApplicationAuditLogger.log(auditLogEntry, success);

	        if (success) {
				// Actual processing of request proceeds
				log.debug("Authorization passed");
			} else {
				requestContext.abortWith(authorizationResponse);
			}
		}
	}

}
