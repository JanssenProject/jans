/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.couchbase.operation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.gluu.persist.couchbase.impl.CouchbaseBatchOperationWraper;
import org.gluu.persist.couchbase.model.BucketMapping;
import org.gluu.persist.couchbase.operation.BaseOperationService;
import org.gluu.persist.exception.operation.AuthenticationException;
import org.gluu.persist.exception.operation.ConnectionException;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.exception.operation.PersistenceException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ListViewResponse;
import org.gluu.persist.model.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.StringHelper;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Delete;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.query.dsl.path.GroupByPath;
import com.couchbase.client.java.query.dsl.path.LimitPath;
import com.couchbase.client.java.query.dsl.path.MutateLimitPath;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.MutateInBuilder;
import com.couchbase.client.java.subdoc.MutationSpec;

/**
 * Base service which performs all supported Couchbase operations
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class CouchbaseOperationsServiceImpl implements BaseOperationService<CouchbaseConnectionProvider, JsonObject, MutationSpec, Expression, Sort> {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    public static final String DN = "dn";
    public static final String UID = "uid";
    public static final String SUCCESS = "success";
    public static final String USER_PASSWORD = "userPassword";
    public static final String OBJECT_CLASS = "objectClass";

    private CouchbaseConnectionProvider connectionProvider;

    @SuppressWarnings("unused")
    private CouchbaseOperationsServiceImpl() {
    }

    public CouchbaseOperationsServiceImpl(CouchbaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public CouchbaseConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    @Override
    public void setConnectionProvider(CouchbaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public boolean authenticate(final String key, final String password) throws SearchException, AuthenticationException {
        return authenticateImpl(key, password);
    }

    private boolean authenticateImpl(final String key, final String password) throws SearchException, AuthenticationException {
        // TODO: Implemenet
        return true;
    }

    @Override
    public boolean addEntry(String key, JsonObject jsonObject) throws DuplicateEntryException, PersistenceException {
        try {
            BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
            JsonDocument jsonDocument = JsonDocument.create(key, jsonObject);
            JsonDocument result = bucketMapping.getBucket().upsert(jsonDocument);
            if (result != null) {
                return true;
            }
        } catch (CouchbaseException ex) {
            throw new PersistenceException("Failed to add entry", ex);
        }

        return false;
    }

    @Override
    public boolean updateEntry(String key, JsonObject attrs) throws UnsupportedOperationException, SearchException {
        List<MutationSpec> mods = new ArrayList<MutationSpec>();

        for (Entry<String, Object> attrEntry : attrs.toMap().entrySet()) {
            String attributeName = attrEntry.getKey();
            Object attributeValue = attrEntry.getValue();
            if (attributeName.equalsIgnoreCase(CouchbaseOperationsServiceImpl.OBJECT_CLASS)
                    || attributeName.equalsIgnoreCase(CouchbaseOperationsServiceImpl.DN)
                    || attributeName.equalsIgnoreCase(CouchbaseOperationsServiceImpl.USER_PASSWORD)) {
                continue;
            } else {
                if (attributeValue != null) {
                    mods.add(new MutationSpec(Mutation.REPLACE, attributeName, attributeValue));
                }
            }
        }

        return updateEntry(key, mods);
    }

    @Override
    public boolean updateEntry(String key, List<MutationSpec> mods) throws UnsupportedOperationException, SearchException {
        try {
            BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
            MutateInBuilder builder = bucketMapping.getBucket().mutateIn(key);

            return modifyEntry(builder, mods);
        } catch (final CouchbaseException ex) {
            throw new SearchException("Failed to update entry", ex);
        }
    }

    protected boolean modifyEntry(MutateInBuilder builder, List<MutationSpec> mods) throws UnsupportedOperationException, SearchException {
        try {
            for (MutationSpec mod : mods) {
                Mutation type = mod.type();
                if (Mutation.DICT_ADD == type) {
                    builder.insert(mod.path(), mod.fragment());
                } else if (Mutation.REPLACE == type) {
                    builder.replace(mod.path(), mod.fragment());
                } else if (Mutation.DELETE == type) {
                    builder.remove(mod.path());
                } else {
                    throw new UnsupportedOperationException("Operation type '" + type + "' is not implemented");
                }
            }
            
            DocumentFragment<Mutation> result = builder.execute();
            if (result.size() > 0) {
                return result.status(0).isSuccess();
            }
            
            return false;
        } catch (final CouchbaseException ex) {
            throw new SearchException("Failed to update entry", ex);
        }
    }

    @Override
    public boolean delete(String key) throws PersistenceException {
        try {
            BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
            JsonDocument result = bucketMapping.getBucket().remove(key);

            return result != null;
        } catch (CouchbaseException ex) {
            throw new ConnectionException("Failed to delete entry", ex);
        }
    }

    @Override
    public boolean deleteRecursively(String key) throws PersistenceException {
        try {
            BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
            MutateLimitPath deleteQuery = Delete.deleteFrom(Expression.i(bucketMapping.getBucketName())).where(Expression.s("META().id").like("key" + "%"));

            N1qlQueryResult result = bucketMapping.getBucket().query(deleteQuery);
            if (!result.finalSuccess()) {
                throw new SearchException("Failed to delete entries. Query: '" + bucketMapping.toString() + "'");
            }
            
            return true;
        } catch (CouchbaseException ex) {
            throw new ConnectionException("Failed to delete entry", ex);
        }
    }

    @Override
    public JsonObject lookup(String key, String... attributes) throws SearchException {
        try {
            BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
            if (attributes == null) {
                JsonDocument doc = bucketMapping.getBucket().get(key);
                if (doc != null) {
                    return doc.content();
                }
                
            } else {
                N1qlQuery query = N1qlQuery.simple(Select.select(attributes).from(Expression.i(bucketMapping.getBucketName())).limit(1));
                N1qlQueryResult result = bucketMapping.getBucket().query(query);
                if (!result.finalSuccess() || (result.allRows().size() == 0)) {
                    throw new SearchException("Failed to lookup entry");
                }

                return result.allRows().get(0).value();
            }
        } catch (CouchbaseException ex) {
            throw new SearchException("Failed to lookup entry", ex);
        }

        return null;
    }

    @Override
    public <O> ListViewResponse<JsonObject> search(String key, Expression expression, SearchScope scope,
            int startIndex, int pageLimit, int count, Sort[] orderBy,
            CouchbaseBatchOperationWraper<O> batchOperationWraper, boolean returnCount, String... attributes) throws SearchException {
        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        Bucket bucket = bucketMapping.getBucket();

        BatchOperation<O> ldapBatchOperation = null;
        if (batchOperationWraper != null) {
            ldapBatchOperation = (BatchOperation<O>) batchOperationWraper.getBatchOperation();
        }

        if (log.isTraceEnabled()) {
            // Find whole DB search
            if (StringHelper.equalsIgnoreCase(key, "_")) {
                log.trace("Search in whole DB tree", new Exception());
            }
        }

        Expression scopeExpression;
        if (scope == null) {
            scopeExpression = null;
        } else if (SearchScope.BASE == scope) {
            scopeExpression = Expression.s("META().id").eq("key");
        } else {
            scopeExpression = Expression.s("META().id").like("key" + "%");
        }

        Expression finalExpression = scopeExpression.add(expression);

        String[] select = attributes;
        if (select == null) {
            select = new String[] { "*" };
        }
        GroupByPath selectQuery = Select.select(select).from(Expression.i(bucketMapping.getBucketName())).where(finalExpression);
        
        LimitPath baseQuery = selectQuery;
        if (orderBy != null) {
            baseQuery = selectQuery.orderBy(orderBy);
        }

        boolean useCount = count > 0;

        if (useCount) {
            // Use paged result to limit search
            pageLimit = count;
        }

        List<N1qlQueryRow> searchResultList = new ArrayList<N1qlQueryRow>();

        N1qlQueryResult lastResult; 
        if ((pageLimit > 0) || (startIndex > 0)) {
            if (pageLimit == 0) {
                // Default page size
                pageLimit = 100;
            }

            boolean collectSearchResult;

            Statement query = null;
            try {
                List<N1qlQueryRow> lastSearchResultList;
                do {
                    collectSearchResult = true;

                    query = baseQuery.limit(pageLimit).offset(startIndex);
                    lastResult = bucket.query(query);
                    if (!lastResult.finalSuccess()) {
                        throw new SearchException("Failed to search entries. Query: '" + query.toString() + "'");
                    }
                    
                    lastSearchResultList = lastResult.allRows();

                    if (ldapBatchOperation != null) {
                        collectSearchResult = ldapBatchOperation.collectSearchResult(lastSearchResultList.size());
                    }
                    if (collectSearchResult) {
                        searchResultList.addAll(lastSearchResultList);
                    }

                    if (ldapBatchOperation != null) {
                        List<O> entries = batchOperationWraper.createEntities(lastSearchResultList);
                        ldapBatchOperation.performAction(entries);
                    }

                    if (useCount) {
                        break;
                    }
                } while (lastSearchResultList.size() > 0);
            } catch (CouchbaseException ex) {
                throw new SearchException("Failed to search entries. Query: '" + query + "'", ex);
            }
        } else {
            try {
                lastResult = bucket.query(baseQuery);
                if (!lastResult.finalSuccess()) {
                    throw new SearchException("Failed to search entries. Query: '" + baseQuery.toString() + "'");
                }
                
                searchResultList = lastResult.allRows();
            } catch (CouchbaseException ex) {
                throw new SearchException("Failed to search entries. Query: '" + baseQuery.toString() + "'", ex);
            }
        }

        List<JsonObject> resultRows = new ArrayList<JsonObject>(searchResultList.size());
        for (N1qlQueryRow row : searchResultList) {
            resultRows.add(row.value());
        }

        ListViewResponse<JsonObject> result = new ListViewResponse<JsonObject>();
        result.setItemsPerPage(resultRows.size());
        result.setStartIndex(startIndex);
        
        if (returnCount) {
            log.debug("Calculating count.. Query: '" + baseQuery.toString() + "'");
            GroupByPath selectCountQuery = Select.select("count(*)").from(Expression.i(bucketMapping.getBucketName())).where(finalExpression);
            try {
                N1qlQueryResult countResult = bucket.query(baseQuery);
                if (!lastResult.finalSuccess()) {
                    throw new SearchException("Failed to calculate count entries. Query: '" + selectCountQuery.toString() + "'");
                }
                result.setTotalResults(countResult.info().resultCount());
            } catch (CouchbaseException ex) {
                throw new SearchException("Failed to calculate count entries. Query: '" + selectCountQuery.toString() + "'", ex);
            }
        }

        return result;
    }

    @Override
    public boolean isBinaryAttribute(String attribute) {
        return this.connectionProvider.isBinaryAttribute(attribute);
    }

    @Override
    public boolean isCertificateAttribute(String attribute) {
        return this.connectionProvider.isCertificateAttribute(attribute);
    }

    @Override
    public boolean destroy() {
        boolean result = true;

        if (connectionProvider != null) {
            try {
                connectionProvider.destory();
            } catch (Exception ex) {
                log.error("Failed to destory provider correctly");
                result = false;
            }
        }
        
        return result;
    }

}

