package org.gluu.persist.ldap.impl;

import org.gluu.persist.exception.operation.SearchException;
import org.gluu.search.filter.Filter;
import org.gluu.search.filter.FilterType;

/**
 * Filter to LDAP filter convert
 *
 * @author Yuriy Movchan Date: 12/15/2017
 */
public class LdapFilterConverter {

    public com.unboundid.ldap.sdk.Filter convertToLdapFilter(Filter genericFilter) throws SearchException {
        FilterType type = genericFilter.getType();
        if (FilterType.RAW == type) {
            try {
                return com.unboundid.ldap.sdk.Filter.create(genericFilter.getFilterString());
            } catch (com.unboundid.ldap.sdk.LDAPException ex) {
                throw new SearchException("Failed to parse RAW Ldap filter", ex, ex.getResultCode().intValue());
            }
        }

        if ((FilterType.NOT == type) || (FilterType.AND == type) || (FilterType.OR == type)) {
            Filter[] genericFilters = genericFilter.getFilters();
            com.unboundid.ldap.sdk.Filter[] ldapFilters = new com.unboundid.ldap.sdk.Filter[genericFilters.length];

            if (genericFilters != null) {
                for (int i = 0; i < genericFilters.length; i++) {
                    ldapFilters[i] = convertToLdapFilter(genericFilters[i]);
                }

                if (FilterType.NOT == type) {
                    return com.unboundid.ldap.sdk.Filter.createNOTFilter(ldapFilters[0]);
                } else if (FilterType.AND == type) {
                    return com.unboundid.ldap.sdk.Filter.createANDFilter(ldapFilters);
                } else if (FilterType.OR == type) {
                    return com.unboundid.ldap.sdk.Filter.createORFilter(ldapFilters);
                }
            }
        }

        if (FilterType.EQUALITY == type) {
            return com.unboundid.ldap.sdk.Filter.createEqualityFilter(genericFilter.getAttributeName(), genericFilter.getAssertionValue());
        }

        if (FilterType.LESS_OR_EQUAL == type) {
            return com.unboundid.ldap.sdk.Filter.createLessOrEqualFilter(genericFilter.getAttributeName(), genericFilter.getAssertionValue());
        }

        if (FilterType.GREATER_OR_EQUAL == type) {
            return com.unboundid.ldap.sdk.Filter.createGreaterOrEqualFilter(genericFilter.getAttributeName(), genericFilter.getAssertionValue());
        }

        if (FilterType.PRESENCE == type) {
            return com.unboundid.ldap.sdk.Filter.createPresenceFilter(genericFilter.getAttributeName());
        }

        if (FilterType.APPROXIMATE_MATCH == type) {
            return com.unboundid.ldap.sdk.Filter.createApproximateMatchFilter(genericFilter.getAttributeName(), genericFilter.getAssertionValue());
        }

        if (FilterType.SUBSTRING == type) {
            return com.unboundid.ldap.sdk.Filter.createSubstringFilter(genericFilter.getAttributeName(), genericFilter.getSubInitial(),
                    genericFilter.getSubAny(), genericFilter.getSubFinal());
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type), com.unboundid.ldap.sdk.ResultCode.PROTOCOL_ERROR_INT_VALUE);
    }

}
