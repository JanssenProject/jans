package org.gluu.persist.ldap.operation;

import java.util.Collection;
import java.util.List;

import org.gluu.persist.exception.operation.ConnectionException;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.ldap.impl.LdapBatchOperationWraper;
import org.gluu.persist.ldap.operation.impl.LdapConnectionProvider;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.operation.PersistenceOperationService;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldif.LDIFChangeRecord;

public interface LdapOperationService extends PersistenceOperationService {

    LdapConnectionProvider getConnectionProvider();

    void setConnectionProvider(LdapConnectionProvider connectionProvider);

    LdapConnectionProvider getBindConnectionProvider();

    void setBindConnectionProvider(LdapConnectionProvider bindConnectionProvider);

    LDAPConnectionPool getConnectionPool();

    LDAPConnection getConnection() throws LDAPException;

    void releaseConnection(LDAPConnection connection);

    boolean authenticate(String bindDn, String password) throws ConnectionException;

    <T> SearchResult search(String dn, Filter filter, SearchScope scope, LdapBatchOperationWraper<T> batchOperationWraper, int start,
            int searchLimit, int count, Control[] controls, String... attributes) throws SearchException;

    List<SearchResultEntry> searchSearchResultEntryList(String dn, Filter filter, SearchScope scope, int startIndex,
                                                        int count, int pageSize, String sortBy, SortOrder sortOrder,
                                                        PagedResult vlvResponse, String... attributes) throws Exception;

    SearchResult searchVirtualListView(String dn, Filter filter, SearchScope scope, int start, int count,
            String sortBy, SortOrder sortOrder, PagedResult vlvResponse, String... attributes)
            throws Exception;

    /**
     * Lookup entry in the directory
     *
     * @param dn
     * @param attributes
     * @return SearchResultEntry
     * @throws ConnectionException
     */
    SearchResultEntry lookup(String dn, String... attributes) throws ConnectionException;

    /**
     * Use this method to add new entry
     *
     * @param dn
     *            for entry
     * @param atts
     *            attributes for entry
     * @return true if successfully added
     * @throws DuplicateEntryException
     * @throws ConnectionException
     * @throws DuplicateEntryException
     * @throws ConnectionException
     * @throws LDAPException
     */
    boolean addEntry(String dn, Collection<Attribute> atts) throws DuplicateEntryException, ConnectionException;

    /**
     * This method is used to update set of attributes for an entry
     *
     * @param dn
     * @param modifications
     * @return
     * @throws ConnectionException
     * @throws DuplicateEntryException
     */
    boolean updateEntry(String dn, List<Modification> modifications) throws DuplicateEntryException, ConnectionException;

    /**
     * Delete entry from the directory
     *
     * @param dn
     * @throws ConnectionException
     */
    boolean delete(String dn) throws ConnectionException;

    /**
     * Delete entry from the directory
     *
     * @param dn
     * @return 
     * @throws ConnectionException
     */
    boolean deleteRecursively(String dn) throws ConnectionException;

    boolean processChange(LDIFChangeRecord ldifRecord) throws LDAPException;

    int getSupportedLDAPVersion();

    String getSubschemaSubentry();

    boolean destroy();

    boolean isBinaryAttribute(String attributeName);

    boolean isCertificateAttribute(String attributeName);

    String getCertificateAttributeName(String attributeName);

    <T> List<T> sortListByAttributes(List<T> searchResultEntries, Class<T> cls, boolean caseSensitive, boolean ascending, String... sortByAttributes);

}
