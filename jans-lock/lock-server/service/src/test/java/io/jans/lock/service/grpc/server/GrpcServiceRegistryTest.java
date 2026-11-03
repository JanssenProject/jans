/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.jans.lock.model.audit.grpc.AuditServiceGrpc;
import io.jans.lock.model.audit.grpc.HealthRequest;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.GrpcServerMode;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.lock.model.config.grpc.GrpcConfiguration;
import io.jans.lock.model.trace.grpc.GetRecordRequest;
import io.jans.lock.model.trace.grpc.ListProducerKeysRequest;
import io.jans.lock.model.trace.grpc.TraceAdminServiceGrpc;
import io.jans.lock.model.trace.grpc.TraceServiceGrpc;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceImpl;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceProvider;
import io.jans.lock.service.grpc.security.GrpcAuthorizationInterceptor;
import io.jans.lock.service.grpc.trace.GrpcTraceAdminServiceImpl;
import io.jans.lock.service.grpc.trace.GrpcTraceServiceImpl;
import io.jans.lock.service.grpc.trace.GrpcTraceServiceProvider;
import io.jans.lock.service.openid.OpenIdProtection;
import io.jans.lock.service.security.AuthorizationOutcome;
import jakarta.ws.rs.core.Response;

/**
 * Checks that every Lock gRPC service is registered on the gRPC server flavours. A service missing
 * from a flavour answers {@code UNIMPLEMENTED} instead of reaching the authorization interceptor.
 */
class GrpcServiceRegistryTest extends BaseLockServiceTest {

	private static final Logger LOG = LoggerFactory.getLogger(GrpcServiceRegistryTest.class);

	private static GrpcServiceRegistry registry() {
		GrpcAuditServiceProvider auditProvider = mock(GrpcAuditServiceProvider.class);
		when(auditProvider.getService()).thenReturn(new GrpcAuditServiceImpl(null, null, LOG));
		GrpcTraceServiceProvider traceProvider = mock(GrpcTraceServiceProvider.class);
		when(traceProvider.getTraceService()).thenReturn(new GrpcTraceServiceImpl(null, null, LOG));
		when(traceProvider.getTraceAdminService()).thenReturn(new GrpcTraceAdminServiceImpl(null, null, LOG));

		GrpcServiceRegistry registry = new GrpcServiceRegistry();
		setField(registry, "grpcAuditServiceProvider", auditProvider);
		setField(registry, "grpcTraceServiceProvider", traceProvider);
		return registry;
	}

	@Test
	void testGetServices_ContainsAuditAndBothTraceServices() {
		Set<String> names = registry().getServices().stream().map(BindableService::bindService)
				.map(definition -> definition.getServiceDescriptor().getName()).collect(Collectors.toCollection(TreeSet::new));

		assertEquals(new TreeSet<>(List.of(AuditServiceGrpc.SERVICE_NAME, TraceServiceGrpc.SERVICE_NAME,
				TraceAdminServiceGrpc.SERVICE_NAME)), names);
	}

	@Test
	void testPlainServer_RegistersEveryServiceBehindTheInterceptor() throws IOException {
		OpenIdProtection openIdProtection = mock(OpenIdProtection.class);
		when(openIdProtection.authorize(any(), any()))
				.thenReturn(AuthorizationOutcome.denied(Response.status(Response.Status.UNAUTHORIZED).entity("denied").build()));
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getProtectionMode()).thenReturn(LockProtectionMode.OAUTH);

		GrpcAuthorizationInterceptor interceptor = new GrpcAuthorizationInterceptor();
		setField(interceptor, "log", LOG);
		setField(interceptor, "appConfiguration", appConfiguration);
		setField(interceptor, "openIdProtectionService", openIdProtection);
		setField(interceptor, "applicationAuditLogger", mock(ApplicationAuditLogger.class));

		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		GrpcConfiguration grpcConfiguration = new GrpcConfiguration();
		grpcConfiguration.setServerMode(GrpcServerMode.PLAIN_SERVER);
		grpcConfiguration.setGrpcPort(port);
		when(appConfiguration.getGrpcConfiguration()).thenReturn(grpcConfiguration);

		GrpcServerStarter starter = new GrpcServerStarter();
		setField(starter, "log", LOG);
		setField(starter, "appConfiguration", appConfiguration);
		setField(starter, "grpcServiceRegistry", registry());
		setField(starter, "authorizationInterceptor", interceptor);

		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		try {
			starter.initGrpcServer();

			assertUnauthenticated(() -> AuditServiceGrpc.newBlockingStub(channel).processHealth(HealthRequest.getDefaultInstance()));
			assertUnauthenticated(() -> TraceServiceGrpc.newBlockingStub(channel).getRecord(GetRecordRequest.getDefaultInstance()));
			assertUnauthenticated(() -> TraceAdminServiceGrpc.newBlockingStub(channel).listProducerKeys(ListProducerKeysRequest.getDefaultInstance()));
		} finally {
			channel.shutdownNow();
			starter.stopGrpcServer();
		}
	}

	private static void assertUnauthenticated(Runnable call) {
		StatusRuntimeException error = assertThrows(StatusRuntimeException.class, call::run);
		assertEquals(Status.Code.UNAUTHENTICATED, error.getStatus().getCode());
	}
}
