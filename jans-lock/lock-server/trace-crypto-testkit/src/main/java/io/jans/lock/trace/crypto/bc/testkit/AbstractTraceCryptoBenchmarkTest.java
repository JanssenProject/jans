/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.bc.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceHashes;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import io.jans.lock.trace.crypto.TraceVerificationKey;

/**
 * Multi-threaded throughput benchmark of a {@link TraceCryptoProvider}. A subclass supplies the
 * provider; the workload, the thread pool, the measurement and the report live here, so the
 * {@code JAVA} and {@code NATIVE} runs are measured the same way.
 *
 * <p>Every operation runs in a tight loop on several threads sharing one provider, one key and one
 * record. A warm-up phase precedes the measured phase. The report gives the throughput, the mean
 * latency and the p50/p95/p99/max latency, and is printed and appended to
 * {@code target/trace-crypto-bench.csv}.
 *
 * <p>The benchmark is opt-in so it does not slow regular builds. Run it with
 * {@code -Dtrace.bench=true}. Tuning, all optional:
 * <ul>
 * <li>{@code -Dtrace.bench.threads=4} worker threads</li>
 * <li>{@code -Dtrace.bench.warmupMs=2000} warm-up time per operation</li>
 * <li>{@code -Dtrace.bench.durationMs=5000} measured time per operation</li>
 * </ul>
 */
public abstract class AbstractTraceCryptoBenchmarkTest {

	private static final String ENABLED_PROPERTY = "trace.bench";

	private static final String RECORD_RESOURCE = "trace/bench/signed_record.json";

	private static final String CSV_FILE = "target/trace-crypto-bench.csv";

	/** Samples kept per thread; operations beyond the cap still count, they are not timed. */
	private static final int MAX_SAMPLES_PER_THREAD = 2_000_000;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String RECEIPT_DIGEST = "sha256:" + "ab".repeat(32);

	private static final String RECEIPT_PREV = "sha256:" + "1f".repeat(32);

	private final int threads = Integer.getInteger("trace.bench.threads", 4);

	private final long warmupMs = Long.getLong("trace.bench.warmupMs", 2_000L);

	private final long durationMs = Long.getLong("trace.bench.durationMs", 5_000L);

	private TraceCryptoProvider provider;

	private TraceVerificationKey key;

	private TraceSignedRecord record;

	private byte[] signature;

	private JsonNode canonicalizeInput;

	private String expectedDigest;

	private String expectedCanonical;

	private String expectedThumbprint;

	private Map<String, String> jwk;

	/**
	 * @return the provider under test, ready for use
	 */
	protected abstract TraceCryptoProvider createProvider();

	/**
	 * @return the label used in the report, e.g. {@code JAVA}
	 */
	protected abstract String label();

	@BeforeAll
	static void enabled() {
		Assumptions.assumeTrue(Boolean.getBoolean(ENABLED_PROPERTY),
				"benchmark is opt-in: run with -D" + ENABLED_PROPERTY + "=true");
	}

	@BeforeEach
	void setUpFixture() throws IOException {
		assertTrue(threads > 0 && durationMs > 0 && warmupMs >= 0, "invalid benchmark settings");

		provider = createProvider();
		assertTrue(provider.isAvailable(), provider.describe());

		KeyPair keyPair = TraceTestKeys.generateKeyPair();
		jwk = TraceTestKeys.toJwk(keyPair.getPublic());
		key = provider.parseKey(jwk);

		ObjectNode unsigned = (ObjectNode) MAPPER.readTree(readResource(RECORD_RESOURCE));
		unsigned.remove("signature");
		signature = TraceTestKeys.sign(keyPair.getPrivate(),
				provider.canonicalize(unsigned).getBytes(StandardCharsets.UTF_8));
		ObjectNode root = unsigned.deepCopy();
		root.put("signature", StrictBase64Url.encode(signature));
		record = new TraceSignedRecord(root.toString(), root, root.get("producer").asText(),
				root.get("kid").asText());

		canonicalizeInput = root;
		expectedDigest = provider.verifyAndDigest(record, signature, key);
		expectedCanonical = provider.canonicalize(canonicalizeInput);
		expectedThumbprint = key.thumbprint();
	}

