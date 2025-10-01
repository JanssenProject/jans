/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.impl;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.NullExpression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import io.jans.orm.annotation.AttributeEnum;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapFilterConverter;
import io.jans.orm.model.AttributeType;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.sql.operation.SupportedDbType;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

/**
 * Filter to SQL query convert
 *
 * @author Yuriy Movchan Date: 12/16/2020
 */
public class SqlFilterConverter {

    private static final Logger LOG = LoggerFactory.getLogger(SqlFilterConverter.class);

    private static final LdapFilterConverter ldapFilterConverter = new LdapFilterConverter();
	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

	private SqlOperationService operationService;
	private SupportedDbType dbType;

	private Path<String> stringDocAlias = ExpressionUtils.path(String.class, "doc");
	private Path<Boolean> booleanDocAlias = ExpressionUtils.path(Boolean.class, "doc");
	private Path<Integer> integerDocAlias = ExpressionUtils.path(Integer.class, "doc");
	private Path<Long> longDocAlias = ExpressionUtils.path(Long.class, "doc");
	private Path<Date> dateDocAlias = ExpressionUtils.path(Date.class, "doc");
	private Path<Object> objectDocAlias = ExpressionUtils.path(Object.class, "doc");
	
	public static String[] SPECIAL_REGEX_CHARACTERS = new String[] { "\\", "/", ".", "*", "+", "?", "|", "(", ")", "[", "]", "{", "}" };
	
	private Map<String, JDBCType> jdbcEnumTypesMap;


    public SqlFilterConverter(SqlOperationService operationService) {
    	this.operationService = operationService;
    	this.dbType = operationService.getConnectionProvider().getDbType();
    	initJdbcEnumTypesMap();
	}

