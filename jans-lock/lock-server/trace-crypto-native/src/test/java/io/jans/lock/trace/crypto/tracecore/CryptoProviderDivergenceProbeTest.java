/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProvider;

/**
 * Discovery probes, not part of the parity gate: inputs no Lock validator lets through
 * ({@code signed_at} and {@code sequence_number} are far below 2^53) on which the two providers
 * may still differ. Both outputs are printed; "equal" means equal canonical text or both providers
 * rejecting with {@code canonicalization_failed}. The inputs reach the providers as a Jackson
 * tree, so {@code -0} and {@code 1.0} are already normalized by the parser Lock uses.
 */
class CryptoProviderDivergenceProbeTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static TraceCryptoProvider javaCrypto;

	private static NativeTraceCryptoProvider nativeProvider;

	@BeforeAll
	static void providers() {
		javaCrypto = TestProviders.javaProvider();
		nativeProvider = TestProviders.nativeProvider();
		assertTrue(nativeProvider.isAvailable(), nativeProvider.describe());
	}

	@Disabled("FINDING (2026-10-16), remove to re-run: the providers diverge on three inputs Lock never lets through. "
			+ "(1) integer 9007199254740993: java {\"v\":9007199254740993}, native REJECTED canonicalization_failed. "
			+ "(2) 1e21: java {\"v\":1e+21}, native REJECTED canonicalization_failed. Both numbers are outside "
			+ "+-(2^53-1), which the core refuses by profile. (3) 100 nested empty containers: java accepts, native "
			+ "REJECTED canonicalization_failed (native depth limit 64 = 2 * maxJsonDepth 32; Lock's parser stops at 32). "
			+ "Equal: 0.1, -0, 1.0, lone surrogate escape (both reject), 30 nested containers.")
	@Test
	void probes_equalCanonicalTextOrBothRejected() throws Exception {
		Map<String, String> probes = new LinkedHashMap<>();
		probes.put("integer above 2^53", "{\"v\":9007199254740993}");
		probes.put("1e21", "{\"v\":1e21}");
		probes.put("0.1", "{\"v\":0.1}");
		probes.put("-0", "{\"v\":-0}");
		probes.put("1.0", "{\"v\":1.0}");
		probes.put("lone surrogate escape", "{\"v\":\"\\ud800\"}");
		probes.put("empty containers depth 30", nested(30));
		probes.put("empty containers depth 100", nested(100));

		List<String> divergences = new ArrayList<>();
		for (Map.Entry<String, String> probe : probes.entrySet()) {
			JsonNode node = MAPPER.readTree(probe.getValue());
			String javaOutcome = outcome(javaCrypto, node);
			String nativeOutcome = outcome(nativeProvider, node);
			boolean bothRejected = javaOutcome.startsWith("REJECTED") && nativeOutcome.startsWith("REJECTED");
			boolean equal = javaOutcome.equals(nativeOutcome) || bothRejected;
			System.out.println("probe [" + probe.getKey() + "] jackson=" + abbreviate(node.toString()) + "\n  java  = "
					+ abbreviate(javaOutcome) + "\n  native= " + abbreviate(nativeOutcome) + "\n  -> "
					+ (equal ? "equal" : "DIVERGES"));
			if (!equal) {
				divergences.add(probe.getKey());
			}
		}

		assertTrue(divergences.isEmpty(), "providers diverge on: " + divergences);
	}

	private static String outcome(TraceCryptoProvider provider, JsonNode node) {
		try {
			return "OK " + provider.canonicalize(node);
		} catch (TraceCryptoException ex) {
			return "REJECTED " + ex.getReason();
		}
	}

	private static String nested(int depth) {
		StringBuilder text = new StringBuilder("{\"v\":");
		for (int i = 0; i < depth; i++) {
			text.append(i % 2 == 0 ? "[" : "{\"a\":");
		}
		text.append("[]");
		for (int i = depth - 1; i >= 0; i--) {
			text.append(i % 2 == 0 ? "]" : "}");
		}
		return text.append('}').toString();
	}

	/** Shortens {@code text} and writes every non-printable-ASCII char (surrogates included) as \\uXXXX. */
	private static String abbreviate(String text) {
		String shown = text.length() > 160 ? text.substring(0, 160) : text;
		StringBuilder out = new StringBuilder();
		for (char c : shown.toCharArray()) {
			if (c < 0x20 || c > 0x7e) {
				out.append(String.format("\\u%04x", (int) c));
			} else {
				out.append(c);
			}
		}
		return text.length() > 160 ? out + "...(" + text.length() + " chars)" : out.toString();
	}

}
