/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceClientDomainBinding;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.parse.TraceValidationException;

/**
 * Tests for {@link EvidenceDomainResolver}: binding precedence, the default-domain fallback, and
 * the fail-closed domain-id format check (design decision D-1, task 11 acceptance criteria).
 */
class EvidenceDomainResolverTest {

	private static final String NODE_ID = "test-node-1";

	@Mock
	private AppConfiguration appConfiguration;

	private TraceConfiguration traceConfiguration;

	private EvidenceDomainResolver resolver;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		traceConfiguration = new TraceConfiguration();
		when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);

		resolver = new EvidenceDomainResolver();
		resolver.setLog(LoggerFactory.getLogger(EvidenceDomainResolver.class));
		resolver.setAppConfiguration(appConfiguration);
		resolver.setNodeId(NODE_ID);
	}

	private static TraceClientDomainBinding binding(String clientId, String domainId, String... producers) {
		TraceClientDomainBinding binding = new TraceClientDomainBinding();
		binding.setClientId(clientId);
		binding.setEvidenceDomainId(domainId);
		binding.setAllowedProducerIds(Arrays.asList(producers));
		return binding;
	}

	@Test
	void testResolve_ExplicitBinding_WinsOverDefault() {
		traceConfiguration.setDefaultEvidenceDomainId("default-domain");
		traceConfiguration.setClientDomainBindings(
				Collections.singletonList(binding("client-1", "domain-a", "prod-x/1.0.0")));

		TraceRequestContext ctx = resolver.resolve(new SubmitterIdentity("client-1", true));

		assertEquals("client-1", ctx.getClientId());
		assertEquals("domain-a", ctx.getEvidenceDomainId());
		assertEquals(Collections.singletonList("prod-x/1.0.0"), ctx.getAllowedProducerIds());
		assertEquals(NODE_ID, ctx.getNodeId());
	}

	@Test
	void testResolve_NoBindingWithDefault_UsesDefaultAndWildcard() {
		traceConfiguration.setDefaultEvidenceDomainId("default-domain");
		traceConfiguration.setClientDomainBindings(Collections.emptyList());

		TraceRequestContext ctx = resolver.resolve(new SubmitterIdentity("client-2", false));

		assertEquals("default-domain", ctx.getEvidenceDomainId());
		assertEquals(Collections.singletonList("*"), ctx.getAllowedProducerIds());
	}

	@Test
	void testResolve_NoBindingNoDefault_Rejected() {
		traceConfiguration.setClientDomainBindings(Collections.emptyList());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> resolver.resolve(new SubmitterIdentity("client-3", false)));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(EvidenceDomainResolver.REASON_NO_DOMAIN_BINDING, ex.getReason());
	}

	@Test
	void testResolve_ExplicitBindingWithBlankDomainId_FailsClosed() {
		traceConfiguration.setDefaultEvidenceDomainId("default-domain");
		traceConfiguration.setClientDomainBindings(Collections.singletonList(binding("client-4", "", "*")));

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> resolver.resolve(new SubmitterIdentity("client-4", true)));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(EvidenceDomainResolver.REASON_INVALID_DOMAIN_ID, ex.getReason());
	}

	@Test
	void testResolve_InvalidDomainIdFormat_FailsClosed() {
		traceConfiguration.setClientDomainBindings(
				Collections.singletonList(binding("client-5", "bad domain id!", "*")));

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> resolver.resolve(new SubmitterIdentity("client-5", true)));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(EvidenceDomainResolver.REASON_INVALID_DOMAIN_ID, ex.getReason());
	}

}
