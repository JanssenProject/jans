/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.registry;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.store.TraceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Business-level service over {@link TraceStore} for producer-chain pre-registration and the
 * genesis rule (design §8, T-3, design decisions D-5).
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class ProducerChainRegistry {

	static final String REASON_NOT_REGISTERED = "not_registered";

	static final String REASON_GENESIS_HASH = "genesis_hash";

	static final String REASON_SENTINEL_ON_NON_GENESIS = "sentinel_on_non_genesis";

	static final String REASON_EMPTY_PRODUCER_ID = "empty:producer_id";

	static final String REASON_LENGTH_PRODUCER_ID = "length:producer_id";

	static final String REASON_FORMAT_PRODUCER_ID = "format:producer_id";

	static final String REASON_EMPTY_PRODUCER_INSTANCE_ID = "empty:producer_instance_id";

	static final String REASON_LENGTH_PRODUCER_INSTANCE_ID = "length:producer_instance_id";

	static final String REASON_EMPTY_PRODUCER_CHAIN_ID = "empty:producer_chain_id";

	static final String REASON_LENGTH_PRODUCER_CHAIN_ID = "length:producer_chain_id";

	static final String REASON_DUPLICATE_CHAIN = "duplicate_chain";

	/** T-3: {@code jansTraceProducerInstanceId}/{@code jansTraceProducerChainId} are VARCHAR(255). */
	private static final int MAX_IDENTIFIER_LENGTH = 255;

	@Inject
	private TraceStore traceStore;

	/**
	 * @throws TraceValidationException {@code chain_not_registered} when {@code id} has not been
	 *                                  pre-registered (design decision D-5)
	 */
	public TraceChainEntry requireRegistered(ChainIdentity id) {
		return traceStore.findChain(id)
				.orElseThrow(() -> new TraceValidationException(TraceErrorResponseType.CHAIN_NOT_REGISTERED,
						REASON_NOT_REGISTERED));
	}

	/**
	 * Enforces design decision D-5: {@code sequenceNumber == 1} if and only if
	 * {@code prevRecordHash} is the zero-hash sentinel.
	 *
	 * @throws TraceValidationException {@code invalid_genesis} with reason {@code genesis_hash}
	 *                                  (genesis position without the sentinel) or
	 *                                  {@code sentinel_on_non_genesis} (the sentinel at a non-genesis
	 *                                  position)
	 */
	public void checkGenesis(long sequenceNumber, String prevRecordHash) {
		boolean isSentinel = TraceConstants.ZERO_HASH.equals(prevRecordHash);
		if (sequenceNumber == 1) {
			if (!isSentinel) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_GENESIS, REASON_GENESIS_HASH);
			}
		} else if (isSentinel) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_GENESIS,
					REASON_SENTINEL_ON_NON_GENESIS);
		}
	}

	/**
	 * Registers a producer chain. Chains are create-only (T-3): there is no update.
	 *
	 * @throws TraceValidationException {@code invalid_request} when {@code producerId},
	 *                                  {@code producerInstanceId} or {@code producerChainId} is
	 *                                  empty or longer than 255 characters, or when
	 *                                  {@code producerId} does not match the stable producer id
	 *                                  format ({@link TraceConstants#PRODUCER_PATTERN}) that a signed
	 *                                  assertion must carry
	 * @throws TraceConflictException   {@code chain_already_exists} when {@code id} is already
	 *                                  registered
	 */
	public TraceChainEntry register(ChainIdentity id, String registeredBy, long nowMs) {
		try {
			return insertChain(id, registeredBy, nowMs, TraceChainEntry.GENESIS_KIND_PRE_REGISTERED);
		} catch (DuplicateEntryException ex) {
			throw new TraceConflictException(TraceErrorResponseType.CHAIN_ALREADY_EXISTS, REASON_DUPLICATE_CHAIN);
		}
	}

	/**
	 * {@code FIRST_OBSERVED} lookup (design §8, D-23): returns the registered chain; when {@code id}
	 * is unregistered and the record is a genesis ({@code sequenceNumber == 1} with the zero-hash
	 * sentinel), registers the chain with genesis kind {@code first_observed_genesis} and
	 * {@code registeredBy} the submitting client. The caller must call this only after key
	 * resolution, event-kind scope check and signature verification succeeded. Lock only anchors
	 * the chain from the moment it sees it; the registration claims nothing about earlier history.
	 * A concurrent registration of the same chain counts as already registered.
	 *
	 * @throws TraceValidationException {@code invalid_genesis} when the chain is unregistered and the
	 *                                  position/hash pair violates the genesis rule, or
	 *                                  {@code chain_not_registered} when it is unregistered and the
	 *                                  record is not a genesis
	 */
	public TraceChainEntry requireRegisteredOrObserveGenesis(ChainIdentity id, long sequenceNumber,
			String prevRecordHash, String registeredBy, long nowMs) {
		Optional<TraceChainEntry> existing = traceStore.findChain(id);
		if (existing.isPresent()) {
			return existing.get();
		}
		checkGenesis(sequenceNumber, prevRecordHash);
		if (sequenceNumber != 1) {
			throw new TraceValidationException(TraceErrorResponseType.CHAIN_NOT_REGISTERED, REASON_NOT_REGISTERED);
		}
		try {
			return insertChain(id, registeredBy, nowMs, TraceChainEntry.GENESIS_KIND_FIRST_OBSERVED);
		} catch (DuplicateEntryException ex) {
			return traceStore.findChain(id).orElseThrow(() -> new TraceValidationException(
					TraceErrorResponseType.CHAIN_NOT_REGISTERED, REASON_NOT_REGISTERED));
		}
	}

	private TraceChainEntry insertChain(ChainIdentity id, String registeredBy, long nowMs, String genesisKind)
			throws DuplicateEntryException {
		requireIdentifier(id.getProducerId(), REASON_EMPTY_PRODUCER_ID, REASON_LENGTH_PRODUCER_ID);
		if (!TraceConstants.PRODUCER_PATTERN.matcher(id.getProducerId()).matches()) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_FORMAT_PRODUCER_ID);
		}
		requireIdentifier(id.getProducerInstanceId(), REASON_EMPTY_PRODUCER_INSTANCE_ID,
				REASON_LENGTH_PRODUCER_INSTANCE_ID);
		requireIdentifier(id.getProducerChainId(), REASON_EMPTY_PRODUCER_CHAIN_ID, REASON_LENGTH_PRODUCER_CHAIN_ID);

		TraceChainEntry registration = new TraceChainEntry();
		registration.setDomainId(id.getDomainId());
		registration.setProducerId(id.getProducerId());
		registration.setProducerInstanceId(id.getProducerInstanceId());
		registration.setProducerChainId(id.getProducerChainId());
		registration.setRegisteredBy(registeredBy);
		registration.setGenesisKind(genesisKind);
		registration.setCreationDate(new Date(nowMs));
		traceStore.insertChain(registration);
		return registration;
	}

	/**
	 * @param producerIdOrNull restricts the result to one producer, or {@code null} for all
	 *                          producers in the domain
	 */
	public List<TraceChainEntry> list(String domainId, String producerIdOrNull) {
		return traceStore.findChains(domainId, producerIdOrNull);
	}

	private void requireIdentifier(String value, String emptyReason, String lengthReason) {
		if (StringUtils.isEmpty(value)) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, emptyReason);
		}
		if (value.length() > MAX_IDENTIFIER_LENGTH) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, lengthReason);
		}
	}

}
