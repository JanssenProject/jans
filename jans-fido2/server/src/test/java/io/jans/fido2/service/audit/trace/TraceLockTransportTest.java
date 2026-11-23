/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.audit.LockAuditTokenService;

/**
 * Retry and classification for {@link TraceLockTransport}. The HTTP call and the wait are replaced
 * with fakes, so each test pins the exact sequence of attempts, sleeps and outcomes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TraceLockTransportTest {

	private static final String ENDPOINT = "https://lock.example.com/api/v1/audit/trace";
	private static final String RECORD = "{\"event\":{}}";

	@InjectMocks
	private TraceLockTransport transport;

	@Mock
	private Logger log;
	@Mock
	private AppConfiguration appConfiguration;
	@Mock
	private LockAuditTokenService lockAuditTokenService;

	private Fido2Configuration fido2Configuration;
	private final List<Integer> responses = new ArrayList<>();
	private final List<Object> scriptedFailures = new ArrayList<>();
	private final List<String> tokensSeen = new ArrayList<>();
	private final List<Long> sleeps = new ArrayList<>();
	private int postCalls;

	@BeforeEach
	void setUp() {
		fido2Configuration = new Fido2Configuration();
		fido2Configuration.setFido2TraceEndpoint(ENDPOINT);
		when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
		when(lockAuditTokenService.getTraceAccessToken()).thenReturn("trace-token");

		transport.poster = (endpoint, token, body) -> {
			postCalls++;
			tokensSeen.add(token);
			if (!scriptedFailures.isEmpty()) {
				Object next = scriptedFailures.remove(0);
				if (next instanceof Exception) {
					throw (Exception) next;
				}
			}
			return responses.remove(0);
		};
		transport.sleeper = millis -> sleeps.add(millis);
	}

	@Test
	void deliver_ifLockAccepts_returnsAcceptedAfterOneAttempt() {
		responses.add(202);

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
		assertEquals(List.of(), sleeps);
		assertEquals("trace-token", tokensSeen.get(0));
	}

	@Test
	void deliver_ifServerErrorThenAccepted_retriesAfterBackoff() {
		responses.add(503);
		responses.add(202);

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(2, postCalls);
		assertEquals(List.of(200L), sleeps);
	}

	@Test
	void deliver_ifClientRejects_doesNotRetry() {
		responses.add(400);

		assertEquals(TraceLockTransport.Outcome.REJECTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
		assertEquals(List.of(), sleeps);
	}

	@Test
	void deliver_ifConflict_isRejectedNotRetried() {
		responses.add(409);

		assertEquals(TraceLockTransport.Outcome.REJECTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
	}

	@Test
	void deliver_ifAlwaysRateLimited_givesUpAfterMaxAttemptsWithCappedBackoff() {
		for (int i = 0; i < TraceLockTransport.MAX_ATTEMPTS; i++) {
			responses.add(429);
		}

		assertEquals(TraceLockTransport.Outcome.EXHAUSTED, transport.deliver(RECORD));
		assertEquals(TraceLockTransport.MAX_ATTEMPTS, postCalls);
		assertEquals(List.of(200L, 400L, 800L, 1600L), sleeps);
	}

	@Test
	void deliver_ifNetworkErrorThenAccepted_retries() {
		scriptedFailures.add(new java.io.IOException("connection reset"));
		responses.add(202);

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(2, postCalls);
	}

	@Test
	void deliver_ifTokenUnavailableThenAvailable_fetchesTokenAgainWithoutPosting() {
		when(lockAuditTokenService.getTraceAccessToken()).thenReturn(null, "trace-token");
		responses.add(202);

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
	}

	@Test
	void deliver_ifEndpointMissing_sendsNothing() {
		fido2Configuration.setFido2TraceEndpoint(null);

		assertEquals(TraceLockTransport.Outcome.NOT_CONFIGURED, transport.deliver(RECORD));
		assertEquals(0, postCalls);
	}

	@Test
	void deliver_ifEndpointIsPlainHttp_sendsNothing() {
		fido2Configuration.setFido2TraceEndpoint("http://lock.example.com/api/v1/audit/trace");

		assertEquals(TraceLockTransport.Outcome.NOT_CONFIGURED, transport.deliver(RECORD));
		assertEquals(0, postCalls);
	}

	@Test
	void backoff_isExponentialAndCapped() {
		assertEquals(200L, TraceLockTransport.backoffMillis(1));
		assertEquals(400L, TraceLockTransport.backoffMillis(2));
		assertEquals(5_000L, TraceLockTransport.backoffMillis(10));
		assertEquals(5_000L, TraceLockTransport.backoffMillis(1_000));
	}

	@Test
	void retryableStatuses_areTransientOnly() {
		assertEquals(true, TraceLockTransport.isRetryable(429));
		assertEquals(true, TraceLockTransport.isRetryable(503));
		assertEquals(false, TraceLockTransport.isRetryable(401));
		assertEquals(false, TraceLockTransport.isRetryable(409));
	}
}
