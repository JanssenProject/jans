/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.operation.PersistenceOperationService;
import org.gluu.search.filter.Filter;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

/**
 * Methods which Entry Manager must provide
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface PersistenceEntryManager extends EntityManager {

    void addDeleteSubscriber(DeleteNotifier subscriber);

    boolean authenticate(String bindDn, String password);

    boolean authenticate(String baseDN, String userName, String password);

    <T> boolean contains(Class<T> entryClass, String primaryKey);

    <T> boolean contains(String primaryKey, Class<T> entryClass, Filter filter);

    <T> int countEntries(Object entry);

    <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter);

    <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter,  SearchScope scope);

    <T> List<T> createEntities(Class<T> entryClass, Map<String, List<AttributeData>> entriesAttributes);

    Date decodeTime(String date);

    boolean destroy();

    String encodeTime(Date date);

    String[] exportEntry(String dn);

    <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes);

    <T> List<T> findEntries(Object entry);

    /**
     * Search by sample
     *
     * @param entry Sample
     * @param count Maximum result set size
     * @return Result entries
     */
    <T> List<T> findEntries(Object entry, int count);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, int count);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, int count);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                            int start, int count, int chunkSize);

    <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                            BatchOperation<T> batchOperation, int start, int count, int chunkSize);

    // TODO: Combine sortBy and SortOrder into Sort
    <T> PagedResult<T> findPagedEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
                                        SortOrder sortOrder, int start, int count, int chunkSize);

    int getHashCode(Object entry);

    String[] getObjectClasses(Object entry, Class<?> entryClass);

    PersistenceOperationService getOperationService();

    <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String groupByProperties,
                                              String sumByProperties);

    void removeDeleteSubscriber(DeleteNotifier subscriber);

    void removeRecursively(String dn);

    <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String... sortByProperties);

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
