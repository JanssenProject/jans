/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

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
 * {@code lock.benchmark.preload} (committed receipts inserted into the domain before the run, default 0;
 * use it, for example 20000, to expose work which grows with the number of receipts in a domain),
 * {@code lock.benchmark.min-rate} (fail when one-chain records/s is lower, default off),
 * {@code lock.benchmark.max-slowdown} (fail when the last quarter of a one-chain run is more than
 * this many times slower than the first quarter, default off; it flags work which grows with the
 * receipt table, like {@code count(*)}). Results are printed and appended to
 * {@code target/benchmark/trace-ingest.csv}. All data uses a throw-away domain and is removed afterwards.
 *
 * @author Yuriy Movchan
 */
@EnabledIfSystemProperty(named = "lock.benchmark.profile", matches = ".+")
class TraceIngestBenchmark {

	private static final Logger LOG = LoggerFactory.getLogger(TraceIngestBenchmark.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String CSV_HEADER = "timestamp,chains,records_per_chain,total_records,seconds,records_per_s,first_quarter_per_s,last_quarter_per_s,p50_ms,p95_ms";

	private static PersistenceEntryManager entryManager;

	private static String domain;

	@BeforeAll
	static void connect() throws Exception {
		entryManager = createEntryManager(Paths.get(System.getProperty("lock.benchmark.profile")));
		domain = "bench-" + UUID.randomUUID().toString().substring(0, 8);
	}

	@AfterAll
	static void cleanup() {
		if (entryManager == null) {
			return;
		}
		try {
			Filter filter = Filter.createEqualityFilter("jansTraceDomainId", domain);
			String base = OrmTraceStores.BASE_DN;
			entryManager.remove("ou=records," + base, TraceRecordEntry.class, filter, 1_000_000);
			entryManager.remove("ou=receipts," + base, TraceReceiptEntry.class, filter, 1_000_000);
			entryManager.remove("ou=chains," + base, TraceChainEntry.class, filter, 1_000_000);
			entryManager.remove("ou=keys," + base, TraceProducerKeyEntry.class, filter, 1_000_000);
		} finally {
			entryManager.destroy();
		}
	}

	@Test
	void ingestThroughput() throws Exception {
		int records = Integer.getInteger("lock.benchmark.records", 1000);
		List<String> chainCounts = Arrays.asList(System.getProperty("lock.benchmark.chains", "1,4").split(","));
		double minRate = Double.parseDouble(System.getProperty("lock.benchmark.min-rate", "0"));
		double maxSlowdown = Double.parseDouble(System.getProperty("lock.benchmark.max-slowdown", "0"));

		preloadReceipts(Integer.getInteger("lock.benchmark.preload", 0));

		int run = 0;
		for (String chainCount : chainCounts) {
			Result result = runScenario(Integer.parseInt(chainCount.trim()), records, run++);
			report(result);
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

	private static Result runScenario(int chains, int records, int run) throws Exception {
		TraceTestHarness harness = new TraceTestHarness(TraceTestCrypto.javaProvider(),
				keys -> OrmTraceStores.create(entryManager, keys));

		List<List<String>> chainJsons = new ArrayList<>();
		for (int c = 0; c < chains; c++) {
			String suffix = run + "-" + c;
			String producer = "bench-producer-" + suffix;
			String kid = producer + "-kid";
			String instance = "bench-instance-" + suffix;
			String chain = "bench-chain-" + suffix;
			KeyPair key = harness.registerKey(domain, producer, kid);
			harness.registerChain(domain, producer, instance, chain);
			chainJsons.add(buildChain(key, producer, kid, instance, chain, records));
		}

		ExecutorService pool = Executors.newFixedThreadPool(chains);
		List<Future<long[]>> futures = new ArrayList<>();
		long startNs = System.nanoTime();
		for (List<String> jsons : chainJsons) {
			futures.add(pool.submit(() -> {
				long[] durationsNs = new long[jsons.size()];
				for (int i = 0; i < jsons.size(); i++) {
					long begin = System.nanoTime();
					harness.ingest(jsons.get(i), harness.ctx(domain));
					durationsNs[i] = System.nanoTime() - begin;
				}
				return durationsNs;
			}));
		}
		List<long[]> durations = new ArrayList<>();
		for (Future<long[]> future : futures) {
			durations.add(future.get());
		}
		long elapsedNs = System.nanoTime() - startNs;
		pool.shutdown();

		return new Result(chains, records, elapsedNs, durations);
	}

	private static List<String> buildChain(KeyPair key, String producer, String kid, String instance, String chain, int count) {
		List<String> jsons = new ArrayList<>(count);
		String prevHash = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= count; seq++) {
			String json = TraceRecordBuilder.authorizationDecision().producer(producer).kid(kid).producerInstanceId(instance)
					.producerChainId(chain).sequenceNumber(seq).prevRecordHash(prevHash).sign(key.getPrivate());
			jsons.add(json);
			try {
				prevHash = CanonicalHashes.sha256PrefixedOfJcs(MAPPER.readTree(json));
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return jsons;
	}

	private static void report(Result r) throws IOException {
		String line = String.format("%s,%d,%d,%d,%.2f,%.1f,%.1f,%.1f,%.2f,%.2f", Instant.now(), r.chains, r.records,
				r.chains * r.records, r.elapsedNs / 1e9, r.recordsPerSecond(), r.firstQuarterRate(), r.lastQuarterRate(),
				r.percentileMs(50), r.percentileMs(95));
		LOG.info("TRACE ingest benchmark: {}", line);
		System.out.println("TRACE ingest benchmark [" + CSV_HEADER + "]\n  " + line);

		Path csv = Paths.get("target", "benchmark", "trace-ingest.csv");
		Files.createDirectories(csv.getParent());
		if (!Files.exists(csv)) {
			Files.write(csv, (CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		}
		Files.write(csv, (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
	}

	private static PersistenceEntryManager createEntryManager(Path confDir) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(confDir.resolve("jans-sql.properties").toFile())) {
			properties.load(in);
		}
		Properties salt = new Properties();
		try (InputStream in = new FileInputStream(confDir.resolve("salt").toFile())) {
			salt.load(in);
		}

		String sslFactoryArg = properties.getProperty("connection.driver-property.sslfactoryarg");
		if ((sslFactoryArg != null) && sslFactoryArg.startsWith("file:") && !new File(sslFactoryArg.substring(5)).isAbsolute()) {
			properties.setProperty("connection.driver-property.sslfactoryarg",
					"file:" + confDir.resolve(sslFactoryArg.substring(5)).toAbsolutePath());
		}

		try {
			Properties decrypted = PropertiesDecrypter.decryptAllProperties(
					StringEncrypter.instance(salt.getProperty("encodeSalt").trim()), properties);
			// The factory expects keys the way the persistence configuration loader produces them: "<type>#<key>"
			Properties prefixed = new Properties();
			for (String name : decrypted.stringPropertyNames()) {
				prefixed.setProperty(SqlEntryManagerFactory.PERSISTENCE_TYPE + "#" + name, decrypted.getProperty(name));
			}
			return new SqlEntryManagerFactory().createEntryManager(prefixed);
		} catch (StringEncrypter.EncryptionException ex) {
			throw new IllegalStateException("Failed to decrypt database properties", ex);
		}
	}

	private static final class Result {

		final int chains;
		final int records;
		final long elapsedNs;
		final List<long[]> durationsNs;

		Result(int chains, int records, long elapsedNs, List<long[]> durationsNs) {
			this.chains = chains;
			this.records = records;
			this.elapsedNs = elapsedNs;
			this.durationsNs = durationsNs;
		}

		double recordsPerSecond() {
			return chains * (double) records / (elapsedNs / 1e9);
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
			all.sort(null);
			return all.get(Math.min(all.size() - 1, all.size() * percentile / 100)) / 1e6;
		}
	}

}
