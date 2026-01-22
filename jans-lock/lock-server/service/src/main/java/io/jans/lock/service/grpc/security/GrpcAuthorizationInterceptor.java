/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
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
import io.jans.lock.cedarling.service.CedarlingProtection;
import io.jans.lock.model.app.audit.AuditActionType;
import io.jans.lock.model.app.audit.AuditLogEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.openid.OpenIdProtection;
import io.jans.lock.service.ws.rs.audit.AuditRestWebService;
import io.jans.lock.util.HeaderUtils;
import io.jans.lock.util.ServerUtil;
import io.jans.service.security.api.ProtectedApi;
import io.jans.service.security.protect.BaseAuthorizationProtection;
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

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private OpenIdProtection openIdProtectionService;

    @Inject
    private CedarlingProtection cedarlingProtectionService;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = 
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        log.debug("gRPC call to '{}' intercepted", methodName);
        
        BaseAuthorizationProtection authorizationProtection = null;
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
            log.debug("gRPC call requires access to: {}", resourceInfo);

            String clientIp = ServerUtil.getGrpcClientIpAddress(call, headers);
            Context context = ServerUtil.setClientContextIpAddress(clientIp);

            // Process authorization
            Response authorizationResponse = authorizationProtection.processAuthorization(HeaderUtils.findAndExtractBearerToken(headers), resourceInfo);
            boolean success = authorizationResponse == null;

            // Audit logging
            AuditLogEntry auditLogEntry = new AuditLogEntry(clientIp, AuditActionType.GRPC_AUTHZ_FILTER);
            applicationAuditLogger.log(auditLogEntry, success);

            if (!success) {
                log.warn("Authorization failed for gRPC call '{}': {}", methodName, authorizationResponse.getEntity());
                
                // Map HTTP status to gRPC status
                Status grpcStatus = mapHttpStatusToGrpcStatus(authorizationResponse.getStatusInfo().toEnum());
                call.close(grpcStatus.withDescription(String.valueOf(authorizationResponse.getEntity())), new Metadata());
                
                return new ServerCall.Listener<ReqT>() {};
            }

            log.debug("Authorization passed for gRPC call '{}'", methodName);

            return Contexts.interceptCall(context, call, headers, next);
        } catch (Exception e) {
            log.error("Error during gRPC authorization for '{}'", methodName, e);
            
            call.close(Status.INTERNAL
                    .withDescription("Authorization error: " + e.getMessage())
                    .withCause(e), new Metadata());
            
            return new ServerCall.Listener<ReqT>() {};
        }
    }

    /**
     * Map HTTP status codes to gRPC status codes.
     *
     * @param httpStatus HTTP response status
     * @return corresponding gRPC Status
     */
    private Status mapHttpStatusToGrpcStatus(Response.Status httpStatus) {
        if (httpStatus == null) {
            return Status.INTERNAL;
        }

        switch (httpStatus) {
            case UNAUTHORIZED:
                return Status.UNAUTHENTICATED;
            case FORBIDDEN:
                return Status.PERMISSION_DENIED;
            case BAD_REQUEST:
                return Status.INVALID_ARGUMENT;
            case NOT_FOUND:
                return Status.NOT_FOUND;
            case INTERNAL_SERVER_ERROR:
                return Status.INTERNAL;
            case SERVICE_UNAVAILABLE:
                return Status.UNAVAILABLE;
            default:
                return Status.UNKNOWN;
        }
    }

    /**
     * Extract target resource for the gRPC method.
     *
     * @param methodName full gRPC method name (e.g., "io.jans.lock.audit.AuditService/ProcessHealth")
     * @return requested resource info
     */
    private ResourceInfo extractResourceInfo(String methodName) {
        // Parse method name: "package.Service/Method"
        String[] parts = methodName.split("/");
        if (parts.length != 2) {
            log.warn("Invalid gRPC method name format: {}", methodName);
            return null;
        }

        String method = parts[1];
        
        // Map gRPC methods to ResourceInfo (same as REST API)
        Optional<ResourceInfo> resourceInfo = getProtectionApiMethod(AuditRestWebService.class, method);
        if (resourceInfo.isPresent()) {
        	return resourceInfo.get();
        }

        log.warn("No ResourceInfo found for gRPC method: {}", methodName);
        return null;
    }

    private Optional<ResourceInfo> getProtectionApiMethod(Class<?> clazz, String grpcMethodName) {
        for (Method method : clazz.getMethods()) {
        	Optional<ProtectedApi> protectedApi = getProtectedApiAnnotation(method);
        	if (protectedApi.isPresent()) {
       			if (grpcMethodName.equals(protectedApi.get().grpcMethodName())) {
       				GrpcResourceInfo grpcResourceInfo = new GrpcResourceInfo(clazz, method);
    				return Optional.ofNullable(grpcResourceInfo);
    			}
        	}
        }

        return Optional.empty();
    }

	private Optional<ProtectedApi> getProtectedApiAnnotation(AnnotatedElement elem) {
		Optional<ProtectedApi> protectedApi = optAnnnotation(elem, ProtectedApi.class);
		return protectedApi;
	}

    private static <T extends Annotation> Optional<T> optAnnnotation(AnnotatedElement elem, Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }

    class GrpcResourceInfo implements ResourceInfo {
    	
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