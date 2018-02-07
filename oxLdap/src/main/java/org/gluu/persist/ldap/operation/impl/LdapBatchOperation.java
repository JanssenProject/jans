package org.gluu.persist.ldap.operation.impl;

import static com.unboundid.util.Debug.debugException;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.model.BatchOperation;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;

/**
 * @author eugeniuparvan Date: 12/29/16
 * @author Yuriy Movchan Date: 02/07/2018
 */
public abstract class LdapBatchOperation<T> implements BatchOperation<T> {

	private ASN1OctetString cookie;

	private LDAPConnection ldapConnection;

	private LDAPConnectionPool ldapConnectionPool;

	private boolean moreResultsToReturn;

	public LdapBatchOperation(LdapEntryManager ldapEntryManager) {
		this.ldapConnectionPool = ldapEntryManager.getOperationService().getConnectionPool();
		try {
			this.ldapConnection = ldapConnectionPool.getConnection();
		} catch (LDAPException e) {
			debugException(e);
			this.ldapConnection = null;
		}
	}

	@Override
	public void iterateAllByChunks(int batchSize) {
		if (ldapConnection == null)
			return;
		try {
			List<T> objects;
			while (!CollectionUtils.isEmpty(objects = getChunkOrNull(batchSize))) {
				performAction(objects);
				if (objects.size() < batchSize || !moreResultsToReturn)
					break;
			}
		} finally {
			releaseConnection();
		}
	}

	public boolean collectSearchResult(int size) {
		return true;
	}

	public void processSearchResult(List<T> entries) {
	}

	protected ASN1OctetString getCookie() {
		return cookie;
	}

	protected void setCookie(ASN1OctetString cookie) {
		this.cookie = cookie;
	}

	protected void setMoreResultsToReturn(boolean moreResultsToReturn) {
		this.moreResultsToReturn = moreResultsToReturn;
	}

	protected LDAPConnection getLdapConnection() {
		return ldapConnection;
	}

	protected void releaseConnection() {
		ldapConnectionPool.releaseConnection(ldapConnection);
	}

}
