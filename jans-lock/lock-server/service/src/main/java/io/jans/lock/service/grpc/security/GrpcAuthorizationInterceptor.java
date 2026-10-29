/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.lock.model.audit.grpc.AuditServiceGrpc;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.lock.model.trace.grpc.TraceAdminServiceGrpc;
import io.jans.lock.model.trace.grpc.TraceServiceGrpc;
import io.jans.lock.service.CedarlingProtectionService;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.grpc.util.GrpcStatusMapper;
import io.jans.lock.service.openid.OpenIdProtection;
import io.jans.lock.service.security.AuthenticatedClientContext;
import io.jans.lock.service.security.AuthenticatingProtection;
import io.jans.lock.service.security.AuthorizationOutcome;
import io.jans.lock.service.ws.rs.audit.AuditRestWebService;
import io.jans.lock.service.ws.rs.trace.TraceAdminRestWebService;
import io.jans.lock.service.ws.rs.trace.TraceRestWebService;
import io.jans.lock.util.HeaderUtils;
import io.jans.lock.util.ServerUtil;
import io.jans.service.security.api.ProtectedApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * gRPC Server Interceptor for authorization.
 * This is the gRPC equivalent of AuthorizationProcessingFilter for REST.
 * 
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcAuthorizationInterceptor implements ServerInterceptor {

    /** REST interface that carries the {@code @ProtectedApi} metadata of each gRPC service. */
    private static final Map<String, Class<?>> PROTECTED_API_BY_SERVICE = Map.of(
            AuditServiceGrpc.SERVICE_NAME, AuditRestWebService.class,
            TraceServiceGrpc.SERVICE_NAME, TraceRestWebService.class,
            TraceAdminServiceGrpc.SERVICE_NAME, TraceAdminRestWebService.class);

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private OpenIdProtection openIdProtectionService;

    /** Lock's concrete service, injected by its own type so {@code authorize(...)} is reachable. */
    @Inject
    private CedarlingProtectionService cedarlingProtectionService;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        log.debug("gRPC call to '{}' intercepted", methodName);
        
        AuthenticatingProtection authorizationProtection = null;
        if (LockProtectionMode.OAUTH.equals(appConfiguration.getProtectionMode())) {
            log.debug("OAuth protection is enabled");
            
            authorizationProtection = openIdProtectionService;
        } else if (LockProtectionMode.CEDARLING.equals(appConfiguration.getProtectionMode())) {
            log.debug("Cedarling protection is enabled");
            
            authorizationProtection = cedarlingProtectionService;
        } else {
            // Send error if protection mode is not selected
            call.close(Status.PERMISSION_DENIED
                    .withDescription("Authorization error"), new Metadata());
            
            return new ServerCall.Listener<ReqT>() {};
        }

        try {
            // Extract method-specific ResourceInfo
        	ResourceInfo resourceInfo = extractResourceInfo(methodName);
            if (resourceInfo == null) {
                log.warn("gRPC call to '{}' is rejected: no protected API is mapped to it", methodName);
                call.close(Status.PERMISSION_DENIED
                        .withDescription("Access to method is not protected"), new Metadata());

                return new ServerCall.Listener<ReqT>() {};
            }
            log.debug("gRPC call requires access to: {}", resourceInfo);

            String clientIp = ServerUtil.getGrpcClientIpAddress(call, headers);
            Context context = ServerUtil.setClientContextIpAddress(clientIp);

            // Process authorization
            AuthorizationOutcome outcome = authorizationProtection.authorize(HeaderUtils.findAndExtractBearerToken(headers), resourceInfo);
            boolean success = outcome.isAllowed();

            // Audit logging
            AuditLogEntry auditLogEntry = new AuditLogEntry(clientIp, AuditActionType.GRPC_AUTHZ_FILTER);
            applicationAuditLogger.log(auditLogEntry, success);

            if (!success) {
                Response authorizationResponse = outcome.getErrorResponse();
                log.warn("Authorization failed for gRPC call '{}': {}", methodName, authorizationResponse.getEntity());
                
                // Map HTTP status to gRPC status
                Status grpcStatus = GrpcStatusMapper.fromHttpStatus(authorizationResponse.getStatusInfo().toEnum());
                call.close(grpcStatus.withDescription(String.valueOf(authorizationResponse.getEntity())), new Metadata());
                
                return new ServerCall.Listener<ReqT>() {};
            }

            log.debug("Authorization passed for gRPC call '{}'", methodName);

            // Carry the authenticated client alongside the client IP for downstream services
            context = AuthenticatedClientContext.withClient(context, outcome.getClient());

            return Contexts.interceptCall(context, call, headers, next);
        } catch (Exception e) {
            log.error("Error during gRPC authorization for '{}'", methodName, e);
            
            call.close(Status.INTERNAL
                    .withDescription("Authorization error")
                    .withCause(e), new Metadata());
            
            return new ServerCall.Listener<ReqT>() {};
        }
    }

    /**
     * Extract target resource for the gRPC method.
     *
     * @param methodName full gRPC method name (e.g., "io.jans.lock.audit.AuditService/ProcessHealth")
     * @return requested resource info
     */
    ResourceInfo extractResourceInfo(String methodName) {
        // Parse method name: "package.Service/Method"
        String[] parts = methodName.split("/");
        if (parts.length != 2) {
            log.warn("Invalid gRPC method name format: {}", methodName);
            return null;
        }

        Class<?> protectedApi = PROTECTED_API_BY_SERVICE.get(parts[0]);
        if (protectedApi == null) {
            log.warn("No protected API found for gRPC service: {}", parts[0]);
            return null;
        }

        // Map gRPC methods to ResourceInfo (same as REST API)
        Optional<ResourceInfo> resourceInfo = getProtectionApiMethod(protectedApi, parts[1]);
        if (resourceInfo.isPresent()) {
        	return resourceInfo.get();
        }

        log.warn("No ResourceInfo found for gRPC method: {}", methodName);
        return null;
    }

    private Optional<ResourceInfo> getProtectionApiMethod(Class<?> clazz, String grpcMethodName) {
        for (Method method : clazz.getMethods()) {
        	Optional<ProtectedApi> protectedApi = getProtectedApiAnnotation(method);
        	if (protectedApi.isPresent() && grpcMethodName.equals(protectedApi.get().grpcMethodName())) {
   				GrpcResourceInfo grpcResourceInfo = new GrpcResourceInfo(clazz, method);
				return Optional.ofNullable(grpcResourceInfo);
        	}
        }

        return Optional.empty();
    }

	private Optional<ProtectedApi> getProtectedApiAnnotation(AnnotatedElement elem) {
		Optional<ProtectedApi> protectedApi = optAnnotation(elem, ProtectedApi.class);
		return protectedApi;
	}

    private static <T extends Annotation> Optional<T> optAnnotation(AnnotatedElement elem, Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }

    static class GrpcResourceInfo implements ResourceInfo {
    	
    	private Class<?> clazz;
    	private Method method;

		public GrpcResourceInfo( Class<?> clazz, Method method) {
			this.clazz = clazz;
			this.method = method;
		}

		@Override
		public Method getResourceMethod() {
			return method;
		}

		@Override
		public Class<?> getResourceClass() {
			return clazz;
		}

		@Override
		public String toString() {
			return "GrpcResourceInfo [clazz=" + clazz + ", method=" + method + "]";
		}
    }
}