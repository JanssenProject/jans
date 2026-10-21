/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.jans.orm.reflect.util.ReflectHelper;

class LogEntryTest {

	private static LogEntry withContext(Map<String, String> context) {
		LogEntry entry = new LogEntry();
		entry.setContextInformation(context);
		return entry;
	}

	private static Map<String, String> decisionContext() {
		Map<String, String> context = new HashMap<>();
		context.put("request_id", "from-context");
		context.put("policystore_id", "context-store");
		context.put("policystore_version", "1.2.3");
		return context;
	}

	@Test
	void indexedMembers_contextWinsOverExplicit() {
		LogEntry entry = withContext(decisionContext());
		entry.setRequestId("explicit");
		entry.setPolicyStoreId("explicit-store");
		entry.setPolicyStoreVersion("9.9.9");

		assertEquals("from-context", entry.getRequestId());
		assertEquals("context-store", entry.getPolicyStoreId());
		assertEquals("1.2.3", entry.getPolicyStoreVersion());
	}

	@Test
	void indexedMembers_explicitUsedWhenContextLacksKey() {
		LogEntry entry = withContext(new HashMap<>());
		entry.setRequestId("explicit");
		entry.setPolicyStoreVersion("1.0.0");

		assertEquals("explicit", entry.getRequestId());
		assertNull(entry.getPolicyStoreId());
		assertEquals("1.0.0", entry.getPolicyStoreVersion());
	}

	@Test
	void indexedMembers_oversizedOrEmptyContextFallsBackToExplicit() {
		Map<String, String> context = new HashMap<>();
		context.put("request_id", "x".repeat(LogEntry.MAX_REQUEST_ID_LENGTH + 1));
		context.put("policystore_id", "");
		LogEntry entry = withContext(context);
		entry.setRequestId("explicit");
		entry.setPolicyStoreId("explicit-store");

		assertEquals("explicit", entry.getRequestId());
		assertEquals("explicit-store", entry.getPolicyStoreId());
	}

	@Test
	void indexedMembers_oversizedExplicitDropped() {
		LogEntry entry = new LogEntry();
		entry.setRequestId("r".repeat(LogEntry.MAX_REQUEST_ID_LENGTH + 1));
		entry.setPolicyStoreVersion("v".repeat(LogEntry.MAX_POLICY_STORE_FIELD_LENGTH + 1));

		assertNull(entry.getRequestId());
		assertNull(entry.getPolicyStoreVersion());
	}

	@Test
	void orm_readsDerivedValuesThroughGetters() {
		LogEntry entry = withContext(decisionContext());
		entry.setRequestId("r".repeat(LogEntry.MAX_REQUEST_ID_LENGTH + 1));

		assertEquals("from-context", ReflectHelper.getGetter(LogEntry.class, "requestId").get(entry));
		assertEquals("context-store", ReflectHelper.getGetter(LogEntry.class, "policyStoreId").get(entry));
		assertEquals("1.2.3", ReflectHelper.getGetter(LogEntry.class, "policyStoreVersion").get(entry));
	}

	@Test
	void json_writesDerivedValuesAndRoundTrips() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		LogEntry entry = withContext(decisionContext());
		entry.setRequestId("p".repeat(LogEntry.MAX_REQUEST_ID_LENGTH + 1));

		JsonNode json = mapper.readTree(mapper.writeValueAsString(entry));
		LogEntry copy = mapper.readValue(json.toString(), LogEntry.class);

		assertEquals("from-context", json.get("requestId").asText());
		assertEquals("context-store", json.get("policyStoreId").asText());
		assertEquals(entry, copy);
	}

	private static final String CEDARLING_DECISION_LOG = "{"
			+ "\"creation_date\":\"2026-03-23T11:50:37.504Z\","
			+ "\"event_time\":\"2026-03-23T11:50:37.504Z\","
			+ "\"service\":\"my_test_app\","
			+ "\"node_name\":\"0195c6f2-8d1a-7c3e-9b4f-2a6d8e0f1b3c\","
			+ "\"event_type\":\"Decision\","
			+ "\"severity_level\":\"INFO\","
			+ "\"action\":\"Jans::Action::\\\"Update\\\"\","
			+ "\"decision_result\":\"ALLOW\","
			+ "\"requested_resource\":\"Jans::Issue::\\\"random_id\\\"\","
			+ "\"principal_id\":\"Jans::User, Jans::Workload\","
			+ "\"client_id\":\"lock-client-123\","
			+ "\"context_information\":{"
			+ "\"id\":\"0195c6f2-8d1a-7c3e-9b4f-2a6d8e0f1b3d\","
			+ "\"request_id\":\"0195c6f2-8d1a-7c3e-9b4f-2a6d8e0f1b3e\","
			+ "\"policystore_id\":\"test-store\","
			+ "\"policystore_version\":\"1.0.0\","
			+ "\"diagnostics\":{\"reason\":[{\"id\":\"840da5d85403\",\"description\":\"simple policy\"}],\"errors\":[]},"
			+ "\"tokens\":{\"access_token\":{\"jti\":\"9c9f2e77\"}},"
			+ "\"decision_time_micro_sec\":42,"
			+ "\"pushed_data\":{\"keys\":[\"extra_context\"]},"
			+ "\"batch_id\":null"
			+ "}}";

	@Test
	void json_cedarlingDecisionLog_readsSnakeCaseAndNestedContext() throws Exception {
		LogEntry entry = new ObjectMapper().readValue(CEDARLING_DECISION_LOG, LogEntry.class);

		assertEquals("0195c6f2-8d1a-7c3e-9b4f-2a6d8e0f1b3c", entry.getNodeName());
		assertEquals("Decision", entry.getEventType());
		assertEquals("ALLOW", entry.getDecisionResult());
		assertEquals("Jans::Issue::\"random_id\"", entry.getRequestedResource());
		assertEquals("lock-client-123", entry.getClientId());
		assertEquals(1774266637504L, entry.getEventTime().getTime());
		Map<String, String> context = entry.getContextInformation();
		assertEquals("{\"reason\":[{\"id\":\"840da5d85403\",\"description\":\"simple policy\"}],\"errors\":[]}",
				context.get("diagnostics"));
		assertEquals("{\"access_token\":{\"jti\":\"9c9f2e77\"}}", context.get("tokens"));
		assertEquals("42", context.get("decision_time_micro_sec"));
		assertEquals("null", context.get("batch_id"));

		assertEquals("0195c6f2-8d1a-7c3e-9b4f-2a6d8e0f1b3e", entry.getRequestId());
		assertEquals("test-store", entry.getPolicyStoreId());
		assertEquals("1.0.0", entry.getPolicyStoreVersion());
	}

	@Test
	void json_contextInformationNotObject_rejected() {
		ObjectMapper mapper = new ObjectMapper();

		assertThrows(MismatchedInputException.class,
				() -> mapper.readValue("{\"contextInformation\":[1]}", LogEntry.class));
	}

	@Test
	void json_nullContextInformation_staysNull() throws Exception {
		LogEntry entry = new ObjectMapper().readValue("{\"context_information\":null}", LogEntry.class);

		assertNull(entry.getContextInformation());
	}

	@Test
	void json_readsIndexedMembers() throws Exception {
		LogEntry entry = new ObjectMapper().readValue(
				"{\"requestId\":\"r1\",\"policyStoreId\":\"s1\",\"policyStoreVersion\":\"2\"}", LogEntry.class);

		assertEquals("r1", entry.getRequestId());
		assertEquals("s1", entry.getPolicyStoreId());
		assertEquals("2", entry.getPolicyStoreVersion());
	}

}
