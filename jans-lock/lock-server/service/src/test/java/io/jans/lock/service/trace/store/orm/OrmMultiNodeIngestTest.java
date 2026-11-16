/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.ingest.BatchElement;
import io.jans.lock.service.trace.ingest.BatchOutcome;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.orm.PersistenceEntryManager;

/**
 * Correctness under cross-node concurrency on a live SQL database (task 08, deliverable 2): several
 * independent ingest graphs (one {@link TraceTestHarness} each, with its own {@code TraceReceiptChain},
 * pools and node id, so no in-process lock is shared) write bulk batches and single submissions to
 * one domain at the same time. Afterwards {@link DomainAudit} checks the domain: receipt sequences
 * {@code 1..N} with no gap or duplicate, all {@code COMMITTED}, no {@code PENDING} row, record count
 * equal to the accepted outcomes, one record per receipt carrying its identity and digest, and
 * {@code verifyDomain} without a problem. A submission that loses every position race
 * ({@code receipt_allocation_exhausted}) fails closed and is counted, not stored; the test prints how
 * many there were. Opt-in through {@value DatabaseProfile#PROPERTY}.
 */
@EnabledIfSystemProperty(named = DatabaseProfile.PROPERTY, matches = ".+")
class OrmMultiNodeIngestTest {

	private static final int NODES = 3;

	private static final int CHAINS_PER_NODE = 2;

	private static final int RECORDS_PER_CHAIN = 300;

	private static final int BULK_SIZE = 50;

	private static PersistenceEntryManager entryManager;

	private String domain;

	private List<TraceTestHarness> nodes;

	@BeforeAll
	static void connect() throws Exception {
		entryManager = DatabaseProfile.connect();
	}

	@AfterAll
	static void disconnect() {
		if (entryManager != null) {
			entryManager.destroy();
		}
	}

	@BeforeEach
	void setUp() {
		domain = DatabaseProfile.newDomain("nodes");
		nodes = new ArrayList<>();
		for (int n = 0; n < NODES; n++) {
			nodes.add(new TraceTestHarness(TraceTestCrypto.javaProvider(),
					keys -> new InstrumentedTraceStore(OrmTraceStores.create(entryManager, keys))).setNodeId("node-" + n));
		}
	}

	@AfterEach
	void removeDomain() {
		DatabaseProfile.removeDomain(entryManager, domain);
	}

	@Test
	void bulkBatchesFromSeveralNodesKeepOneDenseChain() throws Exception {
		runConcurrently("bulk", 0, 0);
	}

	@Test
	void bulkBatchesWithPoolsFromSeveralNodesKeepOneDenseChain() throws Exception {
		runConcurrently("bulk", 4, 2);
	}

	@Test
	void singleSubmissionsAsBatchesFromSeveralNodesKeepOneDenseChain() throws Exception {
		runConcurrently("single-as-batch", 0, 0);
	}

	@Test
	void mixedPathsFromSeveralNodesKeepOneDenseChain() throws Exception {
		runConcurrently("mixed", 0, 0);
	}

	/**
	 * {@code receipt_allocation_exhausted}: the submission lost {@code receiptAllocationRetryLimit}
	 * position races in a row and failed closed, storing nothing. It is the one failure the test
	 * allows, and it is counted.
	 */
	private static boolean isExhausted(RuntimeException error) {
		return error instanceof TraceStorageException
				&& "receipt_allocation_exhausted".equals(((TraceStorageException) error).getReason());
	}

