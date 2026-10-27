/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.orm.PersistenceEntryManager;

/**
 * Receipt settlement through {@link OrmTraceStore} when the compare-and-swap loses to a concurrent
 * writer, simulated with {@link FakeEntryManagerBehavior#injectVersionConflicts}: the store must
 * re-read, re-apply the state-machine rule and never overwrite a terminal state. The fake is not
 * thread-safe, so no multi-threaded settle test runs against it.
 *
 * @author Yuriy Movchan
 */
class OrmReceiptSettlementTest {

	private static final String DOMAIN = "default";

	private PersistenceEntryManager manager;

	private OrmTraceStore store;

	@BeforeEach
	void setUp() throws Exception {
		manager = FakeEntryManagerBehavior.create();
		store = OrmTraceStores.create(manager);
		TraceReceiptEntry row = new TraceReceiptEntry();
		row.setDomainId(DOMAIN);
		row.setReceiptSeq(1L);
		row.setReceivedAt(new Date(1000L));
		row.setReceivedAtMs(1000L);
		row.setProducerId("producer-1");
		row.setRecordId("record-1");
		row.setRecordKey("record-1-key");
		row.setContentDigest("sha256:" + "a".repeat(64));
		row.setPrevReceiptHash("sha256:" + "0".repeat(64));
		row.setReceiptHash("sha256:" + "b".repeat(64));
		row.setReceiptStateEnum(TraceReceiptState.PENDING);
		row.setNodeId("node-1");
		row.setCreationDate(new Date(1000L));
		store.insertReceipt(row);
	}

	private TraceReceiptState stored() {
		return store.findReceipt(DOMAIN, 1L).orElseThrow().getReceiptStateEnum();
	}

	private static void settleTo(Object stored, TraceReceiptState state) {
		((TraceReceiptEntry) stored).setReceiptStateEnum(state);
	}

	@Test
	void testSettleReceipt_lostCasToSameState_alreadySettledAfterRetry() {
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 1, row -> settleTo(row, TraceReceiptState.COMMITTED));

		ReceiptSettlement outcome = store.settleReceipt(DOMAIN, 1L, TraceReceiptState.COMMITTED);

		assertEquals(ReceiptSettlement.ALREADY_SETTLED, outcome);
		assertEquals(TraceReceiptState.COMMITTED, stored());
	}

	@Test
	void testSettleReceipt_lostCasToOtherTerminalState_conflictAndUnchanged() {
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 1, row -> settleTo(row, TraceReceiptState.COMMITTED));

		ReceiptSettlement outcome = store.settleReceipt(DOMAIN, 1L, TraceReceiptState.VOID);

		assertEquals(ReceiptSettlement.CONFLICT, outcome);
		assertEquals(TraceReceiptState.COMMITTED, stored());
	}

	@Test
	void testSettleReceipt_lostCasWithoutStateChange_retriesAndSettles() {
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 2, row -> {
		});

		ReceiptSettlement outcome = store.settleReceipt(DOMAIN, 1L, TraceReceiptState.VOID);

		assertEquals(ReceiptSettlement.SETTLED, outcome);
		assertEquals(TraceReceiptState.VOID, stored());
	}

	@Test
	void testSettleReceipt_casLostOnEveryAttempt_throwsMergeFailed() {
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 3, row -> {
		});

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> store.settleReceipt(DOMAIN, 1L, TraceReceiptState.COMMITTED));

		assertEquals("merge_failed", ex.getReason());
		assertEquals(TraceReceiptState.PENDING, stored());
	}

}
