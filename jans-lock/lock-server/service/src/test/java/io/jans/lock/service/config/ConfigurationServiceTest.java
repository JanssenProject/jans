/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.service.net.NetworkService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigurationServiceTest {

	private static final String BASE = "https://lock.example.org/jans-lock/api/v1";

	@Mock
	private AppConfiguration appConfiguration;

	@Mock
	private NetworkService networkService;

	@InjectMocks
	private ConfigurationService configurationService;

	private TraceConfiguration traceConfiguration;

	@BeforeEach
	void setUp() {
		traceConfiguration = new TraceConfiguration();
		when(appConfiguration.getBaseEndpoint()).thenReturn(BASE);
		when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);
		when(networkService.getHost(BASE)).thenReturn("lock.example.org");
		configurationService.init();
	}

	@Test
	void getLockConfiguration_publishesAuditEndpointsIncludingBulk() {
		JsonNode audit = configurationService.getLockConfiguration().path("audit");

		for (String type : new String[] { "health", "log", "telemetry" }) {
			assertEquals(BASE + "/audit/" + type, audit.path(type + "_endpoint").asText());
			assertEquals(BASE + "/audit/" + type + "/bulk", audit.path(type + "_bulk_endpoint").asText());
		}
	}

	@Test
	void getLockConfiguration_publishesConfigEndpoints() {
		JsonNode config = configurationService.getLockConfiguration().path("config");

		assertTrue(config.isObject());
		assertEquals(BASE + "/policy", config.path("policy_endpoint").asText());
		assertFalse(config.has("sse_endpoint"));
	}

	@Test
	void getLockConfiguration_traceEnabled_publishesTraceEndpoints() {
		traceConfiguration.setEnabled(true);

		JsonNode audit = configurationService.getLockConfiguration().path("audit");

		assertEquals(BASE + "/audit/trace", audit.path("trace_endpoint").asText());
		assertEquals(BASE + "/audit/trace/bulk", audit.path("trace_bulk_endpoint").asText());
		assertEquals(BASE + "/audit/trace/records", audit.path("trace_record_endpoint").asText());
		assertEquals(BASE + "/audit/trace/executions", audit.path("trace_execution_endpoint").asText());
	}

	@Test
	void getLockConfiguration_traceDisabled_omitsTraceEndpoints() {
		traceConfiguration.setEnabled(false);

		JsonNode audit = configurationService.getLockConfiguration().path("audit");

		assertFalse(audit.has("trace_endpoint"));
		assertFalse(audit.has("trace_bulk_endpoint"));
		assertFalse(audit.has("trace_record_endpoint"));
		assertFalse(audit.has("trace_execution_endpoint"));
		assertTrue(audit.has("health_endpoint"));
	}

}
