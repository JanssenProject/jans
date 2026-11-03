/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.lock.service.openid.OpenIdProtection;
import io.jans.lock.service.security.AuthorizationOutcome;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.model.audit.grpc.AuditServiceGrpc;
import io.jans.lock.model.trace.grpc.TraceAdminServiceGrpc;
import io.jans.lock.model.trace.grpc.TraceServiceGrpc;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.ws.rs.audit.AuditRestWebService;
import io.jans.lock.service.ws.rs.trace.TraceAdminRestWebService;
import io.jans.lock.service.ws.rs.trace.TraceRestWebService;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import jakarta.ws.rs.container.ResourceInfo;

/**
 * Checks that every gRPC method resolves to the REST method carrying its {@code @ProtectedApi} and
 * {@code @ProtectedCedarlingApi} metadata: an unresolved method is denied by the protection layer.
 */
class GrpcAuthorizationInterceptorTest extends BaseLockServiceTest {

	private GrpcAuthorizationInterceptor interceptor;

	private OpenIdProtection openIdProtection;

	@BeforeEach
	void setUp() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getProtectionMode()).thenReturn(LockProtectionMode.OAUTH);
		openIdProtection = mock(OpenIdProtection.class);

		interceptor = new GrpcAuthorizationInterceptor();
		setField(interceptor, "log", LoggerFactory.getLogger(GrpcAuthorizationInterceptor.class));
		setField(interceptor, "appConfiguration", appConfiguration);
		setField(interceptor, "openIdProtectionService", openIdProtection);
		setField(interceptor, "applicationAuditLogger", mock(ApplicationAuditLogger.class));
	}

	@SuppressWarnings("unchecked")
	private ServerCall<Object, Object> callTo(String fullMethodName) {
		MethodDescriptor<Object, Object> descriptor = mock(MethodDescriptor.class);
		when(descriptor.getFullMethodName()).thenReturn(fullMethodName);
		ServerCall<Object, Object> call = mock(ServerCall.class);
		when(call.getMethodDescriptor()).thenReturn(descriptor);
		when(call.getAttributes()).thenReturn(io.grpc.Attributes.EMPTY);
		return call;
	}

	static Stream<Arguments> protectedMethods() {
		return Stream.of(
				Arguments.of(AuditServiceGrpc.SERVICE_NAME, "ProcessHealth", AuditRestWebService.class, "processHealthRequest", ApiAccessConstants.LOCK_HEALTH_WRITE_ACCESS),
				Arguments.of(AuditServiceGrpc.SERVICE_NAME, "ProcessBulkHealth", AuditRestWebService.class, "processBulkHealthRequest", ApiAccessConstants.LOCK_HEALTH_WRITE_ACCESS),
				Arguments.of(AuditServiceGrpc.SERVICE_NAME, "ProcessLog", AuditRestWebService.class, "processLogRequest", ApiAccessConstants.LOCK_LOG_WRITE_ACCESS),
				Arguments.of(AuditServiceGrpc.SERVICE_NAME, "ProcessBulkLog", AuditRestWebService.class, "processBulkLogRequest", ApiAccessConstants.LOCK_LOG_WRITE_ACCESS),
				Arguments.of(AuditServiceGrpc.SERVICE_NAME, "ProcessTelemetry", AuditRestWebService.class, "processTelemetryRequest", ApiAccessConstants.LOCK_TELEMETRY_WRITE_ACCESS),
				Arguments.of(AuditServiceGrpc.SERVICE_NAME, "ProcessBulkTelemetry", AuditRestWebService.class, "processBulkTelemetryRequest", ApiAccessConstants.LOCK_TELEMETRY_WRITE_ACCESS),
				Arguments.of(TraceServiceGrpc.SERVICE_NAME, "SubmitRecord", TraceRestWebService.class, "submitRecord", ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS),
				Arguments.of(TraceServiceGrpc.SERVICE_NAME, "SubmitBulkRecords", TraceRestWebService.class, "submitBulkRecords", ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS),
				Arguments.of(TraceServiceGrpc.SERVICE_NAME, "GetRecord", TraceRestWebService.class, "getRecord", ApiAccessConstants.LOCK_TRACE_READ_ACCESS),
				Arguments.of(TraceServiceGrpc.SERVICE_NAME, "SearchRecords", TraceRestWebService.class, "searchRecords", ApiAccessConstants.LOCK_TRACE_READ_ACCESS),
				Arguments.of(TraceServiceGrpc.SERVICE_NAME, "GetExecution", TraceRestWebService.class, "getExecution", ApiAccessConstants.LOCK_TRACE_READ_ACCESS),
				Arguments.of(TraceAdminServiceGrpc.SERVICE_NAME, "RegisterProducerKey", TraceAdminRestWebService.class, "registerProducerKey", ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS),
				Arguments.of(TraceAdminServiceGrpc.SERVICE_NAME, "ListProducerKeys", TraceAdminRestWebService.class, "listProducerKeys", ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS),
				Arguments.of(TraceAdminServiceGrpc.SERVICE_NAME, "RevokeProducerKey", TraceAdminRestWebService.class, "revokeProducerKey", ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS),
				Arguments.of(TraceAdminServiceGrpc.SERVICE_NAME, "RegisterProducerChain", TraceAdminRestWebService.class, "registerProducerChain", ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS),
				Arguments.of(TraceAdminServiceGrpc.SERVICE_NAME, "ListProducerChains", TraceAdminRestWebService.class, "listProducerChains", ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS));
	}

	@ParameterizedTest
	@MethodSource("protectedMethods")
	void testExtractResourceInfo_ResolvesRestMethodWithProtection(String service, String grpcMethod,
			Class<?> restInterface, String restMethodName, String scope) {
		ResourceInfo info = interceptor.extractResourceInfo(service + "/" + grpcMethod);

		assertNotNull(info, "no ResourceInfo for " + grpcMethod);
		assertEquals(restInterface, info.getResourceClass());
		Method method = info.getResourceMethod();
		assertEquals(restMethodName, method.getName());

		ProtectedApi protectedApi = method.getAnnotation(ProtectedApi.class);
		assertNotNull(protectedApi);
		assertEquals(1, protectedApi.scopes().length);
		assertEquals(scope, protectedApi.scopes()[0]);
		assertNotNull(method.getAnnotation(ProtectedCedarlingApi.class));
	}

	@Test
	void testExtractResourceInfo_EveryGrpcMethodOfEachServiceIsCovered() {
		assertEquals(6, AuditServiceGrpc.getServiceDescriptor().getMethods().size());
		assertEquals(5, TraceServiceGrpc.getServiceDescriptor().getMethods().size());
		assertEquals(5, TraceAdminServiceGrpc.getServiceDescriptor().getMethods().size());

		Stream.of(AuditServiceGrpc.getServiceDescriptor(), TraceServiceGrpc.getServiceDescriptor(),
				TraceAdminServiceGrpc.getServiceDescriptor()).forEach(descriptor -> descriptor.getMethods()
						.forEach(m -> assertNotNull(interceptor.extractResourceInfo(m.getFullMethodName()),
								"unprotected gRPC method " + m.getFullMethodName())));
	}

	@Test
	void testExtractResourceInfo_MethodOfAnotherServiceIsNotResolved() {
		assertNull(interceptor.extractResourceInfo(TraceServiceGrpc.SERVICE_NAME + "/RegisterProducerKey"));
		assertNull(interceptor.extractResourceInfo(TraceAdminServiceGrpc.SERVICE_NAME + "/SubmitRecord"));
		assertNull(interceptor.extractResourceInfo(AuditServiceGrpc.SERVICE_NAME + "/SubmitRecord"));
	}

	@Test
	void testExtractResourceInfo_UnknownServiceMethodOrFormatIsNotResolved() {
		assertNull(interceptor.extractResourceInfo("io.example.Other/SubmitRecord"));
		assertNull(interceptor.extractResourceInfo(TraceServiceGrpc.SERVICE_NAME + "/Nope"));
		assertNull(interceptor.extractResourceInfo("no-slash"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testInterceptCall_UnmappedMethodIsDeniedWithoutAuthorizing() {
		ServerCall<Object, Object> call = callTo(TraceServiceGrpc.SERVICE_NAME + "/Unmapped");
		ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);

		interceptor.interceptCall(call, new Metadata(), next);

		org.mockito.ArgumentCaptor<Status> status = org.mockito.ArgumentCaptor.forClass(Status.class);
		verify(call).close(status.capture(), any(Metadata.class));
		assertEquals(Status.Code.PERMISSION_DENIED, status.getValue().getCode());
		verifyNoInteractions(openIdProtection, next);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testInterceptCall_MappedMethodIsAuthorizedWithItsResourceInfo() {
		ServerCall<Object, Object> call = callTo(TraceServiceGrpc.SERVICE_NAME + "/GetRecord");
		ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
		AuthorizationOutcome denied = AuthorizationOutcome.denied(jakarta.ws.rs.core.Response.status(401).build());
		when(openIdProtection.authorize(any(), any())).thenReturn(denied);

		interceptor.interceptCall(call, new Metadata(), next);

		org.mockito.ArgumentCaptor<ResourceInfo> info = org.mockito.ArgumentCaptor.forClass(ResourceInfo.class);
		verify(openIdProtection).authorize(any(), info.capture());
		assertEquals("getRecord", info.getValue().getResourceMethod().getName());
		org.mockito.ArgumentCaptor<Status> status = org.mockito.ArgumentCaptor.forClass(Status.class);
		verify(call).close(status.capture(), any(Metadata.class));
		assertEquals(Status.Code.UNAUTHENTICATED, status.getValue().getCode());
		verify(next, never()).startCall(any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testInterceptCall_AuthorizationFailureDoesNotLeakExceptionMessage() {
		ServerCall<Object, Object> call = callTo(TraceServiceGrpc.SERVICE_NAME + "/GetRecord");
		ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
		when(openIdProtection.authorize(any(), any()))
				.thenThrow(new IllegalStateException("introspection failed at https://internal.example.org/introspect"));

		interceptor.interceptCall(call, new Metadata(), next);

		org.mockito.ArgumentCaptor<Status> status = org.mockito.ArgumentCaptor.forClass(Status.class);
		verify(call).close(status.capture(), any(Metadata.class));
		assertEquals(Status.Code.INTERNAL, status.getValue().getCode());
		assertEquals("Authorization error", status.getValue().getDescription());
		verify(next, never()).startCall(any(), any());
	}
}
