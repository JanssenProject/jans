/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

import org.gluu.persist.annotation.AttributeEnum;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.couchbase.model.ConvertedExpression;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.ldap.impl.LdapFilterConverter;
import org.gluu.persist.reflect.property.PropertyAnnotation;
import org.gluu.persist.reflect.util.ReflectHelper;
import org.gluu.search.filter.Filter;
import org.gluu.search.filter.FilterType;
import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.functions.StringFunctions;

/**
 * Filter to Couchbase expressions convert
 *
 * @author Yuriy Movchan Date: 05/15/2018
 */
@ApplicationScoped
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

                	if ((FilterType.EQUALITY != tmpFilter.getType()) || (tmpFilter.getFilters() != null)) {
                    	canJoinOrFilters = false;
                    	continue;
                    }

                    Boolean isMultiValuedDetected = determineMultiValuedByType(tmpFilter.getAttributeName(), propertiesAnnotationsMap);
                	if (!Boolean.FALSE.equals(isMultiValuedDetected)) {
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
                		for (Filter eqFilter : joinOrFilters) {
                			jsonArrayValues.add(eqFilter.getAssertionValue());
            			}
                        Expression exp = Expression
                                .par(Expression.path(Expression.path(joinOrAttributeName)).in(jsonArrayValues));
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
            if (currentGenericFilter.isMultiValued() || Boolean.TRUE.equals(isMultiValuedDetected)) {
                return ConvertedExpression.build(Expression.path(buildTypedExpression(currentGenericFilter).in(Expression.path(toInternalAttribute(currentGenericFilter)))), requiredConsistency);
            } else if (Boolean.FALSE.equals(isMultiValuedDetected)) {
            	return ConvertedExpression.build(Expression.path(Expression.path(toInternalAttribute(currentGenericFilter))).eq(buildTypedExpression(currentGenericFilter)), requiredConsistency);
            } else if (hasSubFilters && (isMultiValuedDetected == null)) {
        		ConvertedExpression nameConvertedExpression = convertToCouchbaseFilter(currentGenericFilter.getFilters()[0], propertiesAnnotationsMap);
            	return ConvertedExpression.build(nameConvertedExpression.expression().eq(buildTypedExpression(currentGenericFilter)), requiredConsistency);
            } else {
            	Expression nameExpression;
            	if (hasSubFilters) {
            		ConvertedExpression nameConvertedExpression = convertToCouchbaseFilter(currentGenericFilter.getFilters()[0], propertiesAnnotationsMap);
            		nameExpression = nameConvertedExpression.expression();
            	} else {
            		nameExpression = Expression.path(toInternalAttribute(currentGenericFilter));
            	}
                Expression exp1 = Expression
                        .par(Expression.path(nameExpression).eq(buildTypedExpression(currentGenericFilter)));
                Expression exp2 = Expression
                        .par(Expression.path(buildTypedExpression(currentGenericFilter)).in(nameExpression));
                return ConvertedExpression.build(Expression.par(exp1.or(exp2)), requiredConsistency);
            }
        }

        if (FilterType.LESS_OR_EQUAL == type) {
            return ConvertedExpression.build(Expression.path(Expression.path(toInternalAttribute(currentGenericFilter))).lte(buildTypedExpression(currentGenericFilter)), requiredConsistency);
        }

        if (FilterType.GREATER_OR_EQUAL == type) {
            return ConvertedExpression.build(Expression.path(Expression.path(toInternalAttribute(currentGenericFilter))).gte(buildTypedExpression(currentGenericFilter)), requiredConsistency);
        }

        if (FilterType.PRESENCE == type) {
            return ConvertedExpression.build(Expression.path(Expression.path(toInternalAttribute(currentGenericFilter))).isNotMissing(), requiredConsistency);
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
            return ConvertedExpression.build(Expression.path(Expression.path(toInternalAttribute(currentGenericFilter)).like(Expression.s(like.toString()))), requiredConsistency);
        }

        if (FilterType.LOWERCASE == type) {
        	return ConvertedExpression.build(StringFunctions.lower(currentGenericFilter.getAttributeName()), requiredConsistency);
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type));
    }

	private String toInternalAttribute(Filter filter) {
		if (ArrayHelper.isNotEmpty(filter.getFilters())) {
			
			
		}
		if (couchbaseEntryManager == null) {
			return filter.getAttributeName();
		}

		return couchbaseEntryManager.toInternalAttribute(filter.getAttributeName());
	}

	private Expression buildTypedExpression(Filter currentGenericFilter) {
		if (currentGenericFilter.getAssertionValue() instanceof Boolean) {
			return Expression.x((Boolean) currentGenericFilter.getAssertionValue());
		} else if (currentGenericFilter.getAssertionValue() instanceof Integer) {
			return Expression.x((Integer) currentGenericFilter.getAssertionValue());
		} else if (currentGenericFilter.getAssertionValue() instanceof Long) {
			return Expression.x((Long) currentGenericFilter.getAssertionValue());
		}

		return Expression.s(StringHelper.escapeJson(currentGenericFilter.getAssertionValue()));
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
}
