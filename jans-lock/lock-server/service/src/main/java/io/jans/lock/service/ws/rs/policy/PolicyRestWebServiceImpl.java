package io.jans.lock.service.ws.rs.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import io.jans.lock.cedarling.service.policy.PolicyDownloadService;
import io.jans.lock.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.lock.model.app.audit.AuditActionType;
import io.jans.lock.model.app.audit.AuditLogEntry;
import io.jans.lock.model.error.CommonErrorResponseType;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.ws.rs.base.BaseResource;
import io.jans.net.InetAddressUtility;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

/**
 * Provides server with basic statistic
 *
 * @author Yuriy Movchan Date: 12/02/2024
 */
@Dependent
public class PolicyRestWebServiceImpl extends BaseResource implements PolicyRestWebService {

    @Inject
    private Logger log;
    
    @Inject
    private PolicyDownloadService policyDownloadService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

	@Override
	public Response getPoliciesUriList() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.POLICIES_URI_LIST_READ);
        applicationAuditLogger.log(auditLogEntry);

        if (log.isDebugEnabled()) {
        	log.debug("Request policies URI list");
        }

        Map<String, LoadedPolicySource> policies = policyDownloadService.getLoadedPolicies();

        List<String> uris = new ArrayList<String>(policies.keySet());

        if (uris.size() == 0) {
        	throw errorResponseFactory.notFoundException(CommonErrorResponseType.NOT_FOUND_ERROR, "There is no policies loaded.");
        }

        if (log.isTraceEnabled()) {
			log.trace("Policies URIs: {}", uris);
		}

        if (log.isDebugEnabled()) {
        	log.debug("Sending policies list");
        }

        return Response.ok().entity(uris).build();
	}

	@Override
	public Response getPolicyByUri(@NotNull String uri) {
        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.POLICY_BY_URI_READ);
        applicationAuditLogger.log(auditLogEntry);

        if (log.isDebugEnabled()) {
        	log.debug("Request policy by URI: {}", uri);
        }

        if (StringHelper.isEmpty(uri)) {
        	throw errorResponseFactory.badRequestException(CommonErrorResponseType.INVALID_REQUEST, "URI is not specified");
        }

        Map<String, LoadedPolicySource> policies = policyDownloadService.getLoadedPolicies();

        if (!policies.containsKey(uri)) {
        	throw errorResponseFactory.notFoundException(CommonErrorResponseType.NOT_FOUND_ERROR, String.format("There is no policy: %s.", uri));
        }

        if (log.isDebugEnabled()) {
        	log.debug("Sending policy response for URI: {}", uri);
        }

        return Response.ok().entity(policies.get(uri).getPolicyJson()).build();
	}

}
