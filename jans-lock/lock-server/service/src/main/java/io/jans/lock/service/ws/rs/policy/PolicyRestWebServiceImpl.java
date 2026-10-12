package io.jans.lock.service.ws.rs.policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.core.cedarling.service.policy.PolicyDownloadService;
import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.lock.model.error.CommonErrorResponseType;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.ws.rs.base.BaseResource;
import io.jans.net.InetAddressUtility;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.MediaType;
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

        Response response = null;
        try {
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
	
	        response = Response.ok().entity(uris).build();

	        return response;
        } finally {
            applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
        }
	}

	@Override
	public Response getPolicyByUri(@NotNull String uri) {
        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.POLICY_BY_URI_READ);

        Response response = null;
        try {
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

	        LoadedPolicySource policy = policies.get(uri);
	        if (policy.isArchive()) {
	        	response = Response.ok(policy.getArchive(), PolicyDownloadService.APPLICATION_ZIP_TYPE).tag(policy.getBundleHash()).build();
	        } else {
	        	response = Response.ok().entity(policy.getPolicyJson()).build();
	        }

	        return response;
        } finally {
            applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
        }
	}

	@Override
	public Response getPolicyStores() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.POLICIES_URI_LIST_READ);

        Response response = null;
        try {
	        List<Map<String, Object>> stores = new ArrayList<>();
	        for (LoadedPolicySource policy : policyDownloadService.getLoadedPolicies().values()) {
	        	if (!policy.isArchive()) {
	        		continue;
	        	}

	        	Map<String, Object> store = new LinkedHashMap<>();
	        	store.put("policy_store_id", policy.getPolicyStoreUri());
	        	store.put("policy_store_name", policy.getPolicyStoreName());
	        	store.put("policy_store_version", policy.getPolicyStoreVersion());
	        	store.put("source_uri", policy.getSourceUri());
	        	store.put("bundle_hash", policy.getBundleHash());
	        	store.put("sigstore_bundle", policy.getSigstoreBundle() != null);
	        	stores.add(store);
	        }

	        response = Response.ok().entity(stores).build();

	        return response;
        } finally {
            applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
        }
	}

	@Override
	public Response getPolicySigstoreBundle(@NotNull String uri) {
        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.POLICY_BY_URI_READ);

        Response response = null;
        try {
	        LoadedPolicySource policy = policyDownloadService.getLoadedPolicies().get(uri);
	        if (policy == null || policy.getSigstoreBundle() == null) {
	        	throw errorResponseFactory.notFoundException(CommonErrorResponseType.NOT_FOUND_ERROR, String.format("There is no Sigstore bundle for policy: %s.", uri));
	        }

	        response = Response.ok(policy.getSigstoreBundle(), MediaType.APPLICATION_JSON_TYPE).build();

	        return response;
        } finally {
            applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
        }
	}

}
