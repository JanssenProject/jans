/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.Type.StructField;

import io.jans.orm.annotation.AttributeEnum;
import io.jans.orm.cloud.spanner.model.ConvertedExpression;
import io.jans.orm.cloud.spanner.model.TableMapping;
import io.jans.orm.cloud.spanner.model.ValueWithStructField;
import io.jans.orm.cloud.spanner.operation.SpannerOperationService;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapFilterConverter;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;

/**
 * Filter to Cloud Spanner query convert
 *
 * @author Yuriy Movchan Date: 04/08/2021
 */
public class SpannerFilterConverter {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerFilterConverter.class);
    
    private static final LdapFilterConverter ldapFilterConverter = new LdapFilterConverter();

	private SpannerOperationService operationService;

	private Table tableAlias = new Table(SpannerOperationService.DOC_ALIAS);

	public SpannerFilterConverter(SpannerOperationService operationService) {
    	this.operationService = operationService;
	}

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
    	return convertToSqlFilter(tableMapping, genericFilter, propertiesAnnotationsMap, false);
    }

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, boolean skipAlias) throws SearchException {
    	return convertToSqlFilter(tableMapping, genericFilter, propertiesAnnotationsMap, null, skipAlias);
    }

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
    	return convertToSqlFilter(tableMapping, genericFilter, propertiesAnnotationsMap, processor, false);
    }

	public ConvertedExpression convertToSqlFilter(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor, boolean skipAlias) throws SearchException {
    	Map<String, ValueWithStructField> queryParameters = new HashMap<>();
    	Map<String, Join> joinTables = new HashMap<>();
    	ConvertedExpression convertedExpression = convertToSqlFilterImpl(tableMapping, genericFilter, propertiesAnnotationsMap, queryParameters, joinTables, processor, skipAlias);
    	
    	return convertedExpression;
    }

	private ConvertedExpression convertToSqlFilterImpl(TableMapping tableMapping, Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap,
			Map<String, ValueWithStructField> queryParameters, Map<String, Join> joinTables, Function<? super Filter, Boolean> processor, boolean skipAlias) throws SearchException {
		if (genericFilter == null) {
			return null;
		}

		Filter currentGenericFilter = genericFilter;

        FilterType type = currentGenericFilter.getType();
        if (FilterType.RAW == type) {
        	LOG.warn("RAW Ldap filter to SQL convertion will be removed in new version!!!");
        	currentGenericFilter = ldapFilterConverter.convertRawLdapFilterToFilter(currentGenericFilter.getFilterString());
        	LOG.debug(String.format("Converted RAW filter: %s", currentGenericFilter));
        	type = currentGenericFilter.getType();
        }

        if (processor != null) {
        	processor.apply(currentGenericFilter);
        }

        if ((FilterType.NOT == type) || (FilterType.AND == type) || (FilterType.OR == type)) {
            Filter[] genericFilters = currentGenericFilter.getFilters();
            Expression[] expFilters = new Expression[genericFilters.length];

            if (genericFilters != null) {
            	boolean canJoinOrFilters = FilterType.OR == type; // We can replace only multiple OR with IN
            	List<Filter> joinOrFilters = new ArrayList<Filter>();
            	String joinOrAttributeName = null;
                for (int i = 0; i < genericFilters.length; i++) {
                	Filter tmpFilter = genericFilters[i];
                    expFilters[i] = convertToSqlFilterImpl(tableMapping, tmpFilter, propertiesAnnotationsMap, queryParameters, joinTables, processor, skipAlias).expression();

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

                    Boolean isMultiValuedDetected = determineMultiValuedByType(tmpFilter.getAttributeName(), propertiesAnnotationsMap);
                	if (!Boolean.FALSE.equals(isMultiValuedDetected)) {
                		if (!Boolean.FALSE.equals(currentGenericFilter.getMultiValued())) { 
	                		canJoinOrFilters = false;
	                    	continue;
                		}
                	}
                	
            		if (joinOrAttributeName == null) {
            			joinOrAttributeName = tmpFilter.getAttributeName();
            			joinOrFilters.add(tmpFilter);
            			continue;
            		}
            		if (!joinOrAttributeName.equals(tmpFilter.getAttributeName())) {
                		canJoinOrFilters = false;
                    	continue;
            		}
            		joinOrFilters.add(tmpFilter);
                }

                if (FilterType.NOT == type) {
                    return ConvertedExpression.build(new NotExpression(expFilters[0]), queryParameters, joinTables);
                } else if (FilterType.AND == type) {
                	Expression result = expFilters[0];
                    for (int i = 1; i < expFilters.length; i++) {
                        result = new AndExpression(result, expFilters[i]);
                    }

                    return ConvertedExpression.build(new Parenthesis(result), queryParameters, joinTables);
                } else if (FilterType.OR == type) {
                    if (canJoinOrFilters) {
                		ExpressionList expressionList = new ExpressionList();
                		for (Expression expFilter : expFilters) {
                    		expressionList.addExpressions(((EqualsTo) expFilter).getRightExpression());
                		}
                		
                		Expression inExpression = new InExpression(buildExpression(tableMapping, joinOrFilters.get(0), false, false, propertiesAnnotationsMap, queryParameters, joinTables, processor, skipAlias), expressionList);

                		return ConvertedExpression.build(inExpression, queryParameters, joinTables);
                	} else {
                    	Expression result = expFilters[0];
                        for (int i = 1; i < expFilters.length; i++) {
                            result = new OrExpression(result, expFilters[i]);
                        }

                        return ConvertedExpression.build(new Parenthesis(result), queryParameters, joinTables);
                	}
            	}
            }
        }

        // Generic part for rest of expression types
    	String internalAttribute = toInternalAttribute(currentGenericFilter);
    	boolean multiValued = isMultiValue(tableMapping, internalAttribute, currentGenericFilter, propertiesAnnotationsMap);
    	boolean hasChildTableForAttribute = tableMapping.hasChildTableForAttribute(internalAttribute.toLowerCase());
		Expression leftExpression = buildExpression(tableMapping, currentGenericFilter, multiValued, !hasChildTableForAttribute, propertiesAnnotationsMap, queryParameters, joinTables, processor, skipAlias);

    	if (FilterType.EQUALITY == type) {
        	Expression variableExpression = buildVariableExpression(tableMapping, internalAttribute, currentGenericFilter.getAssertionValue(), queryParameters);
    		Expression expression = new EqualsTo(leftExpression, variableExpression);
    		if (multiValued) {
    			if (hasChildTableForAttribute) {
    				// JOIN jansClnt_Interleave_jansRedirectURI jansRedirectURI ON doc.doc_id = jansRedirectURI.doc_id
    				// WHERE jansRedirectURI.jansRedirectURI = '10'
    	    		addJoinTable(tableMapping, internalAttribute, joinTables);
    			} else {
    				// EXISTS (SELECT _jansRedirectURI FROM UNNEST(doc.jansRedirectURI) _jansRedirectURI WHERE _jansRedirectURI = '10')
    	    		expression = buildExistsInArrayExpression(internalAttribute, expression);
    			}

    			return ConvertedExpression.build(expression, queryParameters, joinTables);
            }
        	return ConvertedExpression.build(expression, queryParameters, joinTables);
        }

        if (FilterType.LESS_OR_EQUAL == type) {
        	Expression variableExpression = buildVariableExpression(tableMapping, internalAttribute, currentGenericFilter.getAssertionValue(), queryParameters);
        	Expression expression = new MinorThanEquals().withLeftExpression(leftExpression).withRightExpression(variableExpression);
    		if (multiValued) {
    			if (hasChildTableForAttribute) {
    	    		addJoinTable(tableMapping, internalAttribute, joinTables);
    			} else {
    	    		expression = buildExistsInArrayExpression(internalAttribute, expression);
    			}

    			return ConvertedExpression.build(expression, queryParameters, joinTables);
            }
        	return ConvertedExpression.build(expression, queryParameters, joinTables);
        }

        if (FilterType.GREATER_OR_EQUAL == type) {
        	Expression variableExpression = buildVariableExpression(tableMapping, internalAttribute, currentGenericFilter.getAssertionValue(), queryParameters);
        	Expression expression = new GreaterThanEquals().withLeftExpression(leftExpression).withRightExpression(variableExpression);
    		if (multiValued) {
    			if (hasChildTableForAttribute) {
    	    		addJoinTable(tableMapping, internalAttribute, joinTables);
    			} else {
    	    		expression = buildExistsInArrayExpression(internalAttribute, expression);
    			}

    			return ConvertedExpression.build(expression, queryParameters, joinTables);
            }
        	return ConvertedExpression.build(expression, queryParameters, joinTables);
        }

        if (FilterType.PRESENCE == type) {
        	Expression expression = new IsNullExpression().withLeftExpression(leftExpression).withNot(true);
    		if (multiValued) {
    			if (hasChildTableForAttribute) {
    	    		addJoinTable(tableMapping, internalAttribute, joinTables);
    			} else {
    	    		expression = buildExistsInArrayExpression(internalAttribute, expression);
    			}

    			return ConvertedExpression.build(expression, queryParameters, joinTables);
            }
        	return ConvertedExpression.build(expression, queryParameters, joinTables);
        }

        if (FilterType.APPROXIMATE_MATCH == type) {
            throw new SearchException("Convertion from APPROXIMATE_MATCH LDAP filter to SQL filter is not implemented");
        }

        if (FilterType.SUBSTRING == type) {
        	StringBuilder like = new StringBuilder();
            if (currentGenericFilter.getSubInitial() != null) {
                like.append(currentGenericFilter.getSubInitial());
            }
            like.append("%");

            String[] subAny = currentGenericFilter.getSubAny();
            if ((subAny != null) && (subAny.length > 0)) {
                for (String any : subAny) {
                    like.append(any);
                    like.append("%");
                }
            }

            if (currentGenericFilter.getSubFinal() != null) {
                like.append(currentGenericFilter.getSubFinal());
            }
            
            String likeValue = like.toString();
        	Expression variableExpression = buildVariableExpression(tableMapping, internalAttribute, likeValue, queryParameters);
        	Expression expression = new LikeExpression().withLeftExpression(leftExpression).withRightExpression(variableExpression);
    		if (multiValued) {
    			if (hasChildTableForAttribute) {
    	    		addJoinTable(tableMapping, internalAttribute, joinTables);
    			} else {
    	    		expression = buildExistsInArrayExpression(internalAttribute, expression);
    			}

    			return ConvertedExpression.build(expression, queryParameters, joinTables);
            }
        	return ConvertedExpression.build(expression, queryParameters, joinTables);
        }

        if (FilterType.LOWERCASE == type) {
    		net.sf.jsqlparser.expression.Function lowerFunction = new net.sf.jsqlparser.expression.Function();
    		lowerFunction.setName("LOWER");
    		lowerFunction.setParameters(new ExpressionList(leftExpression));

        	return ConvertedExpression.build(lowerFunction, queryParameters, joinTables);
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type));
	}

	protected Boolean isMultiValue(TableMapping tableMapping, String attributeName, Filter currentGenericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
		StructField structField = getStructField(tableMapping, attributeName);
		boolean hasChildTableForAttribute = tableMapping.hasChildTableForAttribute(attributeName.toLowerCase());

    	Code columnTypeCode = structField.getType().getCode();
    	if ((Code.ARRAY == columnTypeCode) || hasChildTableForAttribute) {
    		boolean multiValuedDetected = (Boolean.TRUE.equals(currentGenericFilter.getMultiValued()) ||
    				Boolean.TRUE.equals(
    						determineMultiValuedByType(currentGenericFilter.getAttributeName(), propertiesAnnotationsMap)));
    		
    		if (!multiValuedDetected) {
    			LOG.warn(String.format("Сolumn name '%s' was defined as multi valued in table/child table '%s' but detected as single value", attributeName, tableMapping.getTableName()));
    		}

    		return true;
    	}

		return false;
	}

	private StructField getStructField(TableMapping tableMapping, String attributeName) throws SearchException {
		String attributeNameLower = attributeName.toLowerCase();

		StructField structField = tableMapping.getColumTypes().get(attributeNameLower);
		if (structField == null) {
			TableMapping childTableMapping = tableMapping.getChildTableMappingForAttribute(attributeNameLower);
			if (childTableMapping != null) {
				structField = childTableMapping.getColumTypes().get(attributeNameLower);
			}
		}

		if (structField == null) {
	        throw new SearchException(String.format("Unknown column name '%s' in table/child table '%s'", attributeName, tableMapping.getTableName()));
		}

		return structField;
	}

	private Boolean determineMultiValuedByType(String attributeName, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		if ((attributeName == null) || (propertiesAnnotationsMap == null)) {
			return null;
		}

		if (StringHelper.equalsIgnoreCase(attributeName, SpannerEntryManager.OBJECT_CLASS)) {
			return false;
		}

		PropertyAnnotation propertyAnnotation = propertiesAnnotationsMap.get(attributeName);
		if ((propertyAnnotation == null) || (propertyAnnotation.getParameterType() == null)) {
			return null;
		}

		Class<?> parameterType = propertyAnnotation.getParameterType();
		
		boolean isMultiValued = parameterType.equals(Object[].class) || parameterType.equals(String[].class) || ReflectHelper.assignableFrom(parameterType, List.class) || ReflectHelper.assignableFrom(parameterType, AttributeEnum[].class);
		
		return isMultiValued;
	}

	private String toInternalAttribute(Filter filter) {
		String attributeName = filter.getAttributeName();

		if (StringHelper.isEmpty(attributeName)) {
			// Try to find inside sub-filter
			for (Filter subFilter : filter.getFilters()) {
				attributeName = subFilter.getAttributeName();
				if (StringHelper.isNotEmpty(attributeName)) {
					break;
				}
			}
		}

		return toInternalAttribute(attributeName);
	}

	private String toInternalAttribute(String attributeName) {
		if (operationService == null) {
			return attributeName;
		}

		return operationService.toInternalAttribute(attributeName);
	}

	private Expression buildVariableExpression(TableMapping tableMapping, String attributeName, Object attribyteValue, Map<String, ValueWithStructField> queryParameters) throws SearchException {
		StructField structField = getStructField(tableMapping, attributeName);

		String usedAttributeName = attributeName;

		int idx = 0;
		while (queryParameters.containsKey(usedAttributeName) && (idx < 100)) {
			usedAttributeName = attributeName + Integer.toString(idx++);
		}

    	queryParameters.put(usedAttributeName, new ValueWithStructField(attribyteValue, structField));
		return new UserVariable(usedAttributeName);
	}

	private Expression buildExistsInArrayExpression(String attributeName, Expression whereExpression) {
		PlainSelect arrayQuery = new PlainSelect();
		String columnAlias = "_" + attributeName;
		arrayQuery.addSelectItems(new SelectExpressionItem(new Column(tableAlias, SpannerOperationService.DOC_ID)));
		arrayQuery.setWhere(whereExpression);

		TableFunction fromTableFunction = new TableFunction();
		fromTableFunction.setAlias(new Alias(columnAlias, false));
		
		net.sf.jsqlparser.expression.Function unnestFunction = new net.sf.jsqlparser.expression.Function();
		unnestFunction.setName("UNNEST");
		unnestFunction.setParameters(new ExpressionList(new Column(tableAlias, attributeName)));

		fromTableFunction.setFunction(unnestFunction);
		arrayQuery.setFromItem(fromTableFunction);

		SubSelect arraySubSelect = new SubSelect();
		arraySubSelect.setSelectBody(arrayQuery);
		arraySubSelect.withUseBrackets(true);
         
		ExistsExpression existsExpression = new ExistsExpression();
		existsExpression.setRightExpression(arraySubSelect);

		return existsExpression;
	}

	private Expression buildExpression(TableMapping tableMapping, Filter genericFilter, boolean multiValued, boolean useExistsInArray, Map<String, PropertyAnnotation> propertiesAnnotationsMap,
			Map<String, ValueWithStructField> queryParameters, Map<String, Join> joinTables, Function<? super Filter, Boolean> processor, boolean skipAlias) throws SearchException {
    	boolean hasSubFilters = ArrayHelper.isNotEmpty(genericFilter.getFilters());

		if (hasSubFilters) {
    		return convertToSqlFilterImpl(tableMapping, genericFilter.getFilters()[0], propertiesAnnotationsMap, queryParameters, joinTables, processor, skipAlias).expression();
		}
		
		String internalAttribute = toInternalAttribute(genericFilter);
		if (multiValued) {
			skipAlias = true;
			if (useExistsInArray) {
				internalAttribute = "_" + internalAttribute;
			} else {
				internalAttribute = internalAttribute + "." + internalAttribute;
			}
		}
		
		return buildColumnExpression(internalAttribute, skipAlias);
	}

	private Expression buildColumnExpression(String attributeName, boolean skipAlias) {
    	if (skipAlias) {
    	    return new Column(attributeName);
    	}

    	return new Column(tableAlias, attributeName);
	}

	private void addJoinTable(TableMapping tableMapping, String attributeName, Map<String, Join> joinTables) {
		if (joinTables.containsKey(attributeName)) {
			return;
		}

		TableMapping childTableMapping = tableMapping.getChildTableMappingForAttribute(attributeName.toLowerCase());
		
		Table childTable = new Table(childTableMapping.getTableName());
		childTable.setAlias(new Alias(attributeName, false));
		
		EqualsTo onExpression = new EqualsTo().
				withLeftExpression(new Column().withTable(tableAlias).withColumnName(SpannerOperationService.DOC_ID)).
				withRightExpression(new Column().withTable(childTable).withColumnName(SpannerOperationService.DOC_ID));

		Join join = new Join();
		join.setRightItem(childTable);
		join.setOnExpression(onExpression);
		
		joinTables.put(attributeName, join);
	}
	
}
