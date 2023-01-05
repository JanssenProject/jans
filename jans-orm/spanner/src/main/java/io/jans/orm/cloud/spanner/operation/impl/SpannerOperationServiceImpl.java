/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.operation.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Statement.Builder;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.Type.StructField;

import io.jans.orm.cloud.spanner.impl.SpannerBatchOperationWraper;
import io.jans.orm.cloud.spanner.model.ConvertedExpression;
import io.jans.orm.cloud.spanner.model.SearchReturnDataType;
import io.jans.orm.cloud.spanner.model.TableMapping;
import io.jans.orm.cloud.spanner.model.ValueWithStructField;
import io.jans.orm.cloud.spanner.operation.SpannerOperationService;
import io.jans.orm.cloud.spanner.operation.watch.OperationDurationUtil;
import io.jans.orm.cloud.spanner.util.SpannerValueHelper;
import io.jans.orm.exception.operation.DeleteException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.EntryConvertationException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.exception.operation.IncompatibleTypeException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.Sort;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;
import io.jans.orm.operation.auth.PasswordEncryptionHelper;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

import com.google.cloud.spanner.ValueBinder;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * Base service which performs all supported SQL operations
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public class SpannerOperationServiceImpl implements SpannerOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerOperationServiceImpl.class);

	public static final Object[] NO_OBJECTS = new Object[0];

    private Properties props;
    private SpannerConnectionProvider connectionProvider;

	private boolean disableAttributeMapping = false;

	private PersistenceExtension persistenceExtension;

	private DatabaseClient databaseClient;

	private Table tableAlias = new Table("doc");

    @SuppressWarnings("unused")
    private SpannerOperationServiceImpl() {
    }

    public SpannerOperationServiceImpl(Properties props, SpannerConnectionProvider connectionProvider) {
        this.props = props;
        this.connectionProvider = connectionProvider;
        init();
    }

	private void init() {
		this.databaseClient = connectionProvider.getClient();
	}

    @Override
    public SpannerConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    @Override
    public boolean authenticate(String key, String password, String objectClass) throws SearchException {
        return authenticateImpl(key, password, objectClass);
    }

    private boolean authenticateImpl(String key, String password, String objectClass) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        boolean result = false;
        if (password != null) {
	        try {
		        List<AttributeData> attributes = lookup(key, objectClass, USER_PASSWORD);
		        
		        Object userPasswordObj = null;
		        for (AttributeData attribute : attributes) {
		        	if (StringHelper.equalsIgnoreCase(attribute.getName(), USER_PASSWORD)) {
		        		userPasswordObj = attribute.getValue();
		        	}
		        	
		        }
		
		        String userPassword = null;
		        if (userPasswordObj instanceof String) {
		            userPassword = (String) userPasswordObj;
		        }
		
		        if (userPassword != null) {
		        	if (persistenceExtension == null) {
			        	result = PasswordEncryptionHelper.compareCredentials(password, userPassword);
		        	} else {
		        		result = persistenceExtension.compareHashedPasswords(password, userPassword);
		        	}
		        }
	        } catch (EntryConvertationException ex) {
	        	throw new SearchException(String.format("Failed to get '%s' attribute", USER_PASSWORD), ex);
	        }
        }

        Duration duration = OperationDurationUtil.instance().duration(startTime);

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        OperationDurationUtil.instance().logDebug("Spanner operation: bind, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

        return result;
    }

    @Override
    public boolean addEntry(String key, String objectClass, Collection<AttributeData> attributes) throws DuplicateEntryException, PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

        boolean result = addEntryImpl(tableMapping, key, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: add, duration: {}, table: {}, key: {}, attributes: {}", duration, tableMapping.getTableName(), key, attributes);
        
        return result;
    }

	private boolean addEntryImpl(TableMapping tableMapping, String key, Collection<AttributeData> attributes) throws PersistenceException {
		try {
			MessageDigest messageDigest = getMessageDigestInstance();
			Map<String, StructField> columTypes = tableMapping.getColumTypes();

			WriteBuilder mutationBuilder = Mutation.newInsertOrUpdateBuilder(tableMapping.getTableName());
			List<Mutation> mutations = new LinkedList<>();
			for (AttributeData attribute : attributes) {
				String attributeName = attribute.getName();
				StructField attributeType = columTypes.get(attributeName.toLowerCase());

				// If column not inside table we should check if there is child table
				if (attributeType == null) {
					TableMapping childTableMapping = connectionProvider.getChildTableMappingByKey(key, tableMapping, attributeName);
					if (childTableMapping == null) {
			            throw new PersistenceException(String.format("Failed to add entry. Column '%s' is undefined", attributeName));
					}

					Map<String, StructField> childColumTypes = childTableMapping.getColumTypes();
					if (childColumTypes == null) {
			            throw new PersistenceException(String.format("Failed to add entry. Column '%s' is undefined", attributeName));
					}
					StructField childAttributeType = childColumTypes.get(attributeName.toLowerCase());
					
					// Build Mutation for child table
					for (Object value : attribute.getValues()) {
						// Build Mutation for child table
						String dictDocId = getStringUniqueKey(messageDigest, value);

						WriteBuilder childMutationBuilder = Mutation.newInsertOrUpdateBuilder(childTableMapping.getTableName());
						childMutationBuilder.
							set(SpannerOperationService.DOC_ID).to(key).
							set(SpannerOperationService.DICT_DOC_ID).to(dictDocId);
						
						setMutationBuilderValue(childMutationBuilder, childAttributeType, value);

						mutations.add(childMutationBuilder.build());
					}
				} else {
					setMutationBuilderValue(mutationBuilder, attributeType, attribute.getValues());
				}
			}
			mutations.add(0, mutationBuilder.build());

			databaseClient.write(mutations);

			return true;
        } catch (SpannerException | IllegalStateException ex) {
            throw new PersistenceException("Failed to add entry", ex);
        }
	}

	@Override
    public boolean updateEntry(String key, String objectClass, List<AttributeDataModification> mods) throws UnsupportedOperationException, PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = updateEntryImpl(tableMapping, key, mods);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: modify, duration: {}, table: {}, key: {}, mods: {}", duration, tableMapping.getTableName(), key, mods);

        return result;
    }

	private boolean updateEntryImpl(TableMapping tableMapping, String key, List<AttributeDataModification> mods)
			throws PersistenceException {
		try {
			MessageDigest messageDigest = getMessageDigestInstance();
			Map<String, StructField> columTypes = tableMapping.getColumTypes();

			WriteBuilder mutationBuilder = Mutation.newInsertOrUpdateBuilder(tableMapping.getTableName()).
					set(SpannerOperationService.DOC_ID).to(key);
			List<Mutation> mutations = new LinkedList<>();
			for (AttributeDataModification attributeMod : mods) {
				AttributeData attribute = attributeMod.getAttribute();
				AttributeModificationType type = attributeMod.getModificationType();

				String attributeName = attribute.getName();
				StructField attributeType = columTypes.get(attributeName.toLowerCase());

				// If column not inside table we should check if there is child table
				if (attributeType == null) {
					TableMapping childTableMapping = connectionProvider.getChildTableMappingByKey(key, tableMapping, attributeName);
					if (childTableMapping == null) {
						throw new PersistenceException(
								String.format("Failed to update entry. Column '%s' is undefined", attributeName));
					}

					Map<String, StructField> childColumTypes = childTableMapping.getColumTypes();
					StructField childAttributeType = childColumTypes.get(attributeName.toLowerCase());

					// Build Mutation for child table
					Map<String, Object> oldValues = null;
					if ((attributeMod.getOldAttribute() != null) && (attributeMod.getOldAttribute().getValues() != null)) {
						oldValues = new HashMap<>();
						for (Object oldValue : attributeMod.getOldAttribute().getValues()) {
							String dictDocId = getStringUniqueKey(messageDigest, oldValue);
							oldValues.put(dictDocId, oldValue);
						}
					}
					
					if ((AttributeModificationType.ADD == type) ||
							(AttributeModificationType.FORCE_UPDATE == type) || (AttributeModificationType.REPLACE == type)) {
						for (Object value : attribute.getValues()) {
							WriteBuilder childMutationBuilder = Mutation.newInsertOrUpdateBuilder(childTableMapping.getTableName());

							String dictDocId = getStringUniqueKey(messageDigest, value);
							childMutationBuilder.
								set(SpannerOperationService.DOC_ID).to(key).
								set(SpannerOperationService.DICT_DOC_ID).to(dictDocId);

							setMutationBuilderValue(childMutationBuilder, childAttributeType, value);

							mutations.add(childMutationBuilder.build());

							if (oldValues != null) {
								oldValues.remove(dictDocId);
							}
						}
					} else if (AttributeModificationType.REMOVE == type) {
						// Build Mutation for child table
						com.google.cloud.spanner.KeySet.Builder keySetBuilder = KeySet.newBuilder();
						for (Object value : attribute.getValues()) {
							String dictDocId = getStringUniqueKey(messageDigest, value);
							keySetBuilder.addKey(Key.of(key, dictDocId));
						}

						Mutation childMutation = Mutation.delete(childTableMapping.getTableName(), keySetBuilder.build());

						mutations.add(childMutation);
					} else {
						throw new UnsupportedOperationException("Operation type '" + type + "' is not implemented");
					}

					if ((oldValues != null) && (oldValues.size() > 0)) {
						com.google.cloud.spanner.KeySet.Builder keySetBuilder = KeySet.newBuilder();
						for (String removeDictDocId : oldValues.keySet()) {
							keySetBuilder.addKey(Key.of(key, removeDictDocId));
						}

						Mutation childMutation = Mutation.delete(childTableMapping.getTableName(), keySetBuilder.build());

						mutations.add(childMutation);
					}
				} else {
					if ((AttributeModificationType.ADD == type) || (AttributeModificationType.FORCE_UPDATE == type)
							|| (AttributeModificationType.REPLACE == type)) {
						setMutationBuilderValue(mutationBuilder, attributeType, attribute.getValues());
					} else if (AttributeModificationType.REMOVE == type) {
						removeMutationBuilderValue(mutationBuilder, attribute, attributeType);
					} else {
						throw new UnsupportedOperationException("Operation type '" + type + "' is not implemented");
					}

				}
			}
			mutations.add(0, mutationBuilder.build());

			databaseClient.write(mutations);

			return true;
		} catch (SpannerException | IllegalStateException ex) {
			throw new PersistenceException("Failed to update entry", ex);
		}
	}

	@Override
    public boolean delete(String key, String objectClass) throws EntryNotFoundException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = deleteImpl(tableMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: delete, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

        return result;
    }

	private boolean deleteImpl(TableMapping tableMapping, String key) throws EntryNotFoundException {
		try {
			List<Mutation> mutations = new ArrayList<>();

			mutations.add(
				Mutation.delete(tableMapping.getTableName(), Key.of(key))
			);
			databaseClient.write(mutations);

			return true;
        } catch (SpannerException ex) {
            throw new EntryNotFoundException("Failed to delete entry", ex);
        }
	}

    @Override
    public long delete(String key, String objectClass, ConvertedExpression expression, int count) throws DeleteException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

    	long result = deleteImpl(tableMapping, expression, count);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: delete_search, duration: {}, table: {}, key: {}, expression: {}, count: {}", duration, tableMapping.getTableName(), key, expression, count);

        return result;
    }

    private long deleteImpl(TableMapping tableMapping, ConvertedExpression expression, int count) throws DeleteException {
		try {
			Table table = buildTable(tableMapping);

			// select
			PlainSelect sqlSelectQuery = new PlainSelect();
			sqlSelectQuery.setFromItem(table);

			// doc_id
			Column selectDocIdColumn = new Column(tableAlias, DOC_ID);
			SelectExpressionItem selectDocIdItem = new SelectExpressionItem(selectDocIdColumn);

			sqlSelectQuery.addSelectItems(selectDocIdItem);

			applyWhereExpression(sqlSelectQuery, expression);

			long useCount = connectionProvider.getMaximumResultDeleteSize();
			if (count > 0) {
				useCount = Math.min(count, useCount);
            }

			Limit limit = new Limit();
			limit.setRowCount(new LongValue(useCount));

			sqlSelectQuery.setLimit(limit);

			SubSelect subSelect = new SubSelect();
			subSelect.setSelectBody(sqlSelectQuery);
			subSelect.withUseBrackets(true);

			Expression inExpression = new InExpression(selectDocIdColumn, subSelect);

			Delete sqlDeleteQuery = new Delete();
			sqlDeleteQuery.setTable(table);
			sqlDeleteQuery.setWhere(inExpression);

			Statement.Builder statementBuilder = Statement.newBuilder(sqlDeleteQuery.toString());
			applyParametersBinding(statementBuilder, expression);

			Statement statement = statementBuilder.build();
            LOG.debug("Executing delete query: '{}'", statement);

			Long rowDeleted = databaseClient.readWriteTransaction().run(new TransactionCallable<Long>() {
				@Override
				public Long run(TransactionContext transaction) throws Exception {
					long rowCount = transaction.executeUpdate(statement);
					return rowCount;
				}
			});

			return rowDeleted;
        } catch (SpannerException | IncompatibleTypeException ex) {
            throw new DeleteException(String.format("Failed to delete entries. Expression: '%s'", expression.expression()), ex);
        }
	}

    @Override
    public boolean deleteRecursively(String key, String objectClass) throws EntryNotFoundException, SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = deleteRecursivelyImpl(tableMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: delete_tree, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

        return result;
    }

	private boolean deleteRecursivelyImpl(TableMapping tableMapping, String key) throws SearchException, EntryNotFoundException {
    	LOG.warn("Removing only base key without sub-tree. Table: {}, Key: {}", tableMapping.getTableName(), key);
    	return deleteImpl(tableMapping, key);
	}

    @Override
    public List<AttributeData> lookup(String key, String objectClass, String... attributes) throws SearchException, EntryConvertationException {
        Instant startTime = OperationDurationUtil.instance().now();
        
    	TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

    	List<AttributeData> result = lookupImpl(tableMapping, key, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: lookup, duration: {}, table: {}, key: {}, attributes: {}", duration, tableMapping.getTableName(), key, attributes);

        return result;
    }

	private List<AttributeData> lookupImpl(TableMapping tableMapping, String key, String... attributes) throws SearchException, EntryConvertationException {
		try {
			String tableName = tableMapping.getTableName();

			// If all requested attributes belong to one table get row by primary key
			Set<String> childTables = connectionProvider.getTableChildAttributes(tableName);
			List<AttributeData> result = null;
			if (childTables == null) {
				// All attributes in one table
				if (attributes == null) {
					// Request all attributes
	                try (ResultSet resultSet = databaseClient.singleUse().read(tableName, KeySet.singleKey(Key.of(key)), tableMapping.getColumTypes().keySet())) {
	    				result = getAttributeDataList(tableMapping.getObjectClass(), resultSet, true);
	                }
				} else {
					// Request only required attributes
	                try (ResultSet resultSet = databaseClient.singleUse().read(tableName, KeySet.singleKey(Key.of(key)), Arrays.asList(attributes))) {
	    				result = getAttributeDataList(tableMapping.getObjectClass(), resultSet, true);
	                }
				}
			} else {
				Table table = buildTable(tableMapping);
				PlainSelect sqlSelectQuery = new PlainSelect();
				sqlSelectQuery.setFromItem(table);

				List<SelectItem> selectItems = buildSelectAttributes(tableMapping, key, attributes);
				sqlSelectQuery.addSelectItems(selectItems);

				Column leftColumn = new Column(tableAlias, DOC_ID);
				UserVariable rightValue = new UserVariable(DOC_ID);

				EqualsTo whereExp = new EqualsTo(leftColumn, rightValue);
				sqlSelectQuery.setWhere(whereExp);

				Limit limit = new Limit();
				limit.setRowCount(new LongValue(1));
	    		sqlSelectQuery.setLimit(limit);

	    		Statement statement = Statement.newBuilder(sqlSelectQuery.toString()).bind(DOC_ID).to(key).build();
                LOG.debug("Executing lookup query: '{}'", statement);

                try (ResultSet resultSet = databaseClient.singleUse().executeQuery(statement)) {
    				result = getAttributeDataList(tableMapping.getObjectClass(), resultSet, true);
                }
			}

			if (result != null) {
				return result;
			}
		} catch (SpannerException ex) {
			throw new SearchException(String.format("Failed to lookup query by key: '%s'", key), ex);
		}

		throw new SearchException(String.format("Failed to lookup entry by key: '%s'", key));
	}

	@Override
    public <O> PagedResult<EntryData> search(String key, String objectClass, ConvertedExpression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
                                              SpannerBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

        PagedResult<EntryData> result = searchImpl(tableMapping, key, expression, scope, attributes, orderBy, batchOperationWraper,
						returnDataType, start, count, pageSize);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: search, duration: {}, table: {}, key: {}, expression: {}, scope: {}, attributes: {}, orderBy: {}, batchOperationWraper: {}, returnDataType: {}, start: {}, count: {}, pageSize: {}", duration, tableMapping.getTableName(), key, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize);

        return result;
	}

	private <O> PagedResult<EntryData> searchImpl(TableMapping tableMapping, String key, ConvertedExpression expression, SearchScope scope, String[] attributes, Sort[] orderBy,
            SpannerBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        BatchOperation<O> batchOperation = null;
        if (batchOperationWraper != null) {
            batchOperation = (BatchOperation<O>) batchOperationWraper.getBatchOperation();
        }

		Table table = buildTable(tableMapping);

		PlainSelect sqlSelectQuery = new PlainSelect();
		sqlSelectQuery.setFromItem(table);

		List<SelectItem> selectItems = buildSelectAttributes(tableMapping, key, attributes);
		sqlSelectQuery.addSelectItems(selectItems);
		
		if (expression != null) {
			applyWhereExpression(sqlSelectQuery, expression);
		}

        if (orderBy != null) {
        	OrderByElement[] orderByElements = new OrderByElement[orderBy.length];
        	for (int i = 0; i < orderBy.length; i++) {
        		Column column = new Column(orderBy[i].getName());
        		orderByElements[i] = new OrderByElement();
        		orderByElements[i].setExpression(column);
        		
        		if (orderBy[i].getSortOrder() != null) {
        			orderByElements[i].setAscDescPresent(true);
        			orderByElements[i].setAsc(SortOrder.ASCENDING == orderBy[i].getSortOrder());
        		}
        	}

            sqlSelectQuery.withOrderByElements(Arrays.asList(orderByElements));
        }

        List<EntryData> searchResultList = new LinkedList<EntryData>();
        if ((SearchReturnDataType.SEARCH == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
        	List<EntryData> lastResult = null;
	        if (pageSize > 0) {
	    		boolean collectSearchResult;
	    		Limit limit = new Limit();
	    		sqlSelectQuery.setLimit(limit);
	    		
	    		Offset offset = new Offset();
	    		sqlSelectQuery.setOffset(offset);
	
	            int currentLimit;
	    		try {
	                int resultCount = 0;
	                int lastCountRows = 0;
	                do {
	                    collectSearchResult = true;
	
	                    currentLimit = pageSize;
	                    if (count > 0) {
	                        currentLimit = Math.min(pageSize, count - resultCount);
	                    }

	                    // Change limit and offset
	    	    		limit.setRowCount(new LongValue(currentLimit));
	    	    		offset.setOffset(new LongValue(start + resultCount));
	                    
	    				Statement.Builder statementBuilder = Statement.newBuilder(sqlSelectQuery.toString());
	    				applyParametersBinding(statementBuilder, expression);

	    				Statement statement = statementBuilder.build();
	                    LOG.debug("Executing query: '{}'", statement);

	                    try (ResultSet resultSet = databaseClient.singleUse().executeQuery(statement)) {
	                    	lastResult = getEntryDataList(tableMapping.getObjectClass(), resultSet);
	                    }

		    			lastCountRows = lastResult.size();
		    			
	                    if (batchOperation != null) {
	                        collectSearchResult = batchOperation.collectSearchResult(lastCountRows);
	                    }
	                    if (collectSearchResult) {
	                        searchResultList.addAll(lastResult);
	                    }
	
	                    if (batchOperation != null) {
	                        List<O> entries = batchOperationWraper.createEntities(lastResult);
	                        batchOperation.performAction(entries);
	                    }
	
	                    resultCount += lastCountRows;
	
	                    if ((count > 0) && (resultCount >= count) || (lastCountRows < currentLimit)) {
	                        break;
	                    }
	                } while (lastCountRows > 0);
	    		} catch (SpannerException | EntryConvertationException | IncompatibleTypeException ex) {
	    			LOG.error("Failed to execute query with expression: '{}'", expression);
	    			throw new SearchException(String.format("Failed to execute query '%s'  with key: '%s'", sqlSelectQuery, key), ex);
	    		}
	        } else {
	    		try {
                    long currentLimit = count;
                    if (currentLimit <= 0) {
                        currentLimit = connectionProvider.getDefaultMaximumResultSize();
                    }

    	    		Limit limit = new Limit();
    	    		limit.setRowCount(new LongValue(currentLimit));
    	    		sqlSelectQuery.setLimit(limit);

    	    		if (start > 0) {
	    	    		Offset offset = new Offset();
	    	    		offset.setOffset(new LongValue(start));
	    	    		sqlSelectQuery.setOffset(offset);
	                }
	
    				Statement.Builder statementBuilder = Statement.newBuilder(sqlSelectQuery.toString());
    				applyParametersBinding(statementBuilder, expression);

    				Statement statement = statementBuilder.build();
                    LOG.debug("Executing query: '{}'", statement);

                    try (ResultSet resultSet = databaseClient.singleUse().executeQuery(statement)) {
		    			lastResult = getEntryDataList(tableMapping.getObjectClass(), resultSet);
		    			searchResultList.addAll(lastResult);
                    }
	            } catch (SpannerException | EntryConvertationException | IncompatibleTypeException ex) {
	    			LOG.error("Failed to execute query with expression: '{}'", expression);
	                throw new SearchException(String.format("Failed to execute query '%s'  with key: '%s'", sqlSelectQuery, key), ex);
	            }
	        }
        }

        PagedResult<EntryData> result = new PagedResult<EntryData>();
        result.setEntries(searchResultList);
        result.setEntriesCount(searchResultList.size());
        result.setStart(start);

        if ((SearchReturnDataType.COUNT == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
    		PlainSelect sqlCountSelectQuery = new PlainSelect();
    		sqlCountSelectQuery.setFromItem(table);

    		Function countFunction = new Function();
    		countFunction.setName("COUNT");
    		countFunction.setParameters(new ExpressionList(Collections.<Expression>singletonList(new Column("*"))));

    		SelectExpressionItem selectCountItem = new SelectExpressionItem(countFunction);
    		selectCountItem.setAlias(new Alias("TOTAL", false));

    		sqlCountSelectQuery.addSelectItems(selectCountItem);

    		if (expression != null) {
    			applyWhereExpression(sqlCountSelectQuery, expression);
    		}

    		try {
    			Statement.Builder statementBuilder = Statement.newBuilder(sqlCountSelectQuery.toString());
    			applyParametersBinding(statementBuilder, expression);

    			Statement statement = statementBuilder.build();
                LOG.debug("Calculating count. Executing query: '{}'", statement);

                try (ResultSet countResult = databaseClient.singleUse().executeQuery(statement)) {
                	if (!countResult.next()) {
                        throw new SearchException(String.format("Failed to calculate count entries. Query: '%s'", statement));
                	}

                	result.setTotalEntriesCount((int) countResult.getLong("TOTAL"));
                }
    		} catch (SpannerException | IncompatibleTypeException ex) {
    			LOG.error("Failed to execute query with expression: '{}'", expression);
    			throw new SearchException(String.format("Failed to build count search entries query. Key: '%s', expression: '%s'", key, expression.expression()), ex);
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

    private List<AttributeData> getAttributeDataList(String objectClass, ResultSet resultSet, boolean skipDn) throws EntryConvertationException {
        try {
            if ((resultSet == null)) {
                return null;
            }

            if (!resultSet.next()) {
            	return null;
            }

            List<AttributeData> result = new ArrayList<AttributeData>();

            Set<String> nullableColumns = connectionProvider.getTableNullableColumns(objectClass); // TODO: Include child table columns

	        List<StructField> structFields = resultSet.getType().getStructFields();
	        int columnsCount = resultSet.getColumnCount();
	        for (int i = 0; i < columnsCount; i++) {
	        	StructField structField = structFields.get(i);
	        	String attributeName = structField.getName();
	        	Code columnTypeCode = structField.getType().getCode();
	        	boolean isNullable = nullableColumns.contains(attributeName.toLowerCase());

	        	if (SpannerOperationService.DOC_ID.equalsIgnoreCase(attributeName) ||
	        		SpannerOperationService.ID.equalsIgnoreCase(attributeName)) {
	        		// Skip internal attributes 
	        		continue;
	        	}

	        	if (skipDn && SpannerOperationService.DN.equalsIgnoreCase(attributeName)) {
	        		// Skip DN attribute 
	        		continue;
	        	}
	
	        	Boolean multiValued = Boolean.FALSE;
	            Object[] attributeValueObjects;
	            if (resultSet.isNull(i)) {
	                attributeValueObjects = NO_OBJECTS;
	                if (isNullable) {
	                	// Ignore columns with default NULL values
	                	continue;
	                }
	            } else {
	            	if (Code.ARRAY == columnTypeCode) {
	            		attributeValueObjects = convertDbArrayToValue(resultSet, structField.getType().getArrayElementType(), i, attributeName);
	            		multiValued = Boolean.TRUE;
	            	} else if (Code.BOOL == columnTypeCode) {
	            		attributeValueObjects = new Object[] { resultSet.getBoolean(i) };
	            	} else if (Code.DATE == columnTypeCode) {
	            		attributeValueObjects = new Object[] { com.google.cloud.Date.toJavaUtilDate(resultSet.getDate(i)) };
	            	} else if (Code.TIMESTAMP == columnTypeCode) {
	            		attributeValueObjects = new Object[] { resultSet.getTimestamp(i).toDate() };
	            	} else if (Code.INT64 == columnTypeCode) {
	            		attributeValueObjects = new Object[] { resultSet.getLong(i) };
	            	} else if (Code.NUMERIC == columnTypeCode) {
	            		attributeValueObjects = new Object[] { resultSet.getBigDecimal(i).longValue() };
	            	} else if (Code.STRING == columnTypeCode) {
						Object value = resultSet.getString(i);
						try {
							value = com.google.cloud.Timestamp.parseTimestamp(value.toString());
						} catch (Exception ex) {
						}
						attributeValueObjects = new Object[] { value };
					} else {
						throw new EntryConvertationException(
								String.format("Column with name '%s' does not contain unsupported type '%s'", attributeName, columnTypeCode));
					}
	            }
	            
	            unescapeValues(attributeValueObjects);
	
	            AttributeData tmpAttribute = new AttributeData(attributeName, attributeValueObjects, multiValued);
	            if (multiValued != null) {
	            	tmpAttribute.setMultiValued(multiValued);
	            }
	            result.add(tmpAttribute);
	        }

	        return result;
        } catch (SpannerException ex) {
        	throw new EntryConvertationException("Failed to convert entry!", ex);
        }
    }

    private List<EntryData> getEntryDataList(String objectClass, ResultSet resultSet) throws EntryConvertationException {
    	List<EntryData> entryDataList = new LinkedList<>();

    	List<AttributeData> attributeDataList = null;
    	do  {
    		attributeDataList = getAttributeDataList(objectClass, resultSet, false);
    		if (attributeDataList != null) {
        		EntryData entryData = new EntryData(attributeDataList);
        		entryDataList.add(entryData);
    		}
    	} while (attributeDataList != null);

    	return entryDataList;
	}
      
    @Override
    public boolean isBinaryAttribute(String attribute) {
        return this.connectionProvider.isBinaryAttribute(attribute);
    }

    @Override
    public boolean isCertificateAttribute(String attribute) {
        return this.connectionProvider.isCertificateAttribute(attribute);
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

    @Override
    public DatabaseClient getConnection() {
        return connectionProvider.getClient();
    }

    @Override
    public Map<String, Map<String, StructField>> getMetadata() {
        return connectionProvider.getDatabaseMetaData();
    }

    @Override
    public TableMapping getTabeMapping(String key, String objectClass) {
    	TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

    	Map<String, TableMapping> childTableMapping = connectionProvider.getChildTablesMapping(key, tableMapping);
    	tableMapping.setChildTableMapping(childTableMapping);
    	
    	return tableMapping;
    }

	@Override
    public Set<String> getTabeChildAttributes(String objectClass) {
    	return connectionProvider.getTableChildAttributes(objectClass);
    }
    
    
	@Override
	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		this.persistenceExtension = persistenceExtension;
	}

	@Override
	public boolean isSupportObjectClass(String objectClass) {
		return connectionProvider.getDatabaseMetaData().containsKey(objectClass);
	}

	private List<SelectItem> buildSelectAttributes(TableMapping tableMapping, String key, String ... attributes) throws SearchException {
		String tableName = tableMapping.getTableName();
		Map<String, StructField> columTypes = tableMapping.getColumTypes();

		// Table alias for columns
		// Column dn
		Column selectDnColumn = new Column(tableAlias, DN);
		SelectExpressionItem selectDnItem = new SelectExpressionItem(selectDnColumn);

		// Column doc_id
		Column selectDocIdColumn = new Column(tableAlias, DOC_ID);
		SelectExpressionItem selectDocIdItem = new SelectExpressionItem(selectDocIdColumn);

		if (ArrayHelper.isEmpty(attributes)) {
			// Select all columns
			AllTableColumns allColumns = new AllTableColumns(tableAlias);
			List<SelectItem> selectColumns = new ArrayList<SelectItem>();
			selectColumns.add(allColumns);

			// Add columns from child tables
			List<SelectExpressionItem> selectChildColumns = buildSelectAttributeFromChildTables(tableName);
			selectColumns.addAll(selectChildColumns);

			return selectColumns;
		} else if ((attributes.length == 1) && StringHelper.isEmpty(attributes[0])) {
        	// Compatibility with base persistence layer when application pass attributes new String[] { "" }
			List<SelectItem> selectColumns = Arrays.asList(selectDnItem, selectDocIdItem);

			// Add columns from child tables
			List<SelectExpressionItem> selectChildColumns = buildSelectAttributeFromChildTables(tableName);
			selectColumns.addAll(selectChildColumns);

			return selectColumns;
		}
		
		List<SelectItem> expresisons = new ArrayList<SelectItem>(attributes.length + 2);
		
        boolean hasDn = false;
		for (String attributeName : attributes) {
			StructField attributeType = columTypes.get(attributeName.toLowerCase());
			SelectExpressionItem selectExpressionItem;

			// If column not inside table we should check if there is child table
			if (attributeType == null) {
				TableMapping childTableMapping = connectionProvider.getChildTableMappingByKey(key, tableMapping, attributeName);
				if (childTableMapping == null) {
		            throw new SearchException(String.format("Failed to build select attributes. Column '%s' is undefined", attributeName));
				}

				// Add columns from child table
				selectExpressionItem = buildSelectAttributeFromChildTable(tableName, attributeName);
			} else {
				Column selectColumn = new Column(tableAlias, attributeName);
				selectExpressionItem = new SelectExpressionItem(selectColumn);
			}

			expresisons.add(selectExpressionItem);

			hasDn |= StringHelper.equals(attributeName, DN);
		}

		if (!hasDn) {
			expresisons.add(selectDnItem);
		}

		expresisons.add(selectDocIdItem);

		return expresisons;
	}

	private Table buildTable(TableMapping tableMapping) {
		Table tableRelationalPath = new Table(tableMapping.getTableName());
		tableRelationalPath.setAlias(new Alias(DOC_ALIAS, false));

		return tableRelationalPath;
	}

	private List<SelectExpressionItem> buildSelectAttributeFromChildTables(String tableName) {
		List<SelectExpressionItem> selectChildColumns = new ArrayList<>();
		Set<String> childAttributes = connectionProvider.getTableChildAttributes(tableName);
		if (childAttributes != null) {
			selectChildColumns = new ArrayList<>();
			for (String childAttribute : childAttributes) {
				SelectExpressionItem selectChildColumn = buildSelectAttributeFromChildTable(tableName, childAttribute);
				selectChildColumns.add(selectChildColumn);
			}
		}

		return selectChildColumns;
	}

	private SelectExpressionItem buildSelectAttributeFromChildTable(String tableName, String childAttribute) {
		Function arrayFunction = new Function();
		arrayFunction.setName("ARRAY");
		arrayFunction.setAllColumns(false);

		SelectExpressionItem arraySelectItem = new SelectExpressionItem(arrayFunction);
		arraySelectItem.setAlias(new Alias(childAttribute, false));

		PlainSelect attrSelect = new PlainSelect();

		SubSelect attrSubSelect = new SubSelect();
		attrSubSelect.setSelectBody(attrSelect);
		attrSubSelect.withUseBrackets(false);
		arrayFunction.setParameters(new ExpressionList(attrSubSelect));

		Table attrTableSelect = new Table(tableName + "_" + childAttribute);
		attrTableSelect.setAlias(new Alias("c", false));
		attrSelect.setFromItem(attrTableSelect);
		
		Column attrSelectColumn = new Column(attrTableSelect, childAttribute);

		attrSelect.addSelectItems(new SelectExpressionItem(attrSelectColumn));

		Column attrLeftColumn = new Column(tableAlias, DOC_ID);

		Column attrRightColumn = new Column(attrTableSelect, DOC_ID);

		EqualsTo attrEquals = new EqualsTo(attrLeftColumn, attrRightColumn);

		attrSelect.withWhere(attrEquals);

		return arraySelectItem;
	}

	@Override
	public String escapeValue(String value) {
//		return StringHelper.escapeJson(value);
		return value;
	}

	@Override
	public void escapeValues(Object[] realValues) {
//		for (int i = 0; i < realValues.length; i++) {
//        	if (realValues[i] instanceof String) {
//        		realValues[i] = StringHelper.escapeJson(realValues[i]);
//        	}
//        }
	}

	@Override
	public String unescapeValue(String value) {
//		return StringHelper.unescapeJson(value);
		return value;
	}

	@Override
	public void unescapeValues(Object[] realValues) {
//		for (int i = 0; i < realValues.length; i++) {
//        	if (realValues[i] instanceof String) {
//        		realValues[i] = StringHelper.unescapeJson(realValues[i]);
//        	}
//        }
	}

	@Override
	public String toInternalAttribute(String attributeName) {
		return attributeName;
//		if (isDisableAttributeMapping()) {
//			return attributeName;
//		}
//
//		return KeyShortcuter.shortcut(attributeName);
	}

	@Override
	public String[] toInternalAttributes(String[] attributeNames) {
		return attributeNames;
//		if (isDisableAttributeMapping() || ArrayHelper.isEmpty(attributeNames)) {
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
//		if (isDisableAttributeMapping()) {
//			return internalAttributeName;
//		}
//
//		return KeyShortcuter.fromShortcut(internalAttributeName);
	}

	@Override
	public String[] fromInternalAttributes(String[] internalAttributeNames) {
		return internalAttributeNames;
//		if (isDisableAttributeMapping() || ArrayHelper.isEmpty(internalAttributeNames)) {
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

	private void applyParametersBinding(Statement.Builder builder, ConvertedExpression expression) throws IncompatibleTypeException {
		if (expression == null) {
			return;
		}

		Map<String, ValueWithStructField> queryParameters = expression.queryParameters();
		for (Entry<String, ValueWithStructField> queryParameterEntry : queryParameters.entrySet()) {
			String attributeName = queryParameterEntry.getKey();
			ValueWithStructField valueWithStructField = queryParameterEntry.getValue();
			ValueBinder<Builder> valueBinder = builder.bind(attributeName);

			setMutationBuilderValue(valueBinder, valueWithStructField.getStructField(), true, valueWithStructField.getValue());
		}
	}

	private void applyWhereExpression(Delete sqlDeleteQuery, ConvertedExpression expression) {
		if (expression == null) {
			return;
		}

		Expression whereExp = expression.expression();
		sqlDeleteQuery.setWhere(whereExp);
		Map<String, Join> joinTables = expression.joinTables();
		if (joinTables != null) {
			sqlDeleteQuery.setJoins(new ArrayList<>(joinTables.values()));
		}
	}

	private void applyWhereExpression(PlainSelect sqlSelectQuery, ConvertedExpression expression) {
		if (expression == null) {
			return;
		}

		Expression whereExp = expression.expression();
		sqlSelectQuery.setWhere(whereExp);
		Map<String, Join> joinTables = expression.joinTables();
		if ((joinTables != null) && (joinTables.size() > 0)) {
			sqlSelectQuery.setJoins(new ArrayList<>(joinTables.values()));
		}
	}

	private void setMutationBuilderValue(WriteBuilder mutation, StructField attributeType, Object ... values) throws IncompatibleTypeException {
		ValueBinder<WriteBuilder> valueBinder = mutation.set(attributeType.getName());

		setMutationBuilderValue(valueBinder, attributeType, false, values);
	}

	private void setMutationBuilderValue(ValueBinder<?> valueBinder, StructField attributeType, boolean useArrayElementType,
			Object ... values) throws IncompatibleTypeException {
		if ((values == null) || (values.length == 0)) {
			return;
		}

		Code typeCode = attributeType.getType().getCode();
		if (useArrayElementType && (Code.ARRAY == typeCode)) {
			typeCode = attributeType.getType().getArrayElementType().getCode();
		}

		if (Code.BOOL == typeCode) {
			valueBinder.to(SpannerValueHelper.toBoolean(values[0]));
		} else if (Code.DATE == typeCode) {
			valueBinder.to(SpannerValueHelper.toGoogleDate(values[0]));
		} else if (Code.TIMESTAMP == typeCode) {
			valueBinder.to(SpannerValueHelper.toGoogleTimestamp(values[0]));
		} else if (Code.INT64 == typeCode) {
			valueBinder.to(SpannerValueHelper.toLong(values[0]));
		} else if (Code.NUMERIC == typeCode) {
			valueBinder.to(SpannerValueHelper.toBigDecimal(values[0]));
		} else if (Code.STRING == typeCode) {
			Object value = values[0];
	        if (value instanceof Date) {
				valueBinder.to(SpannerValueHelper.toGoogleTimestamp(value).toString());
	        } else {
	        	valueBinder.to(SpannerValueHelper.toString(value));
	        }
		} else if (Code.ARRAY == typeCode) {
			Code arrayCode = attributeType.getType().getArrayElementType().getCode();
			if (Code.BOOL == arrayCode) {
				valueBinder.toBoolArray(SpannerValueHelper.toBooleanList(values));
			} else if (Code.DATE == arrayCode) {
				valueBinder.toDateArray(SpannerValueHelper.toGoogleDateList(values));
			} else if (Code.TIMESTAMP == arrayCode) {
				valueBinder.toTimestampArray(SpannerValueHelper.toGoogleTimestampList(values));
			} else if (Code.INT64 == arrayCode) {
				valueBinder.toInt64Array(SpannerValueHelper.toLongList(values));
			} else if (Code.NUMERIC == arrayCode) {
				valueBinder.toNumericArray(SpannerValueHelper.toBigDecimalList(values));
			} else if (Code.STRING == arrayCode) {
				valueBinder.toStringArray(SpannerValueHelper.toStringList(values));
			}
		} else {
			throw new IncompatibleTypeException(String.format(
					"Array column with name '%s' does not contain supported type '%s'", attributeType.getName(), attributeType.getType()));
		}
	}

	private void removeMutationBuilderValue(WriteBuilder mutation, AttributeData attribute, StructField attributeType) throws EntryConvertationException {
		ValueBinder<WriteBuilder> valueBinder = mutation.set(attributeType.getName());

		Code typeCode = attributeType.getType().getCode();
		if (Code.BOOL == typeCode) {
			valueBinder.to((Boolean) null);
		} else if (Code.DATE == typeCode) {
			valueBinder.to((com.google.cloud.Date) null);
		} else if (Code.TIMESTAMP == typeCode) {
			valueBinder.to((com.google.cloud.Timestamp) null);
		} else if (Code.INT64 == typeCode) {
			valueBinder.to((Long) null);
		} else if (Code.NUMERIC == typeCode) {
			valueBinder.to((BigDecimal) null);
		} else if (Code.STRING == typeCode) {
			valueBinder.to((String) null);
		} else if (Code.ARRAY == typeCode) {
			Code arrayCode = attributeType.getType().getArrayElementType().getCode();
			if (Code.BOOL == arrayCode) {
				valueBinder.toBoolArray((boolean[]) null);
			} else if (Code.DATE == arrayCode) {
				valueBinder.toDateArray((List<com.google.cloud.Date>) null);
			} else if (Code.TIMESTAMP == arrayCode) {
				valueBinder.toTimestampArray((List<com.google.cloud.Timestamp>) null);
			} else if (Code.INT64 == arrayCode) {
				valueBinder.toInt64Array((long[]) null);
			} else if (Code.NUMERIC == arrayCode) {
				valueBinder.toNumericArray((List<BigDecimal>) null);
			} else if (Code.STRING == arrayCode) {
				valueBinder.toStringArray((List<String>) null);
			}
		} else {
			throw new EntryConvertationException(String.format(
					"Array column with name '%s' does not contain supported type '%s'", attribute.getName(), attributeType));
		}
	}

	private Object[] convertDbArrayToValue(ResultSet resultSet, Type elementType, int columnIndex,
			String attributeName) throws EntryConvertationException {
		Code elementCode = elementType.getCode();
		if (Code.BOOL == elementCode) {
			return resultSet.getBooleanList(columnIndex).toArray(NO_OBJECTS);
		} else if (Code.DATE == elementCode) {
			return SpannerValueHelper.toJavaDateArrayFromSpannerDateList(resultSet.getDateList(columnIndex));
		} else if (Code.TIMESTAMP == elementCode) {
			return SpannerValueHelper.toJavaDateArrayFromSpannerTimestampList(resultSet.getTimestampList(columnIndex));
		} else if (Code.INT64 == elementCode) {
			return resultSet.getLongList(columnIndex).toArray(NO_OBJECTS);
		} else if (Code.NUMERIC == elementCode) {
			return SpannerValueHelper.toJavaLongArrayFromBigDecimalList(resultSet.getBigDecimalList(columnIndex));
		} else if (Code.STRING == elementCode) {
			return resultSet.getStringList(columnIndex).toArray(NO_OBJECTS);
		}

		throw new EntryConvertationException(String.format(
				"Array column with name '%s' does not contain supported type '%s'", attributeName, elementType));
	}

	public String getStringUniqueKey(MessageDigest messageDigest, Object value) {
		if (value == null) {
			return "null";
		}

		String str = StringHelper.toString(value);
		byte[] digest = messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));

		return Hex.encodeHexString(digest);
	}

	public static MessageDigest getMessageDigestInstance() {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available!");
		}

		return messageDigest;
	}

}
