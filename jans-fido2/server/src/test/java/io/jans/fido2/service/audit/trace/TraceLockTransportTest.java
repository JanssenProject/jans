/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
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
 * Retry and classification for {@link TraceLockTransport}. The HTTP call, the wait and the jitter are
 * replaced with fakes, so each test pins the exact sequence of attempts, delays and outcomes.
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
	private final List<TraceLockTransport.PostResult> responses = new ArrayList<>();
	private final List<Exception> scriptedFailures = new ArrayList<>();
	private final List<String> tokensSeen = new ArrayList<>();
	private final List<String> endpointsSeen = new ArrayList<>();
	private final List<Long> sleeps = new ArrayList<>();
	private int postCalls;

	@BeforeEach
	void setUp() {
		fido2Configuration = new Fido2Configuration();
		fido2Configuration.setFido2TraceEndpoint(ENDPOINT);
		when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
		when(lockAuditTokenService.getTraceAccessToken(any(), any(), any())).thenReturn("trace-token");

		transport.poster = (endpoint, token, body) -> {
			postCalls++;
			tokensSeen.add(token);
			endpointsSeen.add(endpoint);
			if (!scriptedFailures.isEmpty()) {
				Exception next = scriptedFailures.remove(0);
				if (next != null) {
					throw next;
				}
			}
			return responses.remove(0);
		};
		transport.sleeper = millis -> sleeps.add(millis);
		transport.jitter = () -> 1.0;
	}

	private static TraceLockTransport.PostResult status(int status) {
		return new TraceLockTransport.PostResult(status, null, null);
	}

	private static TraceLockTransport.PostResult status(int status, String retryAfter) {
		return new TraceLockTransport.PostResult(status, retryAfter, null);
	}

	private static TraceLockTransport.PostResult status(int status, String retryAfter, String body) {
		return new TraceLockTransport.PostResult(status, retryAfter, body);
	}

	@Test
	void deliver_ifLockAccepts_returnsAcceptedAfterOneAttempt() {
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
		assertEquals(List.of(), sleeps);
		assertEquals("trace-token", tokensSeen.get(0));
	}

	@Test
	void deliver_ifServerErrorThenAccepted_retriesAfterOneSecond() {
		responses.add(status(503));
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(2, postCalls);
		assertEquals(List.of(1_000L), sleeps);
	}

	@Test
	void deliver_ifClientRejects_doesNotRetry() {
		responses.add(status(400));

		assertEquals(TraceLockTransport.Outcome.REJECTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
		assertEquals(List.of(), sleeps);
	}

	@Test
	void deliver_ifConflict_isRejectedNotRetried() {
		responses.add(status(409));

		assertEquals(TraceLockTransport.Outcome.REJECTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
	}

	@Test
	void deliver_ifBodySaysNotRetryable_rejectsEvenOnServerError() {
		responses.add(status(503, null, "{\"error\":{\"retryable\":false}}"));

		assertEquals(TraceLockTransport.Outcome.REJECTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
	}

	@Test
	void deliver_ifBodySaysRetryable_retriesEvenOnClientError() {
		responses.add(status(400, null, "{\"retryable\":true}"));
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(2, postCalls);
	}

	@Test
	void deliver_ifAlwaysRateLimited_givesUpAfterMaxAttemptsWithExponentialDelays() {
		for (int i = 0; i < TraceLockTransport.MAX_ATTEMPTS; i++) {
			responses.add(status(429));
		}

		assertEquals(TraceLockTransport.Outcome.EXHAUSTED, transport.deliver(RECORD));
		assertEquals(TraceLockTransport.MAX_ATTEMPTS, postCalls);
		assertEquals(List.of(1_000L, 2_000L, 4_000L, 8_000L), sleeps);
	}

	@Test
	void deliver_ifRetryAfterIsLongerThanJitter_waitsForRetryAfter() {
		responses.add(status(429, "3"));
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(List.of(3_000L), sleeps);
	}

	@Test
	void deliver_ifRetryAfterBeyondCap_givesUpWithoutWaiting() {
		responses.add(status(429, "3600"));

		assertEquals(TraceLockTransport.Outcome.EXHAUSTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
		assertEquals(List.of(), sleeps);
	}

	@Test
	void deliver_ifNetworkErrorThenAccepted_retries() {
		scriptedFailures.add(new IOException("connection reset"));
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(2, postCalls);
		assertEquals(List.of(1_000L), sleeps);
	}

	@Test
	void deliver_ifJitterIsZero_retriesImmediately() {
		transport.jitter = () -> 0.0;
		responses.add(status(503));
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(List.of(0L), sleeps);
	}

	@Test
	void deliver_ifTokenUnavailableThenAvailable_fetchesTokenAgainWithoutPosting() {
		when(lockAuditTokenService.getTraceAccessToken(any(), any(), any())).thenReturn(null, "trace-token");
		responses.add(status(202));

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
	}

	@Test
	void deliver_ifConfigurationReloadsDuringRetry_stopsBeforePosting() {
		responses.add(status(503));
		responses.add(status(202));
		transport.sleeper = millis -> {
			sleeps.add(millis);
			transport.onConfigurationUpdate(null);
		};

		assertEquals(TraceLockTransport.Outcome.NOT_CONFIGURED, transport.deliver(RECORD));
		assertEquals(1, postCalls);
	}

	@Test
	void deliver_ifConfigurationReloadsAndTokenIsUnavailable_stopsWithoutRequestingMoreTokens() {
		when(lockAuditTokenService.getTraceAccessToken(any(), any(), any())).thenReturn(null);
		responses.add(status(202));
		transport.sleeper = millis -> transport.onConfigurationUpdate(null);

		assertEquals(TraceLockTransport.Outcome.NOT_CONFIGURED, transport.deliver(RECORD));
		assertEquals(0, postCalls);
		verify(lockAuditTokenService, times(1)).getTraceAccessToken(any(), any(), any());
	}

	@Test
	void deliver_ifEndpointChangesWithoutReloadEvent_retriesToTheEndpointItStartedWith() {
		responses.add(status(503));
		responses.add(status(202));
		transport.sleeper = millis -> fido2Configuration.setFido2TraceEndpoint("https://other.example.com/api/v1/audit/trace");

		assertEquals(TraceLockTransport.Outcome.ACCEPTED, transport.deliver(RECORD));
		assertEquals(List.of(ENDPOINT, ENDPOINT), endpointsSeen);
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
	void backoffCeiling_doublesFromOneSecondAndIsCappedAtFiveMinutes() {
		assertEquals(1_000L, TraceLockTransport.backoffCeilingMillis(1));
		assertEquals(2_000L, TraceLockTransport.backoffCeilingMillis(2));
		assertEquals(4_000L, TraceLockTransport.backoffCeilingMillis(3));
		assertEquals(300_000L, TraceLockTransport.backoffCeilingMillis(10));
		assertEquals(300_000L, TraceLockTransport.backoffCeilingMillis(1_000));
	}

	@Test
	void retryAfter_parsesSecondsAndHttpDate() {
		Instant now = Instant.parse("2026-11-25T10:00:00Z");

		assertEquals(5_000L, TraceLockTransport.parseRetryAfterMillis("5", now));
		assertEquals(0L, TraceLockTransport.parseRetryAfterMillis(null, now));
		assertEquals(0L, TraceLockTransport.parseRetryAfterMillis("soon", now));
		assertEquals(30_000L, TraceLockTransport.parseRetryAfterMillis("Wed, 25 Nov 2026 10:00:30 GMT", now));
		assertEquals(0L, TraceLockTransport.parseRetryAfterMillis("Wed, 25 Nov 2026 09:00:00 GMT", now));
	}

	@Test
	void declaredRetryable_readsMemberOrReturnsNull() {
		assertNull(TraceLockTransport.declaredRetryable(null));
		assertNull(TraceLockTransport.declaredRetryable("{\"error\":\"invalid_request\"}"));
		assertEquals(false, TraceLockTransport.declaredRetryable("{\"error\":{\"retryable\":false}}"));
		assertEquals(true, TraceLockTransport.declaredRetryable("{\"retryable\":true}"));
	}
}
