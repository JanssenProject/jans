package org.gluu.site.ldap.persistence;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

import static com.unboundid.util.Debug.debugException;

/**
 * Created by eugeniuparvan on 12/29/16.
 */
public abstract class BatchOperation<T> {

    private ASN1OctetString cookie;

    private LDAPConnection ldapConnection;

    private LDAPConnectionPool ldapConnectionPool;

    private boolean moreResultsToReturn;

    protected abstract List<T> getChunkOrNull(int batchSize);

    protected abstract void performAction(List<T> objects);

    public BatchOperation(LdapEntryManager ldapEntryManager) {
        this.ldapConnectionPool = ldapEntryManager.getLdapOperationService().getConnectionPool();
        try {
            this.ldapConnection = ldapConnectionPool.getConnection();
        } catch (LDAPException e) {
            debugException(e);
            this.ldapConnection = null;
        }
    }

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
        }finally {
            ldapConnectionPool.releaseConnection(ldapConnection);
        }
    }

    public ASN1OctetString getCookie() {
        return cookie;
    }

    public void setCookie(ASN1OctetString cookie) {
        this.cookie = cookie;
    }

    public void setMoreResultsToReturn(boolean moreResultsToReturn) {
        this.moreResultsToReturn = moreResultsToReturn;
    }

    public LDAPConnection getLdapConnection() {
        return ldapConnection;
    }
}