	@Test
	void benchmarkVerifyAndDigest() {
		measure("verifyAndDigest", worker -> () -> provider.verifyAndDigest(record, signature, key),
				expectedDigest::equals);
	}

	@Test
	void benchmarkCanonicalize() {
		measure("canonicalize", worker -> () -> provider.canonicalize(canonicalizeInput), expectedCanonical::equals);
	}

	@Test
	void benchmarkParseKey() {
		measure("parseKey", worker -> () -> provider.parseKey(jwk).thumbprint(), expectedThumbprint::equals);
	}

	@Test
	void benchmarkReceiptHash() {
		measure("receiptHash", worker -> new Operation() {
			private long sequence = worker * 1_000_000_000L;

			@Override
			public String run() {
				sequence++;
				return provider.receiptHash("domain-1", sequence, 1_800_000_000_000L + sequence, "producer-1",
						"record-" + sequence, RECEIPT_DIGEST, sequence == 1 ? TraceHashes.ZERO_HASH : RECEIPT_PREV);
			}
		}, result -> result.startsWith("sha256:") && result.length() == 71);
	}

	// -- engine -------------------------------------------------------------------------------------

	@FunctionalInterface
	private interface Operation {

		String run();

	}

	@FunctionalInterface
	private interface ResultCheck {

		boolean accept(String result);

	}

	private void measure(String name, IntFunction<Operation> factory, ResultCheck check) {
		Result warmup = runPhase(factory, check, warmupMs, false);
		assertNull(warmup.failure, () -> name + " failed during warm-up: " + warmup.failure);

		Result result = runPhase(factory, check, durationMs, true);
		assertNull(result.failure, () -> name + " failed: " + result.failure);
		assertEquals(0L, result.wrongResults, name + " returned a wrong result");
		assertTrue(result.operations > 0, name + " completed no operation");

		report(name, result);
	}

	private Result runPhase(IntFunction<Operation> factory, ResultCheck check, long phaseMs, boolean record) {
		if (phaseMs <= 0) {
			return new Result(0, 0, 0, new long[0], null, new long[threads]);
		}

		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch go = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Worker[] workers = new Worker[threads];
		Thread[] pool = new Thread[threads];
		long phaseNanos = phaseMs * 1_000_000L;

		for (int i = 0; i < threads; i++) {
			workers[i] = new Worker(factory.apply(i), check, record, phaseNanos, ready, go, failure);
			pool[i] = new Thread(workers[i], "trace-bench-" + i);
			pool[i].start();
		}

		try {
			ready.await();
			long start = System.nanoTime();
			go.countDown();
			for (Thread thread : pool) {
				thread.join();
			}
			long elapsed = System.nanoTime() - start;
			return collect(workers, elapsed, failure.get());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("benchmark interrupted", ex);
		}
	}

	private Result collect(Worker[] workers, long elapsedNanos, Throwable failure) {
		long operations = 0;
		long wrong = 0;
		int sampled = 0;
		long[] perThread = new long[workers.length];
		for (int i = 0; i < workers.length; i++) {
			operations += workers[i].operations;
			wrong += workers[i].wrongResults;
			sampled += workers[i].sampleCount;
			perThread[i] = workers[i].operations;
		}
		long[] samples = new long[sampled];
		int offset = 0;
		for (Worker worker : workers) {
			System.arraycopy(worker.samples, 0, samples, offset, worker.sampleCount);
			offset += worker.sampleCount;
		}
		Arrays.sort(samples);
		return new Result(operations, wrong, elapsedNanos, samples, failure, perThread);
	}

