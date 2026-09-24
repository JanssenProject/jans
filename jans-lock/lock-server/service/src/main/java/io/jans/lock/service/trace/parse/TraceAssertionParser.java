/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Turns an incoming TRACE request body into a {@link ParsedAssertion}: bounded reading, strict
 * UTF-8 decoding, and JSON parsing under the request limits (design §14, decision D-12), all
 * before any cryptographic work is done. Common-field validation (design §7.1) is
 * {@link CommonAssertionValidator}'s job, not this class's.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceAssertionParser {

	private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	@Inject
	private AppConfiguration appConfiguration;

	/**
	 * @param body the raw request body stream; never read beyond {@code maxRequestBytes + 1} bytes
	 * @return the parsed, limit-checked assertion
	 * @throws TraceValidationException {@code invalid_request} if the body exceeds
	 *         {@code maxRequestBytes}, is not well-formed UTF-8, starts with a byte-order mark, is
	 *         not syntactically valid JSON within the configured limits, has trailing content
	 *         after the root value, or does not parse to a JSON object
	 */
	public ParsedAssertion parse(InputStream body) {
		TraceConfiguration config = appConfiguration.getTraceConfiguration();

		byte[] bytes = readBounded(body, config.getMaxRequestBytes());
		String rawText = decodeUtf8(bytes);

		ObjectMapper mapper = buildMapper(config);
		JsonNode root = parseJson(mapper, rawText);

		if (!root.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "root_not_object");
		}
		walk(root, config.getMaxArrayLength(), config.getMaxObjectMembers());

		return new ParsedAssertion(rawText, (ObjectNode) root);
	}

	private byte[] readBounded(InputStream body, int maxRequestBytes) {
		// The 2-arg constructor is deprecated in commons-io 2.17 in favor of the builder, but it is
		// still present and avoids the builder's generic self-type chaining.
		@SuppressWarnings("deprecation")
		BoundedInputStream bounded = new BoundedInputStream(body, (long) maxRequestBytes + 1);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxRequestBytes + 1, 1 << 16));
			IOUtils.copy(bounded, out);
			byte[] bytes = out.toByteArray();
			if (bytes.length > maxRequestBytes) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "body_too_large");
			}
			return bytes;
		} catch (IOException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "unreadable_body", ex);
		}
	}

	private String decodeUtf8(byte[] bytes) {
		if (bytes.length >= UTF8_BOM.length && bytes[0] == UTF8_BOM[0] && bytes[1] == UTF8_BOM[1]
				&& bytes[2] == UTF8_BOM[2]) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "utf8_bom");
		}
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			return decoder.decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "malformed_utf8", ex);
		}
	}

	private ObjectMapper buildMapper(TraceConfiguration config) {
		StreamReadConstraints constraints = StreamReadConstraints.builder()
				.maxNestingDepth(config.getMaxJsonDepth())
				.maxStringLength(config.getMaxStringLength())
				.build();
		JsonFactory factory = JsonFactory.builder().streamReadConstraints(constraints).build();
		factory.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
		ObjectMapper mapper = new ObjectMapper(factory);
		mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
		return mapper;
	}

	private JsonNode parseJson(ObjectMapper mapper, String rawText) {
		try {
			// Parsing the decoded String (not the raw bytes) is what makes a leading U+FEFF land in
			// the token stream as an invalid character rather than being silently stripped the way
			// Jackson's byte-stream bootstrapper would; the explicit BOM check above still runs
			// first so that case gets its own reason rather than a generic parse error.
			return mapper.readTree(rawText);
		} catch (StreamConstraintsException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "json_limit_exceeded",
					ex);
		} catch (MismatchedInputException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "trailing_content",
					ex);
		} catch (JsonProcessingException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST, "malformed_json", ex);
		}
	}

	private void walk(JsonNode node, int maxArrayLength, int maxObjectMembers) {
		if (node.isObject()) {
			if (node.size() > maxObjectMembers) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST,
						"max_object_members_exceeded");
			}
			for (JsonNode child : node) {
				walk(child, maxArrayLength, maxObjectMembers);
			}
		} else if (node.isArray()) {
			ArrayNode array = (ArrayNode) node;
			if (array.size() > maxArrayLength) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_REQUEST,
						"max_array_length_exceeded");
			}
			for (JsonNode child : array) {
				walk(child, maxArrayLength, maxObjectMembers);
			}
		}
	}

}
