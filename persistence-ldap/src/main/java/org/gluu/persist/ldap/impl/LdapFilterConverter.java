package org.gluu.persist.ldap.impl;

import javax.enterprise.context.ApplicationScoped;

import org.gluu.persist.exception.operation.SearchException;
import org.gluu.search.filter.Filter;
import org.gluu.search.filter.FilterType;

/**
 * Filter to LDAP filter convert
 *
 * @author Yuriy Movchan Date: 12/15/2017
 */
@ApplicationScoped
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

    public Filter convertRawLdapFilterToFilter(String rawFilter) throws SearchException {
    	com.unboundid.ldap.sdk.Filter ldapFilter;
        try {
        	ldapFilter = com.unboundid.ldap.sdk.Filter.create(rawFilter);
        } catch (com.unboundid.ldap.sdk.LDAPException ex) {
            throw new SearchException("Failed to parse RAW Ldap filter", ex, ex.getResultCode().intValue());
        }

        return convertRawLdapFilterToFilterImpl(ldapFilter);
   }

    protected Filter convertRawLdapFilterToFilterImpl(com.unboundid.ldap.sdk.Filter ldapFilter) throws SearchException {
        byte type = ldapFilter.getFilterType();

        if ((com.unboundid.ldap.sdk.Filter.FILTER_TYPE_NOT == type) || (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_AND == type) || (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_OR == type)) {
        	com.unboundid.ldap.sdk.Filter[] ldapFilters = ldapFilter.getComponents();
            Filter[] genericFilters = new Filter[ldapFilters.length];

            if (ldapFilters != null) {
                for (int i = 0; i < ldapFilters.length; i++) {
                	genericFilters[i] = convertRawLdapFilterToFilterImpl(ldapFilters[i]);
                }

                if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_NOT == type) {
                    return Filter.createNOTFilter(genericFilters[0]);
                } else if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_AND == type) {
                    return Filter.createANDFilter(genericFilters);
                } else if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_OR == type) {
                    return Filter.createORFilter(genericFilters);
                }
            }
        }

        if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_EQUALITY == type) {
            return Filter.createEqualityFilter(ldapFilter.getAttributeName(), ldapFilter.getAssertionValue());
        }

        if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_LESS_OR_EQUAL == type) {
            return Filter.createLessOrEqualFilter(ldapFilter.getAttributeName(), ldapFilter.getAssertionValue());
        }

        if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_GREATER_OR_EQUAL == type) {
            return Filter.createGreaterOrEqualFilter(ldapFilter.getAttributeName(), ldapFilter.getAssertionValue());
        }

        if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_PRESENCE == type) {
            return Filter.createPresenceFilter(ldapFilter.getAttributeName());
        }

        if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_APPROXIMATE_MATCH == type) {
            return Filter.createApproximateMatchFilter(ldapFilter.getAttributeName(), ldapFilter.getAssertionValue());
        }

        if (com.unboundid.ldap.sdk.Filter.FILTER_TYPE_SUBSTRING == type) {
            return Filter.createSubstringFilter(ldapFilter.getAttributeName(), ldapFilter.getSubInitialString(),
                    ldapFilter.getSubAnyStrings(), ldapFilter.getSubFinalString());
        }

        throw new SearchException(String.format("Unknown filter type '%s'", type), com.unboundid.ldap.sdk.ResultCode.PROTOCOL_ERROR_INT_VALUE);
    }

}
