/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.impl;

import io.jans.orm.exception.operation.SearchScopeException;
import io.jans.orm.model.SearchScope;

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
