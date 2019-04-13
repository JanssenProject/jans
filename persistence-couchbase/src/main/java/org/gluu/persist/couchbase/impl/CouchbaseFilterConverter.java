/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import org.gluu.persist.exception.operation.SearchException;
import org.gluu.search.filter.Filter;
import org.gluu.search.filter.FilterType;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * Filter to Couchbase expressions convert
 *
 * @author Yuriy Movchan Date: 05/15/2018
 */
public class CouchbaseFilterConverter {

    public Expression convertToLdapFilter(Filter genericFilter) throws SearchException {
        FilterType type = genericFilter.getType();
        if (FilterType.RAW == type) {
            throw new SearchException("Convertion from RAW Ldap filter to couchbasefilter is not implemented");
        }

        if ((FilterType.NOT == type) || (FilterType.AND == type) || (FilterType.OR == type)) {
            Filter[] genericFilters = genericFilter.getFilters();
            Expression[] expFilters = new Expression[genericFilters.length];

            if (genericFilters != null) {
                for (int i = 0; i < genericFilters.length; i++) {
                    expFilters[i] = convertToLdapFilter(genericFilters[i]);
                }

                if (FilterType.NOT == type) {
                    return Expression.par(expFilters[0].not());
                } else if (FilterType.AND == type) {
                    Expression result = expFilters[0];
                    for (int i = 1; i < expFilters.length; i++) {
                        result = result.and(expFilters[i]);
                    }
                    return Expression.par(result);
                } else if (FilterType.OR == type) {
                    Expression result = expFilters[0];
                    for (int i = 1; i < expFilters.length; i++) {
                        result = result.or(expFilters[i]);
                    }
                    return Expression.par(result);
                }
            }
        }

        if (FilterType.EQUALITY == type) {
            if (genericFilter.isArrayAttribute()) {
                return Expression.path(Expression.s(genericFilter.getAssertionValue()).in(Expression.path(genericFilter.getAttributeName())));
            } else {
                Expression exp1 = Expression
                        .par(Expression.path(Expression.path(genericFilter.getAttributeName())).eq(Expression.s(genericFilter.getAssertionValue())));
                Expression exp2 = Expression
                        .par(Expression.path(Expression.s(genericFilter.getAssertionValue())).in(Expression.path(genericFilter.getAttributeName())));
                return Expression.par(exp1.or(exp2));
            }
        }

        if (FilterType.LESS_OR_EQUAL == type) {
            return Expression.path(Expression.path(genericFilter.getAttributeName())).lte(Expression.s(genericFilter.getAssertionValue()));
        }

        if (FilterType.GREATER_OR_EQUAL == type) {
            return Expression.path(Expression.path(genericFilter.getAttributeName())).gte(Expression.s(genericFilter.getAssertionValue()));
        }

        if (FilterType.PRESENCE == type) {
            return Expression.path(Expression.path(genericFilter.getAttributeName())).isNotMissing();
        }

        if (FilterType.APPROXIMATE_MATCH == type) {
            throw new SearchException("Convertion from APPROXIMATE_MATCH LDAP filter to Couchbase filter is not implemented");
        }

        if (FilterType.SUBSTRING == type) {
            StringBuilder like = new StringBuilder();
            if (genericFilter.getSubInitial() != null) {
                like.append(genericFilter.getSubInitial());
            }
            like.append("%");

            String[] subAny = genericFilter.getSubAny();
            if ((subAny != null) && (subAny.length > 0)) {
                for (String any : subAny) {
                    like.append(any);
                    like.append("%");
                }
            }

            if (genericFilter.getSubFinal() != null) {
                like.append(genericFilter.getSubFinal());
            }
            return Expression.path(Expression.path(genericFilter.getAttributeName()).like(Expression.s(like.toString())));
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type));
    }

}