	/**
	 * @param path {@code bulk} (every chain in batches of {@link #BULK_SIZE}), {@code single-as-batch}
	 *             (every record alone through {@code ingestAsBatch}) or {@code mixed} (node 0 bulk,
	 *             node 1 single-as-batch, node 2 the old single path, all in one domain)
	 */
	private void runConcurrently(String path, int verifyThreads, int postCommitThreads) throws Exception {
		TraceTestHarness registrar = nodes.get(0);
		List<List<String>> chains = new ArrayList<>();
		List<TraceTestHarness> owners = new ArrayList<>();
		for (int n = 0; n < NODES; n++) {
			TraceTestHarness node = nodes.get(n);
			node.traceConfiguration().setBatchVerifyThreads(verifyThreads);
			node.traceConfiguration().setPostCommitCorrelationThreads(postCommitThreads);
			String nodePath = path.equals("mixed") ? new String[] { "bulk", "single-as-batch", "single" }[n % 3] : path;
			if (nodePath.equals("single-as-batch")) {
				node.useSingleAsBatchPath();
			}
			for (int c = 0; c < CHAINS_PER_NODE; c++) {
				String producer = "nodes-producer-" + n + "-" + c;
				String kid = producer + "-kid";
				KeyPair key = registrar.registerKey(domain, producer, kid);
				registrar.registerChain(domain, producer, "nodes-instance", "nodes-chain");
				chains.add(TraceRecordBuilder.linkedChain(key, producer, kid, "nodes-instance", "nodes-chain",
						RECORDS_PER_CHAIN));
				owners.add(node);
			}
		}

		AtomicInteger accepted = new AtomicInteger();
		AtomicInteger exhausted = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(chains.size());
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < chains.size(); i++) {
			List<String> jsons = chains.get(i);
			TraceTestHarness node = owners.get(i);
			boolean bulk = path.equals("bulk") || (path.equals("mixed") && nodes.indexOf(node) % 3 == 0);
			futures.add(pool.submit(() -> {
				if (bulk) {
					for (int from = 0; from < jsons.size(); from += BULK_SIZE) {
						List<BatchElement> elements = new ArrayList<>();
						for (String json : jsons.subList(from, Math.min(jsons.size(), from + BULK_SIZE))) {
							elements.add(new BatchElement(elements.size(), json.getBytes(StandardCharsets.UTF_8)));
						}
						for (BatchOutcome outcome : node.ingestionService().ingestBatch(elements, node.ctx(domain))) {
							if (!outcome.isAccepted()) {
								assertTrue(isExhausted(outcome.getError()), () -> String.valueOf(outcome.getError()));
								exhausted.incrementAndGet();
								continue;
							}
							assertTrue(!outcome.getResult().isIdempotentReplay());
							accepted.incrementAndGet();
						}
					}
					return null;
				}
				for (String json : jsons) {
					try {
						assertTrue(!node.ingest(json, node.ctx(domain)).isIdempotentReplay());
						accepted.incrementAndGet();
					} catch (TraceStorageException ex) {
						assertTrue(isExhausted(ex), ex::toString);
						exhausted.incrementAndGet();
					}
				}
				return null;
			}));
		}
		// Wait for every chain before any assertion: a failed chain must not leave the others writing
		// while the domain is audited and removed.
		Exception failure = null;
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception ex) {
				failure = failure == null ? ex : failure;
			}
		}
		pool.shutdown();
		if (failure != null) {
			throw failure;
		}
		for (TraceTestHarness node : nodes) {
			node.drainExecutors();
		}

		int submitted = chains.size() * RECORDS_PER_CHAIN;
		int expected = accepted.get();
		assertEquals(submitted, accepted.get() + exhausted.get(), "every submission was accepted or failed closed");
		int retries = 0;
		for (TraceTestHarness node : nodes) {
			InstrumentedTraceStore store = (InstrumentedTraceStore) node.traceStore();
			retries += store.batchDuplicates() + store.receiptDuplicates();
			assertEquals(0, store.batchFailures(), "no batch failed for a reason other than a collision");
		}
		DomainAudit audit = new DomainAudit(registrar.traceStore(), entryManager, domain).run(expected, true,
				registrar.receiptChain());
		System.out.println("  multi-node " + path + " verify=" + verifyThreads + " post-commit=" + postCommitThreads
				+ ": submitted=" + submitted + " accepted=" + expected + " exhausted=" + exhausted.get() + " retries=" + retries
				+ " " + audit);
		assertTrue(audit.ok(), audit::toString);
		assertEquals(expected, audit.head());
	}

}
