/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import io.jans.orm.annotation.AttributesList;
import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.PersistenceMetadata;
import io.jans.orm.model.SearchProjection;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.search.filter.Filter;

/**
 * Methods which Entry Manager must provide
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface PersistenceEntryManager extends EntityManager {

	enum PERSITENCE_TYPES {ldap, couchbase, sql, spanner, hybrid};

	@Deprecated
    boolean authenticate(String primaryKey, String password);
    <T> boolean authenticate(String primaryKey, Class<T> entryClass, String password);

    <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password);

	void persist(Object entry);

	Void merge(Object entry);

	/**
	 * Conditionally updates {@code entry}, succeeding only if the entry's currently stored
	 * {@code @Version} value still equals the value held on {@code entry} at call time. On success,
	 * increments the stored version and writes the same new value back onto {@code entry}.
	 *
	 * <p>Like {@link #merge(Object)}, this call pre-reads the entry's current attributes to build
	 * the diff. If the entry does not exist at all when the call begins, the backend's normal
	 * not-found behavior for {@code merge()} applies (e.g. {@code EntryPersistenceException}), not
	 * {@code VersionMismatchException} -- a backend cannot cheaply tell "never existed" apart from
	 * "deleted before this call" without an extra round trip, and this method does not add one.
	 * {@code VersionMismatchException} is reserved for a version that no longer matches at the final
	 * compare-and-swap, which also covers the entry being deleted in the race window between this
	 * call's own pre-read and its write.</p>
	 *
	 * @throws io.jans.orm.exception.MappingException if entry's class has more than one
	 *         {@code @Version} field, or that field's current value on {@code entry} is null
	 * @throws io.jans.orm.exception.VersionMismatchException if the stored version no longer matches
	 *         at the compare-and-swap (another writer updated first, or the entry was deleted after
	 *         this call's own pre-read)
	 * @throws io.jans.orm.exception.UnsupportedOperationException if the active persistence backend
	 *         does not implement versioned updates, or {@code entry}'s class has zero
	 *         {@code @Version} fields
	 */
	Void updateWithVersion(Object entry);

	/**
	 * Reports whether {@link #persistBatch(List)} is implemented atomically by this backend. Only the
	 * SQL backend implements it; every other backend returns {@code false} and its
	 * {@code persistBatch} throws {@link io.jans.orm.exception.UnsupportedOperationException}.
	 *
	 * <p>A hybrid entry manager returns {@code true} only when every configured backend supports it.
	 * Use {@link #isAtomicBatchSupported(String)} to ask for the backend that stores a given DN.</p>
	 *
	 * @return {@code true} when {@code persistBatch} is atomic on this backend
	 */
	default boolean isAtomicBatchSupported() {
		return false;
	}

	/**
	 * Reports whether {@link #persistBatch(List)} is atomic for entries stored under {@code primaryKey}.
	 * Single-backend managers return {@link #isAtomicBatchSupported()}; a hybrid entry manager returns
	 * the capability of the backend that the DN maps to.
	 *
	 * @param primaryKey DN (or base DN) of the entries to write
	 * @return {@code true} when {@code persistBatch} is atomic for that DN
	 */
	default boolean isAtomicBatchSupported(String primaryKey) {
		return isAtomicBatchSupported();
	}

	/**
	 * Inserts all entries in ONE transaction: either every entry is stored or none is. There are no
	 * partial results. Entries may be of different entity classes (and tables). The insertion order is
	 * the list order.
	 *
	 * <p>Each entry is mapped exactly as {@link #persist(Object)} maps it: the DN comes from the
	 * {@code @DN} property and is handled the same way, object classes and expiration are the same, and
	 * the {@code @Version} property of an entry is initialized exactly as {@code persist} initializes
	 * it (the entry object is updated in place before the write).</p>
	 *
	 * <p>An empty list is a no-op. The method is stateless and thread safe; on the SQL backend the
	 * batch is one transaction on one pooled connection. There is no general transaction API: the
	 * batch is the unit of atomicity.</p>
	 *
	 * <p>The default implementation always throws {@link io.jans.orm.exception.UnsupportedOperationException},
	 * also for an empty list. Callers check {@link #isAtomicBatchSupported()} first and fall back to
	 * {@link #persist(Object)} per entry.</p>
	 *
	 * @param entries entries to insert, in order
	 * @throws io.jans.orm.exception.MappingException {@code entries} is {@code null} or contains a
	 *         {@code null} element, or an entry cannot be mapped; nothing is stored
	 * @throws io.jans.orm.exception.EntryPersistenceException an entry with the same DN already exists:
	 *         the cause is a {@link io.jans.orm.exception.operation.DuplicateEntryException} (checked, so it
	 *         is wrapped exactly as {@code persist} wraps it), with {@code getDn()} set to the failing DN
	 *         when the backend reports it; the transaction is rolled back and nothing is stored. Any other
	 *         failure is an {@code EntryPersistenceException} without that cause; nothing is stored
	 * @throws io.jans.orm.exception.UnsupportedOperationException the backend has no atomic batch
	 */
	default void persistBatch(List<?> entries) {
		throw new io.jans.orm.exception.UnsupportedOperationException(String.format(
				"Atomic batch insert (persistBatch) is not supported by '%s' persistence backend", getPersistenceType()));
	}

	/**
	 * Reads entries by DN. Backends with a native implementation (SQL) read them in one round trip
	 * per table; the default implementation works on every backend and reads one DN at a time, so
	 * callers never need a capability check.
	 *
	 * <p>Missing DNs are absent from the result. Result keys are the DNs as passed; the iteration
	 * order of the result follows the first occurrence of each DN in {@code dns}. A DN passed more than
	 * once is read once. An empty collection returns an empty map.</p>
	 *
	 * @param entryClass entity class of all requested entries
	 * @param dns DNs to read
	 * @return found entries keyed by DN; never {@code null}
	 * @throws io.jans.orm.exception.MappingException {@code dns} is {@code null} or contains a
	 *         {@code null} or empty DN
	 * @throws io.jans.orm.exception.EntryPersistenceException a read fails for a reason other than a
	 *         missing entry. The default implementation checks existence with {@code contains}, which
	 *         reports a failed read as a missing entry on backends that swallow the read error, so a DN
	 *         can be absent from the result although the read failed. The SQL implementation reports
	 *         every read failure.
	 */
	default <T> Map<String, T> findByDns(Class<T> entryClass, Collection<String> dns) {
		if (dns == null) {
			throw new MappingException("DNs to find entries are null");
		}

		Map<String, T> result = new LinkedHashMap<>();
		Set<String> processed = new HashSet<>();
		for (String dn : dns) {
			if (dn == null || dn.isEmpty()) {
				throw new MappingException("DN to find entry is null");
			}
			if (!processed.add(dn) || !contains(dn, entryClass)) {
				continue;
			}

			try {
				result.put(dn, find(dn, entryClass, null));
			} catch (EntryPersistenceException ex) {
				// The entry can be removed between contains() and find(): skip it only in that case
				if (contains(dn, entryClass)) {
					throw ex;
				}
			}
		}

		return result;
	}

	@Deprecated
	boolean contains(Object entity);

    <T> boolean contains(String primaryKey, Class<T> entryClass);
    <T> boolean contains(String primaryKey, Class<T> entryClass, Filter filter);

    <T> int countEntries(Object entry);

    <T> int countEntries(String primaryKey, Class<T> entryClass, Filter filter);
    <T> int countEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope);

    <T> List<T> createEntities(Class<T> entryClass, Map<String, List<AttributeData>> entriesAttributes);

    <T> T find(Object primaryKey, Class<T> entryClass, String[] ldapReturnAttributes);

    /**
     * Search by sample
     *
     * @param entry Sample
     * @return Result entries
     */
    <T> List<T> findEntries(Object entry);
    <T> List<T> findEntries(Object entry, int count);

    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, int count);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, int count);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                            int start, int count, int chunkSize);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                            BatchOperation<T> batchOperation, int start, int count, int chunkSize);

    /**
     * Sorted search which returns at most count entries starting from start without calculating total
     * entries count. Prefer it over findPagedEntries when total count is not needed.
     * When sortBy is specified it replaces the entity default sort (@DataEntry sortBy) where the backend
     * supports it, so the database can read rows in index order. Backends which can't skip the count
     * fall back to findPagedEntries.
     */
    @SuppressWarnings("java:S107")
    default <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
                                    SortOrder sortOrder, int start, int count, int chunkSize) {
        return findPagedEntries(primaryKey, entryClass, filter, ldapReturnAttributes, sortBy, sortOrder, start, count, chunkSize)
                .getEntries();
    }

    // TODO: Combine sortBy and SortOrder into Sort
    <T> PagedResult<T> findPagedEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
                                        SortOrder sortOrder, int start, int count, int chunkSize);

    /**
     * Server-side GROUP BY / DISTINCT search. Returned rows are raw projections without DN
     * (they represent groups, not entries). PagedResult.totalEntriesCount is the total number
     * of groups (or distinct rows) matching the filter; start/count page over groups.
     */
    default <T> PagedResult<EntryData> findAggregatedEntries(String primaryKey, Class<T> entryClass, Filter filter,
            SearchProjection projection, int start, int count) {
        throw new UnsupportedOperationException(String.format(
                "Server-side aggregation is not supported by '%s' persistence backend", getPersistenceType()));
    }

    /**
     * SELECT DISTINCT over projection attributes, mapped to partial entity beans.
     * Only projected properties are populated and the DN property is null — returned beans
     * must not be passed to merge/remove. Projection must be distinct-only (no aggregates).
     */
    default <T> List<T> findDistinctEntries(String primaryKey, Class<T> entryClass, Filter filter,
            SearchProjection projection, int start, int count) {
        throw new UnsupportedOperationException(String.format(
                "Server-side DISTINCT is not supported by '%s' persistence backend", getPersistenceType()));
    }

	void remove(Object entry);

	@Deprecated
	void remove(String dn);
	<T> void removeByDn(String dn, String[] objectClasses);
	<T> void remove(String primaryKey, Class<T> entryClass);

	<T> int remove(String primaryKey, Class<T> entryClass, Filter filter, int count);
	
	@Deprecated
    void removeRecursively(String primaryKey);

	<T> void removeRecursively(String primaryKey, Class<T> entryClass);
	<T> void removeRecursivelyFromDn(String primaryKey, String[] objectClasses);

    boolean hasBranchesSupport(String primaryKey);
    boolean hasExpirationSupport(String primaryKey);
	String getPersistenceType();
    String getPersistenceType(String primaryKey);
    PersistenceMetadata getPersistenceMetadata(String primaryKey);
    Map<String, Map<String, AttributeType>> getTableColumnsMap();

    Date decodeTime(String primaryKey, String date);
    String encodeTime(String primaryKey, Date date);

    int getHashCode(Object entry);

    String[] getObjectClasses(Object entry, Class<?> entryClass);

    <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String groupByProperties,
                                              String sumByProperties);

    void addDeleteSubscriber(DeleteNotifier subscriber);
    void removeDeleteSubscriber(DeleteNotifier subscriber);

    <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String... sortByProperties);

    @Deprecated
    List<AttributeData> exportEntry(String dn);

	<T> List<AttributeData> exportEntry(String dn, String objectClass);

    <T> void importEntry(String dn, Class<T> entryClass, List<AttributeData> data);

    PersistenceOperationService getOperationService();
    PersistenceEntryManager getPersistenceEntryManager(String persistenceType);

    void setPersistenceExtension(PersistenceExtension persistenceExtension);

    <T> AttributeType getAttributeType(String primaryKey, Class<T> entryClass, String propertyName);

    Class<?> getCustomAttributesListItemType(Object entry, AttributesList attributesList,
			String propertyName);
	List<AttributeData> getAttributeDataListFromCustomAttributesList(Object entry, AttributesList attributesList,
			String propertyName);
	List<Object> getCustomAttributesListFromAttributeDataList(Object entry, AttributesList attributesList,
			String propertyName, Collection<AttributeData> attributes);
	<T> List<PropertyAnnotation> getEntryPropertyAnnotations(Class<T> entryClass); 

    boolean destroy();

    default void clear() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void close() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default Query createNamedQuery(String name) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        throw new UnsupportedOperationException("Method not implemented.");        
    }

    default Query createNativeQuery(String sqlString) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default Query createNativeQuery(String sqlString, @SuppressWarnings("rawtypes") Class resultClass) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default Query createNativeQuery(String sqlString, String resultSetMapping) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default Query createQuery(String qlString) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default Query createQuery(CriteriaUpdate updateQuery) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default Query createQuery(CriteriaDelete deleteQuery) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        throw new UnsupportedOperationException("Method not implemented.");
    }    

    default void flush() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default Object getDelegate() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default FlushModeType getFlushMode() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default <T> T getReference(Class<T> entryClass, Object primaryKey) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default EntityTransaction getTransaction() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default boolean isOpen() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void joinTransaction() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void lock(Object entry, LockModeType lockMode) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default void lock(Object entity, LockModeType lockMode,
            Map<String, Object> properties)  {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void refresh(Object entry) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default void refresh(Object entity, Map<String, Object> properties) {
        throw new UnsupportedOperationException("Method not implemented.");
    } 
    
    default void refresh(Object entity, LockModeType lockMode)  {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default void refresh(Object entity, LockModeType lockMode,
            Map<String, Object> properties) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void setFlushMode(FlushModeType flushMode) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        throw new UnsupportedOperationException("Method not implemented.");        
    }
    
    default StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, Class... resultClasses) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, String... resultSetMappings) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default boolean isJoinedToTransaction() {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> T unwrap(Class<T> cls) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default EntityManagerFactory getEntityManagerFactory() {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default CriteriaBuilder getCriteriaBuilder() {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default Metamodel getMetamodel() {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default EntityGraph<?> createEntityGraph(String graphName) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default  EntityGraph<?> getEntityGraph(String graphName) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void setProperty(String propertyName, Object value) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default LockModeType getLockMode(Object entity) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default void detach(Object entity) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> T find(Class<T> entityClass, Object primaryKey,
            LockModeType lockMode) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> T find(Class<T> entityClass, Object primaryKey, 
            Map<String, Object> properties) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
    
    default <T> T find(Class<T> entityClass, Object primaryKey,
            LockModeType lockMode, 
            Map<String, Object> properties) {
        throw new UnsupportedOperationException("Method not implemented.");
    }
}
