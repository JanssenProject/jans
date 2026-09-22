/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.ArrayList;
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
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ChainRegistration;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.ReceiptRow;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.store.DuplicateEntryException;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Production {@link TraceStore} over jans-orm's {@link PersistenceEntryManager} (design §5 step 8,
 * §12, §13; task 01 D-7, D-8, D-9). Duplicate detection is backend-agnostic: a {@code contains(dn)}
 * pre-check, then a re-probe of {@code contains(dn)} after any {@code EntryPersistenceException}
 * from {@code persist} — SQL backends never throw a {@code DuplicateEntryException} in the cause
 * chain, so that type is never inspected here (design decision D-8).
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
	private static final String ATTR_CAP_KEYS = "jansTraceCapKeys";
	private static final String ATTR_TOKEN_KEYS = "jansTraceTokenKeys";
	private static final String ATTR_RECEIPT_SEQ = "jansTraceReceiptSeq";
	private static final String ATTR_RECEIPT_HASH = "jansTraceReceiptHash";
	private static final String ATTR_RECEIPT_STATE = "jansTraceReceiptState";
	private static final String ATTR_RECEIVED_AT_MS = "jansTraceReceivedAtMs";
	private static final String ATTR_PRODUCER_ID = "jansTraceProducerId";

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private StaticConfiguration staticConfiguration;

	@PostConstruct
	public void init() {
		BaseDnConfiguration baseDnConfiguration = staticConfiguration.getBaseDn();
		String trace = baseDnConfiguration == null ? null : baseDnConfiguration.getTrace();
		log.debug("TRACE ORM store resolved base DN: {}", trace);
	}

	/**
	 * Test seam: bypasses CDI injection of {@link Logger}.
	 */
	void setLog(Logger log) {
		this.log = log;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link PersistenceEntryManager}.
	 */
	void setPersistenceEntryManager(PersistenceEntryManager persistenceEntryManager) {
		this.persistenceEntryManager = persistenceEntryManager;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link StaticConfiguration}.
	 */
	void setStaticConfiguration(StaticConfiguration staticConfiguration) {
		this.staticConfiguration = staticConfiguration;
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

	// -- duplicate-safe insert (design decision D-8) -------------------------------------------

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
	public void insertRecord(StoredTraceRecord record) throws DuplicateEntryException, TraceStorageException {
		String key = TraceKeys.recordKey(record.getIdentity());
		String dn = TraceKeys.recordDn(baseDn(), key);
		TraceRecordEntry entity = TraceEntityMapper.toEntity(record, dn);
		duplicateSafeInsert(dn, TraceRecordEntry.class, entity, key);
	}

	@Override
	public boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags) throws TraceStorageException {
		String key = TraceKeys.recordKey(id);
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
		// jansTraceLate is fixed at ingestion (design §10, T-1); never overwritten here.
		try {
			persistenceEntryManager.merge(entity);
		} catch (EntryPersistenceException e) {
			throw new TraceStorageException("merge_failed", "Failed to update record flags for " + key, e);
		}
		return true;
	}

	@Override
	public Optional<StoredTraceRecord> findRecord(RecordIdentity id) {
		String key = TraceKeys.recordKey(id);
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
			// Defense in depth (design decision D-9): the key already embeds the domain, so this
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
		String key = TraceKeys.executionKey(exec);
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
		String key = TraceKeys.chainPositionKey(position);
		Filter filter = Filter.createANDFilter(
				Filter.createEqualityFilter(ATTR_DOMAIN_ID, position.getChainIdentity().getDomainId()),
				Filter.createEqualityFilter(ATTR_CHAIN_POS_KEY, key));
		return toStoredList(persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class, filter));
	}

	@Override
	public List<StoredTraceRecord> findRecordsByCapability(String domainId, String capabilityId) {
		String key = TraceKeys.capabilityKey(domainId, capabilityId);
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
				Filter.createEqualityFilter(ATTR_CAP_KEYS, key).multiValued());
		return toStoredList(persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class, filter));
	}

	@Override
	public List<StoredTraceRecord> findRecordsByToken(String domainId, TokenRef token) {
		String key = TraceKeys.tokenKey(domainId, token);
		Filter filter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
				Filter.createEqualityFilter(ATTR_TOKEN_KEYS, key).multiValued());
		return toStoredList(persistenceEntryManager.findEntries(recordsBase(), TraceRecordEntry.class, filter));
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
	public void insertReceipt(ReceiptRow row) throws DuplicateEntryException, TraceStorageException {
		String key = TraceKeys.receiptKey(row.getDomainId(), row.getReceiptSequence());
		String dn = TraceKeys.receiptDn(baseDn(), key);
		TraceReceiptEntry entity = TraceEntityMapper.toEntity(row, dn);
		duplicateSafeInsert(dn, TraceReceiptEntry.class, entity, key);
	}

	@Override
	public boolean updateReceiptState(String domainId, long receiptSequence, TraceReceiptState state)
			throws TraceStorageException {
		String key = TraceKeys.receiptKey(domainId, receiptSequence);
		String dn = TraceKeys.receiptDn(baseDn(), key);
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
		entity.setReceiptState(state.name());
		try {
			persistenceEntryManager.merge(entity);
		} catch (EntryPersistenceException e) {
			throw new TraceStorageException("merge_failed", "Failed to update receipt state for " + key, e);
		}
		return true;
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
	public Optional<ReceiptRow> findReceipt(String domainId, long receiptSequence) {
		String key = TraceKeys.receiptKey(domainId, receiptSequence);
		String dn = TraceKeys.receiptDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceReceiptEntry.class)) {
			return Optional.empty();
		}
		try {
			TraceReceiptEntry entity = persistenceEntryManager.find(dn, TraceReceiptEntry.class, null);
			return Optional.of(TraceEntityMapper.toReceiptRow(entity));
		} catch (EntryPersistenceException e) {
			log.debug("TRACE receipt {} disappeared before read", key);
			return Optional.empty();
		}
	}

	@Override
	public List<ReceiptRow> findPendingReceiptsOlderThan(long cutoffMs, int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		Filter filter = Filter.createANDFilter(
				Filter.createEqualityFilter(ATTR_RECEIPT_STATE, TraceReceiptState.PENDING.name()),
				Filter.createLessOrEqualFilter(ATTR_RECEIVED_AT_MS, cutoffMs));
		List<TraceReceiptEntry> entities = persistenceEntryManager.findEntries(receiptsBase(), TraceReceiptEntry.class,
				filter, limit);
		return entities.stream().map(TraceEntityMapper::toReceiptRow).collect(Collectors.toList());
	}

	// -- registries -----------------------------------------------------------------------------

	@Override
	public Optional<ChainRegistration> findChain(ChainIdentity id) {
		String key = TraceKeys.chainKey(id);
		String dn = TraceKeys.chainDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceChainEntry.class)) {
			return Optional.empty();
		}
		try {
			TraceChainEntry entity = persistenceEntryManager.find(dn, TraceChainEntry.class, null);
			return Optional.of(TraceEntityMapper.toChainRegistration(entity));
		} catch (EntryPersistenceException e) {
			log.debug("TRACE chain {} disappeared before read", key);
			return Optional.empty();
		}
	}

	@Override
	public void insertChain(ChainRegistration registration) throws DuplicateEntryException, TraceStorageException {
		String key = TraceKeys.chainKey(registration.getChainIdentity());
		String dn = TraceKeys.chainDn(baseDn(), key);
		TraceChainEntry entity = TraceEntityMapper.toEntity(registration, dn);
		duplicateSafeInsert(dn, TraceChainEntry.class, entity, key);
	}

	@Override
	public List<ChainRegistration> findChains(String domainId, String producerIdOrNull) {
		Filter filter = producerIdOrNull == null ? Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId)
				: Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
						Filter.createEqualityFilter(ATTR_PRODUCER_ID, producerIdOrNull));
		List<TraceChainEntry> entities = persistenceEntryManager.findEntries(chainsBase(), TraceChainEntry.class,
				filter);
		return entities.stream().map(TraceEntityMapper::toChainRegistration).collect(Collectors.toList());
	}

	@Override
	public Optional<ProducerKey> findProducerKey(String domainId, String producerId, String kid) {
		String key = TraceKeys.producerKeyKey(domainId, producerId, kid);
		String dn = TraceKeys.producerKeyDn(baseDn(), key);
		if (!persistenceEntryManager.contains(dn, TraceProducerKeyEntry.class)) {
			return Optional.empty();
		}
		try {
			TraceProducerKeyEntry entity = persistenceEntryManager.find(dn, TraceProducerKeyEntry.class, null);
			return Optional.of(TraceEntityMapper.toProducerKey(entity));
		} catch (EntryPersistenceException e) {
			log.debug("TRACE producer key {} disappeared before read", key);
			return Optional.empty();
		}
	}

	@Override
	public void insertProducerKey(ProducerKey key) throws DuplicateEntryException, TraceStorageException {
		String mapKey = TraceKeys.producerKeyKey(key.getDomainId(), key.getProducerId(), key.getKid());
		String dn = TraceKeys.producerKeyDn(baseDn(), mapKey);
		TraceProducerKeyEntry entity = TraceEntityMapper.toEntity(key, dn);
		duplicateSafeInsert(dn, TraceProducerKeyEntry.class, entity, mapKey);
	}

	@Override
	public boolean revokeProducerKey(String domainId, String producerId, String kid, long revokedAtMs)
			throws TraceStorageException {
		String key = TraceKeys.producerKeyKey(domainId, producerId, kid);
		String dn = TraceKeys.producerKeyDn(baseDn(), key);
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
			// Idempotent: an already-revoked key keeps its original revokedAt.
			return true;
		}
		entity.setRevokedAt(new Date(revokedAtMs));
		try {
			persistenceEntryManager.merge(entity);
		} catch (EntryPersistenceException e) {
			throw new TraceStorageException("merge_failed", "Failed to revoke producer key " + key, e);
		}
		return true;
	}

	@Override
	public List<ProducerKey> findProducerKeys(String domainId, String producerIdOrNull) {
		Filter filter = producerIdOrNull == null ? Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId)
				: Filter.createANDFilter(Filter.createEqualityFilter(ATTR_DOMAIN_ID, domainId),
						Filter.createEqualityFilter(ATTR_PRODUCER_ID, producerIdOrNull));
		List<TraceProducerKeyEntry> entities = persistenceEntryManager.findEntries(keysBase(),
				TraceProducerKeyEntry.class, filter);
		return entities.stream().map(TraceEntityMapper::toProducerKey).collect(Collectors.toList());
	}

}
