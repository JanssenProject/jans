/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.load;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Counts the outcome of every record sent by {@link TraceHttpLoad}: accepted, idempotent replay or
 * rejected (per error id), plus the request latencies. Thread-safe.
 *
 * <p>A {@code 207} bulk response or a {@code 202} single response is read element by element. Any
 * other status rejects every record of the request under the key {@code http_<status>:<error>/<reason>}.
 * Records the response does not account for (a short or unreadable body) are rejected as
 * {@code unaccounted}, so {@code accepted + replay + rejected == sent} always holds.
 */
final class LoadTally {

	static final int STATUS_BULK_OK = 207;

	static final int STATUS_SINGLE_OK = 202;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private long sent;

	private long accepted;

	private long replay;

	private long rejected;

	private long coverageGap;

	private long requests;

	private final Map<String, Long> errors = new TreeMap<>();

	private final List<Long> latenciesNs = new ArrayList<>();

	private final List<Long> replayLatenciesNs = new ArrayList<>();

	/**
	 * @param status    HTTP status of the request
	 * @param body      response body, may be {@code null}
	 * @param sentCount records in the request
	 * @param bulk      {@code true} for {@code POST /audit/trace/bulk}, {@code false} for {@code POST /audit/trace}
	 * @param latencyNs request latency
	 */
	synchronized void add(int status, String body, int sentCount, boolean bulk, long latencyNs) {
		requests++;
		sent += sentCount;
		int expected = bulk ? STATUS_BULK_OK : STATUS_SINGLE_OK;
		JsonNode root = parse(body);
		boolean hadReplay = false;
		if (status != expected) {
			rejectAll(errorKey("http_" + status, root), sentCount);
		} else {
			List<JsonNode> entries = new ArrayList<>();
			if (bulk) {
				JsonNode results = root == null ? null : root.get("results");
				if (results != null && results.isArray()) {
					results.forEach(entries::add);
				}
			} else if (root != null && root.isObject()) {
				entries.add(root);
			}
			int counted = 0;
			for (JsonNode entry : entries) {
				if (counted == sentCount) {
					break;
				}
				counted++;
				if (entry.path("accepted").asBoolean(false)) {
					if (entry.path("idempotent_replay").asBoolean(false)) {
						replay++;
						hadReplay = true;
					} else {
						accepted++;
					}
					if (entry.path("coverage_gap_flag").asBoolean(false)) {
						coverageGap++;
					}
				} else {
					rejectAll(errorKey("rejected", entry.get("error")), 1);
				}
			}
			if (counted < sentCount) {
				rejectAll("unaccounted", sentCount - counted);
			}
		}
		(hadReplay ? replayLatenciesNs : latenciesNs).add(latencyNs);
	}

	/** Records a request that got no HTTP response at all. */
	synchronized void addFailure(String cause, int sentCount, long latencyNs) {
		requests++;
		sent += sentCount;
		rejectAll("io:" + cause, sentCount);
		latenciesNs.add(latencyNs);
	}

	private void rejectAll(String key, int count) {
		rejected += count;
		errors.merge(key, (long) count, Long::sum);
	}

	private static String errorKey(String prefix, JsonNode error) {
		if (error == null || !error.isObject()) {
			return prefix;
		}
		String id = error.path("error").asText("");
		String reason = error.path("reason").asText("");
		if (id.isEmpty() && reason.isEmpty()) {
			return prefix;
		}
		return prefix + ":" + id + (reason.isEmpty() ? "" : "/" + reason);
	}

	private static JsonNode parse(String body) {
		if (body == null || body.isEmpty()) {
			return null;
		}
		try {
			return MAPPER.readTree(body);
		} catch (java.io.IOException ex) {
			return null;
		}
	}

	synchronized long sent() {
		return sent;
	}

	synchronized long accepted() {
		return accepted;
	}

	synchronized long replay() {
		return replay;
	}

	synchronized long rejected() {
		return rejected;
	}

	synchronized long coverageGap() {
		return coverageGap;
	}

	synchronized long requests() {
		return requests;
	}

	synchronized boolean balanced() {
		return accepted + replay + rejected == sent;
	}

	/** @return {@code key=count} pairs joined by {@code ;}, or an empty string */
	synchronized String errorSummary() {
		StringBuilder sb = new StringBuilder();
		errors.forEach((key, count) -> sb.append(sb.length() == 0 ? "" : ";").append(key).append('=').append(count));
		return sb.toString();
	}

	/** @return latency percentile in ms over all requests, or {@code NaN} without requests */
	synchronized double percentileMs(int percentile) {
		List<Long> all = new ArrayList<>(latenciesNs);
		all.addAll(replayLatenciesNs);
		return percentileMs(all, percentile);
	}

	/** @return latency percentile in ms over the requests with at least one replay element */
	synchronized double replayPercentileMs(int percentile) {
		return percentileMs(new ArrayList<>(replayLatenciesNs), percentile);
	}

	/** @return latency percentile in ms over the requests without replay elements */
	synchronized double plainPercentileMs(int percentile) {
		return percentileMs(new ArrayList<>(latenciesNs), percentile);
	}

	synchronized int replayRequests() {
		return replayLatenciesNs.size();
	}

	static double percentileMs(List<Long> values, int percentile) {
		if (values.isEmpty()) {
			return Double.NaN;
		}
		Collections.sort(values);
		int index = (int) Math.ceil(values.size() * percentile / 100.0) - 1;
		return values.get(Math.max(0, Math.min(values.size() - 1, index))) / 1e6;
	}

}
