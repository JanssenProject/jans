/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import java.util.List;
import java.util.Optional;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;

/**
 * The persistence contract shared by the ingestion, correlation and receipt-chain services
 * (design §5, §8, §9, §10, §13). {@link io.jans.lock.service.trace.store.TraceKeys} is the single
 * key function every implementation must use; {@code InMemoryTraceStore} (test sources, this
 * task) and the ORM-backed store (task 13) are both required to pass the same
 * {@code TraceStoreContractTest} suite.
 *
 * <p>Every {@code find*} method is domain-scoped both because the composite key already embeds the
 * domain (design decision D-6) and, for the "bare identifier" lookups that intentionally omit part
 * of the identity (producer, execution authority), by an explicit {@code domainId} filter — a
 * client of one evidence domain must never see another domain's rows (design decision D-1).
 *
 * @author Yuriy Movchan
 */
public interface TraceStore {

	// -- records ------------------------------------------------------------------------------

	/**
	 * @throws DuplicateEntryException if a record with the same {@link RecordIdentity} already
	 *                                  exists (design decision D-8 step 5)
	 * @throws TraceStorageException   on any other persistence failure
	 */
	void insertRecord(StoredTraceRecord record) throws DuplicateEntryException, TraceStorageException;

	/**
	 * Replaces the three mutable correlation flags ({@code coverageGap}, {@code chainLinkFailure},
	 * {@code equivocation}) on the record identified by {@code id}; {@code late} is fixed at
	 * ingestion and is never changed here (design §10, T-1, design decision D-9).
	 *
	 * @return {@code true} if a matching record was found and updated, {@code false} otherwise
	 * @throws TraceStorageException on a persistence failure
	 */
	boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags) throws TraceStorageException;

	Optional<StoredTraceRecord> findRecord(RecordIdentity id);

	/**
	 * Bare-{@code record_id} lookup, ignoring {@code producer_id} — used to detect the
	 * {@code ambiguous_identifier} case (design §14).
	 */
	List<StoredTraceRecord> findRecordsByBareRecordId(String domainId, String recordId);

	/**
	 * @return records for the given execution, ordered by {@code receiptSequence} ascending
	 *         (stable), paged by {@code start}/{@code count}
	 */
	List<StoredTraceRecord> findRecordsByExecution(ExecutionIdentity exec, int start, int count);

	/**
	 * Bare-{@code trace_execution_id} lookup, ignoring {@code execution_authority} — distinct
	 * authorities that have used this execution id in the domain (design §14).
	 */
	List<String> findExecutionAuthorities(String domainId, String traceExecutionId);

	/**
	 * @return every record at this chain position; more than one row is an equivocation, not an
	 *         error (design decision D-9)
	 */
	List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position);

	List<StoredTraceRecord> findRecordsByCapability(String domainId, String capabilityId);

	List<StoredTraceRecord> findRecordsByToken(String domainId, TokenRef token);

	long countRecords(String domainId);

	// -- receipts -------------------------------------------------------------------------------

	/**
	 * Inserts a receipt-chain position allocation claim. The caller sets only the business fields
	 * ({@code domainId}, {@code receiptSeq}, {@code receivedAt(Ms)}, {@code producerId},
	 * {@code recordId}, {@code recordKey}, {@code contentDigest}, {@code prevReceiptHash},
	 * {@code receiptHash}, {@code receiptState}, {@code nodeId}); the store computes and sets
	 * {@code id}/{@code dn} from the D-6 key function.
	 *
	 * @throws DuplicateEntryException if a receipt already exists at this
	 *                                  {@code (domainId, receiptSequence)} (design decision D-8
	 *                                  step 4 — another node claimed the sequence)
	 * @throws TraceStorageException   on any other persistence failure
	 */
	void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException, TraceStorageException;

	/**
	 * @return {@code true} if a matching receipt was found and updated, {@code false} otherwise
	 * @throws TraceStorageException on a persistence failure
	 */
	boolean updateReceiptState(String domainId, long receiptSequence, TraceReceiptState state)
			throws TraceStorageException;

	/**
	 * @return the receipt with the highest {@code receiptSequence} in the domain, in any state
	 *         (design decision D-8 step 2)
	 */
	Optional<ReceiptHead> findReceiptHead(String domainId);

	Optional<TraceReceiptEntry> findReceipt(String domainId, long receiptSequence);

	/**
	 * @return {@code PENDING} receipts received before {@code cutoffMs}, across every domain,
	 *         limited to {@code limit} rows — feeds {@code TraceReceiptRepairTimer} (task 18)
	 */
	List<TraceReceiptEntry> findPendingReceiptsOlderThan(long cutoffMs, int limit);

	// -- registries -----------------------------------------------------------------------------

	Optional<TraceChainEntry> findChain(ChainIdentity id);

	/**
	 * Registers a producer chain. The caller sets only the business fields ({@code domainId},
	 * {@code producerId}, {@code producerInstanceId}, {@code producerChainId},
	 * {@code registeredBy}, {@code creationDate}); the store computes and sets {@code id}/
	 * {@code dn} from the D-6 key function.
	 *
	 * @throws DuplicateEntryException if this chain is already registered (admin:
	 *                                  {@code chain_already_exists})
	 * @throws TraceStorageException   on any other persistence failure
	 */
	void insertChain(TraceChainEntry registration) throws DuplicateEntryException, TraceStorageException;

	/**
	 * @param producerIdOrNull restricts the result to one producer, or {@code null} for all
	 *                          producers in the domain
	 */
	List<TraceChainEntry> findChains(String domainId, String producerIdOrNull);

	Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid);

	/**
	 * Registers a producer key. The caller sets only the business fields ({@code domainId},
	 * {@code producerId}, {@code kid}, {@code publicKeyJwk}, {@code validFrom}, {@code validUntil},
	 * {@code registeredBy}, {@code creationDate}); the store computes and sets {@code id}/
	 * {@code dn} from the D-6 key function.
	 *
	 * @throws DuplicateEntryException if this {@code (domainId, producerId, kid)} is already
	 *                                  registered (admin: {@code key_already_exists})
	 * @throws TraceStorageException   on any other persistence failure
	 */
	void insertProducerKey(TraceProducerKeyEntry key) throws DuplicateEntryException, TraceStorageException;

	/**
	 * Idempotent: revoking an already-revoked key succeeds without changing its
	 * {@code revokedAt}.
	 *
	 * @return {@code true} if a matching key was found (whether or not it was already revoked),
	 *         {@code false} if no such key exists
	 * @throws TraceStorageException on a persistence failure
	 */
	boolean revokeProducerKey(String domainId, String producerId, String kid, long revokedAtMs)
			throws TraceStorageException;

	/**
	 * @param producerIdOrNull restricts the result to one producer, or {@code null} for all
	 *                          producers in the domain
	 */
	List<TraceProducerKeyEntry> findProducerKeys(String domainId, String producerIdOrNull);

}
