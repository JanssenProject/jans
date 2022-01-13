/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.operation.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Delete;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.query.dsl.path.GroupByPath;
import com.couchbase.client.java.query.dsl.path.LimitPath;
import com.couchbase.client.java.query.dsl.path.MutateLimitPath;
import com.couchbase.client.java.query.dsl.path.OffsetPath;
import com.couchbase.client.java.query.dsl.path.ReturningPath;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.MutateInBuilder;
import com.couchbase.client.java.subdoc.MutationSpec;

import io.jans.orm.couchbase.impl.CouchbaseBatchOperationWraper;
import io.jans.orm.couchbase.model.BucketMapping;
import io.jans.orm.couchbase.model.SearchReturnDataType;
import io.jans.orm.couchbase.operation.CouchbaseOperationService;
import io.jans.orm.couchbase.operation.watch.OperationDurationUtil;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.extension.PersistenceExtension;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.DeleteException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.operation.auth.PasswordEncryptionHelper;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

/**
 * Base service which performs all supported Couchbase operations
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class CouchbaseOperationServiceImpl implements CouchbaseOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseOperationServiceImpl.class);

    private Properties props;
    private CouchbaseConnectionProvider connectionProvider;

    private ScanConsistency scanConsistency = ScanConsistency.NOT_BOUNDED;

    private boolean ignoreAttributeScanConsistency = false;
	private boolean attemptWithoutAttributeScanConsistency = true;
	private boolean enableScopeSupport = false;
	private boolean disableAttributeMapping = false;

	private PersistenceExtension persistenceExtension;


    @SuppressWarnings("unused")
    private CouchbaseOperationServiceImpl() {
    }

    public CouchbaseOperationServiceImpl(Properties props, CouchbaseConnectionProvider connectionProvider) {
        this.props = props;
        this.connectionProvider = connectionProvider;
        init();
    }
    
    private void init() {
        if (props.containsKey("connection.scan-consistency")) {
        	String scanConsistencyString = StringHelper.toUpperCase(props.get("connection.scan-consistency").toString());
        	this.scanConsistency = ScanConsistency.valueOf(scanConsistencyString);
        }

        if (props.containsKey("connection.ignore-attribute-scan-consistency")) {
        	this.ignoreAttributeScanConsistency = StringHelper.toBoolean(props.get("connection.ignore-attribute-scan-consistency").toString(), this.ignoreAttributeScanConsistency);
        }

        if (props.containsKey("connection.attempt-without-attribute-scan-consistency")) {
        	this.attemptWithoutAttributeScanConsistency = StringHelper.toBoolean(props.get("attempt-without-attribute-scan-consistency").toString(), this.attemptWithoutAttributeScanConsistency);
        }

        if (props.containsKey("connection.enable-scope-support")) {
        	this.enableScopeSupport = StringHelper.toBoolean(props.get("connection.enable-scope-support").toString(), this.enableScopeSupport);
        }

        if (props.containsKey("connection.disable-attribute-mapping")) {
        	this.disableAttributeMapping = StringHelper.toBoolean(props.get("connection.disable-attribute-mapping").toString(), this.disableAttributeMapping);
        }

        LOG.info("Option scanConsistency: " + scanConsistency);
        LOG.info("Option ignoreAttributeScanConsistency: " + ignoreAttributeScanConsistency);
        LOG.info("Option enableScopeSupport: " + enableScopeSupport);
        LOG.info("Option disableAttributeMapping: " + disableAttributeMapping);
    }

    @Override
    public CouchbaseConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

	@Override
	public boolean authenticate(String key, String password, String objectClass) throws ConnectionException, SearchException, AuthenticationException {
		return authenticateImpl(key, password);
	}

    private boolean authenticateImpl(final String key, final String password) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        boolean result = false;
        if (password != null) {
	        JsonObject entry = lookup(key, null, USER_PASSWORD);
	        Object userPasswordObj = entry.get(USER_PASSWORD);
	
	        String userPassword = null;
	        if (userPasswordObj instanceof JsonArray) {
	            userPassword = ((JsonArray) userPasswordObj).getString(0);
	        } else if (userPasswordObj instanceof String) {
	            userPassword = (String) userPasswordObj;
	        }
	
	        if (userPassword != null) {
	        	if (persistenceExtension == null) {
		        	result = PasswordEncryptionHelper.compareCredentials(password, userPassword);
	        	} else {
	        		result = persistenceExtension.compareHashedPasswords(password, userPassword);
	        	}
	        }
        }

        Duration duration = OperationDurationUtil.instance().duration(startTime);

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        OperationDurationUtil.instance().logDebug("Couchbase operation: bind, duration: {}, bucket: {}, key: {}", duration, bucketMapping.getBucketName(), key);
        
        return result;
    }

    @Override
    public boolean addEntry(String key, JsonObject jsonObject) throws DuplicateEntryException, PersistenceException {
    	return addEntry(key, jsonObject, 0);
    }

    @Override
    public boolean addEntry(String key, JsonObject jsonObject, Integer expiration) throws DuplicateEntryException, PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        boolean result = addEntryImpl(bucketMapping, key, jsonObject, expiration);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: add, duration: {}, bucket: {}, key: {}, json: {}", duration, bucketMapping.getBucketName(), key, jsonObject);
        
        return result;
    }

	private boolean addEntryImpl(BucketMapping bucketMapping, String key, JsonObject jsonObject, Integer expiration) throws PersistenceException {
		try {
			JsonDocument jsonDocument; 
			if (expiration == null) {
	            jsonDocument = JsonDocument.create(key, jsonObject);
			} else {
	            jsonDocument = JsonDocument.create(key, expiration, jsonObject);
			}

			JsonDocument result = bucketMapping.getBucket().upsert(jsonDocument);
            if (result != null) {
                return true;
            }

        } catch (CouchbaseException ex) {
            throw new PersistenceException("Failed to add entry", ex);
        }

        return false;
	}

    @Deprecated
    protected boolean updateEntry(String key, JsonObject attrs) throws UnsupportedOperationException, PersistenceException {
        List<MutationSpec> mods = new ArrayList<MutationSpec>();

        for (Entry<String, Object> attrEntry : attrs.toMap().entrySet()) {
            String attributeName = attrEntry.getKey();
            Object attributeValue = attrEntry.getValue();
            if (attributeName.equalsIgnoreCase(CouchbaseOperationService.OBJECT_CLASS) || attributeName.equalsIgnoreCase(CouchbaseOperationService.DN)
                    || attributeName.equalsIgnoreCase(CouchbaseOperationService.USER_PASSWORD)) {
                continue;
            } else {
                if (attributeValue != null) {
                    mods.add(new MutationSpec(Mutation.REPLACE, attributeName, attributeValue));
                }
            }
        }

        return updateEntry(key, mods, null);
    }

    @Override
    public boolean updateEntry(String key, List<MutationSpec> mods, Integer expiration) throws UnsupportedOperationException, PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        boolean result = updateEntryImpl(bucketMapping, key, mods, expiration);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: modify, duration: {}, bucket: {}, key: {}, mods: {}", duration, bucketMapping.getBucketName(), key, mods);

        return result;
    }

	private boolean updateEntryImpl(BucketMapping bucketMapping, String key, List<MutationSpec> mods, Integer expiration) throws PersistenceException {
		try {
            MutateInBuilder builder = bucketMapping.getBucket().mutateIn(key);
            if (expiration != null) {
            	builder = builder.withExpiry(expiration);
            }

            return modifyEntry(builder, mods);
        } catch (final CouchbaseException ex) {
            throw new PersistenceException("Failed to update entry", ex);
        }
	}

    protected boolean modifyEntry(MutateInBuilder builder, List<MutationSpec> mods) throws UnsupportedOperationException, PersistenceException {
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
            throw new PersistenceException("Failed to update entry", ex);
        }
    }

    @Override
    public boolean delete(String key) throws EntryNotFoundException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        boolean result = deleteImpl(bucketMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: delete, duration: {}, bucket: {}, key: {}", duration, bucketMapping.getBucketName(), key);

        return result;
    }

	private boolean deleteImpl(BucketMapping bucketMapping, String key) throws EntryNotFoundException {
		try {
            JsonDocument result = bucketMapping.getBucket().remove(key);

            return (result != null) && (result.id() != null);
        } catch (CouchbaseException ex) {
            throw new EntryNotFoundException("Failed to delete entry", ex);
        }
	}

    @Override
    public int delete(String key, ScanConsistency scanConsistency, Expression expression, int count) throws DeleteException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
    	ScanConsistency useScanConsistency = getScanConsistency(scanConsistency, false);

    	int result = deleteImpl(bucketMapping, key, useScanConsistency, expression, count);

        String attemptInfo = getScanAttemptLogInfo(scanConsistency, useScanConsistency, false);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: delete_search, duration: {}, bucket: {}, key: {}, expression: {}, count: {}, consistency: {}{}", duration, bucketMapping.getBucketName(), key, expression, count, useScanConsistency, attemptInfo);

        return result;
    }

    private int deleteImpl(BucketMapping bucketMapping, String key, ScanConsistency scanConsistency, Expression expression, int count) throws DeleteException {
        Bucket bucket = bucketMapping.getBucket();

        Expression finalExpression = expression;
        if (enableScopeSupport) { 
			Expression scopeExpression = Expression.path("META().id").like(Expression.s(key + "%"));
			finalExpression = scopeExpression.and(expression);
        }

        MutateLimitPath deleteQuery = Delete.deleteFrom(Expression.i(bucketMapping.getBucketName())).where(finalExpression);
        ReturningPath query = deleteQuery.limit(count);
        LOG.debug("Execution query: '" + query + "'");

        N1qlQueryResult result = bucket.query(N1qlQuery.simple(query, N1qlParams.build().consistency(scanConsistency)));
        if (!result.finalSuccess()) {
            throw new DeleteException(String.format("Failed to delete entries. Query: '%s'. Error: '%s', Error count: '%d'", query, result.errors(),
            		result.info().errorCount()), result.errors().get(0).getInt("code"));
        }

        return result.info().mutationCount();
	}

    @Override
    public boolean deleteRecursively(String key) throws EntryNotFoundException, SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        boolean result = deleteRecursivelyImpl(bucketMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: delete_tree, duration: {}, bucket: {}, key: {}", duration, bucketMapping.getBucketName(), key);

        return result;
    }

	private boolean deleteRecursivelyImpl(BucketMapping bucketMapping, String key) throws SearchException, EntryNotFoundException {
		try {
	        if (enableScopeSupport) {
	            MutateLimitPath deleteQuery = Delete.deleteFrom(Expression.i(bucketMapping.getBucketName()))
	                    .where(Expression.path("META().id").like(Expression.s(key + "%")));
	
	            N1qlQueryResult result = bucketMapping.getBucket().query(deleteQuery);
	            if (!result.finalSuccess()) {
                    throw new SearchException(String.format("Failed to delete entries. Query: '%s'. Error: '%s', Error count: '%d'", deleteQuery, result.errors(),
                    		result.info().errorCount()), result.errors().get(0).getInt("code"));
	            }
	        } else {
	        	LOG.warn("Removing only base key without sub-tree: " + key);
	        	delete(key);
	        }
	    	
            return true;
        } catch (CouchbaseException ex) {
            throw new EntryNotFoundException("Failed to delete entry", ex);
        }
	}

    @Override
    public JsonObject lookup(String key, ScanConsistency scanConsistency, String... attributes) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
    	BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);

        boolean secondTry = false; 
    	ScanConsistency useScanConsistency = getScanConsistency(scanConsistency, attemptWithoutAttributeScanConsistency);
        JsonObject result = null;
        SearchException lastException = null;
		try {
			result = lookupImpl(bucketMapping, key, useScanConsistency, attributes);
		} catch (SearchException ex) {
			lastException = ex;
		}
        if ((result == null) || result.isEmpty()) {
        	ScanConsistency useScanConsistency2 = getScanConsistency(scanConsistency, false);
        	if (!useScanConsistency2.equals(useScanConsistency)) {
        		useScanConsistency = useScanConsistency2;
                secondTry = true; 
                result = lookupImpl(bucketMapping, key, useScanConsistency, attributes);
        	} else {
        		if (lastException != null) {
            		throw lastException;
        		}
        	}
        }

        String attemptInfo = getScanAttemptLogInfo(scanConsistency, useScanConsistency, secondTry);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: lookup, duration: {}, bucket: {}, key: {}, attributes: {}, consistency: {}{}", duration, bucketMapping.getBucketName(), key, attributes, useScanConsistency, attemptInfo);

        return result;
    }

	private JsonObject lookupImpl(BucketMapping bucketMapping, String key, ScanConsistency scanConsistency, String... attributes) throws SearchException {
		try {
            Bucket bucket = bucketMapping.getBucket();
            if (ArrayHelper.isEmpty(attributes)) {
                JsonDocument doc = bucket.get(key);
                if (doc != null) {
                    return doc.content();
                }

            } else {
                JsonDocument doc = bucket.get(key);
                if (doc != null) {
                	Set<String> docAtributesKeep = new HashSet<String>(Arrays.asList(attributes));
//                	docAtributesKeep.add(CouchbaseOperationService.DN);

                	for (Iterator<String> it = doc.content().getNames().iterator(); it.hasNext();) {
						String docAtribute = (String) it.next();
						if (!docAtributesKeep.contains(docAtribute)) {
							it.remove();
						}
					}

                	return doc.content();
                }

//            	N1qlParams params = N1qlParams.build().consistency(scanConsistency);
//            	OffsetPath select = Select.select(attributes).from(Expression.i(bucketMapping.getBucketName())).useKeys(Expression.s(key)).limit(1);
//                N1qlQueryResult result = bucket.query(N1qlQuery.simple(select, params));
//                if (!result.finalSuccess()) {
//                	throw new SearchException(String.format("Failed to lookup entry. Errors: %s", result.errors()), result.info().errorCount());
//                }
//
//                if (result.allRows().size() == 1) {
//                    return result.allRows().get(0).value();
//                }

            }
        } catch (CouchbaseException ex) {
            throw new SearchException("Failed to lookup entry", ex);
        }

        throw new SearchException("Failed to lookup entry");
	}

	@Override
    public <O> PagedResult<JsonObject> search(String key, ScanConsistency scanConsistency, Expression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
                                              CouchbaseBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);

        boolean secondTry = false;
    	ScanConsistency useScanConsistency = getScanConsistency(scanConsistency, attemptWithoutAttributeScanConsistency);
        PagedResult<JsonObject> result = null;
        int attemps = 20;
        do {
			attemps--;
			try {
				result = searchImpl(bucketMapping, key, useScanConsistency, expression, scope, attributes, orderBy, batchOperationWraper,
						returnDataType, start, count, pageSize);
				break;
			} catch (SearchException ex) {
				if (ex.getErrorCode() != 5000) {
					throw ex;
				}
				
				LOG.warn("Waiting for Indexer Warmup...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ex2) {}
			}
        } while (attemps > 0);
        if ((result == null) || (result.getEntriesCount() == 0)) {
        	ScanConsistency useScanConsistency2 = getScanConsistency(scanConsistency, false);
        	if (!useScanConsistency2.equals(useScanConsistency)) {
        		useScanConsistency = useScanConsistency2;
                result = searchImpl(bucketMapping, key, useScanConsistency, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize);
                secondTry = true;
        	}
        }

        String attemptInfo = getScanAttemptLogInfo(scanConsistency, useScanConsistency, secondTry);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: search, duration: {}, bucket: {}, key: {}, expression: {}, scope: {}, attributes: {}, orderBy: {}, batchOperationWraper: {}, returnDataType: {}, start: {}, count: {}, pageSize: {}, consistency: {}{}", duration, bucketMapping.getBucketName(), key, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize, useScanConsistency, attemptInfo);

        return result;
	}

	private <O> PagedResult<JsonObject> searchImpl(BucketMapping bucketMapping, String key, ScanConsistency scanConsistency, Expression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
            CouchbaseBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        Bucket bucket = bucketMapping.getBucket();

        BatchOperation<O> batchOperation = null;
        if (batchOperationWraper != null) {
            batchOperation = (BatchOperation<O>) batchOperationWraper.getBatchOperation();
        }

        if (LOG.isTraceEnabled()) {
            // Find whole DB search
            if (StringHelper.equalsIgnoreCase(key, "_")) {
                LOG.trace("Search in whole DB tree", new Exception());
            }
        }

        Expression finalExpression = expression;
        if (enableScopeSupport) { 
			Expression scopeExpression;
			if (scope == null) {
				scopeExpression = null;
			} else if (SearchScope.BASE == scope) {
				scopeExpression = Expression.path("META().id").like(Expression.s(key + "%"))
						.and(Expression.path("META().id").notLike(Expression.s(key + "\\\\_%\\\\_")));
			} else {
				scopeExpression = Expression.path("META().id").like(Expression.s(key + "%"));
			}

			if (scopeExpression != null) {
				finalExpression = scopeExpression.and(expression);
			}
        } else {
            if (scope != null) {
            	LOG.debug("Ignoring scope '" + scope + " for expression: " + expression);
            }
        }

        String[] select = attributes;
        if (select == null) {
            select = new String[] { "jans_doc.*", CouchbaseOperationService.DN };
        } else if ((select.length == 1) && StringHelper.isEmpty(select[0])) {
        	// Compatibility with base persistence layer when application pass filter new String[] { "" }
            select = new String[] { CouchbaseOperationService.DN };
        } else {
            boolean hasDn = Arrays.asList(select).contains(CouchbaseOperationService.DN);
            if (!hasDn) {
                select = ArrayHelper.arrayMerge(select, new String[] { CouchbaseOperationService.DN });
            }
        }
        GroupByPath selectQuery = Select.select(select).from(Expression.i(bucketMapping.getBucketName())).as("jans_doc").where(finalExpression);

        LimitPath baseQuery = selectQuery;
        if (orderBy != null) {
            baseQuery = selectQuery.orderBy(orderBy);
        }

        List<N1qlQueryRow> searchResultList = new ArrayList<N1qlQueryRow>();

        if ((SearchReturnDataType.SEARCH == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
	        N1qlQueryResult lastResult = null;
	        if (pageSize > 0) {
	            boolean collectSearchResult;
	
	            Statement query = null;
	            int currentLimit;
	            try {
	                List<N1qlQueryRow> lastSearchResultList;
	                int resultCount = 0;
	                do {
	                    collectSearchResult = true;
	
	                    currentLimit = pageSize;
	                    if (count > 0) {
	                        currentLimit = Math.min(pageSize, count - resultCount);
	                    }
	
	                    query = baseQuery.limit(currentLimit).offset(start + resultCount);
	                    LOG.debug("Execution query: '" + query + "'");
	                    lastResult = bucket.query(N1qlQuery.simple(query, N1qlParams.build().consistency(scanConsistency)));
	                    if (!lastResult.finalSuccess()) {
		                    throw new SearchException(String.format("Failed to search entries. Query: '%s'. Error: '%s', Error count: '%d'", query, lastResult.errors(),
		                            lastResult.info().errorCount()), lastResult.errors().get(0).getInt("code"));
	                    }
	
	                    lastSearchResultList = lastResult.allRows();
	
	                    if (batchOperation != null) {
	                        collectSearchResult = batchOperation.collectSearchResult(lastSearchResultList.size());
	                    }
	                    if (collectSearchResult) {
	                        searchResultList.addAll(lastSearchResultList);
	                    }
	
	                    if (batchOperation != null) {
	                        List<O> entries = batchOperationWraper.createEntities(lastSearchResultList);
	                        batchOperation.performAction(entries);
	                    }
	
	                    resultCount += lastSearchResultList.size();
	
	                    if ((count > 0) && (resultCount >= count)) {
	                        break;
	                    }
	                } while (lastSearchResultList.size() > 0);
	            } catch (CouchbaseException ex) {
	                throw new SearchException("Failed to search entries. Query: '" + query + "'", ex);
	            }
	        } else {
	            try {
	                Statement query = baseQuery;
	                if (count > 0) {
	                    query = ((LimitPath) query).limit(count);
	                }
	                if (start > 0) {
	                    query = ((OffsetPath) query).offset(start);
	                }
	
	                LOG.debug("Execution query: '" + query + "'");
	                lastResult = bucket.query(N1qlQuery.simple(query, N1qlParams.build().consistency(scanConsistency)));
	                if (!lastResult.finalSuccess()) {
	                    throw new SearchException(String.format("Failed to search entries. Query: '%s'. Error: '%s', Error count: '%d'", baseQuery, lastResult.errors(),
	                            lastResult.info().errorCount()), lastResult.errors().get(0).getInt("code"));
	                }
	
	                searchResultList.addAll(lastResult.allRows());
	            } catch (CouchbaseException ex) {
	                throw new SearchException("Failed to search entries. Query: '" + baseQuery.toString() + "'", ex);
	            }
	        }
        }

        List<JsonObject> resultRows = new ArrayList<JsonObject>(searchResultList.size());
        for (N1qlQueryRow row : searchResultList) {
            resultRows.add(row.value());
        }

        PagedResult<JsonObject> result = new PagedResult<JsonObject>();
        result.setEntries(resultRows);
        result.setEntriesCount(resultRows.size());
        result.setStart(start);

        if ((SearchReturnDataType.COUNT == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
            GroupByPath selectCountQuery = Select.select("COUNT(*) as TOTAL").from(Expression.i(bucketMapping.getBucketName()))
                    .where(finalExpression);
            try {
                LOG.debug("Calculating count. Execution query: '" + selectCountQuery + "'");
                N1qlQueryResult countResult = bucket.query(N1qlQuery.simple(selectCountQuery, N1qlParams.build().consistency(scanConsistency)));
                if (!countResult.finalSuccess() || (countResult.info().resultCount() != 1)) {
                    throw new SearchException(String.format("Failed to calculate count entries. Query: '%s'. Error: '%s', Error count: '%d'", selectCountQuery, countResult.errors(),
                    		countResult.info().errorCount()), countResult.errors().get(0).getInt("code"));
                }
                result.setTotalEntriesCount(countResult.allRows().get(0).value().getInt("TOTAL"));
            } catch (CouchbaseException ex) {
                throw new SearchException("Failed to calculate count entries. Query: '" + selectCountQuery.toString() + "'", ex);
            }
        }

        return result;
    }

    public String[] createStoragePassword(String[] passwords) {
        if (ArrayHelper.isEmpty(passwords)) {
            return passwords;
        }

        String[] results = new String[passwords.length];
        for (int i = 0; i < passwords.length; i++) {
			if (persistenceExtension == null) {
				results[i] = PasswordEncryptionHelper.createStoragePassword(passwords[i], connectionProvider.getPasswordEncryptionMethod());
			} else {
				results[i] = persistenceExtension.createHashedPassword(passwords[i]);
			}
        }

        return results;
    }

    @Override
    public boolean isBinaryAttribute(String attribute) {
        return this.connectionProvider.isBinaryAttribute(attribute);
    }

    @Override
    public boolean isCertificateAttribute(String attribute) {
        return this.connectionProvider.isCertificateAttribute(attribute);
    }

    private ScanConsistency getScanConsistency(ScanConsistency operationScanConsistency, boolean ignore) {
    	if (ignore) {
    		return scanConsistency;
    	}

    	if (ignoreAttributeScanConsistency) {
        	return scanConsistency;
    	}

    	if (operationScanConsistency != null) {
    		return operationScanConsistency;
    	}

    	return scanConsistency;
	}

	public ScanConsistency getScanConsistency() {
		return scanConsistency;
	}

    public boolean isDisableAttributeMapping() {
		return disableAttributeMapping;
	}

	@Override
    public boolean destroy() {
        boolean result = true;

        if (connectionProvider != null) {
            try {
                connectionProvider.destroy();
            } catch (Exception ex) {
                LOG.error("Failed to destroy provider correctly");
                result = false;
            }
        }

        return result;
    }

    @Override
    public boolean isConnected() {
        return connectionProvider.isConnected();
    }

	protected String getScanAttemptLogInfo(ScanConsistency scanConsistency, ScanConsistency usedScanConsistency, boolean secondTry) {
		String attemptInfo = "";
        if (secondTry) {
        	attemptInfo = ", attempt: second";
        } else {
        	ScanConsistency useScanConsistency2 = getScanConsistency(scanConsistency, false);
        	if (!useScanConsistency2.equals(usedScanConsistency)) {
        		attemptInfo = ", attempt: first";
        	}
        }

        return attemptInfo;
	}

	@Override
	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		this.persistenceExtension = persistenceExtension;
	}

	@Override
	public boolean isSupportObjectClass(String objectClass) {
		return true;
	}

}
