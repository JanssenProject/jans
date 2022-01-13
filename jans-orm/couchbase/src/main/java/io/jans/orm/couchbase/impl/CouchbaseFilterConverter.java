/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.functions.Collections;
import com.couchbase.client.java.query.dsl.functions.Collections.SatisfiesBuilder;
import com.couchbase.client.java.query.dsl.functions.StringFunctions;

import io.jans.orm.annotation.AttributeEnum;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.couchbase.model.ConvertedExpression;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapFilterConverter;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.Pair;
import io.jans.orm.util.StringHelper;

/**
 * Filter to Couchbase expressions convert
 *
 * @author Yuriy Movchan Date: 05/15/2018
 */
public class CouchbaseFilterConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseFilterConverter.class);
    
    private LdapFilterConverter ldapFilterConverter = new LdapFilterConverter();

	private CouchbaseEntryManager couchbaseEntryManager;

    public CouchbaseFilterConverter(CouchbaseEntryManager couchbaseEntryManager) {
    	this.couchbaseEntryManager = couchbaseEntryManager;
	}

	public ConvertedExpression convertToCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
    	return convertToCouchbaseFilter(genericFilter, propertiesAnnotationsMap, null);
    }

    public ConvertedExpression convertToCouchbaseFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
        Filter currentGenericFilter = genericFilter;

        FilterType type = currentGenericFilter.getType();
        if (FilterType.RAW == type) {
        	LOG.warn("RAW Ldap filter to Couchbase convertion will be removed in new version!!!");
        	currentGenericFilter = ldapFilterConverter.convertRawLdapFilterToFilter(currentGenericFilter.getFilterString());
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
                    expFilters[i] = convertToCouchbaseFilter(tmpFilter, propertiesAnnotationsMap, processor);

                    // Check if we can replace OR with IN
                	if (!canJoinOrFilters) {
                		continue;
                	}
                	
                	if (tmpFilter.getMultiValued() != null) {
                		canJoinOrFilters = false;
                    	continue;
                	}

                	if ((FilterType.EQUALITY != tmpFilter.getType()) || (tmpFilter.getFilters() != null)) {
                    	canJoinOrFilters = false;
                    	continue;
                    }

                    Boolean isMultiValuedDetected = determineMultiValuedByType(tmpFilter.getAttributeName(), propertiesAnnotationsMap);
                	if (!Boolean.FALSE.equals(isMultiValuedDetected) && !Boolean.FALSE.equals(currentGenericFilter.getMultiValued())) {
            			canJoinOrFilters = false;
            			continue;
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
                    return ConvertedExpression.build(Expression.par(expFilters[0].expression().not()), expFilters[0].consistency());
                } else if (FilterType.AND == type) {
                    for (int i = 0; i < expFilters.length; i++) {
                        requiredConsistency |= expFilters[i].consistency();
                    }

                    Expression result = expFilters[0].expression();
                    for (int i = 1; i < expFilters.length; i++) {
                        result = result.and(expFilters[i].expression());
                    }
                    return ConvertedExpression.build(Expression.par(result), requiredConsistency);
                } else if (FilterType.OR == type) {
                    for (int i = 0; i < expFilters.length; i++) {
                        requiredConsistency |= expFilters[i].consistency();
                    }

                    if (canJoinOrFilters) {
                		JsonArray jsonArrayValues = JsonArray.create();
                    	Filter lastEqFilter = null;
                		for (Filter eqFilter : joinOrFilters) {
                			lastEqFilter = eqFilter;

                			jsonArrayValues.add(eqFilter.getAssertionValue());
            			}

                		Expression exp = Expression
                                .par(buildPath(lastEqFilter, propertiesAnnotationsMap, processor).getFirst().in(jsonArrayValues));
                        return ConvertedExpression.build(exp, requiredConsistency);
                	} else {
	                    Expression result = expFilters[0].expression();
	                    for (int i = 1; i < expFilters.length; i++) {
	                        result = result.or(expFilters[i].expression());
	                    }

	                    return ConvertedExpression.build(Expression.par(result), requiredConsistency);
                	}
            	}
            }
        }

        if (FilterType.EQUALITY == type) {
        	boolean hasSubFilters = ArrayHelper.isNotEmpty(currentGenericFilter.getFilters());
        	Boolean isMultiValuedDetected = determineMultiValuedByType(currentGenericFilter.getAttributeName(), propertiesAnnotationsMap);

        	String internalAttribute = toInternalAttribute(currentGenericFilter);
			Pair<Expression, Expression> pairExpression = buildPath(currentGenericFilter, propertiesAnnotationsMap, processor);
    		if (Boolean.TRUE.equals(currentGenericFilter.getMultiValued()) || Boolean.TRUE.equals(isMultiValuedDetected)) {
            	return ConvertedExpression.build(
            			Collections.anyIn(internalAttribute + "_", pairExpression.getFirst()).
            			satisfies(pairExpression.getSecond().eq(buildTypedExpression(currentGenericFilter))),
            			requiredConsistency);
            } else if (Boolean.FALSE.equals(currentGenericFilter.getMultiValued()) || Boolean.FALSE.equals(isMultiValuedDetected) ||
            			(hasSubFilters && (isMultiValuedDetected == null))) {
            	return ConvertedExpression.build(pairExpression.getSecond().eq(buildTypedExpression(currentGenericFilter)), requiredConsistency);
            } else {
            	Expression nameExpression = pairExpression.getSecond();
                Expression exp1 = Expression
                        .par(Expression.path(nameExpression).eq(buildTypedExpression(currentGenericFilter)));
                Expression exp2 = Expression
                        .par(Expression.path(buildTypedExpression(currentGenericFilter)).in(nameExpression));
                return ConvertedExpression.build(Expression.par(exp1.or(exp2)), requiredConsistency);
            }
        }

        if (FilterType.LESS_OR_EQUAL == type) {
        	String internalAttribute = toInternalAttribute(currentGenericFilter);
			Pair<Expression, Expression> pairExpression = buildPath(currentGenericFilter, propertiesAnnotationsMap, processor);
            if (isMultiValue(currentGenericFilter, propertiesAnnotationsMap)) {
            	return ConvertedExpression.build(
            			Collections.anyIn(internalAttribute + "_", pairExpression.getFirst()).
            			satisfies(pairExpression.getSecond().lte(buildTypedExpression(currentGenericFilter))),
            			requiredConsistency);
            } else {
            	return ConvertedExpression.build(pairExpression.getSecond().lte(buildTypedExpression(currentGenericFilter)), requiredConsistency);
            }
        }

        if (FilterType.GREATER_OR_EQUAL == type) {
        	String internalAttribute = toInternalAttribute(currentGenericFilter);
			Pair<Expression, Expression> pairExpression = buildPath(currentGenericFilter, propertiesAnnotationsMap, processor);
            if (isMultiValue(currentGenericFilter, propertiesAnnotationsMap)) {
            	return ConvertedExpression.build(
            			Collections.anyIn(internalAttribute + "_", pairExpression.getFirst()).
            			satisfies(pairExpression.getSecond().gte(buildTypedExpression(currentGenericFilter))),
            			requiredConsistency);
            } else {
            	return ConvertedExpression.build(pairExpression.getSecond().gte(buildTypedExpression(currentGenericFilter)), requiredConsistency);
            }
        }

        if (FilterType.PRESENCE == type) {
        	String internalAttribute = toInternalAttribute(currentGenericFilter);
			Pair<Expression, Expression> pairExpression = buildPath(currentGenericFilter, propertiesAnnotationsMap, processor);
            if (isMultiValue(currentGenericFilter, propertiesAnnotationsMap)) {
            	return ConvertedExpression.build(
            			Collections.anyIn(internalAttribute + "_", pairExpression.getFirst()).
            			satisfies(pairExpression.getSecond().isNotMissing()),
            			requiredConsistency);
            } else {
            	return ConvertedExpression.build(pairExpression.getSecond().isNotMissing(), requiredConsistency);
            }
        }

        if (FilterType.APPROXIMATE_MATCH == type) {
            throw new SearchException("Convertion from APPROXIMATE_MATCH LDAP filter to Couchbase filter is not implemented");
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
        	String internalAttribute = toInternalAttribute(currentGenericFilter);
			Pair<Expression, Expression> pairExpression = buildPath(currentGenericFilter, propertiesAnnotationsMap, processor);
            if (isMultiValue(currentGenericFilter, propertiesAnnotationsMap)) {
            	return ConvertedExpression.build(
            			Collections.anyIn(internalAttribute + "_", pairExpression.getFirst()).
            			satisfies(pairExpression.getSecond().like(Expression.s(escapeValue(like.toString())))),
            			requiredConsistency);
            } else {
            	return ConvertedExpression.build(pairExpression.getSecond().like(Expression.s(escapeValue(like.toString()))), requiredConsistency);
            }
        }

        if (FilterType.LOWERCASE == type) {
        	return ConvertedExpression.build(StringFunctions.lower(currentGenericFilter.getAttributeName()), requiredConsistency);
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type));
    }

	protected Boolean isMultiValue(Filter currentGenericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		Boolean isMultiValuedDetected = determineMultiValuedByType(currentGenericFilter.getAttributeName(), propertiesAnnotationsMap);
		if (Boolean.TRUE.equals(currentGenericFilter.getMultiValued()) || Boolean.TRUE.equals(isMultiValuedDetected)) {
			return true;
		}

		return false;
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
		if (couchbaseEntryManager == null) {
			return attributeName;
		}

		return couchbaseEntryManager.toInternalAttribute(attributeName);
	}

	private Expression buildTypedExpression(Filter currentGenericFilter) {
		if (currentGenericFilter.getAssertionValue() instanceof Boolean) {
			return Expression.x((Boolean) currentGenericFilter.getAssertionValue());
		} else if (currentGenericFilter.getAssertionValue() instanceof Integer) {
			return Expression.x((Integer) currentGenericFilter.getAssertionValue());
		} else if (currentGenericFilter.getAssertionValue() instanceof Long) {
			return Expression.x((Long) currentGenericFilter.getAssertionValue());
		}

		return Expression.s(escapeValue(currentGenericFilter.getAssertionValue()));
	}

	private Pair<Expression, Expression> buildPath(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
		boolean hasSubFilters = ArrayHelper.isNotEmpty(genericFilter.getFilters());
		boolean isMultiValue = isMultiValue(genericFilter, propertiesAnnotationsMap);
		String internalAttribute = toInternalAttribute(genericFilter);

		Expression expression = Expression.path(Expression.path(internalAttribute));
		Expression innerExpression = null;
		if (isMultiValue) {
			expression = Expression.path(Expression.path(internalAttribute));
			innerExpression = null;
			if (hasSubFilters) {
	    		Filter clonedFilter = genericFilter.getFilters()[0].clone();
	    		clonedFilter.setAttributeName(internalAttribute + "_");
	
	    		innerExpression = convertToCouchbaseFilter(clonedFilter, propertiesAnnotationsMap, processor).expression();
			} else {
				innerExpression = Expression.path(Expression.path(internalAttribute + "_"));
			}
		} else {
			if (hasSubFilters) {
				innerExpression = convertToCouchbaseFilter(genericFilter.getFilters()[0], propertiesAnnotationsMap, processor).expression();
			} else {
				innerExpression = Expression.path(Expression.path(internalAttribute));
			}
		}
		
		return new Pair<>(expression, innerExpression);
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

	public static String escapeValue(Object str) {
		String result = StringHelper.escapeJson(str);
		
		// Workaround for Couchbase 6.6
		result = result.replace("\\\\", "\\u005c");
		
		return result;
	}

}
