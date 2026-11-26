/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.util.StringHelper;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Turns a finished passkey ceremony into a signed FIDO_CEREMONY record and hands it to
 * {@link TraceLockTransport}. Runs on a background thread, so the ceremony's request thread never waits
 * for signing or for Lock. Does nothing unless {@code fido2TraceEnabled} is true and the ceremony has a
 * session, since the record needs {@code session_id}.
 */
@ApplicationScoped
public class TraceEmitter {

	static final String CHAIN_STATE_FILE_NAME = "trace-chain-state.json";

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private TraceLockTransport transport;

	/** Package-visible so tests can run deliveries inline. */
	Executor delivery;
	/** Package-visible so tests can supply the key without a keystore file. */
	Supplier<PrivateKey> keySupplier;

	/** Records waiting for delivery. A full queue drops the newest record and counts it; it never blocks a ceremony. */
	static final int QUEUE_CAPACITY = 1000;

	private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(QUEUE_CAPACITY), r -> {
				Thread thread = new Thread(r, "trace-emitter");
				thread.setDaemon(true);
				return thread;
			}, new ThreadPoolExecutor.AbortPolicy());

	private final AtomicLong droppedRecords = new AtomicLong();

	private final AtomicReference<Pipeline> pipelineRef = new AtomicReference<>();

	/** Everything that depends on one configuration object. A reload replaces the configuration, so this is rebuilt. */
	static final class Pipeline {
		final Fido2Configuration configuration;
		final TraceRecordBuilder builder;
		final TraceChainState chainState;

		Pipeline(Fido2Configuration configuration, TraceRecordBuilder builder, TraceChainState chainState) {
			this.configuration = configuration;
			this.builder = builder;
			this.chainState = chainState;
		}
	}

	/**
	 * Queues one record for a finished ceremony, without authenticator detail. Never throws and never blocks.
	 *
	 * @param sessionId the ceremony's session state; no record is built without one
	 */
	public void emit(TraceRecordBuilder.CeremonyType type, TraceRecordBuilder.Outcome outcome, String sessionId) {
		emit(type, outcome, sessionId, null, null);
	}

	/**
	 * Queues one record for a finished ceremony. Never throws and never blocks on signing or delivery.
	 *
	 * @param sessionId the ceremony's session state; no record is built without one
	 * @param authenticatorAttachment platform or cross-platform, or null when the ceremony did not report one
	 * @param credentialId the WebAuthn credential ID (base64url), or null; hashed into the record
	 */
	public void emit(TraceRecordBuilder.CeremonyType type, TraceRecordBuilder.Outcome outcome, String sessionId,
			String authenticatorAttachment, String credentialId) {
		Fido2Configuration configuration = appConfiguration.getFido2Configuration();
		if (configuration == null || !configuration.isFido2TraceEnabled()) {
			return;
		}
		if (StringHelper.isEmpty(sessionId)) {
			log.debug("TRACE record skipped: the ceremony has no session");
			return;
		}
		Runnable task = () -> deliver(configuration, type, outcome, sessionId, authenticatorAttachment, credentialId);
		try {
			if (delivery != null) {
				delivery.execute(task);
			} else {
				executor.execute(task);
			}
		} catch (RejectedExecutionException e) {
			droppedRecords.incrementAndGet();
			log.error("TRACE record dropped: the delivery queue is full or the emitter is shut down; dropped so far {}",
					droppedRecords.get());
		}
	}

	/** @return the number of records dropped because the queue was full or the emitter was shut down */
	public long droppedRecords() {
		return droppedRecords.get();
	}

	void deliver(Fido2Configuration configuration, TraceRecordBuilder.CeremonyType type,
			TraceRecordBuilder.Outcome outcome, String sessionId, String authenticatorAttachment, String credentialId) {
		try {
			Pipeline current = pipeline(configuration);
			ObjectNode unsigned = current.builder.build(new TraceRecordBuilder.Ceremony(type, outcome, sessionId,
					appConfiguration.getIssuer(), authenticatorAttachment, credentialId));
			ObjectNode signed = current.chainState.signAndAdvance(unsigned);
			// The transport reads the endpoint and credentials from the current configuration. A record queued
			// under an older configuration would be posted to the new one, so it is dropped instead.
			if (appConfiguration.getFido2Configuration() != configuration) {
				log.error("TRACE record {} not sent: the configuration changed after it was signed",
						signed.path("record_id").asText());
				return;
			}
			TraceLockTransport.Outcome result = transport.deliver(signed.toString());
			if (result != TraceLockTransport.Outcome.ACCEPTED) {
				log.error("TRACE record {} not delivered to Lock, outcome {}; later records will show a coverage gap",
						signed.path("record_id").asText(), result);
			}
		} catch (RuntimeException e) {
			log.error("Failed to build or sign a TRACE record, record not sent", e);
		}
	}

	private Pipeline pipeline(Fido2Configuration configuration) {
		Pipeline current = pipelineRef.get();
		if (current != null && current.configuration == configuration) {
			return current;
		}
		synchronized (this) {
			current = pipelineRef.get();
			if (current != null && current.configuration == configuration) {
				return current;
			}
			String producerId = configuration.getFido2TraceProducerId();
			if (StringHelper.isEmpty(producerId)) {
				throw new IllegalStateException("fido2TraceProducerId is not set");
			}
			PrivateKey key = keySupplier != null ? keySupplier.get() : TraceKeyLoader.fromConfiguration(configuration, s -> s);
			TraceSigner signer = new TraceSigner(key);
			Path stateFile = Path.of(configuration.getFido2TraceKeyStorePath()).resolveSibling(CHAIN_STATE_FILE_NAME);
			TraceChainState chainState = new TraceChainState(stateFile, producerId, signer);
			TraceRecordBuilder builder = new TraceRecordBuilder(producerId, producerId, Clock.systemUTC(),
					() -> UUID.randomUUID().toString(), configuration.isFido2TraceIncludeCredentialIdHash());
			current = new Pipeline(configuration, builder, chainState);
			pipelineRef.set(current);
			return current;
		}
	}

	@PreDestroy
	void shutdown() {
		executor.shutdown();
	}
}
