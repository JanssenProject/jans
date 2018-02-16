package org.gluu.persist.ldap.impl;

import org.gluu.persist.exception.operation.SearchScopeException;
import org.gluu.persist.model.SearchScope;

/**
 * Simple filter without dependency to specific persistence filter mechanism
 *
 * @author Yuriy Movchan Date: 12/15/2017
 */
public class LdapSearchScopeConverter {

    public com.unboundid.ldap.sdk.SearchScope convertToLdapSearchScope(SearchScope searchScope) throws SearchScopeException {
        if (SearchScope.BASE == searchScope) {
            return com.unboundid.ldap.sdk.SearchScope.BASE;
        }
        if (SearchScope.ONE == searchScope) {
            return com.unboundid.ldap.sdk.SearchScope.ONE;
        }
        if (SearchScope.SUB == searchScope) {
            return com.unboundid.ldap.sdk.SearchScope.SUB;
        }

        throw new SearchScopeException(String.format("Unknown search scope '%s'", searchScope));
    }

}
