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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

class LogEntryTest {

	private static LogEntry withContext(Map<String, String> context) {
		LogEntry entry = new LogEntry();
		entry.setContextInformation(context);
		return entry;
	}

	@Test
	void indexContextInformation_copiesDecisionLogKeys() {
		Map<String, String> context = new HashMap<>();
		context.put("request_id", "0198a5e2-7c1f-7b3e-9d2a-3f4e5a6b7c8d");
		context.put("policystore_id", "store-1");
		context.put("policystore_version", "1.2.3");
		LogEntry entry = withContext(context);

		entry.indexContextInformation();

		assertEquals("0198a5e2-7c1f-7b3e-9d2a-3f4e5a6b7c8d", entry.getRequestId());
		assertEquals("store-1", entry.getPolicyStoreId());
		assertEquals("1.2.3", entry.getPolicyStoreVersion());
	}

	@Test
	void indexContextInformation_keepsExplicitValues() {
		Map<String, String> context = new HashMap<>();
		context.put("request_id", "from-context");
		context.put("policystore_id", "context-store");
		LogEntry entry = withContext(context);
		entry.setRequestId("explicit");

		entry.indexContextInformation();

		assertEquals("explicit", entry.getRequestId());
		assertEquals("context-store", entry.getPolicyStoreId());
		assertNull(entry.getPolicyStoreVersion());
	}

	@Test
	void indexContextInformation_skipsEmptyAndOversizedValues() {
		Map<String, String> context = new HashMap<>();
		context.put("request_id", "x".repeat(LogEntry.MAX_REQUEST_ID_LENGTH + 1));
		context.put("policystore_id", "");
		context.put("policystore_version", "v".repeat(LogEntry.MAX_POLICY_STORE_FIELD_LENGTH + 1));
		LogEntry entry = withContext(context);

		entry.indexContextInformation();

		assertNull(entry.getRequestId());
		assertNull(entry.getPolicyStoreId());
		assertNull(entry.getPolicyStoreVersion());
	}

	@Test
	void indexContextInformation_oversizedExplicitValues_replacedFromContextOrDropped() {
		Map<String, String> context = new HashMap<>();
		context.put("request_id", "from-context");
		LogEntry entry = withContext(context);
		entry.setRequestId("r".repeat(LogEntry.MAX_REQUEST_ID_LENGTH + 1));
		entry.setPolicyStoreId("s".repeat(LogEntry.MAX_POLICY_STORE_FIELD_LENGTH + 1));

		entry.indexContextInformation();

		assertEquals("from-context", entry.getRequestId());
		assertNull(entry.getPolicyStoreId());
	}

	@Test
	void indexContextInformation_noContext_keepsFittingExplicitValue() {
		LogEntry entry = new LogEntry();
		entry.setPolicyStoreVersion("1.0.0");

		entry.indexContextInformation();

		assertEquals("1.0.0", entry.getPolicyStoreVersion());
	}

	@Test
	void indexContextInformation_noContext_noChange() {
		LogEntry entry = new LogEntry();

		entry.indexContextInformation();

		assertNull(entry.getRequestId());
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

		entry.indexContextInformation();

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
	void json_readsIndexedMembers() throws Exception {
		LogEntry entry = new ObjectMapper().readValue(
				"{\"requestId\":\"r1\",\"policyStoreId\":\"s1\",\"policyStoreVersion\":\"2\"}", LogEntry.class);

		assertEquals("r1", entry.getRequestId());
		assertEquals("s1", entry.getPolicyStoreId());
		assertEquals("2", entry.getPolicyStoreVersion());
	}

}
