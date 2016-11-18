/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.ldap.model;

/**
 * LDAP search scope
 * 
 * @author Yuriy Movchan Date: 11/18/2016
 */
public enum SearchScope {

	/**
	 * A predefined baseObject scope value, which indicates that only the entry
	 * specified by the base DN should be considered.
	 */
	BASE(com.unboundid.ldap.sdk.SearchScope.BASE),

	/**
	 * A predefined singleLevel scope value, which indicates that only entries
	 * that are immediate subordinates of the entry specified by the base DN
	 * (but not the base entry itself) should be considered.
	 */
	ONE(com.unboundid.ldap.sdk.SearchScope.ONE),

	/**
	 * A predefined wholeSubtree scope value, which indicates that the base
	 * entry itself and any subordinate entries (to any depth) should be
	 * considered.
	 */
	SUB(com.unboundid.ldap.sdk.SearchScope.SUB);

	private com.unboundid.ldap.sdk.SearchScope ldapSearchScope;

	SearchScope(com.unboundid.ldap.sdk.SearchScope ldapSearchScope) {
		this.ldapSearchScope = ldapSearchScope;

	}

	public com.unboundid.ldap.sdk.SearchScope getLdapSearchScope() {
		return ldapSearchScope;
	}

}
