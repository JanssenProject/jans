/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;

/**
 * HTTP load client for the TRACE ingest endpoints of one Lock node (task 13 of series 2). Opt-in: it
 * runs only with {@code -Dlock.load.url}, and its class name does not match the default surefire
 * includes, so it is started explicitly:
 *
 * <pre>
 * mvn -o -B -pl model,service -am -Ddownload.plugin.skip=true clean test -Dtest=TraceHttpLoad \
 *     -Dsurefire.failIfNoSpecifiedTests=false -Dlock.load.url=https://jans-dev.jans.info \
 *     -Djavax.net.ssl.trustStore=/path/stand.jks -Djavax.net.ssl.trustStorePassword=changeit \
 *     -Dlock.load.label=4/2 -Dlock.load.points=8x1x100,8x4x100 -Dlock.load.records=20000
 * </pre>
 *
 * <p>A point is {@code <chains>x<connections>x<bulk size>}. For every point the client registers fresh
 * producers (one producer, key and chain per chain) through the admin API, signs
 * {@code lock.load.records} linked records spread over the chains, and sends them from
 * {@code <connections>} threads over HTTP/1.1: bulk requests of one chain each, dispatched round robin
 * over the chains in chain order ({@code lock.load.mode=bulk}), or one record per request
 * ({@code lock.load.mode=single}, {@code POST /audit/trace}). Two requests of one chain can be in
 * flight at the same time when the connections outnumber the chains; the coverage-gap count shows it.
 * The first point is run once more before everything else as a warm-up (run 0, {@code lock.load.warmup}).
 *
 * <p>Measured per run: accepted / replay / rejected records (asserted to add up to the records sent),
 * error ids, request latency p50/p95, records/s (accepted records over the wall time of the run), and
 * from the VM sampler ({@link VmSampler}, {@code lock.load.ssh}) the CPU of the Lock JVM, PostgreSQL,
 * Apache and the whole VM, WAL syncs per second and the top waits. Results are printed and appended
 * to {@code target/load/trace-http-load.csv}.
 *
 * <p>The token comes from the client credentials grant of the test client, read from
 * {@code client/profiles/<lock.load.profile>/config-jans-lock-test-data.properties} (keys
 * {@code trace.client.id}, {@code trace.client.secret}) or the environment variables
 * {@code LOCK_TRACE_CLIENT_ID} / {@code LOCK_TRACE_CLIENT_SECRET}. Tokens and secrets are never logged.
 * TLS is verified by the JVM; for a self-signed stand pass a trust store.
 */
@EnabledIfSystemProperty(named = "lock.load.url", matches = ".+")
class TraceHttpLoad {

	static final String CSV_HEADER = "timestamp,label,mode,chains,connections,bulk_size,run,sent,accepted,replay,rejected,seconds,records_per_s,req_p50_ms,req_p95_ms,lock_cpu_avg,lock_cpu_p90,pg_cpu_avg,pg_cpu_p90,apache_cpu_avg,vm_cpu_avg,client_cpu_avg,wal_sync_per_s,top_waits,coverage_gap,replay_requests,replay_p50_ms,replay_p95_ms,plain_p50_ms,plain_p95_ms,errors";

	static final String SCOPE_WRITE = "https://jans.io/oauth/lock/trace.write";

	static final String SCOPE_ADMIN = "https://jans.io/oauth/lock/trace.admin";

	static final int MAX_BULK_REQUEST_BYTES = 4 * 1024 * 1024;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** One load point: chains, client connections, records per bulk request. */
	static final class Point {
		final int chains;
		final int connections;
		final int bulkSize;

		Point(int chains, int connections, int bulkSize) {
			if (chains <= 0 || connections <= 0 || bulkSize <= 0) {
				throw new IllegalArgumentException(
						"Point dimensions must be positive: " + chains + "x" + connections + "x" + bulkSize);
			}
			this.chains = chains;
			this.connections = connections;
			this.bulkSize = bulkSize;
		}

		static List<Point> parse(String spec) {
			List<Point> points = new ArrayList<>();
			for (String item : spec.split(",")) {
				String[] parts = item.trim().split("x");
				if (parts.length != 3) {
					throw new IllegalArgumentException("Point must be <chains>x<connections>x<bulk>: " + item);
				}
				points.add(new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
			}
			return points;
		}
	}

	/** One request: the records of one chain, in chain order. */
	static final class Slice {
		final int chain;
		final List<String> records;

