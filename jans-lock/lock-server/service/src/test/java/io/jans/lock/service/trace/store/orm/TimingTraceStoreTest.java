/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.store.TraceStore;

/**
 * @author Yuriy Movchan
 */
class TimingTraceStoreTest {

	private InMemoryTraceStore inner;

	private TimingTraceStore store;

	@BeforeEach
	void setUp() {
		inner = new InMemoryTraceStore();
		store = new TimingTraceStore(inner);
	}

	private static TraceChainEntry chain(String chainId) {
		TraceChainEntry entry = new TraceChainEntry();
		entry.setDomainId("domain-1");
		entry.setProducerId("producer-1");
		entry.setProducerInstanceId("instance-1");
		entry.setProducerChainId(chainId);
		entry.setRegisteredBy("admin");
		entry.setCreationDate(new Date(0));
		return entry;
	}

	@Test
	void callIsCountedOnceAndDelegated() throws Exception {
		store.insertChain(chain("chain-1"));
		assertTrue(store.findChain(new ChainIdentity("domain-1", "producer-1", "instance-1", "chain-1")).isPresent());

		TimingTraceStore.Snapshot snapshot = store.snapshot();
		assertEquals(1, snapshot.get("insertChain").calls());
		assertEquals(1, snapshot.get("findChain").calls());
		assertEquals(2, snapshot.totalCalls());
		assertEquals(1, snapshot.writeCalls());
		assertEquals(1, snapshot.readCalls());
		assertNull(snapshot.get("findRecord"));
	}

	@Test
	void timeIsNonNegativeAndSumsToTotal() throws Exception {
		store.insertChain(chain("chain-1"));
		store.findReceiptHead("domain-1");

		TimingTraceStore.Snapshot snapshot = store.snapshot();
		long sum = 0;
		for (TimingTraceStore.MethodStats stats : snapshot.methods()) {
			assertTrue(stats.totalNanos() >= 0);
			sum += stats.totalNanos();
		}
		assertEquals(sum, snapshot.totalNanos());
	}

	@Test
	void resetClearsEverything() throws Exception {
		store.insertChain(chain("chain-1"));
		assertFalse(store.snapshot().isEmpty());

		store.reset();

		assertTrue(store.snapshot().isEmpty());
		assertEquals(0, store.snapshot().totalCalls());
		store.findReceiptHead("domain-1");
		assertEquals(1, store.snapshot().totalCalls());
	}

	@Test
	void delegateExceptionPropagatesAndIsCounted() throws Exception {
		store.insertChain(chain("chain-1"));

		assertThrows(DuplicateEntryException.class, () -> store.insertChain(chain("chain-1")));

		TimingTraceStore.MethodStats stats = store.snapshot().get("insertChain");
		assertEquals(2, stats.calls());
		assertTrue(stats.totalNanos() >= 0);
	}

	@Test
	void runtimeExceptionFromDelegatePropagatesAndIsCounted() {
		inner.failNextFindRecord();

		assertThrows(TraceStorageException.class, () -> store.findRecord(null));

		assertEquals(1, store.snapshot().get("findRecord").calls());
	}

	@Test
	void methodsAreClassifiedAsReadOrWrite() {
		for (String write : new String[] { "insertRecord", "insertReceipt", "settleReceipt", "updateRecordFlags",
				"revokeProducerKey", "recordPolicyReleaseServed", "removePolicyReleaseLastServedBefore" }) {
			assertTrue(TimingTraceStore.isWrite(write), write);
		}
		for (String read : new String[] { "findRecord", "findReceiptHead", "findRecordsByChainPosition", "countRecords",
				"findPolicyRelease", "findPendingReceiptsOlderThan" }) {
			assertFalse(TimingTraceStore.isWrite(read), read);
		}
	}

	@Test
	void everyStoreMethodIsClassifiedAndTimed() throws Exception {
		for (java.lang.reflect.Method method : TraceStore.class.getMethods()) {
			assertNotNull(TimingTraceStore.class.getMethod(method.getName(), method.getParameterTypes()));
			assertSame(TimingTraceStore.class, TimingTraceStore.class.getMethod(method.getName(), method.getParameterTypes())
					.getDeclaringClass(), method.getName() + " is not overridden by the decorator");
		}
	}

}
