/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;

/**
 * The request-scoped producer key memo of {@link TraceIngestionService#ingestBatch}: one key read per
 * key and request, validity judged per element, failures never kept, and no memo on the other paths.
 */
class TraceBatchKeyMemoTest {

	private static final String DOMAIN = "domain-a";

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String KID_2 = "cedarling-fleet-1-2026-02";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN = "chain-01JABC9Z0K";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Counts {@code findProducerKey} calls; can fail the next one, slow every one, or run a hook after one. */
	private static final class KeyCountingStore extends InMemoryTraceStore {

		private final AtomicInteger keyReads = new AtomicInteger();

		private volatile boolean failNextKeyRead;

		private volatile long keyReadDelayMs;

		private volatile Runnable afterNextKeyRead;

		KeyCountingStore(TraceKeys keys) {
			super(keys);
		}

		@Override
		public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
			keyReads.incrementAndGet();
			if (failNextKeyRead) {
				failNextKeyRead = false;
				throw new TraceStorageException("read_failed", "injected key read failure");
			}
			long delay = keyReadDelayMs;
			if (delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			Optional<TraceProducerKeyEntry> found = super.findProducerKey(domainId, producerId, kid);
			Runnable hook = afterNextKeyRead;
			if (hook != null) {
				afterNextKeyRead = null;
				hook.run();
			}
			return found;
		}

	}

	private KeyCountingStore store;

	private TraceTestHarness node;

	private KeyPair key;

	private KeyPair key2;

