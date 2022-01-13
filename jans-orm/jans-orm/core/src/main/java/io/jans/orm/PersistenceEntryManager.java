/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.exception.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.operation.PersistenceOperationService;
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

    // TODO: Combine sortBy and SortOrder into Sort
    <T> PagedResult<T> findPagedEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
                                        SortOrder sortOrder, int start, int count, int chunkSize);

	void remove(Object entry);

	@Deprecated
	void remove(String dn);
	<T> void remove(String primaryKey, Class<T> entryClass);

	<T> int remove(String primaryKey, Class<T> entryClass, Filter filter, int count);
	
	@Deprecated
    void removeRecursively(String primaryKey);

	<T> void removeRecursively(String primaryKey, Class<T> entryClass);

    boolean hasBranchesSupport(String primaryKey);
    boolean hasExpirationSupport(String primaryKey);
	String getPersistenceType();
    String getPersistenceType(String primaryKey);

    Date decodeTime(String primaryKey, String date);
    String encodeTime(String primaryKey, Date date);

    int getHashCode(Object entry);

    String[] getObjectClasses(Object entry, Class<?> entryClass);

    <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String groupByProperties,
                                              String sumByProperties);

    void addDeleteSubscriber(DeleteNotifier subscriber);
    void removeDeleteSubscriber(DeleteNotifier subscriber);

    <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String... sortByProperties);

    List<AttributeData> exportEntry(String dn);

    <T> void importEntry(String dn, Class<T> entryClass, List<AttributeData> data);

    PersistenceOperationService getOperationService();
    PersistenceEntryManager getPersistenceEntryManager(String persistenceType);

    void setPersistenceExtension(PersistenceExtension persistenceExtension);

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

    default void refresh(Object entry) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

    default void setFlushMode(FlushModeType flushMode) {
        throw new UnsupportedOperationException("Method not implemented.");
    }

}
