/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;

/**
 * Tests for {@link TraceReceiptRepairTimer#repairPendingReceipts()}, called directly (not through
 * the CDI timer event, per task 18's test requirement): a stale {@code PENDING} claim settles to
 * {@code COMMITTED} when its record row exists, to {@code VOID} otherwise, and a claim younger than
 * {@code pendingReceiptTimeoutSeconds} is left untouched (design §9, TRACE MVP design decision
 * D-8).
 */
class TraceReceiptRepairTimerTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "producer-1/1.0.0";

	private static final String NODE_ID = "node-1";

	private InMemoryTraceStore store;

	private TraceConfiguration traceConfiguration;

	private TraceReceiptChain receiptChain;

	private TraceReceiptRepairTimer timer;

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		traceConfiguration = new TraceConfiguration();
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);

		receiptChain = new TraceReceiptChain();
		setField(receiptChain, "log", LoggerFactory.getLogger(TraceReceiptChain.class));
		setField(receiptChain, "store", store);
		setField(receiptChain, "appConfiguration", appConfiguration);

		timer = new TraceReceiptRepairTimer();
		setField(timer, "log", LoggerFactory.getLogger(TraceReceiptRepairTimer.class));
		setField(timer, "store", store);
		setField(timer, "appConfiguration", appConfiguration);
		setField(timer, "receiptChain", receiptChain);
	}

	private void insertMatchingRecord(String recordId, String contentDigest) throws DuplicateEntryException {
		StoredTraceRecord record = StoredTraceRecordFactory.builder().domainId(DOMAIN).producerId(PRODUCER)
				.recordId(recordId).contentDigest(contentDigest).build();
		store.insertRecord(record);
	}

	@Test
	void testRepairPendingReceipts_StalePendingWithRecord_Committed() throws DuplicateEntryException {
		traceConfiguration.setPendingReceiptTimeoutSeconds(1);
		long receivedAtMs = System.currentTimeMillis() - 10_000L;
		ReceiptClaim claim = receiptChain.claim(DOMAIN, PRODUCER, "rec-1", "rec-1-key", "sha256:" + digest('1'),
				receivedAtMs, NODE_ID);
		insertMatchingRecord("rec-1", "sha256:" + digest('1'));

		timer.repairPendingReceipts();

		assertEquals(TraceReceiptState.COMMITTED,
				store.findReceipt(DOMAIN, claim.getSeq()).get().getReceiptStateEnum());
	}

	@Test
	void testRepairPendingReceipts_StalePendingWithoutRecord_Voided() {
		traceConfiguration.setPendingReceiptTimeoutSeconds(1);
		long receivedAtMs = System.currentTimeMillis() - 10_000L;
		ReceiptClaim claim = receiptChain.claim(DOMAIN, PRODUCER, "rec-2", "rec-2-key", "sha256:" + digest('2'),
				receivedAtMs, NODE_ID);

		timer.repairPendingReceipts();

		assertEquals(TraceReceiptState.VOID, store.findReceipt(DOMAIN, claim.getSeq()).get().getReceiptStateEnum());
	}

	@Test
	void testRepairPendingReceipts_RecentPending_Untouched() {
		traceConfiguration.setPendingReceiptTimeoutSeconds(300);
		long receivedAtMs = System.currentTimeMillis();
		ReceiptClaim claim = receiptChain.claim(DOMAIN, PRODUCER, "rec-3", "rec-3-key", "sha256:" + digest('3'),
				receivedAtMs, NODE_ID);

		timer.repairPendingReceipts();

		assertEquals(TraceReceiptState.PENDING, store.findReceipt(DOMAIN, claim.getSeq()).get().getReceiptStateEnum());
	}

	private static String digest(char c) {
		char[] chars = new char[64];
		java.util.Arrays.fill(chars, c);
		return new String(chars);
	}

}
