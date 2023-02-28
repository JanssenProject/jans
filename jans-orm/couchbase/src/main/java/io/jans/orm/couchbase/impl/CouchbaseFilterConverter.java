/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import io.jans.orm.annotation.AttributeEnum;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapFilterConverter;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.orm.couchbase.model.ConvertedExpression;
import io.jans.orm.couchbase.operation.CouchbaseOperationService;

/**
 * Filter to N1QL parameterized Couchbase query converter
 *
 * @author Yuriy Movchan Date: 05/26/2022
 */
public class CouchbaseFilterConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseFilterConverter.class);
    
    private LdapFilterConverter ldapFilterConverter = new LdapFilterConverter();
	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

	private CouchbaseOperationService operationService;

    public CouchbaseFilterConverter(CouchbaseOperationService operationService) {
    	this.operationService = operationService;
	}

	public ConvertedExpression convertToCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
    	return convertToCouchbaseFilter(genericFilter, propertiesAnnotationsMap, null);
    }

    public ConvertedExpression convertToCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
		JsonObject queryParameters = JsonObject.create();
		return convertToCouchbaseFilter(genericFilter, propertiesAnnotationsMap, queryParameters, processor);
	}

    public ConvertedExpression convertToCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, JsonObject queryParameters, Function<? super Filter, Boolean> processor) throws SearchException {
		if (genericFilter == null) {
			return null;
		}

		Filter currentGenericFilter = genericFilter;

        FilterType type = currentGenericFilter.getType();
        if (FilterType.RAW == type) {
        	LOG.warn("RAW Ldap filter to N1QL convertion will be removed in new version!!!");
        	currentGenericFilter = ldapFilterConverter.convertRawLdapFilterToFilter(currentGenericFilter.getFilterString());
        	LOG.debug(String.format("Converted RAW filter: %s", currentGenericFilter));
        	type = currentGenericFilter.getType();
        }

        boolean requiredConsistency = isRequiredConsistency(currentGenericFilter, propertiesAnnotationsMap);

        if (processor != null) {
        	processor.apply(currentGenericFilter);
        }

        if ((FilterType.NOT == type) || (FilterType.AND == type) || (FilterType.OR == type)) {
            Filter[] genericFilters = currentGenericFilter.getFilters();
            ConvertedExpression[] expFilters = new ConvertedExpression[genericFilters.length];

            if (genericFilters != null) {
            	boolean canJoinOrFilters = FilterType.OR == type; // We can replace only multiple OR with IN
            	List<Filter> joinOrFilters = new ArrayList<Filter>();
            	String joinOrAttributeName = null;
                for (int i = 0; i < genericFilters.length; i++) {
                	Filter tmpFilter = genericFilters[i];
                    expFilters[i] = convertToCouchbaseFilter(tmpFilter, propertiesAnnotationsMap, queryParameters, processor);

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
                    return ConvertedExpression.build("NOT ( " + expFilters[0].expression() + " )", queryParameters, expFilters[0].consistency());
                } else if (FilterType.AND == type) {
                    for (int i = 0; i < expFilters.length; i++) {
                        requiredConsistency |= expFilters[i].consistency();
                    }

                    StringBuilder result = new StringBuilder("( ").append(expFilters[0].expression());
                    for (int i = 1; i < expFilters.length; i++) {
                        result = result.append(" AND ").append(expFilters[i].expression());
                    }
                    result.append(" )");

                    return ConvertedExpression.build(result.toString(), queryParameters, requiredConsistency);
                } else if (FilterType.OR == type) {
                    for (int i = 0; i < expFilters.length; i++) {
                        requiredConsistency |= expFilters[i].consistency();
                    }

                    if (canJoinOrFilters) {
                		List<Object> assertionValues = new ArrayList<Object>();
                		for (ConvertedExpression eqFilter : expFilters) {
                			assertionValues.addAll(eqFilter.getSingleLevelParameters());
            			}

                		StringBuilder result = new StringBuilder(buildLeftExpressionPart(genericFilters[0], false, propertiesAnnotationsMap, queryParameters, processor))
                				.append(" IN [ $").append(assertionValues.get(0));
                		
                        for (int i = 1; i < assertionValues.size(); i++) {
                        	Object assertionValue = assertionValues.get(i);
                			result.append(", $").append(assertionValue);
                		}
                		result.append(" ]");
                		
                        return ConvertedExpression.build(result.toString(), queryParameters, requiredConsistency);
                	} else {
                		StringBuilder result = new StringBuilder("( ").append(expFilters[0].expression());
	                    for (int i = 1; i < expFilters.length; i++) {
	                        result = result.append(" OR ").append(expFilters[i].expression());
	                    }
	                    result.append(" )");

	                    return ConvertedExpression.build(result.toString(), queryParameters, requiredConsistency);
                	}
            	}
            }
        }

        // Generic part for rest of expression types
    	Boolean multiValuedWithUnknown = isMultiValue(currentGenericFilter, propertiesAnnotationsMap);
    	boolean multiValued = Boolean.TRUE.equals(multiValuedWithUnknown);

    	String internalAttribute = toInternalAttribute(currentGenericFilter);

    	if ((FilterType.EQUALITY == type) || (FilterType.LESS_OR_EQUAL == type) || (FilterType.GREATER_OR_EQUAL == type)) {
        	String variableExpression = buildVariableExpression(internalAttribute, multiValued, currentGenericFilter.getAssertionValue(), queryParameters);
			String leftExpressionPart = buildLeftExpressionPart(currentGenericFilter, multiValued, propertiesAnnotationsMap, queryParameters, processor);

			if (multiValued) {
    			// ANY mail_ IN mail SATISFIES mail_ = "test@gluu.org" END
    			String result = String.format("ANY %s_ IN %s SATISFIES %s %s $%s END",
    					internalAttribute, internalAttribute, leftExpressionPart, type.getSign(), variableExpression);
    			
        		return ConvertedExpression.build(result, queryParameters, variableExpression, requiredConsistency);
            }
			
			boolean hasSubFilters = ArrayHelper.isNotEmpty(genericFilter.getFilters());
			if (FilterType.EQUALITY == type) {
				if ((Boolean.FALSE.equals(multiValuedWithUnknown)) || (hasSubFilters && (multiValuedWithUnknown == null))) {
		    		String result = String.format("%s %s $%s", leftExpressionPart, type.getSign(), variableExpression);
		    		return ConvertedExpression.build(result, queryParameters, variableExpression, requiredConsistency);
				} else {
	    			// uid == "test" OR "test" in uid
	    			String result = String.format("( ( %s = $%s ) OR ( $%s IN %s ) )",
	    					leftExpressionPart, variableExpression, variableExpression, internalAttribute);
	    			
	        		return ConvertedExpression.build(result, queryParameters, variableExpression, requiredConsistency);
				}
			}

    		String result = String.format("%s %s $%s", leftExpressionPart, type.getSign(), variableExpression);
    		return ConvertedExpression.build(result, queryParameters, variableExpression, requiredConsistency);
        }

        if (FilterType.PRESENCE == type) {
			String leftExpressionPart = buildLeftExpressionPart(currentGenericFilter, multiValued, propertiesAnnotationsMap, queryParameters, processor);

			if (multiValued) {
    			// ANY mail_ IN mail SATISFIES mail_ = "test@gluu.org" END
    			String result = String.format("ANY %s_ IN %s SATISFIES %s IS NOT MISSING END",
    					internalAttribute, internalAttribute, leftExpressionPart);
    			
        		return ConvertedExpression.build(result, queryParameters, requiredConsistency);
            }

    		String result = String.format("%s IS NOT MISSING", leftExpressionPart);
    		return ConvertedExpression.build(result, queryParameters, requiredConsistency);
        }

        if (FilterType.APPROXIMATE_MATCH == type) {
            throw new SearchException("Convertion from APPROXIMATE_MATCH LDAP filter to Couchbase filter is not implemented");
        }

        if (FilterType.SUBSTRING == type) {
			String leftExpressionPart = buildLeftExpressionPart(currentGenericFilter, multiValued, propertiesAnnotationsMap, queryParameters, processor);
			Set<String> filterParameters = new HashSet<String>();

			StringBuilder like = new StringBuilder();
            if (currentGenericFilter.getSubInitial() != null) {
            	String subInital = currentGenericFilter.getSubInitial() + "%";
            	String variableExpressionInitial = buildVariableExpression(internalAttribute + "_i", multiValued, subInital, queryParameters);
            	filterParameters.add(variableExpressionInitial);
                like.append("$" + variableExpressionInitial);
            }

            String[] subAny = currentGenericFilter.getSubAny();
            if ((subAny != null) && (subAny.length > 0)) {
    			StringBuilder anyBuilder = new StringBuilder("%");
                for (String any : subAny) {
                	anyBuilder.append(any);
                	anyBuilder.append("%");
                }
            	String variableExpressionAny = buildVariableExpression(internalAttribute + "_any", multiValued, anyBuilder.toString(), queryParameters);
                like.append("$" + variableExpressionAny);
            	filterParameters.add(variableExpressionAny);
            }

            if (currentGenericFilter.getSubFinal() != null) {
            	String subFinal = "%" + currentGenericFilter.getSubFinal();
            	String variableExpressionFinal = buildVariableExpression(internalAttribute + "_f", multiValued, subFinal, queryParameters);
            	filterParameters.add(variableExpressionFinal);
                like.append("$" + variableExpressionFinal);
            }

			if (multiValued) {
    			// ANY mail_ IN mail SATISFIES mail_ LIKE "%test%" END
    			String result = String.format("ANY %s_ IN %s SATISFIES %s LIKE %s END",
    					internalAttribute, internalAttribute, leftExpressionPart, like.toString());
    			
    			ConvertedExpression convertedExpression = ConvertedExpression.build(result, queryParameters, requiredConsistency);
    			convertedExpression.getSingleLevelParameters().addAll(filterParameters);
    			
    			return convertedExpression;
            }

    		String result = String.format("%s LIKE %s", leftExpressionPart, like.toString());
    		return ConvertedExpression.build(result, queryParameters, requiredConsistency);
        }

        if (FilterType.LOWERCASE == type) {
			String leftExpressionPart = buildLeftExpressionPart(currentGenericFilter, multiValued, propertiesAnnotationsMap, queryParameters, processor);

			String result = String.format("LOWER( %s )", leftExpressionPart);
        	return ConvertedExpression.build(result, queryParameters, requiredConsistency);
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type));
    }

	protected Boolean isMultiValue(Filter currentGenericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		Boolean isMultiValuedDetected = determineMultiValuedByType(currentGenericFilter.getAttributeName(), propertiesAnnotationsMap);
		if (Boolean.TRUE.equals(currentGenericFilter.getMultiValued()) || Boolean.TRUE.equals(isMultiValuedDetected)) {
			return true;
		} else if (Boolean.FALSE.equals(currentGenericFilter.getMultiValued()) || Boolean.FALSE.equals(isMultiValuedDetected)) {
			return false;
		}

		return null;
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

		if (operationService == null) {
			return attributeName;
		}

		return operationService.toInternalAttribute(attributeName);
	}

	private String buildVariableExpression(String attributeName, boolean multiValued, Object assertionValue, JsonObject queryParameters) throws SearchException {
		String usedAttributeName = attributeName;

		int idx = 0;
		while (queryParameters.containsKey(usedAttributeName) && (idx < 100)) {
			usedAttributeName = "_" + attributeName + "_" + Integer.toString(idx++);
		}

		if (multiValued) {
			// TODO: Check if we really not need to special case for multivalued here
			addQueryParameter(assertionValue, queryParameters, usedAttributeName);
		} else {
			addQueryParameter(assertionValue, queryParameters, usedAttributeName);
		}

		return usedAttributeName;
	}

	private void addQueryParameter(Object assertionValue, JsonObject queryParameters, String usedAttributeName) {
		if (assertionValue instanceof AttributeEnum) {
			queryParameters.put(usedAttributeName, ((AttributeEnum) assertionValue).getValue());
		} else if (assertionValue instanceof Boolean) {
			queryParameters.put(usedAttributeName, (Boolean) assertionValue);
		} else if (assertionValue instanceof Integer) {
			queryParameters.put(usedAttributeName, (Integer) assertionValue);
		} else if (assertionValue instanceof Long) {
			queryParameters.put(usedAttributeName, (Long) assertionValue);
		} else if (assertionValue instanceof Long) {
			queryParameters.put(usedAttributeName, (Long) assertionValue);
		} else if (assertionValue instanceof Date) {
			queryParameters.put(usedAttributeName, operationService.encodeTime((Date) assertionValue));
		} else {
			queryParameters.put(usedAttributeName, assertionValue);
		}
	}

	private String buildLeftExpressionPart(Filter genericFilter, boolean multiValued, Map<String, PropertyAnnotation> propertiesAnnotationsMap, JsonObject queryParameters, Function<? super Filter, Boolean> processor) throws SearchException {
		boolean hasSubFilters = ArrayHelper.isNotEmpty(genericFilter.getFilters());
		String internalAttribute = toInternalAttribute(genericFilter);

		String innerExpression = null;
		if (multiValued) {
			if (hasSubFilters) {
	    		Filter subFilter = genericFilter.getFilters()[0].clone();
	    		subFilter.setAttributeName(internalAttribute + "_");
	
	    		innerExpression = convertToCouchbaseFilter(subFilter, propertiesAnnotationsMap, queryParameters, processor).expression();
			} else {
				innerExpression = internalAttribute + "_";
			}
		} else {
			if (hasSubFilters) {
				Filter subFilter = genericFilter.getFilters()[0];
				innerExpression = convertToCouchbaseFilter(subFilter, propertiesAnnotationsMap, queryParameters, processor).expression();
			} else {
				innerExpression = internalAttribute;
			}
		}
		
		return innerExpression;
	}

	private Boolean determineMultiValuedByType(String attributeName, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		if ((attributeName == null) || (propertiesAnnotationsMap == null)) {
			return null;
		}

		if (StringHelper.equalsIgnoreCase(attributeName, CouchbaseEntryManager.OBJECT_CLASS)) {
			return false;
		}

		PropertyAnnotation propertyAnnotation = propertiesAnnotationsMap.get(attributeName);
		if ((propertyAnnotation == null) || (propertyAnnotation.getParameterType() == null)) {
			return null;
		}

		Class<?> parameterType = propertyAnnotation.getParameterType();
		
		boolean isMultiValued = parameterType.equals(String[].class) || ReflectHelper.assignableFrom(parameterType, List.class) || ReflectHelper.assignableFrom(parameterType, AttributeEnum[].class);
		
		return isMultiValued;
	}

	private boolean isRequiredConsistency(Filter filter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		if (propertiesAnnotationsMap == null) {
			return false;
		}

		String attributeName = filter.getAttributeName();
    	PropertyAnnotation propertyAnnotation = propertiesAnnotationsMap.get(attributeName);
		if ((propertyAnnotation == null) || (propertyAnnotation.getParameterType() == null)) {
			return false;
		}
		AttributeName attributeNameAnnotation = (AttributeName) ReflectHelper.getAnnotationByType(propertyAnnotation.getAnnotations(),
				AttributeName.class);
		
		if (attributeNameAnnotation.consistency()) {
			return true;
		}

		return false;
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

}
