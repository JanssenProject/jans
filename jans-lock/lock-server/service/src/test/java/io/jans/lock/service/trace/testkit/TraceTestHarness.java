/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.testkit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.correlation.TraceCorrelationService;
import io.jans.lock.service.trace.crypto.Ed25519Capability;
import io.jans.lock.service.trace.crypto.Ed25519Verifier;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.ingest.TraceIngestionService;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.TraceAssertionParser;
import io.jans.lock.service.trace.receipt.ChainVerificationReport;
import io.jans.lock.service.trace.receipt.TraceReceiptChain;
import io.jans.lock.service.trace.registry.ProducerChainRegistry;
import io.jans.lock.service.trace.registry.ProducerKeyRegistry;
import io.jans.lock.service.trace.retrieve.TraceRetrievalService;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.validate.EventKindValidatorRegistry;
import io.jans.lock.service.trace.verify.TraceVerificationService;

/**
 * Task 22 consolidated test harness: wires the real TRACE collaborators (parser, common/event-kind
 * validation, verification, correlation, receipt-chain allocation, ingestion, retrieval) around one
 * shared {@link InMemoryTraceStore} and a mutable {@link TraceConfiguration}, exactly the way
 * {@code TraceIngestionServiceTest} (task 19) wires them inline. Nothing here is mocked: every
 * collaborator, including {@link AppConfiguration}, is a real instance.
 *
 * <p>Every "now" in the suite flows through {@link #setNow(long)}: it fixes
 * {@link CommonAssertionValidator}'s clock (design D-11's {@code signed_at_in_future} check) and
 * becomes the default {@code received_at_ms} for {@link #ctx(String, String, String...)}, so a test
 * is fully deterministic without touching the system clock.
 */
public final class TraceTestHarness {

	public static final String DEFAULT_CLIENT_ID = "2200.abcd";

	public static final String DEFAULT_REGISTERED_BY = "2200.admin";

	public static final String DEFAULT_NODE_ID = "node-1";

	/** One second after the design-example fixtures' {@code trace.signed_at} (1700000000s). */
	public static final long DEFAULT_NOW_MS = 1_700_000_001_000L;

	private final InMemoryTraceStore store = new InMemoryTraceStore();

	private final AppConfiguration appConfiguration = new AppConfiguration();

	private final TraceConfiguration traceConfiguration = appConfiguration.getTraceConfiguration();

	private final ProducerKeyRegistry keyRegistry = new ProducerKeyRegistry();

	private final ProducerChainRegistry chainRegistry = new ProducerChainRegistry();

	private final CommonAssertionValidator commonValidator = new CommonAssertionValidator();

	private final TraceCorrelationService correlation = new TraceCorrelationService();

	private final TraceReceiptChain receiptChain = new TraceReceiptChain();

	private final TraceIngestionService ingestionService = new TraceIngestionService();

	private final TraceRetrievalService retrievalService = new TraceRetrievalService();

	private volatile long nowMs = DEFAULT_NOW_MS;

	public TraceTestHarness() {
		TraceTestKeys.installProvider();

		TraceAssertionParser parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);

		setField(commonValidator, "appConfiguration", appConfiguration);
		setField(commonValidator, "clock", fixedClock(nowMs));

		setField(keyRegistry, "traceStore", store);
		setField(keyRegistry, "log", LoggerFactory.getLogger(ProducerKeyRegistry.class));

		setField(chainRegistry, "traceStore", store);

		Ed25519Capability capability = new Ed25519Capability();
		capability.probe();

		TraceVerificationService verification = new TraceVerificationService();
		setField(verification, "log", LoggerFactory.getLogger(TraceVerificationService.class));
		setField(verification, "capability", capability);
		setField(verification, "parser", parser);
		setField(verification, "commonValidator", commonValidator);
		setField(verification, "eventKindRegistry", new EventKindValidatorRegistry());
		setField(verification, "keyRegistry", keyRegistry);
		setField(verification, "verifier", new Ed25519Verifier());

		setField(correlation, "log", LoggerFactory.getLogger(TraceCorrelationService.class));
		setField(correlation, "store", store);
		setField(correlation, "chainRegistry", chainRegistry);
		setField(correlation, "appConfiguration", appConfiguration);

		setField(receiptChain, "log", LoggerFactory.getLogger(TraceReceiptChain.class));
		setField(receiptChain, "store", store);
		setField(receiptChain, "appConfiguration", appConfiguration);