	private void report(String name, Result result) {
		double seconds = result.elapsedNanos / 1e9;
		double opsPerSecond = result.operations / seconds;
		double meanMicros = (result.elapsedNanos * (double) threads / result.operations) / 1_000.0;

		String line = String.format(
				"TRACE-BENCH provider=%s (%s) op=%s threads=%d warmupMs=%d durationMs=%d ops=%d ops/s=%.0f "
						+ "meanUs=%.2f p50Us=%.2f p95Us=%.2f p99Us=%.2f maxUs=%.2f perThreadOps=%s",
				label(), provider.describe(), name, threads, warmupMs, durationMs, result.operations, opsPerSecond,
				meanMicros, percentile(result.samples, 0.50), percentile(result.samples, 0.95),
				percentile(result.samples, 0.99), percentile(result.samples, 1.0), Arrays.toString(result.perThread));
		System.out.println(line);

		appendCsv(String.format("%s,%s,%d,%d,%d,%.0f,%.2f,%.2f,%.2f,%.2f,%.2f", label(), name, threads, durationMs,
				result.operations, opsPerSecond, meanMicros, percentile(result.samples, 0.50),
				percentile(result.samples, 0.95), percentile(result.samples, 0.99), percentile(result.samples, 1.0)));
	}

	private static double percentile(long[] sorted, double fraction) {
		if (sorted.length == 0) {
			return Double.NaN;
		}
		int index = (int) Math.min(sorted.length - 1, Math.max(0, Math.ceil(fraction * sorted.length) - 1));
		return sorted[index] / 1_000.0;
	}

	private static synchronized void appendCsv(String row) {
		try {
			Path file = Paths.get(CSV_FILE);
			Files.createDirectories(file.toAbsolutePath().getParent());
			List<String> lines = new ArrayList<>();
			if (!Files.exists(file)) {
				lines.add("provider,op,threads,durationMs,ops,opsPerSec,meanUs,p50Us,p95Us,p99Us,maxUs");
			}
			lines.add(row);
			Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException ex) {
			System.out.println("TRACE-BENCH could not write " + CSV_FILE + ": " + ex);
		}
	}

	private String readResource(String resource) throws IOException {
		try (InputStream in = AbstractTraceCryptoBenchmarkTest.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalArgumentException("Resource not found: " + resource);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static final class Worker implements Runnable {

		private final Operation operation;

		private final ResultCheck check;

		private final boolean record;

		private final long phaseNanos;

		private final CountDownLatch ready;

		private final CountDownLatch go;

		private final AtomicReference<Throwable> failure;

		private long[] samples = new long[0];

		private int sampleCount;

		private long operations;

		private long wrongResults;

		Worker(Operation operation, ResultCheck check, boolean record, long phaseNanos, CountDownLatch ready,
				CountDownLatch go, AtomicReference<Throwable> failure) {
			this.operation = operation;
			this.check = check;
			this.record = record;
			this.phaseNanos = phaseNanos;
			this.ready = ready;
			this.go = go;
			this.failure = failure;
			if (record) {
				this.samples = new long[1 << 16];
			}
		}

		@Override
		public void run() {
			ready.countDown();
			try {
				go.await();
				long start = System.nanoTime();
				long deadline = start + phaseNanos;
				while (failure.get() == null) {
					long before = System.nanoTime();
					if (before >= deadline) {
						break;
					}
					String result = operation.run();
					long took = System.nanoTime() - before;
					operations++;
					if (!check.accept(result)) {
						wrongResults++;
					}
					if (record && sampleCount < MAX_SAMPLES_PER_THREAD) {
						if (sampleCount == samples.length) {
							samples = Arrays.copyOf(samples, Math.min(MAX_SAMPLES_PER_THREAD, samples.length * 2));
						}
						samples[sampleCount++] = took;
					}
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				failure.compareAndSet(null, ex);
			} catch (Throwable t) {
				failure.compareAndSet(null, t);
			}
		}

	}

	private static final class Result {

		private final long operations;

		private final long wrongResults;

		private final long elapsedNanos;

		private final long[] samples;

		private final Throwable failure;

		private final long[] perThread;

		Result(long operations, long wrongResults, long elapsedNanos, long[] samples, Throwable failure,
				long[] perThread) {
			this.operations = operations;
			this.wrongResults = wrongResults;
			this.elapsedNanos = elapsedNanos;
			this.samples = samples;
			this.failure = failure;
			this.perThread = perThread;
		}

	}

}
