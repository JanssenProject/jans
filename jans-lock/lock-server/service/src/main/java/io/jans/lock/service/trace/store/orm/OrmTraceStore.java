/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.model.trace.entity.TraceRecordEntry;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.PolicyRecordQuery;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.VersionMismatchException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Production {@link TraceStore} over jans-orm's {@link PersistenceEntryManager} (design §5 step 8, §12, §13). Duplicate detection is backend-agnostic: a {@code contains(dn)}
 * pre-check, then a re-probe of {@code contains(dn)} after any {@code EntryPersistenceException}
 * from {@code persist} — SQL backends never throw a {@code DuplicateEntryException} in the cause
 * chain, so that type is never inspected here.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class OrmTraceStore implements TraceStore {

	private static final String ATTR_DOMAIN_ID = "jansTraceDomainId";
	private static final String ATTR_RECORD_ID = "jansTraceRecordId";
	private static final String ATTR_EXEC_KEY = "jansTraceExecKey";
	private static final String ATTR_EXEC_ID = "jansTraceExecId";
	private static final String ATTR_EXEC_AUTHORITY = "jansTraceExecAuthority";
	private static final String ATTR_CHAIN_POS_KEY = "jansTraceChainPosKey";
	private static final String ATTR_TOKEN_KEYS = "jansTraceTokenKeys";
	private static final String ATTR_RECEIPT_SEQ = "jansTraceReceiptSeq";

	private static final int RECEIPT_LOOKUP_BATCH = 100;
	private static final String ATTR_RECEIPT_HASH = "jansTraceReceiptHash";
	private static final String ATTR_RECEIPT_STATE = "jansTraceReceiptState";
	private static final String ATTR_RECEIVED_AT_MS = "jansTraceReceivedAtMs";
	private static final String ATTR_PRODUCER_ID = "jansTraceProducerId";
	private static final String ATTR_POLICY_STORE_ID = "jansTracePolicyStoreId";
	private static final String ATTR_POLICY_STORE_VERSION = "jansTracePolicyStoreVersion";
	private static final String ATTR_BUNDLE_HASH = "jansTraceBundleHash";

	/**
	 * Bound on {@code updateReceiptState}/{@code revokeProducerKey}'s internal, caller-invisible
	 * compare-and-swap retry: unlike {@code updateRecordFlags}, neither method
	 * has a multi-step "plan then insert" gap a caller needs to recover from, so a losing CAS here is
	 * always safe to retry with a fresh read and the same idempotent recomputation, fully inside this
	 * class.
	 */
	private static final int INTERNAL_CAS_RETRY_LIMIT = 3;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private TraceKeys traceKeys;

	@PostConstruct
	public void init() {
		BaseDnConfiguration baseDnConfiguration = staticConfiguration.getBaseDn();
		String trace = baseDnConfiguration == null ? null : baseDnConfiguration.getTrace();
		log.debug("TRACE ORM store resolved base DN: {}", trace);
	}

	private String baseDn() {
		BaseDnConfiguration baseDnConfiguration = staticConfiguration.getBaseDn();
		String trace = baseDnConfiguration == null ? null : baseDnConfiguration.getTrace();
		if (trace == null || trace.isEmpty()) {
			throw new IllegalStateException(
					"TRACE base DN is not configured; set base-dn.trace in static-conf.json");
		}
		return trace;
	}

	private String recordsBase() {
		return "ou=records," + baseDn();
	}

	private String receiptsBase() {
		return "ou=receipts," + baseDn();
	}

	private String chainsBase() {
		return "ou=chains," + baseDn();
	}

	private String keysBase() {
		return "ou=keys," + baseDn();
	}

	// -- duplicate-safe insert -------------------------------------------

	private <T> void duplicateSafeInsert(String dn, Class<T> entityClass, T entity, String key)
			throws DuplicateEntryException, TraceStorageException {
		if (persistenceEntryManager.contains(dn, entityClass)) {
			throw new DuplicateEntryException(key);
		}
		try {
			persistenceEntryManager.persist(entity);
		} catch (EntryPersistenceException e) {
			if (persistenceEntryManager.contains(dn, entityClass)) {
				throw new DuplicateEntryException(key, "Entry already exists: " + key, e);
			}
			throw new TraceStorageException("persist_failed",
					"Failed to persist " + entityClass.getSimpleName() + " " + key, e);
		}
	}

	// -- records ------------------------------------------------------------------------------

	@Override
	public void insertRecord(StoredTraceRecord stored) throws DuplicateEntryException, TraceStorageException {
		String key = traceKeys.recordKey(stored.getIdentity());
		String dn = TraceKeys.recordDn(baseDn(), key);
		TraceRecordEntry entity = TraceEntityMapper.toEntity(stored, dn, traceKeys);
		duplicateSafeInsert(dn, TraceRecordEntry.class, entity, key);
	}

	@Override
	public boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags, long expectedVersion)
			throws TraceStorageException {
		String key = traceKeys.recordKey(id);
		String dn = TraceKeys.recordDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceRecordEntry.class)) {
			return false;
		}
		TraceRecordEntry entity;
		try {
			entity = persistenceEntryManager.find(dn, TraceRecordEntry.class, null);
		} catch (EntryPersistenceException e) {
			log.debug("TRACE record {} disappeared before flag update", key);
			return false;
		}
		entity.setCoverageGap(flags.isCoverageGap());
		entity.setChainLinkFailure(flags.isChainLinkFailure());
		entity.setEquivocation(flags.isEquivocation());
		// jansTraceLate is fixed at ingestion (design §10); never overwritten here.
		entity.setVersion(expectedVersion);
		try {
			persistenceEntryManager.updateWithVersion(entity);
		} catch (VersionMismatchException e) {
			throw new TraceStorageException(TraceStore.VERSION_CONFLICT_REASON,
					"Version conflict updating record flags for " + key, e);
		} catch (EntryPersistenceException e) {
			log.debug("TRACE record {} disappeared before flag update", key);
			return false;
		}
		return true;
	}

	@Override
	public Optional<StoredTraceRecord> findRecord(RecordIdentity id) {
		String key = traceKeys.recordKey(id);
		String dn = TraceKeys.recordDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceRecordEntry.class)) {
			return Optional.empty();
		}
		TraceRecordEntry entity;
		try {
			entity = persistenceEntryManager.find(dn, TraceRecordEntry.class, null);
		} catch (EntryPersistenceException e) {
			log.debug("TRACE record {} disappeared before read", key);
			return Optional.empty();
		}
		if (!id.getDomainId().equals(entity.getDomainId())) {
			// Defense in depth: the key already embeds the domain, so this
			// should never happen, but a client of one domain must never see another's row.
			log.debug("TRACE record {} domain mismatch: expected {}", key, id.getDomainId());
			return Optional.empty();
		}
		return Optional.of(TraceEntityMapper.toStoredTraceRecord(entity));
	}

	@Override
	public List<StoredTraceRecord> findRecordsByBareRecordId(String domainId, String recordId) {
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
				Filter.createEqualityFilter(ATTR_RECORD_ID, recordId));
		return toStoredList(persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class, filter));
	}

	@Override
	public List<StoredTraceRecord> findRecordsByExecution(ExecutionIdentity exec, int start, int count) {
		if (count <= 0) {
			return Collections.emptyList();
		}
		String key = traceKeys.executionKey(exec);
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, exec.getDomainId()),
				Filter.createEqualityFilter(ATTR_EXEC_KEY, key));
		PagedResult<TraceRecordEntry> paged = persistenceEntryManager.findPagedEntries(recordsBase(),
				TraceRecordEntry.class, filter, null, ATTR_RECEIPT_SEQ, SortOrder.ASCENDING, Math.max(0, start), count,
				count);
		return toStoredList(paged.getEntries());
	}

	@Override
	public List<String> findExecutionAuthorities(String domainId, String traceExecutionId) {
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
				Filter.createEqualityFilter(ATTR_EXEC_ID, traceExecutionId));
		List<TraceRecordEntry> entities = persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class,
				filter, new String[] { ATTR_EXEC_AUTHORITY });
		Set<String> distinct = new LinkedHashSet<>();
		for (TraceRecordEntry entity : entities) {
			if (entity.getExecAuthority() != null) {
				distinct.add(entity.getExecAuthority());
			}
		}
		return new ArrayList<>(distinct);
	}

	@Override
	public List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position) {
		String key = traceKeys.chainPositionKey(position);
		Filter filter = Filter.createANDFilter(
				Filter.createEqualityFilter(ATTR_DOMAIN_ID, position.getChainIdentity().getDomainId()),
				Filter.createEqualityFilter(ATTR_CHAIN_POS_KEY, key));
		return toStoredList(persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class, filter));
	}

	@Override
	public List<StoredTraceRecord> findRecordsByToken(String domainId, TokenRef token) {
		String key = traceKeys.tokenKey(domainId, token);
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
				Filter.createEqualityFilter(ATTR_TOKEN_KEYS, key).multiValued());
		return toStoredList(persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class, filter));
	}

	@Override
	public List<StoredTraceRecord> findRecordsByPolicy(String domainId, PolicyRecordQuery query, int start, int count) {
		if (count <= 0) {
			return Collections.emptyList();
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId));
		if (query.getPolicyStoreId() != null) {
			filters.add(Filter.createEqualityFilter(ATTR_POLICY_STORE_ID, query.getPolicyStoreId()));
		}
		if (query.getPolicyStoreVersion() != null) {
			filters.add(Filter.createEqualityFilter(ATTR_POLICY_STORE_VERSION, query.getPolicyStoreVersion()));
		}
		if (query.getBundleHash() != null) {
			filters.add(Filter.createEqualityFilter(ATTR_BUNDLE_HASH, query.getBundleHash()));
		}
		if (query.getReceivedFromMs() != null) {
			filters.add(Filter.createGreaterOrEqualFilter(ATTR_RECEIVED_AT_MS, query.getReceivedFromMs()));
		}
		if (query.getReceivedToMs() != null) {
			filters.add(Filter.createLessOrEqualFilter(ATTR_RECEIVED_AT_MS, query.getReceivedToMs()));
		}
		PagedResult<TraceRecordEntry> paged = persistenceEntryManager.findPagedEntries(recordsBase(),
				TraceRecordEntry.class, Filter.createANDFilter(filters), null, ATTR_RECEIPT_SEQ, SortOrder.ASCENDING,
				Math.max(0, start), count, count);
		return toStoredList(paged.getEntries());
	}

	@Override
	public long countRecords(String domainId) {
		Filter filter = Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId);
		return persistenceEntryManager.countEntries(recordsBase(), TraceRecordEntry.class, filter);
	}

	private static List<StoredTraceRecord> toStoredList(List<TraceRecordEntry> entities) {
		return entities.stream().map(TraceEntityMapper::toStoredTraceRecord).collect(Collectors.toList());
	}

	// -- receipts -------------------------------------------------------------------------------

	@Override
	public void insertReceipt(TraceReceiptEntry entity) throws DuplicateEntryException, TraceStorageException {
		String key = traceKeys.receiptKey(entity.getDomainId(), entity.getReceiptSeq());
		String dn = TraceKeys.receiptDn(baseDn(), key);
		entity.setId(key);
		entity.setDn(dn);
		duplicateSafeInsert(dn, TraceReceiptEntry.class, entity, key);
	}

	@Override
	public boolean updateReceiptState(String domainId, long receiptSequence, TraceReceiptState state)
			throws TraceStorageException {
		String key = traceKeys.receiptKey(domainId, receiptSequence);
		String dn = TraceKeys.receiptDn(baseDn(), key);
		for (int attempt = 1; attempt <= INTERNAL_CAS_RETRY_LIMIT; attempt++) {
			if (!persistenceEntryManager.contains(dn, TraceReceiptEntry.class)) {
				return false;
			}
			TraceReceiptEntry entity;
			try {
				entity = persistenceEntryManager.find(dn, TraceReceiptEntry.class, null);
			} catch (EntryPersistenceException e) {
				log.debug("TRACE receipt {} disappeared before state update", key);
				return false;
			}
			// Idempotent: re-setting the same absolute state is always safe to retry.
			entity.setReceiptState(state.name());
			try {
				persistenceEntryManager.updateWithVersion(entity);
				return true;
			} catch (VersionMismatchException e) {
				log.debug("TRACE receipt {} version conflict updating state, retrying (attempt {}/{})", key, attempt,
						INTERNAL_CAS_RETRY_LIMIT);
			} catch (EntryPersistenceException e) {
				log.debug("TRACE receipt {} disappeared before state update", key);
				return false;
			}
		}
		throw new TraceStorageException("merge_failed",
				"Failed to update receipt state for " + key + " after " + INTERNAL_CAS_RETRY_LIMIT + " retries");
	}

	@Override
	public Optional<ReceiptHead> findReceiptHead(String domainId) {
		Filter filter = Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId);
		PagedResult<TraceReceiptEntry> paged = persistenceEntryManager.findPagedEntries(receiptsBase(),
				TraceReceiptEntry.class, filter, new String[] { ATTR_RECEIPT_SEQ, ATTR_RECEIPT_HASH }, ATTR_RECEIPT_SEQ,
				SortOrder.DESCENDING, 0, 1, 1);
		if (paged.getEntries().isEmpty()) {
			return Optional.empty();
		}
		TraceReceiptEntry entity = paged.getEntries().get(0);
		return Optional.of(new ReceiptHead(entity.getReceiptSeq() == null ? 0L : entity.getReceiptSeq(),
				entity.getReceiptHash()));
	}

	@Override
	public Optional<TraceReceiptEntry> findReceipt(String domainId, long receiptSequence) {
		String key = traceKeys.receiptKey(domainId, receiptSequence);
		String dn = TraceKeys.receiptDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceReceiptEntry.class)) {
			return Optional.empty();
		}
		try {
			return Optional.of(persistenceEntryManager.find(dn, TraceReceiptEntry.class, null));
		} catch (EntryPersistenceException e) {
			log.debug("TRACE receipt {} disappeared before read", key);
			return Optional.empty();
		}
	}

	@Override
	public List<TraceReceiptEntry> findReceipts(String domainId, Collection<Long> receiptSequences) {
		List<Long> sequences = new ArrayList<>(new LinkedHashSet<>(receiptSequences));
		List<TraceReceiptEntry> result = new ArrayList<>(sequences.size());
		for (int from = 0; from < sequences.size(); from += RECEIPT_LOOKUP_BATCH) {
			List<Filter> sequenceFilters = sequences.subList(from, Math.min(sequences.size(), from + RECEIPT_LOOKUP_BATCH))
					.stream().map(seq -> Filter.createEqualityFilter(ATTR_RECEIPT_SEQ, seq)).collect(Collectors.toList());
			Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
					Filter.createORFilter(sequenceFilters));
			result.addAll(persistenceEntryManager.findEntries(receiptsBase(), TraceReceiptEntry.class, filter,
					sequenceFilters.size()));
		}
		return result;
	}

	@Override
	public List<TraceReceiptEntry> findPendingReceiptsOlderThan(long cutoffMs, int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		// jans-orm has no strict less-than filter; milliseconds are integral, so "< cutoffMs" is
		// "<= cutoffMs - 1". Oldest-first ordering before the limit comes from the entity's
		// @DataEntry(sortByName) default sort.
		Filter filter = Filter.createANDFilter(
				Filter.createEqualityFilter(ATTR_RECEIPT_STATE, TraceReceiptState.PENDING.name()),
				Filter.createLessOrEqualFilter(ATTR_RECEIVED_AT_MS, cutoffMs - 1));
		return persistenceEntryManager.findEntries(receiptsBase(), TraceReceiptEntry.class, filter, limit);
	}

	// -- registries -----------------------------------------------------------------------------

	@Override
	public Optional<TraceChainEntry> findChain(ChainIdentity id) {
		String key = traceKeys.chainKey(id);
		String dn = TraceKeys.chainDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceChainEntry.class)) {
			return Optional.empty();
		}
		try {
			return Optional.of(persistenceEntryManager.find(dn, TraceChainEntry.class, null));
		} catch (EntryPersistenceException e) {
			log.debug("TRACE chain {} disappeared before read", key);
			return Optional.empty();
		}
	}

	@Override
	public void insertChain(TraceChainEntry entity) throws DuplicateEntryException, TraceStorageException {
		ChainIdentity identity = new ChainIdentity(entity.getDomainId(), entity.getProducerId(),
				entity.getProducerInstanceId(), entity.getProducerChainId());
		String key = traceKeys.chainKey(identity);
		String dn = TraceKeys.chainDn(baseDn(), key);
		entity.setId(key);
		entity.setDn(dn);
		duplicateSafeInsert(dn, TraceChainEntry.class, entity, key);
	}

	@Override
	public List<TraceChainEntry> findChains(String domainId, String producerIdOrNull) {
		Filter filter = producerIdOrNull == null ? Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId)
				: Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
						Filter.createEqualityFilter(ATTR_PRODUCER_ID, producerIdOrNull));
		return persistenceEntryManager.findEntries(chainsBase(), TraceChainEntry.class, filter);
	}

	@Override
	public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
		String key = traceKeys.producerKeyKey(domainId, producerId, kid);
		String dn = TraceKeys.producerKeyDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceProducerKeyEntry.class)) {
			return Optional.empty();
		}
		try {
			return Optional.of(persistenceEntryManager.find(dn, TraceProducerKeyEntry.class, null));
		} catch (EntryPersistenceException e) {
			log.debug("TRACE producer key {} disappeared before read", key);
			return Optional.empty();
		}
	}

	@Override
	public void insertProducerKey(TraceProducerKeyEntry entity) throws DuplicateEntryException, TraceStorageException {
		String key = traceKeys.producerKeyKey(entity.getDomainId(), entity.getProducerId(), entity.getKid());
		String dn = TraceKeys.producerKeyDn(baseDn(), key);
		entity.setId(key);
		entity.setDn(dn);
		duplicateSafeInsert(dn, TraceProducerKeyEntry.class, entity, key);
	}

	@Override
	public boolean revokeProducerKey(String domainId, String producerId, String kid, long revokedAtMs)
			throws TraceStorageException {
		String key = traceKeys.producerKeyKey(domainId, producerId, kid);
		String dn = TraceKeys.producerKeyDn(baseDn(), key);
		for (int attempt = 1; attempt <= INTERNAL_CAS_RETRY_LIMIT; attempt++) {
			if (!persistenceEntryManager.contains(dn, TraceProducerKeyEntry.class)) {
				return false;
			}
			TraceProducerKeyEntry entity;
			try {
				entity = persistenceEntryManager.find(dn, TraceProducerKeyEntry.class, null);
			} catch (EntryPersistenceException e) {
				log.debug("TRACE producer key {} disappeared before revoke", key);
				return false;
			}
			if (entity.getRevokedAt() != null) {
				// Idempotent: an already-revoked key (whether by us or a concurrent writer) keeps
				// its original revokedAt; a fresh re-read already reflects that, so just report
				// success rather than retrying.
				return true;
			}
			entity.setRevokedAt(new Date(revokedAtMs));
			try {
				persistenceEntryManager.updateWithVersion(entity);
				return true;
			} catch (VersionMismatchException e) {
				log.debug("TRACE producer key {} version conflict revoking, retrying (attempt {}/{})", key, attempt,
						INTERNAL_CAS_RETRY_LIMIT);
			} catch (EntryPersistenceException e) {
				log.debug("TRACE producer key {} disappeared before revoke", key);
				return false;
			}
		}
		throw new TraceStorageException("merge_failed",
				"Failed to revoke producer key " + key + " after " + INTERNAL_CAS_RETRY_LIMIT + " retries");
	}

	@Override
	public List<TraceProducerKeyEntry> findProducerKeys(String domainId, String producerIdOrNull) {
		Filter filter = producerIdOrNull == null ? Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId)
				: Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
						Filter.createEqualityFilter(ATTR_PRODUCER_ID, producerIdOrNull));
		return persistenceEntryManager.findEntries(keysBase(), TraceProducerKeyEntry.class, filter);
	}

}
