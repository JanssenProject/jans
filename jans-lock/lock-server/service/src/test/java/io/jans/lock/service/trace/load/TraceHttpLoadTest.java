/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;

/**
 * Offline checks of {@link TraceHttpLoad}: request bodies, result counting and the CSV line. No network.
 */
class TraceHttpLoadTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void bulkBody_isJsonArrayOfTheRecordsInOrder() throws Exception {
		JsonNode body = MAPPER.readTree(TraceHttpLoad.bulkBody(Arrays.asList("{\"a\":1}", "{\"a\":2}")));
		assertTrue(body.isArray());
		assertEquals(2, body.size());
		assertEquals(1, body.get(0).get("a").asInt());
		assertEquals(2, body.get(1).get("a").asInt());
	}

	@Test
	void bulkBody_refusesBodiesOverMaxBulkRequestBytes() {
		char[] big = new char[TraceHttpLoad.MAX_BULK_REQUEST_BYTES];
		Arrays.fill(big, 'x');
		String record = "\"" + new String(big) + "\"";
		assertThrows(IllegalArgumentException.class, () -> TraceHttpLoad.bulkBody(Collections.singletonList(record)));
	}

	@Test
	void chain_linksEveryRecordToThePreviousDigest() throws Exception {
		KeyPair key = TraceTestKeys.generateKeyPair();
		List<String> chain = TraceHttpLoad.chain(key, "load13-test-c0", 3);
		String prev = TraceConstants.ZERO_HASH;
		long now = Instant.now().getEpochSecond();
		for (int i = 0; i < chain.size(); i++) {
			JsonNode record = MAPPER.readTree(chain.get(i));
			assertEquals("load13-test-c0", record.get("producer").asText());
			assertEquals("load13-test-c0-key", record.get("kid").asText());
			assertEquals(i + 1, record.get("producer_chain").get("sequence_number").asInt());
			assertEquals(prev, record.get("producer_chain").get("prev_record_hash").asText());
			assertTrue(Math.abs(record.get("trace").get("signed_at").asLong() - now) < 60);
			prev = CanonicalHashes.sha256PrefixedOfJcs(record);
		}
	}

	@Test
	void slices_cutEachChainAndInterleaveRoundRobin() {
		List<String> a = IntStream.range(0, 5).mapToObj(i -> "a" + i).collect(Collectors.toList());
		List<String> b = IntStream.range(0, 3).mapToObj(i -> "b" + i).collect(Collectors.toList());
		List<TraceHttpLoad.Slice> slices = TraceHttpLoad.slices(Arrays.asList(a, b), 2);
		assertEquals(Arrays.asList(0, 1, 0, 1, 0), slices.stream().map(s -> s.chain).collect(Collectors.toList()));
		assertEquals(Arrays.asList("a0", "a1"), slices.get(0).records);
		assertEquals(Arrays.asList("b2"), slices.get(3).records);
		assertEquals(Arrays.asList("a4"), slices.get(4).records);
	}

	@Test
	void adminBodies_carryTheRegistrationFields() throws Exception {
		KeyPair key = TraceTestKeys.generateKeyPair();
		JsonNode keyBody = MAPPER.readTree(TraceHttpLoad.keyBody("p1", key, Instant.parse("2026-11-13T10:00:00.123Z")));
		assertEquals("p1", keyBody.get("producer_id").asText());
		assertEquals("p1-key", keyBody.get("kid").asText());
		assertEquals("OKP", keyBody.get("public_key_jwk").get("kty").asText());
		assertEquals("Ed25519", keyBody.get("public_key_jwk").get("crv").asText());
		assertEquals("2026-11-13T10:00:00Z", keyBody.get("valid_from").asText());
		assertEquals("AUTHORIZATION_DECISION", keyBody.get("authorized_event_kinds").get(0).asText());
		JsonNode chainBody = MAPPER.readTree(TraceHttpLoad.chainBody("p1"));
		assertEquals("p1-instance", chainBody.get("producer_instance_id").asText());
		assertEquals("p1-chain", chainBody.get("producer_chain_id").asText());
	}

	@Test
	void tally_countsBulkElements() {
		LoadTally tally = new LoadTally();
		String body = "{\"results\":["
				+ "{\"index\":0,\"accepted\":true,\"idempotent_replay\":false,\"coverage_gap_flag\":false},"
				+ "{\"index\":1,\"accepted\":true,\"idempotent_replay\":true,\"coverage_gap_flag\":true},"
				+ "{\"index\":2,\"accepted\":false,\"error\":{\"error\":\"invalid_signature\",\"reason\":\"bad\"}}]}";
		tally.add(207, body, 3, true, 5_000_000L);
		assertEquals(3, tally.sent());
		assertEquals(1, tally.accepted());
		assertEquals(1, tally.replay());
		assertEquals(1, tally.rejected());
		assertEquals(1, tally.coverageGap());
		assertEquals(1, tally.replayRequests());
		assertEquals("rejected:invalid_signature/bad=1", tally.errorSummary());
		assertTrue(tally.balanced());
	}

	@Test
	void tally_rejectsWholeRequestOnErrorStatusAndShortBodies() {
		LoadTally tally = new LoadTally();
		tally.add(400, "{\"error\":\"invalid_request\",\"reason\":\"bulk_mixed_producers\"}", 100, true, 1L);
		tally.add(207, "{\"results\":[{\"accepted\":true}]}", 3, true, 1L);
		tally.add(207, "not json", 2, true, 1L);
		tally.addFailure("ConnectException", 4, 1L);
		tally.add(202, "{\"accepted\":true,\"idempotent_replay\":false}", 1, false, 1L);
		assertEquals(110, tally.sent());
		assertEquals(2, tally.accepted());
		assertEquals(108, tally.rejected());
		assertTrue(tally.balanced());
		assertEquals("http_400:invalid_request/bulk_mixed_producers=100;io:ConnectException=4;unaccounted=4",
				tally.errorSummary());
	}

	@Test
	void tally_singleModeExpects202() {
		LoadTally tally = new LoadTally();
		tally.add(207, "{\"accepted\":true}", 1, false, 1L);
		assertEquals(1, tally.rejected());
		assertEquals("http_207=1", tally.errorSummary());
	}

	@Test
	void percentile_usesNearestRank() {
		List<Long> values = IntStream.rangeClosed(1, 100).mapToObj(i -> i * 1_000_000L).collect(Collectors.toList());
		assertEquals(50.0, LoadTally.percentileMs(values, 50), 1e-9);
		assertEquals(95.0, LoadTally.percentileMs(values, 95), 1e-9);
		assertTrue(Double.isNaN(LoadTally.percentileMs(new java.util.ArrayList<>(), 50)));
	}

	@Test
	void sampler_parsesLinesAndSummarizesCpu() {
		VmSampler.Sample a = VmSampler.parseSample("S 100.0 1000 500 10 2000 10000 wal_sync=7,CPU=2,");
		VmSampler.Sample b = VmSampler.parseSample(
				"S 101.0 1400 600 20 2600 10800 wal_sync=9,CPU=4,IO:WALSync=1,");
		assertEquals(7, a.walSync);
		assertEquals(Integer.valueOf(4), b.waits.get("CPU"));
		VmSampler.Summary s = VmSampler.summarize(Arrays.asList(a, b), 100, 8);
		assertEquals(1, s.intervals);
		assertEquals(400.0, s.lockAvg, 1e-9);
		assertEquals(100.0, s.pgAvg, 1e-9);
		assertEquals(10.0, s.apacheAvg, 1e-9);
		assertEquals(600.0, s.vmAvg, 1e-9);
		assertEquals(2.0, s.walSyncPerS, 1e-9);
		assertEquals("CPU 4.00;IO:WALSync 1.00", s.topWaits);
	}

	@Test
	void csvLine_hasOneFieldPerHeaderColumn() {
		LoadTally tally = new LoadTally();
		tally.add(400, "{\"error\":\"e\",\"reason\":\"a,b\"}", 2, true, 1L);
		tally.add(207, "{\"results\":[{\"accepted\":true},{\"accepted\":true}]}", 2, true, 3_000_000L);
		VmSampler.Summary vm = VmSampler.summarize(Arrays.asList(
				VmSampler.parseSample("S 1.0 0 0 0 0 0 wal_sync=0,"),
				VmSampler.parseSample("S 2.0 100 100 0 100 800 wal_sync=1,CPU=1,LWLock:WALWrite=2,")), 100, 8);
		String line = TraceHttpLoad.csvLine("2026-11-14T00:00:00Z", "4/2", "bulk",
				new TraceHttpLoad.Point(8, 4, 100), 1, tally, 2.0, vm, 12.5);
		List<String> fields = splitCsv(line);
		List<String> header = Arrays.asList(TraceHttpLoad.CSV_HEADER.split(","));
		assertEquals(header.size(), fields.size());
		assertEquals("4/2", fields.get(header.indexOf("label")));
		assertEquals("4", fields.get(header.indexOf("sent")));
		assertEquals("2", fields.get(header.indexOf("accepted")));
		assertEquals("1.0", fields.get(header.indexOf("records_per_s")));
		assertEquals("100", fields.get(header.indexOf("lock_cpu_avg")));
		assertEquals("13", fields.get(header.indexOf("client_cpu_avg")));
		assertEquals("LWLock:WALWrite 2.00;CPU 1.00", fields.get(header.indexOf("top_waits")));
		assertEquals("http_400:e/a,b=2", fields.get(header.indexOf("errors")));
		assertFalse(fields.get(header.indexOf("req_p95_ms")).isEmpty());
	}

	@Test
	void points_parseChainsConnectionsBulk() {
		List<TraceHttpLoad.Point> points = TraceHttpLoad.Point.parse("8x4x100, 1x16x50");
		assertEquals(2, points.size());
		assertEquals(1, points.get(1).chains);
		assertEquals(16, points.get(1).connections);
		assertEquals(50, points.get(1).bulkSize);
		assertThrows(IllegalArgumentException.class, () -> TraceHttpLoad.Point.parse("8x4"));
	}

	/** Splits one CSV line with RFC 4180 quoting. */
	private static List<String> splitCsv(String line) {
		List<String> fields = new java.util.ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (quoted) {
				if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					field.append('"');
					i++;
				} else if (ch == '"') {
					quoted = false;
				} else {
					field.append(ch);
				}
			} else if (ch == '"') {
				quoted = true;
			} else if (ch == ',') {
				fields.add(field.toString());
				field.setLength(0);
			} else {
				field.append(ch);
			}
		}
		fields.add(field.toString());
		return fields;
	}

}
