/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.QueryException;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;

import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.operation.DeleteException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.EntryConvertationException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;
import io.jans.orm.operation.auth.PasswordEncryptionHelper;
import io.jans.orm.sql.impl.SqlBatchOperationWraper;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.model.JsonAttributeValue;
import io.jans.orm.sql.model.JsonString;
import io.jans.orm.sql.model.SearchReturnDataType;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.sql.operation.SupportedDbType;
import io.jans.orm.sql.operation.watch.OperationDurationUtil;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

/**
 * Base service which performs all supported SQL operations
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public class SqlOperationServiceImpl implements SqlOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(SqlOperationServiceImpl.class);

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    private Properties props;
    private SqlConnectionProvider connectionProvider;

	private boolean disableAttributeMapping = false;

	private boolean mariaDb = false;

	private PersistenceExtension persistenceExtension;

	private SQLQueryFactory sqlQueryFactory;

	private String schemaName;

	private Path<String> docAlias = ExpressionUtils.path(String.class, DOC_ALIAS);
	private Path<String> docInnerAlias = ExpressionUtils.path(String.class, DOC_INNER_ALIAS);

    @SuppressWarnings("unused")
    private SqlOperationServiceImpl() {
    }

    public SqlOperationServiceImpl(Properties props, SqlConnectionProvider connectionProvider) {
        this.props = props;
        this.connectionProvider = connectionProvider;
        init();
    }

	private void init() {
		this.sqlQueryFactory = connectionProvider.getSqlQueryFactory();
		this.schemaName = connectionProvider.getSchemaName();
		this.mariaDb = connectionProvider.isMariaDb();
	}

    @Override
    public SqlConnectionProvider getConnectionProvider() {
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
        OperationDurationUtil.instance().logDebug("SQL operation: bind, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

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
			Map<String, AttributeType> columTypes = tableMapping.getColumTypes();

			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLInsertClause sqlInsertQuery = this.sqlQueryFactory.insert(tableRelationalPath);

			for (AttributeData attribute : attributes) {
				AttributeType attributeType = getAttributeType(columTypes, attribute);
				if (attributeType == null) {
		            throw new PersistenceException(String.format("Failed to find attribute type for '%s'", attribute.getName()));
				}

				boolean multiValued = (attributeType != null) && isJsonColumn(tableMapping.getTableName(), attributeType.getType());

				if (multiValued || Boolean.TRUE.equals(attribute.getMultiValued())) {
					sqlInsertQuery.columns(Expressions.path(Object.class, attribute.getName()));
					sqlInsertQuery.values(convertValueToDbJson(attribute.getValues()));
				} else {
					sqlInsertQuery.columns(Expressions.stringPath(attribute.getName()));
					sqlInsertQuery.values(attribute.getValue());
				}
			}
			
			long rowInserted = sqlInsertQuery.execute();

			return rowInserted == 1;
        } catch (QueryException ex) {
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

	private boolean updateEntryImpl(TableMapping tableMapping, String key, List<AttributeDataModification> mods) throws PersistenceException {
		try {
			Map<String, AttributeType> columTypes = tableMapping.getColumTypes();

			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLUpdateClause sqlUpdateQuery = this.sqlQueryFactory.update(tableRelationalPath);

			for (AttributeDataModification attributeMod : mods) {
				AttributeData attribute = attributeMod.getAttribute();
				Path path = Expressions.stringPath(attribute.getName());

				AttributeType attributeType = getAttributeType(columTypes, attribute);
				if (attributeType == null) {
		            throw new PersistenceException(String.format("Failed to find attribute type for '%s'", attribute.getName()));
				}

				boolean multiValued = (attributeType != null) && isJsonColumn(tableMapping.getTableName(), attributeType.getType());
				
				AttributeModificationType type = attributeMod.getModificationType();
                if ((AttributeModificationType.ADD == type) || (AttributeModificationType.FORCE_UPDATE == type)) {
					if (multiValued || Boolean.TRUE.equals(attribute.getMultiValued())) {
    					sqlUpdateQuery.set(path, convertValueToDbJson(attribute.getValues()));
    				} else {
    					sqlUpdateQuery.set(path, attribute.getValue());
    				}
                } else if (AttributeModificationType.REPLACE == type) {
					if (multiValued || Boolean.TRUE.equals(attribute.getMultiValued())) {
    					sqlUpdateQuery.set(path, convertValueToDbJson(attribute.getValues()));
    				} else {
    					sqlUpdateQuery.set(path, attribute.getValue());
    				}
                } else if (AttributeModificationType.REMOVE == type) {
    				sqlUpdateQuery.setNull(path);
                } else {
                    throw new UnsupportedOperationException("Operation type '" + type + "' is not implemented");
                }
			}

			Predicate whereExp = ExpressionUtils.eq(Expressions.stringPath(SqlOperationService.DOC_ID),
					Expressions.constant(key));

			long rowInserted = sqlUpdateQuery.where(whereExp).execute();

			return rowInserted == 1;
        } catch (QueryException ex) {
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
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLDeleteClause sqlDeleteQuery = this.sqlQueryFactory.delete(tableRelationalPath);

			Predicate exp = ExpressionUtils.eq(Expressions.stringPath(SqlOperationService.DOC_ID), Expressions.constant(key));
			sqlDeleteQuery.where(exp);

			long rowDeleted = sqlDeleteQuery.execute();

			return rowDeleted == 1;
        } catch (QueryException ex) {
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
			Predicate exp = (Predicate) expression.expression();
			SQLDeleteClause sqlDeleteQuery;

			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			if ((count > 0) && (SupportedDbType.POSTGRESQL == connectionProvider.getDbType())) {
				// Workaround because PostgreSQL not supports limit in delete request

				// Inner query
				RelationalPathBase<Object> innerTableRelationalPath = new RelationalPathBase<>(Object.class, DOC_INNER_ALIAS, this.schemaName, tableMapping.getTableName());
				SubQueryExpression<String> sqlSelectQuery = this.sqlQueryFactory.select(Expressions.path(String.class, docInnerAlias, DOC_ID)).from(innerTableRelationalPath)
						.where(exp).limit(count);

				Predicate deleteExp = ExpressionUtils.in(Expressions.stringPath(DOC_ID), sqlSelectQuery);
				sqlDeleteQuery = this.sqlQueryFactory.delete(tableRelationalPath).where(deleteExp);
			} else {
				sqlDeleteQuery = this.sqlQueryFactory.delete(tableRelationalPath).where(exp);
				if (count > 0) {
					sqlDeleteQuery = sqlDeleteQuery.limit(count);
	            }
			}

			long rowDeleted = sqlDeleteQuery.execute();

			return rowDeleted;
        } catch (QueryException ex) {
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
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);

			Predicate whereExp = ExpressionUtils.eq(Expressions.stringPath(SqlOperationService.DOC_ID),
					Expressions.constant(key));
			Expression<?> attributesExp = buildSelectAttributes(attributes);

			SQLQuery<?> sqlSelectQuery = sqlQueryFactory.select(attributesExp).from(tableRelationalPath)
					.where(whereExp).limit(1);
			
			try (ResultSet resultSet = sqlSelectQuery.getResults();) {
				List<AttributeData> result = getAttributeDataList(tableMapping, resultSet, true);
				if (result != null) {
					return result;
				}
			}
		} catch (SQLException | QueryException ex) {
			throw new SearchException(String.format("Failed to lookup query by key: '%s'", key), ex);
		}

		throw new SearchException(String.format("Failed to lookup entry by key: '%s'", key));
	}

	@Override
    public <O> PagedResult<EntryData> search(String key, String objectClass, ConvertedExpression expression, SearchScope scope, String[] attributes, OrderSpecifier<?>[] orderBy,
                                              SqlBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

        PagedResult<EntryData> result = searchImpl(tableMapping, key, expression, scope, attributes, orderBy, batchOperationWraper,
						returnDataType, start, count, pageSize);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: search, duration: {}, table: {}, key: {}, expression: {}, scope: {}, attributes: {}, orderBy: {}, batchOperationWraper: {}, returnDataType: {}, start: {}, count: {}, pageSize: {}", duration, tableMapping.getTableName(), key, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize);

        return result;
	}

	private <O> PagedResult<EntryData> searchImpl(TableMapping tableMapping, String key, ConvertedExpression expression, SearchScope scope, String[] attributes, OrderSpecifier<?>[] orderBy,
            SqlBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        BatchOperation<O> batchOperation = null;
        if (batchOperationWraper != null) {
            batchOperation = (BatchOperation<O>) batchOperationWraper.getBatchOperation();
        }

        RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);

		Expression<?> attributesExp = buildSelectAttributes(attributes);

		SQLQuery<?> sqlSelectQuery;
		if (expression == null) {
			sqlSelectQuery = sqlQueryFactory.select(attributesExp).from(tableRelationalPath);
		} else {
			Predicate whereExp = (Predicate) expression.expression();
			sqlSelectQuery = sqlQueryFactory.select(attributesExp).from(tableRelationalPath).where(whereExp);
		}

        SQLQuery<?> baseQuery = sqlSelectQuery;
        if (orderBy != null) {
            baseQuery = sqlSelectQuery.orderBy(orderBy);
        }

        List<EntryData> searchResultList = new LinkedList<EntryData>();

        String queryStr = null;
        if ((SearchReturnDataType.SEARCH == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
        	List<EntryData> lastResult = null;
	        if (pageSize > 0) {
	            boolean collectSearchResult;
	
	            SQLQuery<?> query;
	            ResultSet resultSet = null;
	            int currentLimit;
	    		try {
	                int resultCount = 0;
	                int lastCountRows = 0;
	                do {
	                    currentLimit = pageSize;
	                    if (count > 0) {
	                        currentLimit = Math.min(pageSize, count - resultCount);
	                    }
	
	                    query = baseQuery.limit(currentLimit).offset(start + resultCount);

	                    queryStr = query.getSQL().getSQL();
	                    LOG.debug("Executing query: '" + queryStr + "'");

	                    resultSet = query.getResults();
	                    lastResult = getEntryDataList(tableMapping, resultSet);

		    			lastCountRows = lastResult.size();
		    			
	                    collectSearchResult = true;
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
	
	                    if (((count > 0) && (resultCount >= count)) || (lastCountRows < currentLimit)) {
	                        break;
	                    }
	                } while (lastCountRows > 0);
        		} catch (QueryException ex) {
        			throw new SearchException(String.format("Failed to build search entries query. Key: '%s', expression: '%s'", key, expression.expression()), ex);
	    		} catch (SQLException | EntryConvertationException ex) {
	    			throw new SearchException(String.format("Failed to execute query '%s'  with key: '%s'", queryStr, key), ex);
	    		} finally {
	    			if (resultSet != null) {
	    				try {
							resultSet.close();
						} catch (SQLException ex) {
			    			throw new SearchException(String.format("Failed to close query after paged result collection. Query '%s'  with key: '%s'", queryStr, key), ex);
						}
	    			}
	    		}
	        } else {
	    		try {
	                SQLQuery<?> query = baseQuery;
	                if (count > 0) {
	                    query = query.limit(count);
	                }
	                if (start > 0) {
	                    query = query.offset(start);
	                }
	
                    queryStr = query.getSQL().getSQL();

                    LOG.debug("Execution query: '" + queryStr + "'");

                    try (ResultSet resultSet = query.getResults()) {
		    			lastResult = getEntryDataList(tableMapping, resultSet);
		    			searchResultList.addAll(lastResult);
                    }
        		} catch (QueryException ex) {
        			String sqlExpression = queryStr;
        			if (StringHelper.isNotEmpty(sqlExpression)) {
        				sqlExpression = expression.expression().toString();
        			}
					throw new SearchException(String.format("Failed to build search entries query. Key: '%s', expression: '%s'", key, sqlExpression), ex);
	            } catch (SQLException | EntryConvertationException ex) {
	                throw new SearchException("Failed to search entries. Query: '" + queryStr + "'", ex);
	            }
	        }
        }

        PagedResult<EntryData> result = new PagedResult<EntryData>();
        result.setEntries(searchResultList);
        result.setEntriesCount(searchResultList.size());
        result.setStart(start);

        if ((SearchReturnDataType.COUNT == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
    		SQLQuery<?> sqlCountSelectQuery;
    		if (expression == null) {
    			sqlCountSelectQuery = sqlQueryFactory.select(Expressions.as(ExpressionUtils.count(Wildcard.all), "TOTAL")).from(tableRelationalPath);
    		} else {
    			Predicate whereExp = (Predicate) expression.expression();
    			sqlCountSelectQuery = sqlQueryFactory.select(Expressions.as(ExpressionUtils.count(Wildcard.all), "TOTAL")).from(tableRelationalPath).where(whereExp);
    		}

    		try {
                queryStr = sqlCountSelectQuery.getSQL().getSQL();
                LOG.debug("Calculating count. Execution query: '" + queryStr + "'");

                try (ResultSet countResult = sqlCountSelectQuery.getResults()) {
                	if (!countResult.next()) {
                        throw new SearchException("Failed to calculate count entries. Query: '" + queryStr + "'");
                	}

                	result.setTotalEntriesCount(countResult.getInt("TOTAL"));
                }
    		} catch (QueryException ex) {
    			throw new SearchException(String.format("Failed to build count search entries query. Key: '%s', expression: '%s'", key, expression.expression()), ex);
            } catch (SQLException ex) {
                throw new SearchException("Failed to calculate count entries. Query: '" + queryStr + "'", ex);
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

    private List<AttributeData> getAttributeDataList(TableMapping tableMapping, ResultSet resultSet, boolean skipDn) throws EntryConvertationException {
        try {
            if ((resultSet == null)) {
                return null;
            }

            if (!resultSet.next()) {
            	return null;
            }

            List<AttributeData> result = new ArrayList<AttributeData>();
	        int columnsCount = resultSet.getMetaData().getColumnCount();
	        for (int i = 1; i <= columnsCount; i++) {
	        	ResultSetMetaData metaData = resultSet.getMetaData();
	        	String shortAttributeName = metaData.getColumnName(i);
	        	String columnTypeName = metaData.getColumnTypeName(i).toLowerCase();
	        	boolean isNullable = metaData.isNullable(i) == ResultSetMetaData.columnNullable;

	        	Object attributeObject = resultSet.getObject(shortAttributeName);

	        	if (SqlOperationService.DOC_ID.equalsIgnoreCase(shortAttributeName) ||
	        		SqlOperationService.ID.equalsIgnoreCase(shortAttributeName)) {
	        		// Skip internal attributes 
	        		continue;
	        	}

	        	if (skipDn && SqlOperationService.DN.equalsIgnoreCase(shortAttributeName)) {
	        		// Skip DN attribute 
	        		continue;
	        	}

	        	String attributeName = fromInternalAttribute(shortAttributeName);
	
	        	Boolean multiValued = Boolean.FALSE;
	            Object[] attributeValueObjects;
	            if (attributeObject == null) {
	                attributeValueObjects = NO_OBJECTS;
	                if (isNullable) {
	                	// Ignore columns with default NULL values
	                	continue;
	                }
	            } else {
	            	if (isJsonColumn(tableMapping.getTableName(), columnTypeName)) {
	            		attributeValueObjects = convertDbJsonToValue(attributeObject.toString());
	            		multiValued = Boolean.TRUE;
	            	} else if (attributeObject instanceof Integer) {
						int columnType = resultSet.getMetaData().getColumnType(i);
						if (columnType == java.sql.Types.SMALLINT) {
							if (attributeObject.equals(0)) {
								attributeObject = Boolean.FALSE;
							} else if (attributeObject.equals(1)) {
								attributeObject = Boolean.TRUE;
							}
						}

						attributeValueObjects = new Object[] { attributeObject };
					} else if ((attributeObject instanceof Boolean) || (attributeObject instanceof Long)) {
						attributeValueObjects = new Object[] { attributeObject };
					} else if (attributeObject instanceof String) {
						Object value = attributeObject.toString();
						Date dateValue = decodeTime(attributeObject.toString(), true);
						if (dateValue != null) {
							value = dateValue;
						}

						attributeValueObjects = new Object[] { value };
					} else if (attributeObject instanceof Timestamp) {
						attributeValueObjects = new Object[] {
								new java.util.Date(((Timestamp) attributeObject).getTime()) };
					} else if (attributeObject instanceof LocalDateTime) {
						attributeValueObjects = new Object[] {
								new java.util.Date(Timestamp.valueOf((LocalDateTime) attributeObject).getTime()) };
					} else {
						Object value = attributeObject.toString();
						attributeValueObjects = new Object[] { value };
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
        } catch (SQLException ex) {
        	throw new EntryConvertationException("Failed to convert entry!", ex);
        }
    }

    private List<EntryData> getEntryDataList(TableMapping tableMapping, ResultSet resultSet) throws EntryConvertationException, SQLException {
    	List<EntryData> entryDataList = new LinkedList<>();

    	while (!resultSet.isLast()) {
    		List<AttributeData> attributeDataList = getAttributeDataList(tableMapping, resultSet, false);
    		if (attributeDataList == null) {
    			break;
    		}

    		EntryData entryData = new EntryData(attributeDataList);
    		entryDataList.add(entryData);
    	}

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
    public Connection getConnection() {
        return connectionProvider.getConnection();
    }

    @Override
    public DatabaseMetaData getMetadata() {
        return connectionProvider.getDatabaseMetaData();
    }

	@Override
	public TableMapping getTabeMapping(String key, String objectClass) {
    	TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
    	
    	return tableMapping;
	}

	@Override
	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		this.persistenceExtension = persistenceExtension;
	}

	@Override
	public boolean isSupportObjectClass(String objectClass) {
		return true;
	}

	private Expression<?> buildSelectAttributes(String ... attributes) {
		if (ArrayHelper.isEmpty(attributes)) {
			return Expressions.list(Wildcard.all, Expressions.path(Object.class, docAlias, DOC_ID));
		} else if ((attributes.length == 1) && StringHelper.isEmpty(attributes[0])) {
        	// Compatibility with base persistence layer when application pass filter new String[] { "" }
			return Expressions.list(Expressions.path(Object.class, docAlias, DN), Expressions.path(Object.class, docAlias, DOC_ID));
		}
		
		List<Expression<?>> expresisons = new ArrayList<Expression<?>>(attributes.length + 2);
		
        boolean hasDn = false;
		for (String attribute : attributes) {
			expresisons.add(Expressions.path(Object.class, docAlias, attribute));

			hasDn |= StringHelper.equals(attribute, DN);
		}

		if (!hasDn) {
			expresisons.add(Expressions.path(Object.class, docAlias, DN));
		}

		expresisons.add(Expressions.path(Object.class, docAlias, DOC_ID));

		return Expressions.list(expresisons.toArray(new Expression<?>[0]));
	}

	private RelationalPathBase<Object> buildTableRelationalPath(TableMapping tableMapping) {
		RelationalPathBase<Object> tableRelationalPath = new RelationalPathBase<>(Object.class, DOC_ALIAS, this.schemaName, tableMapping.getTableName());

		return tableRelationalPath;
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

	@Override
    public String encodeTime(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat jsonDateFormat = new SimpleDateFormat(SqlOperationService.SQL_DATA_FORMAT);
        return jsonDateFormat.format(date);
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
	        try {
	            SimpleDateFormat jsonDateFormat = new SimpleDateFormat(SqlOperationService.SQL_DATA_FORMAT);
	            return jsonDateFormat.parse(date);
	        } catch (ParseException ex2) {
	        	if (!silent) {
		            LOG.error("Failed to decode generalized time '{}'", date, ex2);
	        	}
	
	        	return null;
	        }
        }
    }

	private Object convertValueToDbJson(Object propertyValue) {
		try {
			if (SupportedDbType.POSTGRESQL == connectionProvider.getDbType()) {
				Object[] attributeValue;
				if (propertyValue == null) {
					attributeValue = new Object[0];
				} if (propertyValue instanceof List) {
					attributeValue = ((List<?>) propertyValue).toArray();
				} else if (propertyValue.getClass().isArray()) {
					attributeValue = (Object[]) propertyValue;
				} else {
					attributeValue = new Object[] { propertyValue };
				}
	
				String value = JSON_OBJECT_MAPPER.writeValueAsString(attributeValue);
	
				return new JsonString(value);
			} else {
				JsonAttributeValue attributeValue;
				if (propertyValue == null) {
					attributeValue = new JsonAttributeValue();
				} if (propertyValue instanceof List) {
					attributeValue = new JsonAttributeValue(((List<?>) propertyValue).toArray());
				} else if (propertyValue.getClass().isArray()) {
					attributeValue = new JsonAttributeValue((Object[]) propertyValue);
				} else {
					attributeValue = new JsonAttributeValue(new Object[] { propertyValue });
				}

				String value = JSON_OBJECT_MAPPER.writeValueAsString(attributeValue);

				return value;
			}
		} catch (Exception ex) {
			LOG.error("Failed to convert '{}' to json value:", propertyValue, ex);
			throw new MappingException(String.format("Failed to convert '%s' to json value", propertyValue));
		}
	}

	private Object[] convertDbJsonToValue(String jsonValue) {
		try {
//			Object[] values = JSON_OBJECT_MAPPER.readValue(jsonValue, Object[].class);
			if (SupportedDbType.POSTGRESQL == connectionProvider.getDbType()) {
				Object[] values = JSON_OBJECT_MAPPER.readValue(jsonValue, Object[].class);

				return values;
			} else {
				JsonAttributeValue attributeValue = JSON_OBJECT_MAPPER.readValue(jsonValue, JsonAttributeValue.class);
				
				Object[] values = null;
				if (attributeValue != null) {
					values = attributeValue.getValues();
				}
	
				return values;
			}
		} catch (Exception ex) {
			LOG.error("Failed to convert json value '{}' to array:", jsonValue, ex);
			throw new MappingException(String.format("Failed to convert json value '%s' to array", jsonValue));
		}
	}

	public boolean isJsonColumn(String tableName, String columnTypeName) {
		if (columnTypeName == null) {
			return false;
		}

		if (mariaDb && SqlOperationService.LONGTEXT_TYPE_NAME.equals(columnTypeName)) {
			return true;
		}
		
//		String engineType = connectionProvider.getEngineType(tableName);
//		if ((engineType != null) && engineType.equalsIgnoreCase("mariadb")) {
//			return "longtext".equals(columnTypeName);
//		}

		return SqlOperationService.JSON_TYPE_NAME.equals(columnTypeName) || SqlOperationService.JSONB_TYPE_NAME.equals(columnTypeName);
		
	}

	private AttributeType getAttributeType(Map<String, AttributeType> columTypes, AttributeData attribute) {
		return columTypes.get(attribute.getName().toLowerCase());
	}

}
