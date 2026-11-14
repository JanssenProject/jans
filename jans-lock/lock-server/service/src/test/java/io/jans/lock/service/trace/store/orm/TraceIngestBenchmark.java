/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.ingest.BatchElement;
import io.jans.lock.service.trace.ingest.BatchOutcome;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.orm.PersistenceEntryManager;

/**
 * TRACE ingest benchmark: signed records go through the real ingest pipeline
 * ({@code TraceIngestionService}) over {@link OrmTraceStore} and a live SQL database, so every
 * ORM round trip is paid. It is never run by the default build: the class name does not match the
 * surefire {@code *Test} pattern. Run it with the {@code benchmark} profile and a database profile
 * folder ({@code jans-sql.properties} and {@code salt}, the same layout as
 * {@code jans-orm/integration-test/profiles/<name>/conf}):
 *
 * <pre>
 * cd jans-lock/lock-server
 * mvn -o -P benchmark -pl model,service -am -Ddownload.plugin.skip=true test \
 *     -Dlock.benchmark.profile=/path/to/jans-orm/integration-test/profiles/jans-dev.jans.info/conf
 * </pre>
 *
 * Optional properties: {@code lock.benchmark.records} (records per chain, default 1000),
 * {@code lock.benchmark.chains} (comma separated parallel chain counts, default {@code 1,4}),
 * {@code lock.benchmark.nodes} (comma separated Lock node counts, default {@code 1}; the scenarios are
 * the cross product with the chain counts: each node is an independent ingest graph with its own
 * {@code TraceReceiptChain}, pools and node id over the same database, the chains of a scenario are
 * spread over the nodes round-robin, and all write the same domain, so the receipt unique key is the
 * only arbiter between nodes; {@code lock.benchmark.scenarios}, for example {@code 1x1,1x1,2x4,4x4},
 * lists {@code <nodes>x<chains>} scenarios explicitly and replaces the cross product), {@code lock.benchmark.preload} (committed receipts inserted into the
 * domain before the run, default 0; use it, for example 20000, to expose work which grows with the
 * number of receipts in a domain), {@code lock.benchmark.min-rate} (fail when one-chain records/s is
 * lower, default off), {@code lock.benchmark.max-slowdown} (fail when the last quarter of a one-chain
 * run is more than this many times slower than the first quarter, default off; it flags work which
 * grows with the receipt table, like {@code count(*)}), {@code lock.benchmark.timing} ({@code true}
 * prints, after each scenario, the calls and time of every store method; the totals go to the CSV as
 * extra columns, empty when timing is off), {@code lock.benchmark.path} ({@code single}, the
 * default: {@code ingest} per record; {@code single-as-batch}: {@code ingestAsBatch} per record, the
 * single submission path with batch ingest enabled; {@code bulk}: {@code ingestBatch} with
 * {@code lock.benchmark.bulk-size} records per call, default 100, each record timed as its share of
 * the call), {@code lock.benchmark.verify-threads} and {@code lock.benchmark.post-commit-threads}
 * (the {@code batchVerifyThreads} and {@code postCommitCorrelationThreads} settings, default 0),
 * {@code lock.benchmark.range-retry-limit}, {@code lock.benchmark.range-backoff-ms} and
 * {@code lock.benchmark.range-max-backoff-ms} (the {@code rangeAllocationRetryLimit},
 * {@code rangeAllocationBackoffMs} and {@code rangeAllocationMaxBackoffMs} settings; the configuration
 * defaults when absent),
 * {@code lock.benchmark.audit} ({@code true}, the default, checks the domain after every scenario:
 * dense committed receipts, one record per receipt, record count equal to the accepted outcomes;
 * after the last scenario it also walks the chain with {@code verifyDomain}).
 *
 * <p>Every scenario reports the retries: {@code insertIngestBatch} calls rejected as duplicate
 * (bulk and single-as-batch; one rejection is one re-planned batch) or {@code insertReceipt} claims
 * that lost their position (single path), for the bulk path the p50 and p95 of one bulk call, and the
 * submissions that failed closed with {@code receipt_allocation_exhausted} (column {@code exhausted};
 * records/s counts accepted records only). Batch paths also print the retry profile: committed
 * batches by the number of rejections before their commit, ranges given up (per-record fallback) by
 * the number of rejections they collected, and the {@code insertRecord} calls of the fallback.
 *
 * <p>{@link #adjacentPositionsAcrossNodes()} is a second, separate measurement, enabled by
 * {@code lock.benchmark.adjacent=<rounds>}: two nodes ingest adjacent positions of one chain at
 * the same time ({@code 2r-1} on node A, {@code 2r} on node B), {@code rounds} times, on every path,
 * and the final {@code coverage_gap}/{@code chain_link_failure} flags are compared with a
 * sequential ingest of the same records. It reports how many records end with flags that the
 * sequential ingest does not give (stale flags); it fixes nothing.
 *
 * <p>Results are printed and appended to {@code target/benchmark/trace-ingest.csv}. All data uses
 * throw-away domains which are removed afterwards.
 *
 * @author Yuriy Movchan
 */