		setField(ingestionService, "log", LoggerFactory.getLogger(TraceIngestionService.class));
		setField(ingestionService, "verification", verification);
		setField(ingestionService, "correlation", correlation);
		setField(ingestionService, "receiptChain", receiptChain);
		setField(ingestionService, "store", store);

		setField(retrievalService, "traceStore", store);
	}

	// -- shared state access -----------------------------------------------------------------------

	public InMemoryTraceStore store() {
		return store;
	}

	public TraceConfiguration traceConfiguration() {
		return traceConfiguration;
	}

	/**
	 * Fixes "now" for both {@link CommonAssertionValidator}'s {@code signed_at_in_future} check and
	 * the default {@code received_at_ms} used by {@link #ctx(String, String, String...)}.
	 */
	public void setNow(long nowMs) {
		this.nowMs = nowMs;
		setField(commonValidator, "clock", fixedClock(nowMs));
	}

	public long now() {
		return nowMs;
	}

	// -- registries ---------------------------------------------------------------------------------

	public KeyPair registerKey(String domainId, String producerId, String kid) {
		return registerKey(domainId, producerId, kid, 0L, null);
	}

	/** Registers a caller-supplied key pair instead of generating one (e.g. to reuse it across domains). */
	public KeyPair registerKey(String domainId, String producerId, String kid, KeyPair keyPair) {
		keyRegistry.register(domainId, producerId, kid, TraceTestKeys.toJwk(keyPair.getPublic()), 0L, null,
				DEFAULT_REGISTERED_BY, 0L);
		return keyPair;
	}

	public KeyPair registerKey(String domainId, String producerId, String kid, long validFromMs, Long validUntilMs) {
		KeyPair keyPair = TraceTestKeys.generateKeyPair();
		keyRegistry.register(domainId, producerId, kid, TraceTestKeys.toJwk(keyPair.getPublic()), validFromMs,
				validUntilMs, DEFAULT_REGISTERED_BY, Math.max(validFromMs, 0L));
		return keyPair;
	}

	public void registerChain(String domainId, String producerId, String instanceId, String chainId) {
		registerChain(domainId, producerId, instanceId, chainId, nowMs);
	}

	public void registerChain(String domainId, String producerId, String instanceId, String chainId, long atMs) {
		chainRegistry.register(new ChainIdentity(domainId, producerId, instanceId, chainId), DEFAULT_REGISTERED_BY,
				atMs);
	}

	// -- request context ------------------------------------------------------------------------------

	/** @return a context received "now" ({@link #setNow(long)}), by {@link #DEFAULT_CLIENT_ID} */
	public TraceRequestContext ctx(String domainId, String... allowedProducerIds) {
		return ctx(domainId, DEFAULT_CLIENT_ID, allowedProducerIds);
	}

	/** @return a context received "now" ({@link #setNow(long)}) */
	public TraceRequestContext ctx(String domainId, String clientId, String... allowedProducerIds) {
		return ctx(domainId, clientId, nowMs, allowedProducerIds);
	}

	public TraceRequestContext ctx(String domainId, String clientId, long receivedAtMs, String... allowedProducerIds) {
		List<String> allowed = allowedProducerIds.length == 0 ? Collections.singletonList("*")
				: Arrays.asList(allowedProducerIds);
		return new TraceRequestContext(clientId, domainId, allowed, receivedAtMs, DEFAULT_NODE_ID);
	}

	// -- ingestion / retrieval ------------------------------------------------------------------------

	public AcceptanceResult ingest(String json, TraceRequestContext ctx) {
		return ingestionService.ingest(body(json), ctx);
	}

	public StoredTraceRecord getRecord(String domainId, String recordId, String producerIdOrNull) {
		return retrievalService.findRecord(domainId, recordId, producerIdOrNull);
	}

	public TraceRetrievalService.ExecutionResult getExecution(String domainId, String traceExecutionId,
			String executionAuthorityOrNull, int start, int count) {
		return retrievalService.getExecution(domainId, traceExecutionId, executionAuthorityOrNull, start, count);
	}

	/** Diagnostic walk of the whole domain (design §9); see {@link TraceReceiptChain#verifyDomain}. */
	public ChainVerificationReport verifyDomain(String domainId) {
		return receiptChain.verifyDomain(domainId, 100_000);
	}

	private static InputStream body(String json) {
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	private static Clock fixedClock(long epochMs) {
		return Clock.fixed(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC);
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

}
