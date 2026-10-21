/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.audit;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reads {@code contextInformation} as a flat string map. A string value is kept as is; any other
 * value (object, array, number, boolean, null) becomes its compact JSON text, the same encoding
 * the gRPC audit path uses for Cedarling decision logs.
 */
public class ContextInformationDeserializer extends JsonDeserializer<Map<String, String>> {

	@Override
	public Map<String, String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		JsonNode node = parser.readValueAsTree();
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.isObject()) {
			return context.reportInputMismatch(Map.class, "contextInformation must be a JSON object");
		}
		Map<String, String> result = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> field : node.properties()) {
			JsonNode value = field.getValue();
			result.put(field.getKey(), value.isTextual() ? value.textValue() : value.toString());
		}
		return result;
	}

	@Override
	public Map<String, String> getNullValue(DeserializationContext context) {
		return null;
	}

}
