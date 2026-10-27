/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.client.trace.TraceAssertions;
import io.jans.lock.client.trace.TraceAssertions.AuthorizationDecision;
import io.jans.lock.client.trace.TraceAssertions.CapabilityInvoked;
import io.jans.lock.client.trace.TraceSigningKey;

/**
 * Manual end-to-end exercise of the TRACE API (design §7, §11) against a real deployed Lock Server. It is a JUnit 5 test that reuses the {@code -Dcfg=<profile>} client-profile mechanism
 * ({@link BaseLockClientTest}) already reserved for this purpose (see
 * {@code profiles/default/README.md}). Skips itself entirely unless both a real server and a
 * TRACE-scoped client id/secret are configured ({@link #hasTraceClient()}).
 *
 * <p><b>Required client setup</b> (not automated here): the configured
 * client must hold all three TRACE scopes simultaneously ({@code trace.write},
 * {@code trace.readonly}, {@code trace.admin}), granted manually. The setup-generated Lock client
 * only receives scopes from {@code lock-plugin-swagger.yaml} (config-api plugin), not from
 * jans-lock's own {@code lock-server.yaml}.
 *
 * <p>Methods run in a fixed order ({@link MethodOrderer.OrderAnnotation}) and share state via
 * instance fields ({@link TestInstance.Lifecycle#PER_CLASS}): later scenarios build on records
 * accepted by earlier ones, the same way the sections of a manual script would.
 *
 * <p>Not covered: the optional "readonly token, second client, wrong domain → 404" scenario — it needs a second OAuth client bound to a different evidence domain, which
 * the single reserved {@code traceClientId} profile key does not provide.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TraceEndToEndTest extends BaseLockClientTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Distinguishes identifiers across repeated runs so admin registration never collides (keys are create-only). */
	private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);

	private final String producerA = "trace-e2e-a-" + RUN_ID + "/1.0.0";
	private final String producerB = "trace-e2e-b-" + RUN_ID + "/1.0.0";
	private final String kidA = "trace-e2e-kid-a-" + RUN_ID;
	private final String kidB = "trace-e2e-kid-b-" + RUN_ID;
	private final String chainIdA = "trace-e2e-chain-a-" + RUN_ID;
	private final String chainIdB = "trace-e2e-chain-b-" + RUN_ID;
	private final String instanceIdA = "trace-e2e-instance-a-" + RUN_ID;
	private final String instanceIdB = "trace-e2e-instance-b-" + RUN_ID;
	private final String traceExecutionId = "trace-e2e-exec-" + RUN_ID;
	private final String executionAuthority = "spiffe://example.org/agent/trace-e2e-" + RUN_ID;

	private static final String DEFAULT_API_BASE_PATH = "/jans-lock/api/v1";
	private static final String HEALTH_ENDPOINT_SUFFIX = "/audit/health";

	private HttpClient client;
	private String apiBasePath;
	private JsonNode wellKnownAudit = MAPPER.createObjectNode();
	private String adminToken;
	private String writeToken;
	private String readToken;

	private TraceSigningKey keyA;
	private TraceSigningKey keyB;

	private String recordIdA;
	private String recordA_signedJson;
	private long receiptSequenceA;

	private String recordIdB;

	@BeforeEach
	void setUp() {
		assumeTrue(hasTraceClient(),
				"No real server/TRACE client configured (-Dcfg=<profile> with trace.client.id/secret); skipping, see profiles/default/README.md");
		if (client == null) {
			client = newInsecureHttpClient();
			apiBasePath = discoverApiBasePath();
		}
	}

	/** Well-known document advertises the health, log and telemetry bulk audit endpoints. */
	@Test
	@Order(0)
	void wellKnownPublishesBulkAuditEndpoints() {
		for (String type : new String[] { "health", "log", "telemetry" }) {
			assertEquals(apiBasePath + "/audit/" + type + "/bulk",
					pathOf(wellKnownAudit.path(type + "_bulk_endpoint").asText("")));
		}
	}

	/** Well-known document advertises the TRACE submit, bulk, record and execution endpoints. */
	@Test
	@Order(0)
	void wellKnownPublishesTraceEndpoints() {
		assertEquals(apiBasePath + "/audit/trace", pathOf(wellKnownAudit.path("trace_endpoint").asText("")));
		assertEquals(apiBasePath + "/audit/trace/bulk", pathOf(wellKnownAudit.path("trace_bulk_endpoint").asText("")));
		assertEquals(apiBasePath + "/audit/trace/records",
				pathOf(wellKnownAudit.path("trace_record_endpoint").asText("")));
		assertEquals(apiBasePath + "/audit/trace/executions",
				pathOf(wellKnownAudit.path("trace_execution_endpoint").asText("")));
	}

	/** Client obtains one token each for trace.admin, trace.write and trace.readonly; later steps reuse them. */
	@Test
	@Order(1)
	void obtainTokensForAllThreeTraceScopes() throws Exception {
		adminToken = getToken("https://jans.io/oauth/lock/trace.admin");
		writeToken = getToken("https://jans.io/oauth/lock/trace.write");
		readToken = getToken("https://jans.io/oauth/lock/trace.readonly");

		assertTrue(isPresent(adminToken), "Failed to obtain a trace.admin token; is the client granted that scope?");
		assertTrue(isPresent(writeToken), "Failed to obtain a trace.write token; is the client granted that scope?");
		assertTrue(isPresent(readToken), "Failed to obtain a trace.readonly token; is the client granted that scope?");
	}

	/** Admin registers a signing key and a chain for each of two producers; later steps sign records with these keys. */
	@Test
	@Order(2)
	void adminRegistersProducerKeysAndChains() throws Exception {
		keyA = TraceSigningKey.generate();
		keyB = TraceSigningKey.generate();

		HttpResponse<String> keyAResponse = registerProducerKey(producerA, kidA, keyA);
		assertEquals(201, keyAResponse.statusCode(), keyAResponse.body());

		HttpResponse<String> keyBResponse = registerProducerKey(producerB, kidB, keyB);
		assertEquals(201, keyBResponse.statusCode(), keyBResponse.body());

		HttpResponse<String> chainAResponse = registerProducerChain(producerA, instanceIdA, chainIdA);
		assertEquals(201, chainAResponse.statusCode(), chainAResponse.body());

		HttpResponse<String> chainBResponse = registerProducerChain(producerB, instanceIdB, chainIdB);
		assertEquals(201, chainBResponse.statusCode(), chainBResponse.body());
	}

	/** Registering the same producer key again fails with 409, because keys are create-only. */
	@Test
	@Order(3)
	void adminRejectsDuplicateKeyRegistration() throws Exception {
		HttpResponse<String> response = registerProducerKey(producerA, kidA, keyA);
		assertEquals(409, response.statusCode(), response.body());
		assertEquals("key_already_exists", readTree(response).get("error").asText());
	}

	/** Admin lists keys and chains for producer A and sees the entries registered earlier. */
	@Test
	@Order(4)
	void adminListsRegisteredKeysAndChains() throws Exception {
		HttpResponse<String> keys = getJson("/audit/trace/admin/producer-keys?producer_id="
				+ URLEncoder.encode(producerA, StandardCharsets.UTF_8), adminToken);
		assertEquals(200, keys.statusCode(), keys.body());
		JsonNode keyList = readTree(keys).get("keys");
		assertTrue(keyList.isArray() && (keyList.size() >= 1), "Expected at least the just-registered key: " + keys.body());

		HttpResponse<String> chains = getJson("/audit/trace/admin/producer-chains?producer_id="
				+ URLEncoder.encode(producerA, StandardCharsets.UTF_8), adminToken);
		assertEquals(200, chains.statusCode(), chains.body());
		JsonNode chainList = readTree(chains).get("chains");
		assertTrue(chainList.isArray() && (chainList.size() >= 1), "Expected at least the just-registered chain: " + chains.body());
	}

	/** Producer A submits the first record of its chain; Lock accepts it and allocates a receipt sequence. */
	@Test
	@Order(5)
	void writeAcceptsGenesisAuthorizationDecisionFromProducerA() throws Exception {
		recordIdA = UUID.randomUUID().toString();
		recordA_signedJson = new AuthorizationDecision().producer(producerA).kid(kidA).recordId(recordIdA)
				.signedAt(Instant.now().getEpochSecond()).traceExecutionId(traceExecutionId)
				.executionAuthority(executionAuthority).producerInstanceId(instanceIdA).producerChainId(chainIdA)
				.sequenceNumber(1).prevRecordHash(TraceAssertions.ZERO_HASH).sign(keyA);

		HttpResponse<String> response = submitRecord(recordA_signedJson, writeToken);
		assertEquals(202, response.statusCode(), response.body());
		JsonNode body = readTree(response);
		assertTrue(body.get("accepted").asBoolean());
		assertFalse(body.get("idempotent_replay").asBoolean());
		assertFalse(body.get("coverage_gap_flag").asBoolean());
		assertFalse(body.get("chain_link_failure_flag").asBoolean());
		assertFalse(body.get("equivocation_flag").asBoolean());
		receiptSequenceA = body.get("receipt_sequence").asLong();
	}

	/** Producer B submits its first record in the same execution, with producer A's record as parent. */
	@Test
	@Order(6)
	void writeAcceptsGenesisCapabilityInvokedFromProducerB() throws Exception {
		recordIdB = UUID.randomUUID().toString();
		String signedJson = new CapabilityInvoked().producer(producerB).kid(kidB).recordId(recordIdB)
				.signedAt(Instant.now().getEpochSecond()).traceExecutionId(traceExecutionId)
				.executionAuthority(executionAuthority).producerInstanceId(instanceIdB).producerChainId(chainIdB)
				.sequenceNumber(1).prevRecordHash(TraceAssertions.ZERO_HASH).parent(producerA, recordIdA).sign(keyB);

		HttpResponse<String> response = submitRecord(signedJson, writeToken);
		assertEquals(202, response.statusCode(), response.body());
		assertTrue(readTree(response).get("accepted").asBoolean());
	}

	/** Resubmitting identical content returns the original receipt as an idempotent replay. */
	@Test
	@Order(7)
	void writeReplayOfIdenticalContentIsIdempotent() throws Exception {
		HttpResponse<String> response = submitRecord(recordA_signedJson, writeToken);
		assertEquals(202, response.statusCode(), response.body());
		JsonNode body = readTree(response);
		assertTrue(body.get("idempotent_replay").asBoolean());
		assertEquals(receiptSequenceA, body.get("receipt_sequence").asLong(),
				"Replay must return the original receipt, never allocate a new one");
	}

	/** Resubmitting the same record identity with different signed content fails with 409 record_conflict. */
	@Test
	@Order(8)
	void writeSameIdentityWithDifferentContentConflicts() throws Exception {
		String resignedWithDifferentContent = new AuthorizationDecision().producer(producerA).kid(kidA)
				.recordId(recordIdA) // same identity as the Order(5) record
				.signedAt(Instant.now().getEpochSecond() + 1) // changes the signed content, hence content_digest
				.traceExecutionId(traceExecutionId).executionAuthority(executionAuthority)
				.producerInstanceId(instanceIdA).producerChainId(chainIdA).sequenceNumber(1)
				.prevRecordHash(TraceAssertions.ZERO_HASH).sign(keyA);

		HttpResponse<String> response = submitRecord(resignedWithDifferentContent, writeToken);
		assertEquals(409, response.statusCode(), response.body());
		assertEquals("record_conflict", readTree(response).get("error").asText());
	}

	/** A record with a corrupted signature is rejected with 400 invalid_signature. */
	@Test
	@Order(9)
	void writeTamperedSignatureIsRejected() throws Exception {
		String freshRecordId = UUID.randomUUID().toString();
		String signedJson = new AuthorizationDecision().producer(producerA).kid(kidA).recordId(freshRecordId)
				.signedAt(Instant.now().getEpochSecond()).traceExecutionId(traceExecutionId)
				.executionAuthority(executionAuthority).producerInstanceId(instanceIdA).producerChainId(chainIdA)
				.sequenceNumber(2).prevRecordHash(TraceAssertions.contentDigest(recordA_signedJson)).sign(keyA);
		// Flip the last base64url character of the signature; the rejected submission consumes no
		// chain position, so sequence_number 2 remains available for Order(15).
		String tampered = tamperSignature(signedJson);

		HttpResponse<String> response = submitRecord(tampered, writeToken);
		assertEquals(400, response.statusCode(), response.body());
		assertEquals("invalid_signature", readTree(response).get("error").asText());
	}

	/** A signed evidence_domain_id is ignored and flagged, because Lock derives the domain from the client. */
	@Test
	@Order(10)
	void writeBodyWithEvidenceDomainIdIsIgnoredAndFlagged() throws Exception {
		AuthorizationDecision builder = new AuthorizationDecision().producer(producerA).kid(kidA)
				.recordId(UUID.randomUUID().toString()).signedAt(Instant.now().getEpochSecond())
				.traceExecutionId(traceExecutionId + "-reserved").executionAuthority(executionAuthority)
				.producerInstanceId(instanceIdA).producerChainId(chainIdA).sequenceNumber(99)
				.prevRecordHash(TraceAssertions.contentDigest(recordA_signedJson));
		var tree = builder.unsignedTree();
		tree.put("evidence_domain_id", "attempted-domain-override");
		String signedJson = TraceAssertions.sign(tree, keyA);

		HttpResponse<String> response = submitRecord(signedJson, writeToken);
		assertEquals(202, response.statusCode(), response.body());
		JsonNode body = readTree(response);
		assertTrue(body.get("reserved_member_flag").asBoolean());
		assertEquals("evidence_domain_id", body.get("reserved_members").get(0).asText());
	}

	/** A trace.readonly token cannot submit records; Lock answers 401 or 403. */
	@Test
	@Order(11)
	void writeWithReadonlyScopedTokenIsRejected() throws Exception {
		// Exercises the same "TRACE API access control" check a wrong-scope (log.write) request targets, using the readonly token instead: the reserved TRACE client is granted
		// only the three trace.* scopes (see profiles/default/README.md), so trace.readonly used
		// against the write endpoint is a scope the client genuinely holds, just not for this path.
		HttpResponse<String> response = submitRecord(recordA_signedJson, readToken);
		assertTrue((response.statusCode() == 401) || (response.statusCode() == 403),
				"Expected 401/403, got " + response.statusCode() + ": " + response.body());
	}

	/** A submission without an access token fails with 401. */
	@Test
	@Order(12)
	void writeWithoutTokenIsRejected() throws Exception {
		HttpResponse<String> response = submitRecord(recordA_signedJson, null);
		assertEquals(401, response.statusCode(), response.body());
	}

	/** A read-scoped token retrieves record A with its verification result and original receipt sequence. */
	@Test
	@Order(13)
	void readReturnsStoredRecordEnvelope() throws Exception {
		HttpResponse<String> response = getUrl(
				advertisedEndpoint("trace_record_endpoint", "/audit/trace/records") + "/"
						+ URLEncoder.encode(recordIdA, StandardCharsets.UTF_8) + "?producer_id="
						+ URLEncoder.encode(producerA, StandardCharsets.UTF_8),
				readToken);
		assertEquals(200, response.statusCode(), response.body());
		JsonNode body = readTree(response);
		assertEquals(recordIdA, body.get("record_id").asText());
		assertTrue(body.get("verification").get("signature_valid").asBoolean());
		assertEquals(receiptSequenceA, body.get("ingestion").get("receipt_sequence").asLong());
	}

	/** Execution read returns both accepted records ordered by receipt sequence. */
	@Test
	@Order(14)
	void readExecutionReturnsBothRecordsOrderedByReceiptSequence() throws Exception {
		HttpResponse<String> response = getUrl(advertisedEndpoint("trace_execution_endpoint", "/audit/trace/executions")
				+ "/" + URLEncoder.encode(traceExecutionId, StandardCharsets.UTF_8) + "?execution_authority="
				+ URLEncoder.encode(executionAuthority, StandardCharsets.UTF_8), readToken);
		assertEquals(200, response.statusCode(), response.body());
		JsonNode body = readTree(response);
		assertEquals("not_assessed", body.get("completeness").asText());
		JsonNode records = body.get("records");
		assertEquals(2, records.size(), response.body());
		assertEquals(recordIdA, records.get(0).get("record_id").asText(), "Producer A's record was accepted first");
		assertEquals(recordIdB, records.get(1).get("record_id").asText(), "Producer B's record was accepted second");
	}

	/** Admin revokes producer A's key; a later submission signed with it fails with key_not_valid. */
	@Test
	@Order(15)
	void adminRevokesKeyThenSubmissionIsRejected() throws Exception {
		HttpResponse<String> revoke = postJson("/audit/trace/admin/producer-keys/revoke",
				MAPPER.createObjectNode().put("producer_id", producerA).put("kid", kidA).toString(), adminToken);
		assertEquals(200, revoke.statusCode(), revoke.body());
		assertNotNull(readTree(revoke).get("revoked_at").asText(null), "revoked_at must be set after revocation");

		String signedJson = new AuthorizationDecision().producer(producerA).kid(kidA)
				.recordId(UUID.randomUUID().toString()).signedAt(Instant.now().getEpochSecond())
				.traceExecutionId(traceExecutionId).executionAuthority(executionAuthority)
				.producerInstanceId(instanceIdA).producerChainId(chainIdA).sequenceNumber(2)
				.prevRecordHash(TraceAssertions.contentDigest(recordA_signedJson)).sign(keyA);

		HttpResponse<String> response = submitRecord(signedJson, writeToken);
		assertEquals(400, response.statusCode(), response.body());
		assertEquals("key_not_valid", readTree(response).get("error").asText());
	}

	// -- HTTP helpers -------------------------------------------------------------------------------

	/**
	 * Lock runs either standalone ({@code /jans-lock}) or embedded in jans-auth ({@code /jans-auth}),
	 * so the API base path is taken from the well-known document rather than hardcoded.
	 */
	private String discoverApiBasePath() {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/.well-known/lock-server-configuration"))
					.GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				wellKnownAudit = readTree(response).path("audit");
				String healthEndpoint = wellKnownAudit.path("health_endpoint").asText("");
				String path = healthEndpoint.isEmpty() ? "" : URI.create(healthEndpoint).getPath();
				if (path != null && path.endsWith(HEALTH_ENDPOINT_SUFFIX)) {
					return path.substring(0, path.length() - HEALTH_ENDPOINT_SUFFIX.length());
				}
			}
		} catch (Exception ex) {
			// fall through to the default path
		}
		return DEFAULT_API_BASE_PATH;
	}

	private String getToken(String scope) throws Exception {
		String body = "grant_type=client_credentials&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
		String basicAuth = Base64.getEncoder()
				.encodeToString((traceClientId() + ":" + traceClientSecret()).getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/jans-auth/restv1/token"))
				.header("Authorization", "Basic " + basicAuth)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			return null;
		}
		JsonNode tree = MAPPER.readTree(response.body());
		return tree.path("access_token").asText(null);
	}

	private HttpResponse<String> submitRecord(String signedAssertionJson, String token) throws Exception {
		return postUrl(advertisedEndpoint("trace_endpoint", "/audit/trace"), signedAssertionJson, token);
	}

	private HttpResponse<String> registerProducerKey(String producerId, String kid, TraceSigningKey key)
			throws Exception {
		var body = MAPPER.createObjectNode();
		body.put("producer_id", producerId);
		body.put("kid", kid);
		var jwk = body.putObject("public_key_jwk");
		key.publicJwk().forEach(jwk::put);
		var kinds = body.putArray("authorized_event_kinds");
		kinds.add("AUTHORIZATION_DECISION");
		kinds.add("CAPABILITY_INVOKED");
		kinds.add("RUNTIME_EFFECT");
		body.put("valid_from", "2020-01-01T00:00:00Z");
		body.putNull("valid_until");
		return postJson("/audit/trace/admin/producer-keys", body.toString(), adminToken);
	}

	private HttpResponse<String> registerProducerChain(String producerId, String producerInstanceId,
			String producerChainId) throws Exception {
		var body = MAPPER.createObjectNode();
		body.put("producer_id", producerId);
		body.put("producer_instance_id", producerInstanceId);
		body.put("producer_chain_id", producerChainId);
		return postJson("/audit/trace/admin/producer-chains", body.toString(), adminToken);
	}

	/** Absolute URL advertised in the well-known document, falling back to the base path when it is not published. */
	private String advertisedEndpoint(String key, String fallbackPath) {
		String advertised = wellKnownAudit.path(key).asText("");
		return advertised.isEmpty() ? baseUrl() + apiBasePath + fallbackPath : advertised;
	}

	private static String pathOf(String url) {
		return url.isEmpty() ? "" : URI.create(url).getPath();
	}

	private HttpResponse<String> postJson(String path, String jsonBody, String token) throws Exception {
		return postUrl(baseUrl() + apiBasePath + path, jsonBody, token);
	}

	private HttpResponse<String> postUrl(String url, String jsonBody, String token) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
		if (isPresent(token)) {
			builder.header("Authorization", "Bearer " + token);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> getJson(String pathWithQuery, String token) throws Exception {
		return getUrl(baseUrl() + apiBasePath + pathWithQuery, token);
	}

	private HttpResponse<String> getUrl(String url, String token) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
		if (isPresent(token)) {
			builder.header("Authorization", "Bearer " + token);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static JsonNode readTree(HttpResponse<String> response) throws Exception {
		return MAPPER.readTree(response.body());
	}

	private static boolean isPresent(String value) {
		return (value != null) && !value.isEmpty();
	}

	/** Flips the last base64url character of the {@code signature} field, keeping the field well-formed. */
	private static String tamperSignature(String signedJson) throws Exception {
		JsonNode tree = MAPPER.readTree(signedJson);
		String signature = tree.get("signature").asText();
		char last = signature.charAt(signature.length() - 1);
		char replacement = (last == 'A') ? 'B' : 'A';
		String tamperedSignature = signature.substring(0, signature.length() - 1) + replacement;
		return ((com.fasterxml.jackson.databind.node.ObjectNode) tree).put("signature", tamperedSignature).toString();
	}

}
