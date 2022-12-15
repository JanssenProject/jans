/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeType;
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
	List<AttributeData> getAttributesList(Object entry);

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
