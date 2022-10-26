/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.operation.impl;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.DeleteException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.Sort;
import io.jans.orm.model.SortOrder;
import io.jans.orm.operation.auth.PasswordEncryptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.msg.ResponseStatus;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInResult;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.UpsertOptions;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.java.query.QueryStatus;

import io.jans.orm.couchbase.impl.CouchbaseBatchOperationWraper;
import io.jans.orm.couchbase.model.BucketMapping;
import io.jans.orm.couchbase.model.ConvertedExpression;
import io.jans.orm.couchbase.model.SearchReturnDataType;
import io.jans.orm.couchbase.operation.CouchbaseOperationService;
import io.jans.orm.couchbase.operation.watch.OperationDurationUtil;

/**
 * Base service which performs all supported Couchbase operations
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class CouchbaseOperationServiceImpl implements CouchbaseOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseOperationServiceImpl.class);

    private Properties props;
    private CouchbaseConnectionProvider connectionProvider;

    private QueryScanConsistency queryScanConsistency = QueryScanConsistency.NOT_BOUNDED;

    private boolean ignoreAttributeQueryScanConsistency = false;
	private boolean attemptWithoutAttributeQueryScanConsistency = true;
	private boolean enableScopeSupport = false;
	private boolean disableAttributeMapping = false;

	private PersistenceExtension persistenceExtension;

	public CouchbaseOperationServiceImpl() {
    }

    public CouchbaseOperationServiceImpl(Properties props, CouchbaseConnectionProvider connectionProvider) {
        this.props = props;
        this.connectionProvider = connectionProvider;
        init();
    }
    
    private void init() {
        if (props.containsKey("connection.scan-consistency")) {
        	String queryScanConsistencyString = StringHelper.toUpperCase(props.get("connection.scan-consistency").toString());
        	this.queryScanConsistency = QueryScanConsistency.valueOf(queryScanConsistencyString);
        }

        if (props.containsKey("connection.ignore-attribute-scan-consistency")) {
        	this.ignoreAttributeQueryScanConsistency = StringHelper.toBoolean(props.get("connection.ignore-attribute-scan-consistency").toString(), this.ignoreAttributeQueryScanConsistency);
        }

        if (props.containsKey("connection.attempt-without-attribute-scan-consistency")) {
        	this.attemptWithoutAttributeQueryScanConsistency = StringHelper.toBoolean(props.get("attempt-without-attribute-scan-consistency").toString(), this.attemptWithoutAttributeQueryScanConsistency);
        }

        if (props.containsKey("connection.enable-scope-support")) {
        	this.enableScopeSupport = StringHelper.toBoolean(props.get("connection.enable-scope-support").toString(), this.enableScopeSupport);
        }

        if (props.containsKey("connection.disable-attribute-mapping")) {
        	this.disableAttributeMapping = StringHelper.toBoolean(props.get("connection.disable-attribute-mapping").toString(), this.disableAttributeMapping);
        }

        LOG.info("Option queryScanConsistency: " + queryScanConsistency);
        LOG.info("Option ignoreAttributeQueryScanConsistency: " + ignoreAttributeQueryScanConsistency);
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
	        JsonObject entry = lookup(key, USER_PASSWORD);
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
			UpsertOptions upsertOptions = UpsertOptions.upsertOptions();
			if (expiration != null) {
				upsertOptions = upsertOptions.expiry(Duration.ofSeconds(expiration));
			}

			MutationResult result = bucketMapping.getBucket().defaultCollection().upsert(key, jsonObject, upsertOptions);
            if (result != null) {
                return true;
            }

        } catch (CouchbaseException ex) {
            throw new PersistenceException(String.format("Failed to add entry with key '%s'", key), ex);
        }

        return false;
	}

    @Deprecated
    protected boolean updateEntry(String key, JsonObject attrs) throws UnsupportedOperationException, PersistenceException {
        List<MutateInSpec> mods = new ArrayList<MutateInSpec>();

        for (Entry<String, Object> attrEntry : attrs.toMap().entrySet()) {
            String attributeName = attrEntry.getKey();
            Object attributeValue = attrEntry.getValue();
            if (attributeName.equalsIgnoreCase(CouchbaseOperationService.OBJECT_CLASS) || attributeName.equalsIgnoreCase(CouchbaseOperationService.DN)
                    || attributeName.equalsIgnoreCase(CouchbaseOperationService.USER_PASSWORD)) {
                continue;
            } else {
                if (attributeValue != null) {
                    mods.add(MutateInSpec.replace(attributeName, attributeValue));
                }
            }
        }

        return updateEntry(key, mods, null);
    }

    @Override
    public boolean updateEntry(String key, List<MutateInSpec> mods, Integer expiration) throws PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        boolean result = updateEntryImpl(bucketMapping, key, mods, expiration);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: modify, duration: {}, bucket: {}, key: {}, mods: {}", duration, bucketMapping.getBucketName(), key, mods);

        return result;
    }

	private boolean updateEntryImpl(BucketMapping bucketMapping, String key, List<MutateInSpec> mods, Integer expiration) throws PersistenceException {
		try {
			MutateInOptions options = MutateInOptions.mutateInOptions();
            if (expiration != null) {
				options.expiry(Duration.ofSeconds(expiration));
			}

            MutateInResult result = bucketMapping.getBucket().defaultCollection().mutateIn(key, mods, options);

            return result != null;
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
			MutationResult result = bucketMapping.getBucket().defaultCollection().remove(key);

            return result != null;
        } catch (CouchbaseException ex) {
            throw new EntryNotFoundException(String.format("Failed to delete entry by key '%s'", key), ex);
        }
	}

    @Override
    public int delete(String key, QueryScanConsistency queryScanConsistency, ConvertedExpression expression, int count) throws DeleteException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
    	QueryScanConsistency useQueryScanConsistency = getQueryScanConsistency(queryScanConsistency, false);

    	int result = deleteImpl(bucketMapping, key, useQueryScanConsistency, expression, count);

        String attemptInfo = getScanAttemptLogInfo(queryScanConsistency, useQueryScanConsistency, false);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: delete_search, duration: {}, bucket: {}, key: {}, expression: {}, count: {}, consistency: {}{}", duration, bucketMapping.getBucketName(), key, expression, count, useQueryScanConsistency, attemptInfo);

        return result;
    }

    private int deleteImpl(BucketMapping bucketMapping, String key, QueryScanConsistency queryScanConsistency, ConvertedExpression expression, int count) throws DeleteException {
        StringBuilder query = new StringBuilder("DELETE FROM `").append(bucketMapping.getBucketName()).append("` WHERE ").append(expression.expression());
        if (enableScopeSupport) { 
        	query.append("AND META().id LIKE ").append(key).append("%");
        }

        query.append(" LIMIT ").append(count).append(" RETURNING default.*");
        LOG.debug("Execution query: '" + query + "'");

        QueryOptions queryOptions = QueryOptions.queryOptions().scanConsistency(queryScanConsistency).parameters(expression.getQueryParameters());
        QueryResult result = connectionProvider.getCluster().query(query.toString(), queryOptions);
        if (QueryStatus.SUCCESS != result.metaData().status()) {
            throw new DeleteException(String.format("Failed to delete entries. Query: '%s'. Warnings: '%s'", query, result.metaData().warnings()));
        }

        return result.rowsAsObject().size();
	}

    @Override
    public boolean deleteRecursively(String key) throws EntryNotFoundException, DeleteException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        boolean result = deleteRecursivelyImpl(bucketMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: delete_tree, duration: {}, bucket: {}, key: {}", duration, bucketMapping.getBucketName(), key);

        return result;
    }

	private boolean deleteRecursivelyImpl(BucketMapping bucketMapping, String key) throws EntryNotFoundException, DeleteException {
		try {
	        if (enableScopeSupport) {
	            StringBuilder query = new StringBuilder("DELETE FROM `").append(bucketMapping.getBucketName()).append("` WHERE ").
	            		append("META().id LIKE $1%");
	
	            LOG.debug("Execution query: '" + query + "'");

	            QueryOptions queryOptions = QueryOptions.queryOptions().scanConsistency(queryScanConsistency).parameters(JsonArray.from(key));
	            QueryResult result = connectionProvider.getCluster().query(query.toString(), queryOptions);
	            if (QueryStatus.SUCCESS != result.metaData().status()) {
	                throw new DeleteException(String.format("Failed to delete entries. Query: '%s'. Warnings: '%s'", query, result.metaData().warnings()));
	            }

	            return true;
	        } else {
	        	LOG.warn("Removing only base key without sub-tree: " + key);
	        	delete(key);
	        }
	    	
            return true;
        } catch (CouchbaseException ex) {
            throw new DeleteException("Failed to delete entry", ex);
        }
	}

    @Override
    public JsonObject lookup(String key, String... attributes) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
    	BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);
        JsonObject result = lookupImpl(bucketMapping, key, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: lookup, duration: {}, bucket: {}, key: {}, attributes: {}", duration, bucketMapping.getBucketName(), key, attributes);

        return result;
    }

	private JsonObject lookupImpl(BucketMapping bucketMapping, String key, String... attributes) throws SearchException {
		try {
            Bucket bucket = bucketMapping.getBucket();
            if (ArrayHelper.isEmpty(attributes)) {
                JsonObject doc = bucket.defaultCollection().get(key).contentAsObject();
                if (doc != null) {
                    return doc;
                }
            } else {
            	// Server allows to request only max 16 fields 
            	if (attributes.length > 16) {
	                JsonObject doc = bucket.defaultCollection().get(key).contentAsObject();

	                Set<String> docAtributesKeep = new HashSet<String>(Arrays.asList(attributes));
//                	docAtributesKeep.add(CouchbaseOperationService.DN);

                	for (Iterator<String> it = doc.getNames().iterator(); it.hasNext();) {
						String docAtribute = (String) it.next();
						if (!docAtributesKeep.contains(docAtribute)) {
							it.remove();
						}
					}

                	return doc;
            	} else {
	            	GetOptions options = GetOptions.getOptions().project(Arrays.asList(attributes));
	                JsonObject doc = bucket.defaultCollection().get(key, options).contentAsObject();
	                if (doc != null) {
	                	return doc;
	                }
            	}
            }
        } catch (CouchbaseException ex) {
        	if (ResponseStatus.SUBDOC_FAILURE == ex.context().responseStatus()) {
        		// No fields for return
        		return JsonObject.create();
        	}
            throw new SearchException(String.format("Failed to lookup entry by key '%s'", key), ex);
        }

        throw new SearchException(String.format("Failed to lookup entry by key '%s'", key));
	}

	@Override
    public <O> PagedResult<JsonObject> search(String key, QueryScanConsistency queryScanConsistency, ConvertedExpression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
                                              CouchbaseBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        BucketMapping bucketMapping = connectionProvider.getBucketMappingByKey(key);

        boolean secondTry = false;
    	QueryScanConsistency useQueryScanConsistency = getQueryScanConsistency(queryScanConsistency, attemptWithoutAttributeQueryScanConsistency);
        PagedResult<JsonObject> result = null;
        int attemps = 20;
        do {
			attemps--;
			try {
				result = searchImpl(bucketMapping, key, useQueryScanConsistency, expression, scope, attributes, orderBy, batchOperationWraper,
						returnDataType, start, count, pageSize);
				break;
			} catch (SearchException ex) {
				// TODO: Check if it's not needed in CB 7.x and SDK 3.x
				if (ex.getErrorCode() != 5000) {
					throw ex;
				}
				
				LOG.warn("Waiting for Indexer Warmup...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ex2) {}
			}
        } while (attemps > 0);
        if ((result == null) || (result.getTotalEntriesCount() == 0)) {
        	QueryScanConsistency useQueryScanConsistency2 = getQueryScanConsistency(queryScanConsistency, false);
        	if (!useQueryScanConsistency2.equals(useQueryScanConsistency)) {
        		useQueryScanConsistency = useQueryScanConsistency2;
                result = searchImpl(bucketMapping, key, useQueryScanConsistency, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize);
                secondTry = true;
        	}
        }

        String attemptInfo = getScanAttemptLogInfo(queryScanConsistency, useQueryScanConsistency, secondTry);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("Couchbase operation: search, duration: {}, bucket: {}, key: {}, expression: {}, scope: {}, attributes: {}, orderBy: {}, batchOperationWraper: {}, returnDataType: {}, start: {}, count: {}, pageSize: {}, consistency: {}{}", duration, bucketMapping.getBucketName(), key, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize, useQueryScanConsistency, attemptInfo);

        return result;
	}

	private <O> PagedResult<JsonObject> searchImpl(BucketMapping bucketMapping, String key, QueryScanConsistency queryScanConsistency, ConvertedExpression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
            CouchbaseBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
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

        StringBuilder finalExpression = new StringBuilder(expression.expression());
        if (enableScopeSupport) { 
			if (SearchScope.BASE == scope) {
				finalExpression.append(" AND META().id NOT LIKE ").append(key).append("\\\\_%\\\\_");
			} else {
				finalExpression.append(" AND META().id LIKE ").append(key).append("%");
			}
        } else {
            if (scope != null) {
            	LOG.debug("Ignoring scope '" + scope + " for expression: " + expression);
            }
        }

        String[] select = attributes;
        if (ArrayHelper.isEmpty(select)) {
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

        StringBuilder baseQuery = new StringBuilder("SELECT ").append(StringHelper.toString(backticksAttributes(select))).append(" FROM `").append(bucketMapping.getBucketName()).append("` AS jans_doc ").
        		append("WHERE ").append(finalExpression);

        StringBuilder baseQueryWithOrder = new StringBuilder(baseQuery);
        if (ArrayHelper.isNotEmpty(orderBy)) {
        	baseQueryWithOrder.append(" ORDER BY ");
        	for (int i = 0; i < orderBy.length; i++) {
        		if (i > 0) {
        			baseQueryWithOrder.append(", ");
        		}
        		Sort order = orderBy[i];
        		baseQueryWithOrder.append(order.getName());
            	if ((order.getSortOrder() != null) && (SortOrder.DEFAULT != order.getSortOrder())) {
            		baseQueryWithOrder.append(" ").append(order.getSortOrder().getShortValue());
            	}
        	}
        }

        QueryOptions queryOptions = QueryOptions.queryOptions().scanConsistency(queryScanConsistency).parameters(expression.getQueryParameters());

        List<JsonObject> searchResultList = new ArrayList<JsonObject>();

        int totalEntriesCount = 0;
        if ((SearchReturnDataType.SEARCH == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
        	QueryResult lastResult = null;
	        if (pageSize > 0) {
	            boolean collectSearchResult;
	
	            StringBuilder query = null;
	            int currentLimit;
	            int lastCountRows = 0;
	            try {
	                List<JsonObject> lastSearchResultList;
					do {
	                    collectSearchResult = true;
	
	                    currentLimit = pageSize;
	                    if (count > 0) {
	                        currentLimit = Math.min(pageSize, count - totalEntriesCount);
	                    }
	
	                    query = new StringBuilder(baseQueryWithOrder).append(" LIMIT ").append(currentLimit).append(" OFFSET ").append(start + totalEntriesCount);

	                    LOG.debug("Execution query: '" + query + "'");

	                    lastResult = connectionProvider.getCluster().query(query.toString(), queryOptions);
	    	            if (QueryStatus.SUCCESS != lastResult.metaData().status()) {
	    	                throw new SearchException(String.format("Failed to search entries. Query: '%s'. Warnings: '%s'", query, lastResult.metaData().warnings()));
	    	            }
	
	                    lastSearchResultList = lastResult.rowsAsObject();
		                lastCountRows = lastSearchResultList.size();

	                    if (batchOperation != null) {
	                        collectSearchResult = batchOperation.collectSearchResult(lastCountRows);
	                    }
	                    if (collectSearchResult) {
	                        searchResultList.addAll(lastSearchResultList);
	                    }
	
	                    if ((batchOperation != null) && (lastCountRows > 0)) {
	                        List<O> entries = batchOperationWraper.createEntities(lastSearchResultList);
	                        batchOperation.performAction(entries);
	                    }
	
	                    totalEntriesCount += lastCountRows;
	
	                    if ((count > 0) && (totalEntriesCount >= count) || (lastCountRows < currentLimit)) {
	                        break;
	                    }
	                } while (lastCountRows > 0);
	            } catch (CouchbaseException ex) {
	                throw new SearchException("Failed to search entries. Query: '" + query + "'", ex);
	            }
	        } else {
	            try {
                    StringBuilder query = new StringBuilder(baseQueryWithOrder);
	                if (count > 0) {
	                	query.append(" LIMIT ").append(count);
	                }
	                if (start > 0) {
	                	query.append(" OFFSET ").append(start);
	                }
	
	                LOG.debug("Execution query: '" + query + "'");

	                lastResult = connectionProvider.getCluster().query(query.toString(), queryOptions);
    	            if (QueryStatus.SUCCESS != lastResult.metaData().status()) {
    	                throw new SearchException(String.format("Failed to search entries. Query: '%s'. Warnings: '%s'", query, lastResult.metaData().warnings()));
    	            }
	
	                searchResultList.addAll(lastResult.rowsAsObject());
	            } catch (CouchbaseException ex) {
	                throw new SearchException("Failed to search entries. Query: '" + baseQuery.toString() + "'", ex);
	            }
	        }
        }

        List<JsonObject> resultRows = new ArrayList<JsonObject>(searchResultList.size());
        for (JsonObject row : searchResultList) {
            resultRows.add(row);
        }

        PagedResult<JsonObject> result = new PagedResult<JsonObject>();
        result.setEntries(resultRows);
        result.setEntriesCount(resultRows.size());
        result.setStart(start);

        if ((SearchReturnDataType.COUNT == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
            StringBuilder selectCountQuery = new StringBuilder("SELECT COUNT(*) as TOTAL").append(" FROM `").append(bucketMapping.getBucketName()).append("` AS jans_doc ").
            		append("WHERE ").append(finalExpression);
            try {
                LOG.debug("Calculating count. Execution query: '" + selectCountQuery + "'");

                QueryResult countResult = connectionProvider.getCluster().query(selectCountQuery.toString(), queryOptions);
	            if ((QueryStatus.SUCCESS != countResult.metaData().status()) && (countResult.rowsAsObject().size() != 1)) {
	                throw new SearchException(String.format("Failed to calculate count entries. Query: '%s'. Warnings: '%s'", selectCountQuery, countResult.metaData().warnings()));
	            }
                result.setTotalEntriesCount(countResult.rowsAsObject().get(0).getInt("TOTAL"));
            } catch (CouchbaseException ex) {
                throw new SearchException("Failed to calculate count entries. Query: '" + selectCountQuery.toString() + "'", ex);
            }
        } else {
        	result.setTotalEntriesCount(totalEntriesCount);
        }

        return result;
    }

	private String[] backticksAttributes(String[] attributes) {
		if (ArrayHelper.isEmpty(attributes)) {
			return attributes;
		}
		
		String[] resultAttributes = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i].contains("*")) {
				resultAttributes[i] = attributes[i];
			} else {
				resultAttributes[i] = '`' + attributes[i] + "`";
			}
		}

		return resultAttributes;
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

    private QueryScanConsistency getQueryScanConsistency(QueryScanConsistency operationQueryScanConsistency, boolean ignore) {
    	if (ignore) {
    		return queryScanConsistency;
    	}

    	if (ignoreAttributeQueryScanConsistency) {
        	return queryScanConsistency;
    	}

    	if (operationQueryScanConsistency != null) {
    		return operationQueryScanConsistency;
    	}

    	return queryScanConsistency;
	}

	public QueryScanConsistency getQueryScanConsistency() {
		return queryScanConsistency;
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

	protected String getScanAttemptLogInfo(QueryScanConsistency queryScanConsistency, QueryScanConsistency usedQueryScanConsistency, boolean secondTry) {
		String attemptInfo = "";
        if (secondTry) {
        	attemptInfo = ", attempt: second";
        } else {
        	QueryScanConsistency useQueryScanConsistency2 = getQueryScanConsistency(queryScanConsistency, false);
        	if (!useQueryScanConsistency2.equals(usedQueryScanConsistency)) {
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

	@Override
	public String toInternalAttribute(String attributeName) {
		return attributeName;
//		if (getOperationService().isDisableAttributeMapping()) {
//			return attributeName;
//		}
//
//		return KeyShortcuter.shortcut(attributeName);
	}

	@Override
	public String[] toInternalAttributes(String[] attributeNames) {
		return attributeNames;
//		if (getOperationService().isDisableAttributeMapping() || ArrayHelper.isEmpty(attributeNames)) {
//			return attributeNames;
//		}
//		
//		String[] resultAttributeNames = new String[attributeNames.length];
//		
//		for (int i = 0; i < attributeNames.length; i++) {
//			resultAttributeNames[i] = KeyShortcuter.shortcut(attributeNames[i]);
//		}
//		
//		return resultAttributeNames;
	}

	@Override
	public String fromInternalAttribute(String internalAttributeName) {
		return internalAttributeName;
//		if (getOperationService().isDisableAttributeMapping()) {
//			return internalAttributeName;
//		}
//
//		return KeyShortcuter.fromShortcut(internalAttributeName);
	}

	@Override
	public String[] fromInternalAttributes(String[] internalAttributeNames) {
		return internalAttributeNames;
//		if (getOperationService().isDisableAttributeMapping() || ArrayHelper.isEmpty(internalAttributeNames)) {
//			return internalAttributeNames;
//		}
//		
//		String[] resultAttributeNames = new String[internalAttributeNames.length];
//		
//		for (int i = 0; i < internalAttributeNames.length; i++) {
//			resultAttributeNames[i] = KeyShortcuter.fromShortcut(internalAttributeNames[i]);
//		}
//		
//		return resultAttributeNames;
	}

    @Override
    public String encodeTime(Date date) {
        if (date == null) {
            return null;
        }
        
        try {
            String utcDate = ISO_INSTANT.format(Instant.ofEpochMilli(date.getTime()));
            // Drop UTC zone identifier to comply with format employed in CB: yyyy-MM-dd'T'HH:mm:ss.SSS
//            return utcDate.substring(0, utcDate.length() - 1);
            return utcDate;
        } catch (DateTimeException ex) {
        	LOG.error("Cannot format date '{}' as ISO", date, ex);
        	return null;
        }
	}

    @Override
    public Date decodeTime(String date, boolean silent) {
        if (StringHelper.isEmpty(date)) {
            return null;
        }

        // Add ending Z if necessary
        String dateZ = date.endsWith("Z") ? date : date + "Z";
        try {
            return new Date(Instant.parse(dateZ).toEpochMilli());
        } catch (DateTimeParseException ex) {
        	if (!silent) {
	            LOG.error("Failed to decode generalized time '{}'", date, ex);
        	}

        	return null;
        }
    }

}