		Slice(int chain, List<String> records) {
			this.chain = chain;
			this.records = records;
		}
	}

	private String baseUrl;

	private String lockApi;

	private HttpClient http;

	private String clientId;

	private String clientSecret;

	@Test
	void sweep() throws Exception {
		baseUrl = stripSlash(System.getProperty("lock.load.url"));
		lockApi = baseUrl + "/" + stripSlash(System.getProperty("lock.load.context", "jans-lock")) + "/api/v1/audit/trace";
		String label = System.getProperty("lock.load.label", "");
		boolean bulk = !"single".equalsIgnoreCase(System.getProperty("lock.load.mode", "bulk"));
		List<Point> points = Point.parse(System.getProperty("lock.load.points", "8x4x100"));
		int records = Integer.parseInt(System.getProperty("lock.load.records", "20000"));
		int runs = Integer.parseInt(System.getProperty("lock.load.runs", "2"));
		boolean warmup = Boolean.parseBoolean(System.getProperty("lock.load.warmup", "true"));
		String sshHost = System.getProperty("lock.load.ssh", "jans-dev");
		loadCredentials();

		TraceTestKeys.installProvider();
		http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10))
				.executor(Executors.newCachedThreadPool()).build();

		List<String> lines = new ArrayList<>();
		if (warmup) {
			lines.add(runPoint(label, bulk, points.get(0), records, 0, sshHost));
		}
		for (Point point : points) {
			for (int run = 1; run <= runs; run++) {
				lines.add(runPoint(label, bulk, point, records, run, sshHost));
			}
		}
		System.out.println("TRACE HTTP load [" + CSV_HEADER + "]");
		lines.forEach(line -> System.out.println("  " + line));
	}

	private String runPoint(String label, boolean bulk, Point point, int records, int run, String sshHost)
			throws Exception {
		String tag = "load13-" + Long.toString(System.currentTimeMillis(), 36) + "-"
				+ UUID.randomUUID().toString().substring(0, 4);
		String adminToken = token(SCOPE_ADMIN);
		List<List<String>> chains = new ArrayList<>();
		int perChain = records / point.chains;
		int remainder = records % point.chains;
		List<KeyPair> keys = new ArrayList<>();
		for (int c = 0; c < point.chains; c++) {
			KeyPair key = TraceTestKeys.generateKeyPair();
			register(adminToken, producer(tag, c), key);
			keys.add(key);
		}
		long signStart = System.nanoTime();
		chains.addAll(IntStream.range(0, point.chains).parallel()
				.mapToObj(c -> chain(keys.get(c), producer(tag, c), perChain + (c < remainder ? 1 : 0)))
				.collect(Collectors.toList()));
		double signSeconds = (System.nanoTime() - signStart) / 1e9;
		List<Slice> slices = slices(chains, bulk ? point.bulkSize : 1);
		String writeToken = token(SCOPE_WRITE);

		LoadTally tally = new LoadTally();
		AtomicInteger next = new AtomicInteger();
		ExecutorService workers = Executors.newFixedThreadPool(point.connections);
		try (VmSampler sampler = VmSampler.start(sshHost)) {
			sampler.awaitFirstSample(15_000);
			int firstSample = Math.max(0, sampler.sampleCount() - 1);
			long cpuStart = processCpuNs();
			long start = System.nanoTime();
			for (int w = 0; w < point.connections; w++) {
				workers.submit(() -> {
					int index;
					while ((index = next.getAndIncrement()) < slices.size()) {
						send(slices.get(index).records, bulk, writeToken, tally);
					}
					return null;
				});
			}
			workers.shutdown();
			assertTrue(workers.awaitTermination(2, TimeUnit.HOURS), "load run did not finish");
			double seconds = (System.nanoTime() - start) / 1e9;
			double clientCpu = (processCpuNs() - cpuStart) / 1e9 / seconds * 100.0;
			int samplesAtEnd = sampler.sampleCount();
			long deadline = System.currentTimeMillis() + 3_000;
			while (sampler.sampleCount() == samplesAtEnd && System.currentTimeMillis() < deadline) {
				Thread.sleep(50);
			}
			VmSampler.Summary vm = sampler.summarize(firstSample);

			assertEquals(records, tally.sent(), "records sent");
			assertTrue(tally.balanced(), "accepted + replay + rejected must equal sent");
			String line = csvLine(Instant.now().toString(), label, bulk ? "bulk" : "single", point, run, tally,
					seconds, vm, clientCpu);
			System.out.printf(Locale.ROOT,
					"load %s %s chains=%d connections=%d bulk=%d run=%d: sent=%d accepted=%d replay=%d rejected=%d %.1f rec/s p50=%.1f p95=%.1f ms lock=%.0f%% (p90 %.0f%%) pg=%.0f%% apache=%.0f%% vm=%.0f%% client=%.0f%% waits=[%s] errors=[%s] (signing %.1f s, %d sampler intervals)%n",
					label, bulk ? "bulk" : "single", point.chains, point.connections, point.bulkSize, run, tally.sent(),
					tally.accepted(), tally.replay(), tally.rejected(), tally.accepted() / seconds, tally.percentileMs(50),
					tally.percentileMs(95), vm.lockAvg, vm.lockP90, vm.pgAvg, vm.apacheAvg, vm.vmAvg, clientCpu,
					vm.topWaits, tally.errorSummary(), signSeconds, vm.intervals);
			appendCsv(line);
			return line;
		} finally {
			workers.shutdownNow();
		}
	}

	private void send(List<String> records, boolean bulk, String token, LoadTally tally) {
		String body = bulk ? bulkBody(records) : records.get(0);
		HttpRequest request = HttpRequest.newBuilder(URI.create(bulk ? lockApi + "/bulk" : lockApi))
				.timeout(Duration.ofMinutes(5)).header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + token)
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
		long start = System.nanoTime();
		try {
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			tally.add(response.statusCode(), response.body(), records.size(), bulk, System.nanoTime() - start);
		} catch (IOException ex) {
			tally.addFailure(ex.getClass().getSimpleName(), records.size(), System.nanoTime() - start);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			tally.addFailure("interrupted", records.size(), System.nanoTime() - start);
		}
	}

	// -- records and request bodies -------------------------------------------------------------------

	static String producer(String tag, int chain) {
		return tag + "-c" + chain;
	}

	/** @return {@code count} signed, linked records of one chain, signed now */
	static List<String> chain(KeyPair key, String producer, int count) {
		long signedAt = Instant.now().getEpochSecond();
		List<String> records = new ArrayList<>(count);
		String prevHash = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= count; seq++) {
			String json = TraceRecordBuilder.authorizationDecision().producer(producer).kid(producer + "-key")
					.producerInstanceId(producer + "-instance").producerChainId(producer + "-chain").sequenceNumber(seq)
					.prevRecordHash(prevHash).signedAt(signedAt).sign(key.getPrivate());
			records.add(json);
			try {
				prevHash = CanonicalHashes.sha256PrefixedOfJcs(MAPPER.readTree(json));
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return records;
	}

	/**
	 * @return the requests of a run: each chain cut into consecutive slices of {@code size} records,
	 *         ordered round robin over the chains (slice 0 of every chain, then slice 1, ...)
	 */
	static List<Slice> slices(List<List<String>> chains, int size) {
		List<List<Slice>> perChain = new ArrayList<>();
		int longest = 0;
		for (int c = 0; c < chains.size(); c++) {
			List<String> records = chains.get(c);
			List<Slice> slices = new ArrayList<>();
			for (int from = 0; from < records.size(); from += size) {
				slices.add(new Slice(c, records.subList(from, Math.min(records.size(), from + size))));
			}
			perChain.add(slices);
			longest = Math.max(longest, slices.size());
		}
		List<Slice> ordered = new ArrayList<>();
		for (int i = 0; i < longest; i++) {
			for (List<Slice> slices : perChain) {
				if (i < slices.size()) {
					ordered.add(slices.get(i));
				}
			}
		}
		return ordered;
	}

	/** @return the JSON array of the records, as sent to {@code POST /audit/trace/bulk} */
	static String bulkBody(List<String> records) {
		String body = "[" + String.join(",", records) + "]";
		if (body.getBytes(StandardCharsets.UTF_8).length > MAX_BULK_REQUEST_BYTES) {
			throw new IllegalArgumentException("bulk body exceeds maxBulkRequestBytes");
		}
		return body;
	}

	static String keyBody(String producer, KeyPair key, Instant validFrom) {
		ObjectNode body = MAPPER.createObjectNode();
		body.put("producer_id", producer);
		body.put("kid", producer + "-key");
		body.set("public_key_jwk", MAPPER.valueToTree(TraceTestKeys.toJwk(key.getPublic())));
		body.putArray("authorized_event_kinds").add("AUTHORIZATION_DECISION");
		body.put("valid_from", validFrom.truncatedTo(ChronoUnit.SECONDS).toString());
		return body.toString();
	}

	static String chainBody(String producer) {
		ObjectNode body = MAPPER.createObjectNode();
		body.put("producer_id", producer);
		body.put("producer_instance_id", producer + "-instance");
		body.put("producer_chain_id", producer + "-chain");
		return body.toString();
	}

	// -- CSV --------------------------------------------------------------------------------------------

	static String csvLine(String timestamp, String label, String mode, Point point, int run, LoadTally tally,
			double seconds, VmSampler.Summary vm, double clientCpu) {
		return String.join(",", timestamp, csv(label), mode, String.valueOf(point.chains),
				String.valueOf(point.connections), String.valueOf(point.bulkSize), String.valueOf(run),
				String.valueOf(tally.sent()), String.valueOf(tally.accepted()), String.valueOf(tally.replay()),
				String.valueOf(tally.rejected()), num(seconds, 3), num(tally.accepted() / seconds, 1),
				num(tally.percentileMs(50), 2), num(tally.percentileMs(95), 2), num(vm.lockAvg, 0), num(vm.lockP90, 0),
				num(vm.pgAvg, 0), num(vm.pgP90, 0), num(vm.apacheAvg, 0), num(vm.vmAvg, 0), num(clientCpu, 0),
				num(vm.walSyncPerS, 1), csv(vm.topWaits), String.valueOf(tally.coverageGap()),
				String.valueOf(tally.replayRequests()), num(tally.replayPercentileMs(50), 2),
				num(tally.replayPercentileMs(95), 2), num(tally.plainPercentileMs(50), 2),
				num(tally.plainPercentileMs(95), 2), csv(tally.errorSummary()));
	}

	private static String num(double value, int decimals) {
		return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	/** Quotes a CSV field that contains a comma or a quote. */
	static String csv(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private static void appendCsv(String line) throws IOException {
		Path csv = Paths.get("target", "load", "trace-http-load.csv");
		Files.createDirectories(csv.getParent());
		if (!Files.exists(csv)) {
			Files.write(csv, (CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		}
		Files.write(csv, (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
	}

	// -- OAuth and admin API ---------------------------------------------------------------------------

	private void loadCredentials() throws IOException {
		clientId = System.getenv("LOCK_TRACE_CLIENT_ID");
		clientSecret = System.getenv("LOCK_TRACE_CLIENT_SECRET");
		if (clientId == null || clientSecret == null) {
			String profile = System.getProperty("lock.load.profile", "jans-dev.jans.info");
			Path file = Paths.get("..", "client", "profiles", profile, "config-jans-lock-test-data.properties");
			Properties props = new Properties();
			try (InputStream in = Files.newInputStream(file)) {
				props.load(in);
			}
			clientId = clientId != null ? clientId : props.getProperty("trace.client.id");
			clientSecret = clientSecret != null ? clientSecret : props.getProperty("trace.client.secret");
		}
		if (clientId == null || clientSecret == null) {
			throw new IllegalStateException("trace.client.id / trace.client.secret not configured");
		}
	}

	private String token(String scope) throws IOException, InterruptedException {
		String basic = Base64.getEncoder()
				.encodeToString((enc(clientId) + ":" + enc(clientSecret)).getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/jans-auth/restv1/token"))
				.timeout(Duration.ofSeconds(30)).header("Authorization", "Basic " + basic)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scope=" + enc(scope))).build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode token = response.statusCode() == 200 ? MAPPER.readTree(response.body()).get("access_token") : null;
		if (token == null || token.asText().isEmpty()) {
			throw new IllegalStateException("token request for " + scope + " failed with HTTP " + response.statusCode());
		}
		return token.asText();
	}

	private void register(String adminToken, String producer, KeyPair key) throws IOException, InterruptedException {
		adminPost("/admin/producer-keys", adminToken, keyBody(producer, key, Instant.now().minus(1, ChronoUnit.DAYS)));
		adminPost("/admin/producer-chains", adminToken, chainBody(producer));
	}

	private void adminPost(String path, String adminToken, String body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(lockApi + path)).timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json").header("Authorization", "Bearer " + adminToken)
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 201) {
			throw new IllegalStateException("POST " + path + " returned HTTP " + response.statusCode() + ": "
					+ response.body());
		}
	}

	private static String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String stripSlash(String value) {
		return value.replaceAll("^/+", "").replaceAll("/+$", "");
	}

	private static long processCpuNs() {
		java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuTime();
		}
		return 0L;
	}

}