	@BeforeEach
	void setUp() {
		node = new TraceTestHarness(TraceTestCrypto.javaProvider(), keys -> {
			store = new KeyCountingStore(keys);
			return store;
		});
		key = node.registerKey(DOMAIN, PRODUCER, KID);
		key2 = node.registerKey(DOMAIN, PRODUCER, KID_2);
		node.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN);
		store.keyReads.set(0);
	}

	@AfterEach
	void tearDown() {
		node.drainExecutors();
	}

	private static String digestOf(String json) {
		try {
			return CanonicalHashes.sha256PrefixedOfJcs(MAPPER.readTree(json));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/** A linked chain of {@code count} records starting at {@code firstSeq}; odd positions use {@code oddKid} when set. */
	private List<String> chain(int firstSeq, int count, String oddKid) {
		List<String> records = new ArrayList<>();
		String prev = TraceConstants.ZERO_HASH;
		for (int seq = firstSeq; seq < firstSeq + count; seq++) {
			boolean second = oddKid != null && seq % 2 == 1;
			String json = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE)
					.producerChainId(CHAIN).kid(second ? oddKid : KID).sequenceNumber(seq).prevRecordHash(prev)
					.sign((second ? key2 : key).getPrivate());
			records.add(json);
			prev = digestOf(json);
		}
		return records;
	}

	private List<String> chain(int count) {
		return chain(1, count, null);
	}

	private List<BatchOutcome> submit(List<String> jsons) {
		List<BatchElement> elements = new ArrayList<>();
		for (int i = 0; i < jsons.size(); i++) {
			elements.add(new BatchElement(i, jsons.get(i).getBytes(StandardCharsets.UTF_8)));
		}
		return node.ingestionService().ingestBatch(elements, node.ctx(DOMAIN));
	}

	private static void assertAllAccepted(List<BatchOutcome> outcomes) {
		for (int i = 0; i < outcomes.size(); i++) {
			int index = i;
			assertTrue(outcomes.get(i).isAccepted(), () -> "element " + index + ": " + outcomes.get(index));
		}
	}

	private static void assertKeyNotValid(BatchOutcome outcome, String reason) {
		assertFalse(outcome.isAccepted());
		TraceValidationException error = (TraceValidationException) outcome.getError();
		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, error.getErrorId());
		assertEquals(reason, error.getReason());
	}

	@Test
	void oneKidInBatchOfHundredReadsTheKeyOnce() {
		List<BatchOutcome> outcomes = submit(chain(100));

		assertAllAccepted(outcomes);
		assertEquals(1, store.keyReads.get());
	}

	@Test
	void twoKidsInOneBatchReadTwoKeys() {
		List<BatchOutcome> outcomes = submit(chain(1, 100, KID_2));

		assertAllAccepted(outcomes);
		assertEquals(2, store.keyReads.get());
	}

	@Test
	void parallelVerificationOnFourThreadsStillReadsTheKeyOnce() {
		node.traceConfiguration().setBatchVerifyThreads(4);
		store.keyReadDelayMs = 20;

		List<BatchOutcome> outcomes = submit(chain(100));

		assertAllAccepted(outcomes);
		assertEquals(1, store.keyReads.get());
	}

	@Test
	void parallelVerificationWithTwoKidsReadsTwoKeys() {
		node.traceConfiguration().setBatchVerifyThreads(4);
		store.keyReadDelayMs = 20;

		List<BatchOutcome> outcomes = submit(chain(1, 100, KID_2));

		assertAllAccepted(outcomes);
		assertEquals(2, store.keyReads.get());
	}

	@Test
	void revokedKeyFailsEveryElementThatUsesItAndIsReadAgainEachTime() {
		node.revokeKey(DOMAIN, PRODUCER, KID_2, 0L);
		store.keyReads.set(0);

		List<BatchOutcome> outcomes = submit(chain(1, 10, KID_2));

		for (int i = 0; i < 10; i++) {
			if (i % 2 == 0) {
				assertKeyNotValid(outcomes.get(i), "revoked");
			} else {
				assertTrue(outcomes.get(i).isAccepted(), "the elements signed with the other key are accepted");
			}
		}
		assertEquals(1 + 5, store.keyReads.get(), "a failing key is not kept: one read per element using it");
	}

	@Test
	void notYetValidKeyFailsEveryElementWithTheSameReason() {
		String futureKid = "cedarling-fleet-1-2099";
		KeyPair future = node.registerKey(DOMAIN, PRODUCER, futureKid, node.now() + 60_000L, null);
		List<String> jsons = new ArrayList<>();
		String prev = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= 3; seq++) {
			String json = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE)
					.producerChainId(CHAIN).kid(futureKid).sequenceNumber(seq).prevRecordHash(prev)
					.sign(future.getPrivate());
			jsons.add(json);
			prev = digestOf(json);
		}
		store.keyReads.set(0);

		List<BatchOutcome> outcomes = submit(jsons);

		for (BatchOutcome outcome : outcomes) {
			assertKeyNotValid(outcome, "not_yet_valid");
		}
		assertEquals(3, store.keyReads.get());
	}

	@Test
	void unknownKidFailsEveryElementAndIsLookedUpAgain() {
		List<String> jsons = new ArrayList<>();
		String prev = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= 3; seq++) {
			String json = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE)
					.producerChainId(CHAIN).kid("not-registered").sequenceNumber(seq).prevRecordHash(prev)
					.sign(key.getPrivate());
			jsons.add(json);
			prev = digestOf(json);
		}

		List<BatchOutcome> outcomes = submit(jsons);

		for (BatchOutcome outcome : outcomes) {
			assertFalse(outcome.isAccepted());
			TraceValidationException error = (TraceValidationException) outcome.getError();
			assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, error.getErrorId());
			assertEquals("key_not_registered", error.getReason());
		}
		assertEquals(3, store.keyReads.get());
	}

	@Test
	void registryFailureFailsOnlyThatElementAndIsNotCached() {
		store.failNextKeyRead = true;

		List<BatchOutcome> outcomes = submit(chain(5));

		assertFalse(outcomes.get(0).isAccepted());
		assertTrue(outcomes.get(0).getError() instanceof TraceStorageException);
		assertEquals("read_failed", ((TraceStorageException) outcomes.get(0).getError()).getReason());
		for (int i = 1; i < 5; i++) {
			assertTrue(outcomes.get(i).isAccepted(), "element " + i);
		}
		assertEquals(2, store.keyReads.get(), "the failed read is not kept; the next element reads again, then once");
	}

	@Test
	void revocationDuringTheRequestIsSeenByTheNextRequest() {
		store.afterNextKeyRead = () -> store.revokeProducerKey(DOMAIN, PRODUCER, KID, 0L);
		List<String> jsons = chain(10);

		List<BatchOutcome> first = submit(jsons.subList(0, 5));
		List<BatchOutcome> second = submit(jsons.subList(5, 10));

		assertAllAccepted(first);
		for (BatchOutcome outcome : second) {
			assertKeyNotValid(outcome, "revoked");
		}
	}

	@Test
	void everyRequestReadsTheKeyAgain() {
		List<String> jsons = chain(10);

		assertAllAccepted(submit(jsons.subList(0, 5)));
		assertAllAccepted(submit(jsons.subList(5, 10)));

		assertEquals(2, store.keyReads.get());
	}

	@Test
	void singleRecordPathReadsTheKeyForEveryRecord() {
		List<String> jsons = chain(3);
		for (String json : jsons) {
			node.ingestionService().ingest(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
					node.ctx(DOMAIN));
		}

		assertEquals(3, store.keyReads.get());
	}

}
