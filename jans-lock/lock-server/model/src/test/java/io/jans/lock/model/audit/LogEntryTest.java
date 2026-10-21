/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	void indexContextInformation_noContext_noChange() {
		LogEntry entry = new LogEntry();

		entry.indexContextInformation();

		assertNull(entry.getRequestId());
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
