/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.ParsedAssertion;
import io.jans.lock.service.trace.parse.TraceAssertionParser;
import io.jans.lock.service.trace.validate.EventKindValidatorRegistry;

/**
 * Task 22 deliverable 3: data-provider test over the static negative fixtures in
 * {@code src/test/resources/trace/negative/} (one JSON file per rule from tasks 08/09), each
 * asserting the exact {@code error} id and, where the rule names one, the {@code reason}. The
 * fixture set and the expected outcomes are generated together in
 * {@code negative/manifest.json}, mirroring the rule list proven interactively by
 * {@code CommonAssertionValidatorTest} and {@code EventKindValidatorsTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceNegativeSchemaTest extends BaseLockServiceTest {

	private static final String NEGATIVE_ROOT = "/trace/negative/";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TraceAssertionParser parser;

	private CommonAssertionValidator commonValidator;

	private EventKindValidatorRegistry registry;

	@BeforeEach
	void setUp() {
		AppConfiguration appConfiguration = new AppConfiguration();

		parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);

		commonValidator = new CommonAssertionValidator();
		setField(commonValidator, "appConfiguration", appConfiguration);
		setField(commonValidator, "clock", Clock.fixed(Instant.parse("2026-06-11T00:00:00Z"), ZoneOffset.UTC));

		registry = new EventKindValidatorRegistry();
	}

	/** Reads {@code negative/manifest.json}: one {@code (file, errorId, reason)} row per rule. */
	static Stream<Arguments> negativeFixtures() throws IOException {
		try (InputStream is = TraceNegativeSchemaTest.class.getResourceAsStream(NEGATIVE_ROOT + "manifest.json")) {
			JsonNode manifest = MAPPER.readTree(is);
			List<Arguments> args = new ArrayList<>();
			for (JsonNode entry : manifest) {
				String file = entry.get("file").asText();
				String errorId = entry.get("errorId").asText();
				JsonNode reasonNode = entry.get("reason");
				String reason = (reasonNode == null || reasonNode.isNull()) ? null : reasonNode.asText();
				args.add(Arguments.of(file, errorId, reason));
			}
			return args.stream();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("negativeFixtures")
	void negativeFixture_rejectedWithExpectedErrorAndReason(String file, String expectedErrorId, String expectedReason)
			throws IOException {
		String json = loadFixture(file);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> runPipeline(json));

		assertEquals(expectedErrorId, ex.getErrorId().getParameter());
		if (expectedReason != null) {
			assertEquals(expectedReason, ex.getReason());
		}
	}

	private void runPipeline(String json) {
		InputStream body = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
		ParsedAssertion assertion = parser.parse(body);
		commonValidator.validate(assertion);
		registry.validate(assertion);
	}

	private static String loadFixture(String file) throws IOException {
		try (InputStream is = TraceNegativeSchemaTest.class.getResourceAsStream(NEGATIVE_ROOT + file)) {
			byte[] bytes = is.readAllBytes();
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

}
