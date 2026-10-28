/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.PolicyRecordQuery;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;

/**
 * The persistence contract shared by the ingestion, correlation and receipt-chain services
 * (design §5, §8, §9, §10, §13). {@link io.jans.lock.service.trace.store.TraceKeys} is the single
 * key function every implementation must use; {@code InMemoryTraceStore} (test sources)
 * and the ORM-backed store are both required to pass the same
 * {@code TraceStoreContractTest} suite.
 *
 * <p>Every {@code find*} method is domain-scoped both because the composite key already embeds the
 * domain and, for the "bare identifier" lookups that intentionally omit part
 * of the identity (producer, execution authority), by an explicit {@code domainId} filter — a
 * client of one evidence domain must never see another domain's rows.
 *
 * @author Yuriy Movchan
 */
public interface TraceStore {

	/**
	 * {@link TraceStorageException#getReason()} value used when {@link #updateRecordFlags} loses a
	 * compare-and-swap race against the expected version. {@link io.jans.lock.service.trace.correlation.TraceCorrelationService} is the only
	 * caller that special-cases this reason for retry; any other reason keeps the pre-existing
	 * log-and-swallow behavior.
	 */
	String VERSION_CONFLICT_REASON = "version_conflict";

	// -- records ------------------------------------------------------------------------------

	/**
	 * @throws DuplicateEntryException if a record with the same {@link RecordIdentity} already
	 *                                  exists
	 * @throws TraceStorageException   on any other persistence failure
	 */
	void insertRecord(StoredTraceRecord stored) throws DuplicateEntryException, TraceStorageException;

	/**
	 * Replaces the three mutable correlation flags ({@code coverageGap}, {@code chainLinkFailure},
	 * {@code equivocation}) on the record identified by {@code id}, succeeding only if the record's
	 * currently stored version still equals {@code expectedVersion} (design §10); {@code late} is fixed at ingestion and is never changed here.
	 *
	 * @return {@code true} if a matching record was found and updated, {@code false} if no record
	 *         with this identity exists
	 * @throws TraceStorageException on a persistence failure, including a lost compare-and-swap
	 *                                race ({@link #VERSION_CONFLICT_REASON})
	 */
	boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags, long expectedVersion)
			throws TraceStorageException;

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
	 *         error
	 */
	List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position);

	List<StoredTraceRecord> findRecordsByToken(String domainId, TokenRef token);

	/**
	 * @return records in the domain whose indexed {@code trace.policy} release matches
	 *         {@code query}, ordered by {@code receiptSequence} ascending, paged by
	 *         {@code start}/{@code count}
	 */
	List<StoredTraceRecord> findRecordsByPolicy(String domainId, PolicyRecordQuery query, int start, int count);

	long countRecords(String domainId);

	// -- receipts -------------------------------------------------------------------------------

	/**
	 * Inserts a receipt-chain position allocation claim. The caller sets only the business fields
	 * ({@code domainId}, {@code receiptSeq}, {@code receivedAt(Ms)}, {@code producerId},
	 * {@code recordId}, {@code recordKey}, {@code contentDigest}, {@code prevReceiptHash},
	 * {@code receiptHash}, {@code receiptState}, {@code nodeId}); the store computes and sets
	 * {@code id}/{@code dn} from {@link TraceKeys}.
	 *
	 * @throws DuplicateEntryException if a receipt already exists at this
	 *                                  {@code (domainId, receiptSequence)} (another node claimed the sequence)
	 * @throws TraceStorageException   on any other persistence failure
	 */
	void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException, TraceStorageException;

	/**
	 * Moves a receipt out of {@code PENDING} by compare-and-swap (design §9): reads the row, checks its
	 * state and writes {@code target} only while the row is still {@code PENDING}. {@code COMMITTED}
	 * and {@code VOID} are terminal; a lost compare-and-swap is re-read and re-evaluated, so a
	 * terminal state is never overwritten.
	 *
	 * @param target {@code COMMITTED} or {@code VOID}
	 * @return {@link ReceiptSettlement#SETTLED}, {@link ReceiptSettlement#ALREADY_SETTLED} (the same
	 *         state is already stored), {@link ReceiptSettlement#CONFLICT} (the other terminal state
	 *         is stored) or {@link ReceiptSettlement#NOT_FOUND}
	 * @throws IllegalArgumentException if {@code target} is {@code PENDING}
	 * @throws TraceStorageException    on a persistence failure, including exhausted compare-and-swap
	 *                                   retries
	 */
	ReceiptSettlement settleReceipt(String domainId, long receiptSequence, TraceReceiptState target)
			throws TraceStorageException;

	/**
	 * @return the receipt with the highest {@code receiptSequence} in the domain, in any state
	 *
	 */
	Optional<ReceiptHead> findReceiptHead(String domainId);

	Optional<TraceReceiptEntry> findReceipt(String domainId, long receiptSequence);

	/**
	 * @return the domain's receipts at the given sequences, in one lookup; sequences without a
	 *         receipt are absent from the result
	 */
	List<TraceReceiptEntry> findReceipts(String domainId, Collection<Long> receiptSequences);

	// -- policy store releases (shared by every node, not domain-scoped) ------------------------

	Optional<TracePolicyReleaseEntry> findPolicyRelease(String policyStoreId, String bundleHash);

	/**
	 * Stores a release first seen by this node; the entry's id and DN are derived here.
	 *
	 * @throws DuplicateEntryException another node stored it first
	 */
	void insertPolicyRelease(TracePolicyReleaseEntry entry) throws DuplicateEntryException, TraceStorageException;

	/**
	 * Records that a node served the release at {@code servedAtMs}: the last-served time only moves
	 * forward (and the version with it), the first-served time only moves back. Concurrent callers
	 * keep the maximum and the minimum.
	 *
	 * @return {@code false} when no such release is stored
	 */
	boolean recordPolicyReleaseServed(String policyStoreId, String bundleHash, String policyStoreVersion,
			long servedAtMs) throws TraceStorageException;

	/** @return up to {@code limit} releases whose last-served time is before {@code cutoffMs} */
	List<TracePolicyReleaseEntry> findPolicyReleasesLastServedBefore(long cutoffMs, int limit);

	/**
	 * Deletes the release only if its last-served time is still before {@code cutoffMs}, in one
	 * conditional delete, so a release another node refreshed meanwhile is kept.
	 *
	 * @return {@code true} when a row was deleted; {@code false} when it was refreshed or is
	 *         already gone
	 */
	boolean removePolicyReleaseLastServedBefore(String policyStoreId, String bundleHash, long cutoffMs)
			throws TraceStorageException;

	/**
	 * @return {@code PENDING} receipts received strictly before {@code cutoffMs} (a receipt
	 *         received exactly at {@code cutoffMs} is excluded), across every domain, oldest
	 *         first by {@code receivedAtMs}, limited to {@code limit} rows — feeds
	 *         {@code TraceReceiptRepairTimer}
	 */
	List<TraceReceiptEntry> findPendingReceiptsOlderThan(long cutoffMs, int limit);

	// -- registries -----------------------------------------------------------------------------

	Optional<TraceChainEntry> findChain(ChainIdentity id);

	/**
	 * Registers a producer chain. The caller sets only the business fields ({@code domainId},
	 * {@code producerId}, {@code producerInstanceId}, {@code producerChainId},
	 * {@code registeredBy}, {@code creationDate}); the store computes and sets {@code id}/
	 * {@code dn} from {@link TraceKeys}.
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
	 * {@code dn} from {@link TraceKeys}.
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