@EnabledIfSystemProperty(named = DatabaseProfile.PROPERTY, matches = ".+")
class TraceIngestBenchmark {

	private static final Logger LOG = LoggerFactory.getLogger(TraceIngestBenchmark.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String CSV_HEADER = "timestamp,chains,records_per_chain,total_records,seconds,records_per_s,first_quarter_per_s,last_quarter_per_s,p50_ms,p95_ms,store_calls_per_record,writes_per_record,reads_per_record,path,nodes,batches,retries,batch_p50_ms,batch_p95_ms,exhausted";

	private static PersistenceEntryManager entryManager;

	private static String domain;

	private static final List<String> extraDomains = new ArrayList<>();

	@BeforeAll
	static void connect() throws Exception {
		entryManager = DatabaseProfile.connect();
		domain = DatabaseProfile.newDomain("run");
	}

	@AfterAll
	static void cleanup() {
		if (entryManager == null) {
			return;
		}
		try {
			DatabaseProfile.removeDomain(entryManager, domain);
			for (String extra : extraDomains) {
				DatabaseProfile.removeDomain(entryManager, extra);
			}
		} finally {
			entryManager.destroy();
		}
	}

	@Test
	void ingestThroughput() throws Exception {
		int records = intProperty("lock.benchmark.records", 1000);
		List<String> chainCounts = Arrays.asList(System.getProperty("lock.benchmark.chains", "1,4").split(","));
		List<String> nodeCounts = Arrays.asList(System.getProperty("lock.benchmark.nodes", "1").split(","));
		double minRate = Double.parseDouble(System.getProperty("lock.benchmark.min-rate", "0"));
		double maxSlowdown = Double.parseDouble(System.getProperty("lock.benchmark.max-slowdown", "0"));
		boolean audit = Boolean.parseBoolean(System.getProperty("lock.benchmark.audit", "true"));
		int preload = intProperty("lock.benchmark.preload", 0);

		preloadReceipts(preload);

		List<int[]> scenarios = new ArrayList<>();
		String explicit = System.getProperty("lock.benchmark.scenarios", "").trim();
		if (!explicit.isEmpty()) {
			for (String scenario : explicit.split(",")) {
				String[] parts = scenario.trim().split("x");
				scenarios.add(new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) });
			}
		} else {
			for (String nodeCount : nodeCounts) {
				for (String chainCount : chainCounts) {
					scenarios.add(new int[] { Integer.parseInt(nodeCount.trim()), Integer.parseInt(chainCount.trim()) });
				}
			}
		}
		long accepted = preload;
		int run = 0;
		for (int[] scenario : scenarios) {
			boolean last = run == scenarios.size() - 1;
			Result result = runScenario(scenario[0], scenario[1], records, run++);
			accepted += result.chains * (long) result.records - result.exhausted;
			report(result);
			if (audit) {
				audit(accepted, last);
			}
			if (result.chains == 1) {
				if (minRate > 0) {
					assertTrue(result.recordsPerSecond() >= minRate, String
							.format("one chain ingests %.1f records/s, expected at least %.1f", result.recordsPerSecond(), minRate));
				}
				if (maxSlowdown > 0) {
					assertTrue(result.firstQuarterRate() <= result.lastQuarterRate() * maxSlowdown,
							String.format("last quarter %.1f records/s is more than %.1f times slower than first quarter %.1f",
									result.lastQuarterRate(), maxSlowdown, result.firstQuarterRate()));
				}
			}
		}
	}

	private static void audit(long expectedAccepted, boolean walkChain) {
		TraceTestHarness harness = newNode("audit", 0);
		long start = System.nanoTime();
		DomainAudit audit = new DomainAudit(harness.traceStore(), entryManager, domain).run(expectedAccepted, walkChain,
				harness.receiptChain());
		String line = String.format(Locale.ROOT, "domain audit: head=%d receipts=%d records=%d walked=%s seconds=%.1f problems=%s",
				audit.head(), audit.receipts(), audit.records(), walkChain, (System.nanoTime() - start) / 1e9,
				audit.problems());
		LOG.info("TRACE ingest benchmark {}", line);
		System.out.println("  " + line);
		assertTrue(audit.ok(), () -> "domain audit failed: " + audit);
	}

	/** Decimal only: {@code Integer.getInteger} would read {@code 010} as octal and {@code 08} as the default. */
	private static int intProperty(String name, int defaultValue) {
		String value = System.getProperty(name);
		return (value == null || value.trim().isEmpty()) ? defaultValue : Integer.parseInt(value.trim());
	}

	private static void preloadReceipts(int count) throws Exception {
		if (count <= 0) {
			return;
		}
		OrmTraceStore store = OrmTraceStores.create(entryManager, TraceTestCrypto.traceKeys());
		ExecutorService pool = Executors.newFixedThreadPool(8);
		List<Future<?>> futures = new ArrayList<>();
		long startNs = System.nanoTime();
		for (int seq = 1; seq <= count; seq++) {
			long position = seq;
			futures.add(pool.submit(() -> {
				TraceReceiptEntry row = new TraceReceiptEntry();
				row.setDomainId(domain);
				row.setReceiptSeq(position);
				row.setReceivedAt(new Date(TraceTestHarness.DEFAULT_NOW_MS));
				row.setReceivedAtMs(TraceTestHarness.DEFAULT_NOW_MS);
				row.setProducerId("bench-preload");
				row.setRecordId("preload-" + position);
				row.setRecordKey("preload-" + position);
				row.setContentDigest(TraceConstants.ZERO_HASH);
				row.setPrevReceiptHash(TraceConstants.ZERO_HASH);
				row.setReceiptHash(TraceConstants.ZERO_HASH);
				row.setReceiptStateEnum(TraceReceiptState.COMMITTED);
				row.setNodeId("bench-node");
				row.setCreationDate(new Date(TraceTestHarness.DEFAULT_NOW_MS));
				store.insertReceipt(row);
				return null;
			}));
		}
		for (Future<?> future : futures) {
			future.get();
		}
		pool.shutdown();
		LOG.info("Preloaded {} receipts in {} s", count, (System.nanoTime() - startNs) / 1e9);
	}

	/**
	 * One simulated Lock node: its own harness (receipt chain, pools, node id) over its own
	 * {@link InstrumentedTraceStore} and the shared entry manager.
	 */
	private static TraceTestHarness newNode(String label, int index) {
		TraceTestHarness harness = new TraceTestHarness(TraceTestCrypto.javaProvider(),
				keys -> new InstrumentedTraceStore(OrmTraceStores.create(entryManager, keys)));
		harness.setNodeId("bench-" + label + "-node-" + index);
		return harness;
	}

	private static InstrumentedTraceStore storeOf(TraceTestHarness harness) {
		return (InstrumentedTraceStore) harness.traceStore();
	}

	private static Result runScenario(int nodes, int chains, int records, int run) throws Exception {
		boolean timing = Boolean.getBoolean("lock.benchmark.timing");
		String path = ingestPath();
		int bulkSize = intProperty("lock.benchmark.bulk-size", 100);
		int verifyThreads = intProperty("lock.benchmark.verify-threads", 0);
		int postCommitThreads = intProperty("lock.benchmark.post-commit-threads", 0);

		List<TraceTestHarness> nodeHarnesses = new ArrayList<>();
		for (int n = 0; n < nodes; n++) {
			TraceTestHarness harness = newNode(path, n);
			if (path.equals("single-as-batch")) {
				harness.useSingleAsBatchPath();
			}
			harness.traceConfiguration().setBatchVerifyThreads(verifyThreads);
			harness.traceConfiguration().setPostCommitCorrelationThreads(postCommitThreads);
			applyRangeSettings(harness);
			nodeHarnesses.add(harness);
		}

		// Keys and chains live in the shared database: register them once, through node 0.
		TraceTestHarness registrar = nodeHarnesses.get(0);
		List<List<String>> chainJsons = new ArrayList<>();
		for (int c = 0; c < chains; c++) {
			String suffix = run + "-" + c;
			String producer = "bench-producer-" + suffix;
			String kid = producer + "-kid";
			String instance = "bench-instance-" + suffix;
			String chain = "bench-chain-" + suffix;
			KeyPair key = registrar.registerKey(domain, producer, kid);
			registrar.registerChain(domain, producer, instance, chain);
			chainJsons.add(buildChain(key, producer, kid, instance, chain, records));
		}

		for (TraceTestHarness harness : nodeHarnesses) {
			storeOf(harness).reset();
			storeOf(harness).resetCounters();
		}
		ExecutorService pool = Executors.newFixedThreadPool(chains);
		List<Future<long[]>> futures = new ArrayList<>();
		List<List<Long>> batchDurations = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger exhausted = new AtomicInteger();
		long startNs = System.nanoTime();
		for (int c = 0; c < chainJsons.size(); c++) {
			List<String> jsons = chainJsons.get(c);
			TraceTestHarness harness = nodeHarnesses.get(c % nodes);
			futures.add(pool.submit(() -> {
				long[] durationsNs = new long[jsons.size()];
				if (path.equals("bulk")) {
					List<Long> calls = new ArrayList<>();
					for (int from = 0; from < jsons.size(); from += bulkSize) {
						int to = Math.min(from + bulkSize, jsons.size());
						long begin = System.nanoTime();
						exhausted.addAndGet(ingestBulk(harness, jsons.subList(from, to)));
						storeOf(harness).endSubmission();
						long elapsed = System.nanoTime() - begin;
						calls.add(elapsed);
						Arrays.fill(durationsNs, from, to, elapsed / (to - from));
					}
					batchDurations.add(calls);
					return durationsNs;
				}
				for (int i = 0; i < jsons.size(); i++) {
					long begin = System.nanoTime();
					try {
						harness.ingest(jsons.get(i), harness.ctx(domain));
					} catch (TraceStorageException ex) {
						if (!isExhausted(ex)) {
							throw ex;
						}
						exhausted.incrementAndGet();
					}
					storeOf(harness).endSubmission();
					durationsNs[i] = System.nanoTime() - begin;
				}
				return durationsNs;
			}));
		}
		List<long[]> durations = new ArrayList<>();
		Exception failure = null;
		for (Future<long[]> future : futures) {
			try {
				durations.add(future.get());
			} catch (Exception ex) {
				failure = failure == null ? ex : failure;
			}
		}
		long elapsedNs = System.nanoTime() - startNs;
		pool.shutdown();
		if (failure != null) {
			throw failure;
		}
		for (TraceTestHarness harness : nodeHarnesses) {
			harness.drainExecutors();
		}

		TimingTraceStore.Snapshot merged = timing ? mergeTiming(nodeHarnesses) : null;
		int batches = 0;
		int retries = 0;
		for (TraceTestHarness harness : nodeHarnesses) {
			InstrumentedTraceStore store = storeOf(harness);
			if (path.equals("single")) {
				retries += store.receiptDuplicates();
			} else {
				retries += store.batchDuplicates();
				batches += store.batchCalls() - store.batchDuplicates() - store.batchFailures();
			}
		}
		if (path.equals("single")) {
			batches = chains * records;
		}
		List<Long> allBatchDurations = new ArrayList<>();
		for (List<Long> calls : batchDurations) {
			allBatchDurations.addAll(calls);
		}
		Result result = new Result(nodes, chains, records, elapsedNs, durations, merged, batches, retries,
				allBatchDurations, exhausted.get());
		if (!path.equals("single")) {
			result.retryProfile = retryProfile(nodeHarnesses);
		}
		return result;
	}

	private static void applyRangeSettings(TraceTestHarness harness) {
		String limit = System.getProperty("lock.benchmark.range-retry-limit");
		String base = System.getProperty("lock.benchmark.range-backoff-ms");
		String max = System.getProperty("lock.benchmark.range-max-backoff-ms");
		if (limit != null && !limit.trim().isEmpty()) {
			harness.traceConfiguration().setRangeAllocationRetryLimit(Integer.parseInt(limit.trim()));
		}
		if (base != null && !base.trim().isEmpty()) {
			harness.traceConfiguration().setRangeAllocationBackoffMs(Integer.parseInt(base.trim()));
		}
		if (max != null && !max.trim().isEmpty()) {
			harness.traceConfiguration().setRangeAllocationMaxBackoffMs(Integer.parseInt(max.trim()));
		}
	}

	/** Committed batches and abandoned ranges by retry count, summed over the nodes, plus fallback record inserts. */
	private static String retryProfile(List<TraceTestHarness> nodeHarnesses) {
		Map<Integer, Integer> committed = new TreeMap<>();
		Map<Integer, Integer> abandoned = new TreeMap<>();
		int fallbackRecordInserts = 0;
		for (TraceTestHarness harness : nodeHarnesses) {
			InstrumentedTraceStore store = storeOf(harness);
			store.committedAfterRetries().forEach((k, v) -> committed.merge(k, v, Integer::sum));
			store.abandonedAfterRetries().forEach((k, v) -> abandoned.merge(k, v, Integer::sum));
			fallbackRecordInserts += store.recordInserts();
		}
		int abandonedTotal = abandoned.values().stream().mapToInt(Integer::intValue).sum();
		TraceTestHarness first = nodeHarnesses.get(0);
		return String.format(Locale.ROOT,
				"retry profile: rangeAllocationRetryLimit=%d backoffMs=%d maxBackoffMs=%d committed_after_k_retries=%s abandoned_after_k_retries=%s fallback_batches=%d fallback_record_inserts=%d",
				first.traceConfiguration().getRangeAllocationRetryLimit(),
				first.traceConfiguration().getRangeAllocationBackoffMs(),
				first.traceConfiguration().getRangeAllocationMaxBackoffMs(), committed, abandoned, abandonedTotal,
				fallbackRecordInserts);
	}

	/**
	 * {@code receipt_allocation_exhausted}: the submission lost {@code receiptAllocationRetryLimit}
	 * position races in a row and failed closed. The benchmark counts it and goes on; every other
	 * failure aborts the run.
	 */
	private static boolean isExhausted(RuntimeException error) {
		return error instanceof TraceStorageException
				&& "receipt_allocation_exhausted".equals(((TraceStorageException) error).getReason());
	}

	/** The timing tables of all nodes added up: same methods, summed calls and nanos. */
	private static TimingTraceStore.Snapshot mergeTiming(List<TraceTestHarness> nodeHarnesses) {
		List<TimingTraceStore.Snapshot> snapshots = new ArrayList<>();
		for (TraceTestHarness harness : nodeHarnesses) {
			snapshots.add(storeOf(harness).snapshot());
		}
		return TimingTraceStore.Snapshot.sum(snapshots);
	}

	private static String ingestPath() {
		String path = System.getProperty("lock.benchmark.path", "single").trim();
		if (!Arrays.asList("single", "single-as-batch", "bulk").contains(path)) {
			throw new IllegalArgumentException("lock.benchmark.path must be single, single-as-batch or bulk: " + path);
		}
		return path;
	}

	private static String pathColumn() {
		String path = ingestPath();
		String column = path.equals("bulk") ? "bulk-" + intProperty("lock.benchmark.bulk-size", 100) : path;
		int verifyThreads = intProperty("lock.benchmark.verify-threads", 0);
		int postCommitThreads = intProperty("lock.benchmark.post-commit-threads", 0);
		return verifyThreads + postCommitThreads == 0 ? column
				: column + "-v" + verifyThreads + "-p" + postCommitThreads;
	}

	private static int ingestBulk(TraceTestHarness harness, List<String> jsons) {
		return ingestBulk(harness, jsons, domain);
	}

	/** @return the elements that failed with {@code receipt_allocation_exhausted}; any other failure is thrown */
	private static int ingestBulk(TraceTestHarness harness, List<String> jsons, String domainId) {
		List<BatchElement> elements = new ArrayList<>(jsons.size());
		for (int i = 0; i < jsons.size(); i++) {
			elements.add(new BatchElement(i, jsons.get(i).getBytes(StandardCharsets.UTF_8)));
		}
		int exhausted = 0;
		for (BatchOutcome outcome : harness.ingestionService().ingestBatch(elements, harness.ctx(domainId))) {
			if (!outcome.isAccepted()) {
				if (!isExhausted(outcome.getError())) {
					throw outcome.getError();
				}
				exhausted++;
			}
		}
		return exhausted;
	}

	private static List<String> buildChain(KeyPair key, String producer, String kid, String instance, String chain, int count) {
		return TraceRecordBuilder.linkedChain(key, producer, kid, instance, chain, count);
	}

	private static void report(Result r) throws IOException {
		String line = String.format(Locale.ROOT, "%s,%d,%d,%d,%.2f,%.1f,%.1f,%.1f,%.2f,%.2f,%s,%s,%d,%d,%d,%s,%s,%d", Instant.now(),
				r.chains, r.records, r.chains * r.records, r.elapsedNs / 1e9, r.recordsPerSecond(), r.firstQuarterRate(),
				r.lastQuarterRate(), r.percentileMs(50), r.percentileMs(95), timingColumns(r), pathColumn(), r.nodes,
				r.batches, r.retries, r.batchPercentileMs(50), r.batchPercentileMs(95), r.exhausted);
		LOG.info("TRACE ingest benchmark: {}", line);
		System.out.println("TRACE ingest benchmark [" + CSV_HEADER + "]\n  " + line);
		if (r.retryProfile != null) {
			LOG.info("TRACE ingest benchmark {}", r.retryProfile);
			System.out.println("  " + r.retryProfile);
		}
		if (r.timing != null) {
			System.out.print(timingTable(r));
		}

		Path csv = Paths.get("target", "benchmark", "trace-ingest.csv");
		Files.createDirectories(csv.getParent());
		prepareCsv(csv);
		Files.write(csv, (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
	}

	/**
	 * Makes sure {@code csv} starts with the current header. A file with an older header, which is a prefix
	 * of the current one, is rewritten with empty values for the new columns. A file with any other header is
	 * moved aside, so rows and header always match.
	 */
	private static void prepareCsv(Path csv) throws IOException {
		if (!Files.exists(csv)) {
			Files.write(csv, (CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			return;
		}
		List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
		String header = lines.isEmpty() ? "" : lines.get(0);
		if (header.equals(CSV_HEADER)) {
			return;
		}
		if (!header.isEmpty() && CSV_HEADER.startsWith(header + ",")) {
			int missing = CSV_HEADER.split(",").length - header.split(",").length;
			StringBuilder padding = new StringBuilder();
			for (int i = 0; i < missing; i++) {
				padding.append(',');
			}
			List<String> migrated = new ArrayList<>(lines.size());
			migrated.add(CSV_HEADER);
			for (String row : lines.subList(1, lines.size())) {
				migrated.add(row.isEmpty() ? row : row + padding);
			}
			Files.write(csv, migrated, StandardCharsets.UTF_8);
			return;
		}
		Path aside = csv.resolveSibling("trace-ingest-" + System.currentTimeMillis() + ".csv");
		Files.move(csv, aside);
		LOG.warn("{} has an unknown header, moved to {}", csv, aside);
		Files.write(csv, (CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
	}

	private static String timingColumns(Result r) {
		if (r.timing == null) {
			return ",,";
		}
		double total = r.chains * (double) r.records;
		return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", r.timing.totalCalls() / total, r.timing.writeCalls() / total,
				r.timing.readCalls() / total);
	}

	/** Share is of thread wall time: elapsed time times the number of chains, which is the elapsed time for one chain. */
	private static String timingTable(Result r) {
		double records = r.chains * (double) r.records;
		double threadWallNs = (double) r.elapsedNs * r.chains;
		StringBuilder out = new StringBuilder();
		out.append(String.format("Store timing, %d node(s), %d chain(s), %d records%n", r.nodes, r.chains, r.chains * r.records));
		out.append(String.format("  %-38s %-5s %9s %12s %11s %9s %7s%n", "method", "type", "calls", "calls/record", "total ms",
				"mean ms", "share"));
		for (TimingTraceStore.MethodStats stats : r.timing.methods()) {
			out.append(String.format("  %-38s %-5s %9d %12.2f %11.1f %9.3f %6.1f%%%n", stats.method(),
					stats.write() ? "write" : "read", stats.calls(), stats.calls() / records, stats.totalNanos() / 1e6,
					stats.totalNanos() / 1e6 / stats.calls(), 100.0 * stats.totalNanos() / threadWallNs));
		}
		out.append(String.format("  store calls per record %.2f, writes per record %.2f, reads per record %.2f, "
				+ "store time %.1f%% of thread wall time%n", r.timing.totalCalls() / records, r.timing.writeCalls() / records,
				r.timing.readCalls() / records, 100.0 * r.timing.totalNanos() / threadWallNs));
		return out.toString();
	}

	// -- adjacent positions on two nodes ----------------------------------------------------------

	/**
	 * Two nodes ingest adjacent positions of one chain at the same time, on every path, and the final
	 * flags are compared with a sequential ingest of the same records (see the class Javadoc).
	 */
	@Test
	void adjacentPositionsAcrossNodes() throws Exception {
		int rounds = intProperty("lock.benchmark.adjacent", 0);
		Assumptions.assumeTrue(rounds > 0, "lock.benchmark.adjacent is not set");
		int verifyThreads = intProperty("lock.benchmark.verify-threads", 0);
		int postCommitThreads = intProperty("lock.benchmark.post-commit-threads", 0);

		String[][] variants = { { "single", "1" }, { "single-as-batch", "1" }, { "bulk", "1" }, { "bulk", "10" } };
		for (String[] variant : variants) {
			String path = variant[0];
			int block = Integer.parseInt(variant[1]);
			String concurrentDomain = DatabaseProfile.newDomain("adj");
			String sequentialDomain = DatabaseProfile.newDomain("adjseq");
			extraDomains.add(concurrentDomain);
			extraDomains.add(sequentialDomain);

			TraceTestHarness nodeA = newNode("adj-" + path, 0);
			TraceTestHarness nodeB = newNode("adj-" + path, 1);
			TraceTestHarness sequential = newNode("adjseq-" + path, 0);
			for (TraceTestHarness harness : Arrays.asList(nodeA, nodeB)) {
				if (path.equals("single-as-batch")) {
					harness.useSingleAsBatchPath();
				}
				harness.traceConfiguration().setBatchVerifyThreads(verifyThreads);
				harness.traceConfiguration().setPostCommitCorrelationThreads(postCommitThreads);
			}

			String producer = "bench-adj-producer";
			String kid = producer + "-kid";
			String instance = "bench-adj-instance";
			String chain = "bench-adj-chain";
			KeyPair key = nodeA.registerKey(concurrentDomain, producer, kid);
			nodeA.registerChain(concurrentDomain, producer, instance, chain);
			sequential.registerKey(sequentialDomain, producer, kid, key);
			sequential.registerChain(sequentialDomain, producer, instance, chain);
			int total = rounds * 2 * block;
			List<String> jsons = buildChain(key, producer, kid, instance, chain, total);

			ExecutorService pool = Executors.newFixedThreadPool(2);
			long startNs = System.nanoTime();
			for (int round = 0; round < rounds; round++) {
				CyclicBarrier barrier = new CyclicBarrier(2);
				List<String> partA = jsons.subList(round * 2 * block, round * 2 * block + block);
				List<String> partB = jsons.subList(round * 2 * block + block, (round + 1) * 2 * block);
				Future<?> a = pool.submit(() -> ingestPart(nodeA, path, partA, concurrentDomain, barrier));
				Future<?> b = pool.submit(() -> ingestPart(nodeB, path, partB, concurrentDomain, barrier));
				a.get();
				b.get();
			}
			long elapsedNs = System.nanoTime() - startNs;
			pool.shutdown();
			nodeA.drainExecutors();
			nodeB.drainExecutors();

			for (String json : jsons) {
				sequential.ingest(json, sequential.ctx(sequentialDomain));
			}

			int stale = 0;
			List<String> examples = new ArrayList<>();
			for (int i = 0; i < jsons.size(); i++) {
				String recordId = MAPPER.readTree(jsons.get(i)).path("record_id").asText();
				StoredTraceRecord concurrent = nodeA.traceStore()
						.findRecord(new RecordIdentity(concurrentDomain, producer, recordId)).orElse(null);
				StoredTraceRecord expected = sequential.traceStore()
						.findRecord(new RecordIdentity(sequentialDomain, producer, recordId)).orElse(null);
				assertTrue(concurrent != null && expected != null, "record " + (i + 1) + " is missing");
				if (!flagsOf(concurrent).equals(flagsOf(expected))) {
					stale++;
					if (examples.size() < 10) {
						examples.add("seq " + (i + 1) + " concurrent " + flagsOf(concurrent) + " sequential " + flagsOf(expected));
					}
				}
			}
			int retries = storeOf(nodeA).batchDuplicates() + storeOf(nodeB).batchDuplicates()
					+ storeOf(nodeA).receiptDuplicates() + storeOf(nodeB).receiptDuplicates();
			DomainAudit audit = new DomainAudit(nodeA.traceStore(), entryManager, concurrentDomain).run(total, true,
					nodeA.receiptChain());
			String line = String.format(Locale.ROOT,
					"adjacent positions: path=%s block=%d rounds=%d records=%d seconds=%.2f retries=%d stale_flags=%d audit_ok=%s examples=%s",
					path, block, rounds, total, elapsedNs / 1e9, retries, stale, audit.ok(), examples);
			LOG.info("TRACE ingest benchmark {}", line);
			System.out.println("  " + line);
			assertTrue(audit.ok(), () -> "domain audit failed: " + audit);
		}
	}

	private static Void ingestPart(TraceTestHarness harness, String path, List<String> jsons, String domainId,
			CyclicBarrier barrier) throws Exception {
		barrier.await();
		if (path.equals("bulk")) {
			ingestBulk(harness, jsons, domainId);
			return null;
		}
		for (String json : jsons) {
			AcceptanceResult result = harness.ingest(json, harness.ctx(domainId));
			assertTrue(result.isAccepted());
		}
		return null;
	}

	private static String flagsOf(StoredTraceRecord record) {
		return "gap=" + record.getFlags().isCoverageGap() + ",link=" + record.getFlags().isChainLinkFailure() + ",equiv="
				+ record.getFlags().isEquivocation();
	}

	private static final class Result {

		final int nodes;
		final int chains;
		final int records;
		final long elapsedNs;
		final List<long[]> durationsNs;
		final TimingTraceStore.Snapshot timing;
		final int batches;
		final int retries;
		final List<Long> batchDurationsNs;
		final int exhausted;
		String retryProfile;

		Result(int nodes, int chains, int records, long elapsedNs, List<long[]> durationsNs, TimingTraceStore.Snapshot timing,
				int batches, int retries, List<Long> batchDurationsNs, int exhausted) {
			this.exhausted = exhausted;
			this.nodes = nodes;
			this.timing = timing;
			this.chains = chains;
			this.records = records;
			this.elapsedNs = elapsedNs;
			this.durationsNs = durationsNs;
			this.batches = batches;
			this.retries = retries;
			this.batchDurationsNs = batchDurationsNs;
		}

		/** Accepted records per second: submissions that failed closed are not counted. */
		double recordsPerSecond() {
			return (chains * (double) records - exhausted) / (elapsedNs / 1e9);
		}

		/** Records/s of one chain over its first quarter, averaged across chains. */
		double firstQuarterRate() {
			return quarterRate(0);
		}

		double lastQuarterRate() {
			return quarterRate(records - Math.max(1, records / 4));
		}

		private double quarterRate(int from) {
			int span = Math.max(1, records / 4);
			double sumRates = 0;
			for (long[] durations : durationsNs) {
				long total = 0;
				for (int i = from; i < from + span; i++) {
					total += durations[i];
				}
				sumRates += span / (total / 1e9);
			}
			return sumRates;
		}

		double percentileMs(int percentile) {
			List<Long> all = new ArrayList<>();
			for (long[] durations : durationsNs) {
				for (long duration : durations) {
					all.add(duration);
				}
			}
			return percentileMs(all, percentile);
		}

		/** @return the percentile of one bulk call, or an empty column on the other paths */
		String batchPercentileMs(int percentile) {
			if (batchDurationsNs.isEmpty()) {
				return "";
			}
			return String.format(Locale.ROOT, "%.2f", percentileMs(new ArrayList<>(batchDurationsNs), percentile));
		}

		private static double percentileMs(List<Long> all, int percentile) {
			all.sort(null);
			return all.get(Math.min(all.size() - 1, all.size() * percentile / 100)) / 1e6;
		}
	}

}
