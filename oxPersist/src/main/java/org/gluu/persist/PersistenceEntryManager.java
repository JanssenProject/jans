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

/**
 * Methods which Entry Manager must provide
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface PersistenceEntryManager {

    PersistenceOperationService getOperationService();

    void persist(Object entry);

    <T> T merge(T entry);

    void remove(Object entry);

    void removeRecursively(String dn);

    boolean contains(Object entry);

    <T> boolean contains(Class<T> entryClass, String primaryKey);

    <T> boolean contains(String primaryKey, Class<T> entryClass, Filter filter);

    <T> List<T> findEntries(Object entry);

    <T> T find(Class<T> entryClass, Object primaryKey);

    <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes);

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

    boolean authenticate(String bindDn, String password);

    boolean authenticate(String baseDN, String userName, String password);

    <T> int countEntries(Object entry);

    <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter);

    <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter,  SearchScope scope);

    int getHashCode(Object entry);

    String[] getObjectClasses(Object entry, Class<?> entryClass);

    <T> List<T> createEntities(Class<T> entryClass, Map<String, List<AttributeData>> entriesAttributes);

    String[] exportEntry(String dn);

    void addDeleteSubscriber(DeleteNotifier subscriber);

    void removeDeleteSubscriber(DeleteNotifier subscriber);

    boolean destroy();

    String encodeTime(Date date);

    Date decodeTime(String date);

    <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String... sortByProperties);

    <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String groupByProperties,
            String sumByProperties);

}