	private void initJdbcEnumTypesMap() {
		jdbcEnumTypesMap = new HashMap<>();
        for( JDBCType sqlType : JDBCType.class.getEnumConstants()) {
        	jdbcEnumTypesMap.put(StringHelper.toLowerCase(sqlType.name()) , sqlType);
        }
	}

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
    	return convertToSqlFilter(tableMapping, genericFilter, propertiesAnnotationsMap, false);
    }

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, boolean skipAlias) throws SearchException {
    	return convertToSqlFilter(tableMapping, genericFilter, propertiesAnnotationsMap, null, skipAlias);
    }

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor, boolean skipAlias) throws SearchException {
    	Map<String, Class<?>> jsonAttributes = new HashMap<>();
    	ConvertedExpression convertedExpression = convertToSqlFilterImpl(tableMapping, genericFilter, propertiesAnnotationsMap, jsonAttributes, processor, skipAlias);
    	
    	return convertedExpression;
    }

	private ConvertedExpression convertToSqlFilterImpl(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap,
			Map<String, Class<?>> jsonAttributes, Function<? super Filter, Boolean> processor, boolean skipAlias) throws SearchException {
		if (genericFilter == null) {
			return null;
		}

		Filter currentGenericFilter = genericFilter;

        FilterType type = currentGenericFilter.getType();
        if (FilterType.RAW == type) {
        	LOG.warn("RAW Ldap filter to SQL convertion will be removed in new version!!!");
        	currentGenericFilter = ldapFilterConverter.convertRawLdapFilterToFilter(currentGenericFilter.getFilterString());
        	type = currentGenericFilter.getType();
        }

        if (processor != null) {
        	processor.apply(currentGenericFilter);
        }

        if ((FilterType.NOT == type) || (FilterType.AND == type) || (FilterType.OR == type)) {
            Filter[] genericFilters = currentGenericFilter.getFilters();
            Predicate[] expFilters = new Predicate[genericFilters.length];

            if (genericFilters != null) {
            	boolean canJoinOrFilters = FilterType.OR == type; // We can replace only multiple OR with IN
            	List<Filter> joinOrFilters = new ArrayList<Filter>();
            	String joinOrAttributeName = null;
                for (int i = 0; i < genericFilters.length; i++) {
                	Filter tmpFilter = genericFilters[i];
                    expFilters[i] = (Predicate) convertToSqlFilterImpl(tableMapping, tmpFilter, propertiesAnnotationsMap, jsonAttributes, processor, skipAlias).expression();

                    // Check if we can replace OR with IN
                	if (!canJoinOrFilters) {
                		continue;
                	}

                	if ((FilterType.EQUALITY != tmpFilter.getType()) || (tmpFilter.getFilters() != null)) {
                    	canJoinOrFilters = false;
                    	continue;
                    }

                	if (tmpFilter.getMultiValued() != null) {
                		canJoinOrFilters = false;
                    	continue;
                	}

                    Boolean isMultiValuedDetected = determineMultiValuedByType(resolveAttributeName(tableMapping, tmpFilter), propertiesAnnotationsMap);
                	if (!Boolean.FALSE.equals(isMultiValuedDetected)) {
                		if (!Boolean.FALSE.equals(currentGenericFilter.getMultiValued())) { 
	                		canJoinOrFilters = false;
	                    	continue;
                		}
                	}
                	
            		if (joinOrAttributeName == null) {
            			joinOrAttributeName = resolveAttributeName(tableMapping, tmpFilter);
            			joinOrFilters.add(tmpFilter);
            			continue;
            		}
            		if (!joinOrAttributeName.equals(resolveAttributeName(tableMapping, tmpFilter))) {
                		canJoinOrFilters = false;
                    	continue;
            		}
            		joinOrFilters.add(tmpFilter);
                }

                if (FilterType.NOT == type) {
                    return ConvertedExpression.build(ExpressionUtils.predicate(Ops.NOT, expFilters[0]), jsonAttributes);
                } else if (FilterType.AND == type) {
                    return ConvertedExpression.build(ExpressionUtils.allOf(expFilters), jsonAttributes);
                } else if (FilterType.OR == type) {
                    if (canJoinOrFilters) {
                    	List<Object> rightObjs = new ArrayList<>(joinOrFilters.size());
                    	Filter lastEqFilter = null;
                		for (Filter eqFilter : joinOrFilters) {
                			lastEqFilter = eqFilter;

                			Object assertionValue = eqFilter.getAssertionValue();
                			if (assertionValue instanceof AttributeEnum) {
                				assertionValue = ((AttributeEnum) assertionValue).getValue();
                			}
                			rightObjs.add(assertionValue);
            			}
                		
                		return ConvertedExpression.build(ExpressionUtils.in(buildTypedPath(tableMapping, lastEqFilter, propertiesAnnotationsMap, jsonAttributes, processor, skipAlias).expression(), rightObjs), jsonAttributes);
                	} else {
                        return ConvertedExpression.build(ExpressionUtils.anyOf(expFilters), jsonAttributes);
                	}
            	}
            }
        }

        boolean multiValued = isMultiValue(tableMapping, currentGenericFilter, propertiesAnnotationsMap);
    	TypedPath typedPathColumn = buildTypedPath(tableMapping, currentGenericFilter, propertiesAnnotationsMap, jsonAttributes, processor, skipAlias);
    	String pathAttributeName = resolvePathAttributeName(typedPathColumn, currentGenericFilter);
    	Expression columnExpression = typedPathColumn.expression();

    	if (FilterType.EQUALITY == type) {
    		if (multiValued) {
    			if (SupportedDbType.POSTGRESQL == this.dbType) {
    				if (StringHelper.isEmpty(pathAttributeName)) {
            			Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.PGSQL_JSON_CONTAINS, columnExpression,
            					buildTypedArrayExpression(tableMapping, currentGenericFilter));

                		return ConvertedExpression.build(operation, jsonAttributes);
    				} else {
    	            	return buildPostgreSqlMultivaluedComparisionExpression(tableMapping, jsonAttributes,
    							currentGenericFilter, typedPathColumn);
    				}
    			} else {
    				if (StringHelper.isEmpty(pathAttributeName)) {
    					Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_CONTAINS, columnExpression,
		    					buildTypedArrayExpression(tableMapping, currentGenericFilter), buildExpressionArrayPath(pathAttributeName, -1));
		    			return ConvertedExpression.build(operation, jsonAttributes);
    				} else {
    					Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
		        				columnExpression, buildExpressionArrayPath(pathAttributeName, 0));
		        		Expression expression = Expressions.asComparable(operation).eq(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName));
		        		
		        		return ConvertedExpression.build(expression, jsonAttributes);
    				}
    			}
            }
    		Expression typedExpression = buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName);
    		if (typedExpression instanceof NullExpression) {
    			return ConvertedExpression.build(ExpressionUtils.isNull(columnExpression), jsonAttributes);
    		}
        	return ConvertedExpression.build(ExpressionUtils.eq(columnExpression, typedExpression), jsonAttributes);
        }

        if (FilterType.LESS_OR_EQUAL == type) {
            if (multiValued) {
    			if (SupportedDbType.POSTGRESQL == this.dbType) {
	            	return buildPostgreSqlMultivaluedComparisionExpression(tableMapping, jsonAttributes,
							currentGenericFilter, typedPathColumn);
    			} else {
	            	if (currentGenericFilter.getMultiValuedCount() > 1) {
	                	Collection<Predicate> expressions = new ArrayList<>(currentGenericFilter.getMultiValuedCount());
	            		for (int i = 0; i < currentGenericFilter.getMultiValuedCount(); i++) {
	                		Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	                				columnExpression, buildExpressionArrayPath(pathAttributeName, i));
	                		Predicate predicate = Expressions.asComparable(operation).loe(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName));
	
	                		expressions.add(predicate);
	            		}
	
	            		Expression expression = ExpressionUtils.anyOf(expressions);
	
	            		return ConvertedExpression.build(expression, jsonAttributes);
	            	}
	
	            	Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	        				columnExpression, buildExpressionArrayPath(pathAttributeName, 0));
	        		Expression expression = Expressions.asComparable(operation).loe(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName));

	            	return ConvertedExpression.build(expression, jsonAttributes);
    			}
            } else {
            	return ConvertedExpression.build(Expressions.asComparable(columnExpression).loe(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName)), jsonAttributes);
            }
        }

        if (FilterType.GREATER_OR_EQUAL == type) {
            if (multiValued) {
    			if (SupportedDbType.POSTGRESQL == this.dbType) {
	            	return buildPostgreSqlMultivaluedComparisionExpression(tableMapping, jsonAttributes,
							currentGenericFilter, typedPathColumn);
    			} else {
	            	if (currentGenericFilter.getMultiValuedCount() > 1) {
	                	Collection<Predicate> expressions = new ArrayList<>(currentGenericFilter.getMultiValuedCount());
	            		for (int i = 0; i < currentGenericFilter.getMultiValuedCount(); i++) {
	                		Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	                				columnExpression, buildExpressionArrayPath(pathAttributeName, i));
	                		Predicate predicate = Expressions.asComparable(operation).goe(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName));
	
	                		expressions.add(predicate);
	            		}
	            		Expression expression = ExpressionUtils.anyOf(expressions);
	
	            		return ConvertedExpression.build(expression, jsonAttributes);
	            	}
	
	            	Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	        				columnExpression, buildExpressionArrayPath(pathAttributeName, 0));
	        		Expression expression = Expressions.asComparable(operation).goe(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName));
	
	            	return ConvertedExpression.build(expression, jsonAttributes);
    			}
            } else {
            	return ConvertedExpression.build(Expressions.asComparable(columnExpression).goe(buildTypedExpression(tableMapping, currentGenericFilter, pathAttributeName)), jsonAttributes);
            }
        }

        if (FilterType.PRESENCE == type) {
        	Expression expression;
            if (multiValued) {
    			if (SupportedDbType.POSTGRESQL == this.dbType) {
    				Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.PGSQL_JSON_NOT_EMPTY_ARRAY, 
    						columnExpression);
    				return ConvertedExpression.build(operation, jsonAttributes);
    			} else {
	            	if (currentGenericFilter.getMultiValuedCount() > 1) {
	                	Collection<Predicate> expressions = new ArrayList<>(currentGenericFilter.getMultiValuedCount());
	            		for (int i = 0; i < currentGenericFilter.getMultiValuedCount(); i++) {
	            			Predicate predicate = ExpressionUtils.isNotNull(ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	                				columnExpression, buildExpressionArrayPath(pathAttributeName, i)));
	            			expressions.add(predicate);
	            		}
	            		Predicate predicate = ExpressionUtils.anyOf(expressions);
	
	            		return ConvertedExpression.build(predicate, jsonAttributes);
	            	}
	
	            	expression = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	        				columnExpression, buildExpressionArrayPath(pathAttributeName, 0));
    			}
            } else {
            	expression = columnExpression;
            }

            return ConvertedExpression.build(ExpressionUtils.isNotNull(expression), jsonAttributes);
        }

        if (FilterType.APPROXIMATE_MATCH == type) {
            throw new SearchException("Convertion from APPROXIMATE_MATCH LDAP filter to SQL filter is not implemented");
        }

        if (FilterType.SUBSTRING == type) {
        	String matchChar = multiValued && (SupportedDbType.POSTGRESQL == this.dbType) ? ".*" : "%";
        	StringBuilder like = new StringBuilder();
            if (currentGenericFilter.getSubInitial() != null) {
                like.append(currentGenericFilter.getSubInitial());
            }
            like.append(matchChar);

            String[] subAny = currentGenericFilter.getSubAny();
            if ((subAny != null) && (subAny.length > 0)) {
                for (String any : subAny) {
        			if (SupportedDbType.POSTGRESQL == this.dbType) {
        				if (multiValued) {
        					like.append(escapeRegex(any));
        				} else {
        					like.append(any);
        				}
        			} else {
        				like.append(any);
        			}
                    like.append(matchChar);
                }
            }

            if (currentGenericFilter.getSubFinal() != null) {
                like.append(currentGenericFilter.getSubFinal());
            }

            Expression expression;
            if (multiValued) {
    			if (SupportedDbType.POSTGRESQL == this.dbType) {
    				String likeString = StringEscapeUtils.escapeJava(like.toString());
	            	return buildPostgreSqlMultivaluedComparisionExpression(tableMapping, jsonAttributes,
							currentGenericFilter, typedPathColumn, Expressions.constant("like_regex"),
							likeString);
    			} else {
	            	if (currentGenericFilter.getMultiValuedCount() > 1) {
	                	Collection<Predicate> expressions = new ArrayList<>(currentGenericFilter.getMultiValuedCount());
	            		for (int i = 0; i < currentGenericFilter.getMultiValuedCount(); i++) {
	                		Operation<Boolean> operation = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	                				columnExpression, buildExpressionArrayPath(pathAttributeName, i));
	                		Predicate predicate = Expressions.booleanOperation(Ops.LIKE, operation, Expressions.constant(like.toString()));
	
	                		expressions.add(predicate);
	            		}
	            		Predicate predicate = ExpressionUtils.anyOf(expressions);
	
	            		return ConvertedExpression.build(predicate, jsonAttributes);
	            	}
	
	            	expression = ExpressionUtils.predicate(SqlOps.JSON_EXTRACT,
	        				columnExpression, buildExpressionArrayPath(pathAttributeName, 0));
    			}
            } else {
            	expression = columnExpression;
            }

            return ConvertedExpression.build(Expressions.booleanOperation(Ops.LIKE, expression, Expressions.constant(like.toString())), jsonAttributes);
        }

        if (FilterType.LOWERCASE == type) {
        	return ConvertedExpression.build(ExpressionUtils.toLower(columnExpression), jsonAttributes);
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type));
	}

	private Expression<String> buildExpressionArrayPath(String pathAttributeName, int i) {
		if (i == -1) {
			return Expressions.constant("$");
		} else {
			if (StringHelper.isEmpty(pathAttributeName)) {
				return Expressions.constant(String.format("$[%d]", i));
			} else {
				return Expressions.constant(String.format("$[%d].%s", i, pathAttributeName));
			}
		}
	}

	private ConvertedExpression buildPostgreSqlMultivaluedComparisionExpression(TableMapping tableMapping,
			Map<String, Class<?>> jsonAttributes, Filter currentGenericFilter,
			TypedPath columnExpression) throws SearchException {
		Object typedArrayExpressionValue = prepareTypedArrayExpressionValue(tableMapping, currentGenericFilter);
		Expression operationExpression;
		if (FilterType.EQUALITY == currentGenericFilter.getType()) {
			operationExpression = Expressions.constant("==");			
		} else {
			operationExpression = Expressions.constant(currentGenericFilter.getType().getSign());			
		}

		return buildPostgreSqlMultivaluedComparisionExpression(tableMapping, jsonAttributes, currentGenericFilter, columnExpression,
				operationExpression, typedArrayExpressionValue);
	}

	private ConvertedExpression buildPostgreSqlMultivaluedComparisionExpression(TableMapping tableMapping,
			Map<String, Class<?>> jsonAttributes, Filter currentGenericFilter, TypedPath columnExpression, Expression operationExpession, Object expressionValue) {
		Expression<?> typedArrayExpression;
		if (expressionValue == null) {
			typedArrayExpression = Expressions.nullExpression();
		} else {
			if (expressionValue instanceof Boolean) {
				typedArrayExpression = Expressions.asString(((Boolean) expressionValue).toString());
			} else if (expressionValue instanceof String) {
				typedArrayExpression = Expressions.asString("\"" + (String) expressionValue + "\"");
			} else {
				typedArrayExpression = Expressions.constant(expressionValue);
			}
		}
		
		String pathAttributeName = resolvePathAttributeName(columnExpression, currentGenericFilter);
		
		Operation<Boolean> operation = null;
		if (StringHelper.isEmpty(pathAttributeName)) {
			operation = ExpressionUtils.predicate(SqlOps.PGSQL_JSON_PATH_QUERY_EXISTS,
					columnExpression.expression, operationExpession,
					typedArrayExpression);
		} else {
			operation = ExpressionUtils.predicate(SqlOps.PGSQL_JSON_PATH_MAP_QUERY_EXISTS,
					columnExpression.expression, Expressions.constant(pathAttributeName), operationExpession,
					typedArrayExpression);
		}
		return ConvertedExpression.build(operation, jsonAttributes);
	}

	protected Boolean isMultiValue(TableMapping tableMapping, Filter filter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
		String attributeName = resolveAttributeName(tableMapping, filter);
		AttributeType attributeType = null;
		if (StringHelper.isNotEmpty(attributeName)) {
			attributeType = resolveAttributeType(tableMapping, filter);
			if (attributeType == null) {
				if (tableMapping != null) {
					throw new SearchException(String.format(String.format("Failed to find attribute type for '%s'", attributeName)));
				}
			}
		}
		
		if ((attributeType == null) && (filter.getMultiValued() != null)) {
			return filter.getMultiValued();
		}

		Boolean isMultiValuedDetected = determineMultiValuedByType(attributeName, propertiesAnnotationsMap);
		if ((Boolean.TRUE.equals(filter.getMultiValued()) || Boolean.TRUE.equals(isMultiValuedDetected))) {
			if ((attributeType != null) && Boolean.TRUE.equals(attributeType.getMultiValued())) {
				return true;
			}
		}

		return false;
	}

	private AttributeType resolveAttributeType(TableMapping tableMapping, Filter filter) throws SearchException {
		if ((tableMapping == null) || (filter == null)) {
			return null;
		}

		String attributeName = filter.getAttributeName();
		if (attributeName == null) {
			return null;
		}

		String attributeNameLower = attributeName.toLowerCase();
		AttributeType attributeType = tableMapping.getColumTypes().get(attributeNameLower);
		if (attributeType == null) {
			int idx = attributeNameLower.indexOf(".");
			if (idx != -1) {
				attributeNameLower = attributeNameLower.substring(0, idx).toLowerCase();
				attributeType = tableMapping.getColumTypes().get(attributeNameLower);
				if (attributeType != null) {
					return attributeType;
				}
			}
	        throw new SearchException(String.format("Unknown column name '%s' in table/child table '%s'", attributeName, tableMapping.getTableName()));
		}

		return attributeType;
	}

	private String resolveAttributeName(TableMapping tableMapping, Filter filter) throws SearchException {
		if ((tableMapping == null) || (filter == null)) {
			return filter.getAttributeName();
		}

		String attributeName = filter.getAttributeName();
		if (attributeName == null) {
			return null;
		}

		String attributeNameLower = attributeName.toLowerCase();
		
		if (tableMapping.getColumTypes() == null) {
			 throw new SearchException(String.format("Unknown table '%s' and it's column '%s'", tableMapping.getTableName(), attributeName));
		}

		AttributeType attributeType = tableMapping.getColumTypes().get(attributeNameLower);
		if (attributeType == null) {
			int idx = attributeName.indexOf(".");
			if (idx != -1) {
				attributeName = attributeName.substring(0, idx);
				return attributeName;
			}
	        throw new SearchException(String.format("Unknown column name '%s' in table/child table '%s'", attributeName, tableMapping.getTableName()));
		}

		return attributeName;
	}

	private String resolvePathAttributeName(TypedPath typedPathColumn, Filter filter) {
		String fullAttributeName = filter.getAttributeName();
		
		if (StringHelper.isEmpty(fullAttributeName)) {
			return null;
		}

		String attributeName = typedPathColumn.attribute();
		int jsonPathIdx = fullAttributeName.indexOf(attributeName, 0);
		if ((jsonPathIdx > -1) && (attributeName.length() + jsonPathIdx < fullAttributeName.length())) {
			return fullAttributeName.substring(attributeName.length() + jsonPathIdx + 1);
		}
		
		return null;
	}

	private String toInternalAttribute(TableMapping tableMapping, Filter filter) throws SearchException {
		String tableName = tableMapping == null ? null : tableMapping.getTableName();

		String attributeName = resolveAttributeName(tableMapping, filter);

		if (StringHelper.isEmpty(attributeName)) {
			// Try to find inside sub-filter
			for (Filter subFilter : filter.getFilters()) {
				attributeName = resolveAttributeName(tableMapping, subFilter);
				if (StringHelper.isNotEmpty(attributeName)) {
					break;
				}
			}
		}

		return toInternalAttribute(tableName, attributeName);
	}

	private String toInternalAttribute(String tableName, String attributeName) {
		if ((operationService == null) || (tableName == null)) {
			return attributeName;
		}

		return operationService.toInternalAttribute(tableName, attributeName);
	}

	private Expression buildTypedExpression(TableMapping tableMapping, Filter filter, String pathAttributeName) throws SearchException {
		Object expressionValue = prepareTypedExpressionValue(tableMapping, filter);
		
		Expression<?> expression = Expressions.nullExpression();
		if (expressionValue != null) {
			if (StringHelper.isNotEmpty(pathAttributeName) && (SupportedDbType.MYSQL == this.dbType) && (expressionValue instanceof Boolean)) {
				expression = Expressions.constant(((Boolean) expressionValue).toString());
			} else {
				expression = Expressions.constant(expressionValue);
			}
		}
		return expression;
	}

	private Expression buildTypedArrayExpression(TableMapping tableMapping, Filter filter) throws SearchException {
		Object assertionValue = prepareTypedArrayExpressionValue(tableMapping, filter);

		String assertionValueJson = convertValueToJson(Arrays.asList(assertionValue));

		return Expressions.constant(assertionValueJson);
	}

	private Object prepareTypedArrayExpressionValue(TableMapping tableMapping, Filter filter) throws SearchException {
		Object assertionValue = prepareTypedExpressionValue(tableMapping, filter);

		if (assertionValue instanceof Date) {
	        assertionValue = operationService.encodeTime((Date) assertionValue);
		}

		return assertionValue;
	}

	private Object prepareTypedExpressionValue(TableMapping tableMapping, Filter filter) throws SearchException {
		AttributeType attributeType = null;
		String attributeName = resolveAttributeName(tableMapping, filter);
		if (StringHelper.isNotEmpty(attributeName)) {
			attributeType = resolveAttributeType(tableMapping, filter);
			if (attributeType == null) {
				if (tableMapping != null) {
					throw new SearchException(String.format(String.format("Failed to find attribute type for '%s'", attributeName)));
				}
			}
		}

		Object assertionValue = filter.getAssertionValue();
		if (assertionValue instanceof AttributeEnum) {
			assertionValue = ((AttributeEnum) assertionValue).getValue();
		} else if (assertionValue instanceof String) {
			if ((attributeType != null) && SqlOperationService.TIMESTAMP.equals(attributeType.getType())) {
				Date dateValue = operationService.decodeTime((String) assertionValue, true);
				if (dateValue != null) {
					assertionValue = dateValue;
				}
			}

			if (attributeType != null) {
				String columnType = attributeType.getType();

				java.sql.JDBCType jdbcType = null;
				// Fix for PostgreSQL 
				if (StringHelper.equalsIgnoreCase(columnType, "bool")) { 
					jdbcType = java.sql.JDBCType.BOOLEAN;
				} else {
					String lowerCaseColumnType = StringHelper.toLowerCase(columnType);
					if (jdbcEnumTypesMap.containsKey(lowerCaseColumnType)) {
						jdbcType = jdbcEnumTypesMap.get(lowerCaseColumnType);
					} else {
						LOG.trace("Failed to determine JDBC type '{}' ", attributeType.getType());
						// Do nothing. Type is not defined in enum
					}
				}

				if (jdbcType == java.sql.JDBCType.SMALLINT) {
					boolean res = StringHelper.equalsIgnoreCase((String) assertionValue, "true") || StringHelper.equalsIgnoreCase((String) assertionValue, "1");
					assertionValue = res ? 1 : 0;
				} else if (jdbcType == java.sql.JDBCType.BOOLEAN) {
					boolean res = StringHelper.equalsIgnoreCase((String) assertionValue, "true") || StringHelper.equalsIgnoreCase((String) assertionValue, "1");
					assertionValue = res;
				}
			}
		}

		return assertionValue;
	}

	private TypedPath buildTypedPath(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap,
			Map<String, Class<?>> jsonAttributes, Function<? super Filter, Boolean> processor, boolean skipAlias) throws SearchException {
    	boolean hasSubFilters = ArrayHelper.isNotEmpty(genericFilter.getFilters());

		if (hasSubFilters) {
			Expression expression = convertToSqlFilterImpl(tableMapping, genericFilter.getFilters()[0], propertiesAnnotationsMap, jsonAttributes, processor, skipAlias).expression();
			
			return new TypedPath(null, expression);
		}
		
		String internalAttribute = toInternalAttribute(tableMapping, genericFilter);
		
		Expression expression = buildTypedPath(tableMapping, genericFilter, internalAttribute, skipAlias);
		
		return new TypedPath(internalAttribute, expression);
	}

	private Expression buildTypedPath(TableMapping tableMapping, Filter filter, String attributeName, boolean skipAlias) throws SearchException {

		AttributeType attributeType = resolveAttributeType(tableMapping, filter);
		if (attributeType == null) {
			if (tableMapping != null) {
				throw new SearchException(String.format(String.format("Failed to find attribute type for '%s'", attributeName)));
			}
		}

		if ((attributeType != null) && SqlOperationService.TIMESTAMP.equals(attributeType.getType())) {
   	    	if (skipAlias) {
   	    		return Expressions.dateTimePath(Date.class, attributeName);
   	    	} else {
   	    		return Expressions.dateTimePath(Date.class, dateDocAlias, attributeName);
   	    	}
		}
   	    if (filter.getAssertionValue() instanceof String) {
   	    	if (skipAlias) {
   	    		return Expressions.stringPath(attributeName);
   	    	} else {
   	    		return Expressions.stringPath(stringDocAlias, attributeName);
   	    	}
   	    } else if (filter.getAssertionValue() instanceof Boolean) {
   	    	if (skipAlias) {
   	   	    	return Expressions.booleanPath(attributeName);
   	    	} else {
   	   	    	return Expressions.booleanPath(booleanDocAlias, attributeName);
   	    	}
		} else if (filter.getAssertionValue() instanceof Integer) {
   	    	if (skipAlias) {
   	   	    	return Expressions.stringPath(attributeName);
   	    	} else {
   	   	    	return Expressions.stringPath(integerDocAlias, attributeName);
   	    	}
		} else if (filter.getAssertionValue() instanceof Long) {
   	    	if (skipAlias) {
   	   	    	return Expressions.stringPath(attributeName);
   	    	} else {
   	   	    	return Expressions.stringPath(longDocAlias, attributeName);
   	    	}
		}

    	if (skipAlias) {
    	    return Expressions.stringPath(attributeName);
    	} else {
    	    return Expressions.stringPath(objectDocAlias, attributeName);
    	}
	}

	private Boolean determineMultiValuedByType(String attributeName, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		if ((attributeName == null) || (propertiesAnnotationsMap == null)) {
			return null;
		}

		if (StringHelper.equalsIgnoreCase(attributeName, SqlEntryManager.OBJECT_CLASS)) {
			return false;
		}

		PropertyAnnotation propertyAnnotation = propertiesAnnotationsMap.get(attributeName);
		if ((propertyAnnotation == null) || (propertyAnnotation.getParameterType() == null)) {
			return null;
		}

		Class<?> parameterType = propertyAnnotation.getParameterType();
		
		boolean isMultiValued = parameterType.equals(Object[].class) || parameterType.equals(String[].class) || ReflectHelper.assignableFrom(parameterType, List.class) || ReflectHelper.assignableFrom(parameterType, Map.class) || ReflectHelper.assignableFrom(parameterType, AttributeEnum[].class);
		
		return isMultiValued;
	}

	protected String convertValueToJson(Object propertyValue) throws SearchException {
		try {
			String value = JSON_OBJECT_MAPPER.writeValueAsString(propertyValue);

			return value;
		} catch (Exception ex) {
			LOG.error("Failed to convert '{}' to json value:", propertyValue, ex);
			throw new SearchException(String.format("Failed to convert '%s' to json value", propertyValue));
		}
	}

	private Object escapeRegex(String str) {
		String result = str;
		for (String ch : SPECIAL_REGEX_CHARACTERS) {
			result = result.replace(ch, "\\" + ch);
		}

		return result;
	}
	
	class TypedPath {

		private String attributeName;
		private Expression expression;

		public TypedPath(String attributeName, Expression expression) {
			this.attributeName = attributeName;
			this.expression = expression;
		}

		public String attribute() {
			return attributeName;
		}

		public Expression expression() {
			return expression;
		}

	}

}
