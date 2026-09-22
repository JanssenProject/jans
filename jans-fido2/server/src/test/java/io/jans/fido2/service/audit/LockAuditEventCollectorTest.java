/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.model.audit.LockAuditEvent;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the batching/delivery contract {@code LockAuditEventCollector} exists for: events collected
 * between flushes go out as one batch, a Lock Server failure never propagates past
 * {@code processImpl()}, and nothing is buffered (or redelivered) while delivery is disabled.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockAuditEventCollectorTest {

	@InjectMocks
	private LockAuditEventCollector collector;

	@Mock
	private Logger log;
	@Mock
	private AppConfiguration appConfiguration;
	@Mock
	private LockAuditClient lockAuditClient;

	private Fido2Configuration fido2Configuration;

	@BeforeEach
	void setUp() {
		fido2Configuration = new Fido2Configuration();
		fido2Configuration.setLockAuditEnabled(true);
		when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
	}

	/**
	 * The whole point of the collector: two collect() calls flush as a single postBatch() call
	 * carrying both events, not two separate HTTP round-trips.
	 */
	@Test
	void collectTwiceThenProcessImpl_deliversBothEventsInOneBatch() {
		collector.collect(event("registration"));
		collector.collect(event("authentication"));

		collector.processImpl();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<LockAuditEvent>> captor = ArgumentCaptor.forClass(List.class);
		verify(lockAuditClient, times(1)).postBatch(captor.capture());
		assertEquals(2, captor.getValue().size());
	}

	@Test
	void processImpl_ifBufferEmpty_neverCallsClient() {
		collector.processImpl();

		verify(lockAuditClient, never()).postBatch(any());
	}

	/**
	 * The client is fire-and-forget from the caller's perspective; a Lock Server outage must be
	 * absorbed here, not surfaced to whatever fired the flush.
	 */
	@Test
	void processImpl_ifClientThrows_doesNotPropagate() {
		collector.collect(event("registration"));
		doThrow(new RuntimeException("lock unreachable")).when(lockAuditClient).postBatch(any());

		assertDoesNotThrow(() -> collector.processImpl());
	}

	/**
	 * Proves collect() itself drops events while disabled, not just that processImpl() withholds
	 * delivery — otherwise a deployment that never enables this feature would buffer forever.
	 */
	@Test
	void collect_ifDisabledAtCollectTime_isNeverBuffered() {
		fido2Configuration.setLockAuditEnabled(false);
		collector.collect(event("registration"));

		fido2Configuration.setLockAuditEnabled(true);
		collector.processImpl();

		verify(lockAuditClient, never()).postBatch(any());
	}

	/**
	 * A second, empty pass after a successful flush must not re-send what the first pass already
	 * drained — pins that the buffer is actually consumed, not just read.
	 */
	@Test
	void processImpl_secondPassAfterFlush_doesNotRedeliverDrainedEvents() {
		collector.collect(event("registration"));

		collector.processImpl();
		collector.processImpl();

		verify(lockAuditClient, times(1)).postBatch(any());
	}

	private static LockAuditEvent event(String eventType) {
		LockAuditEvent event = new LockAuditEvent();
		event.setEventType(eventType);
		return event;
	}
}
