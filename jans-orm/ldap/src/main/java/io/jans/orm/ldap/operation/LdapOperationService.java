/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.operation;

import java.util.Collection;
import java.util.List;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldif.LDIFChangeRecord;

import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapBatchOperationWraper;
import io.jans.orm.ldap.operation.impl.LdapConnectionProvider;
import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.operation.PersistenceOperationService;

public interface LdapOperationService extends PersistenceOperationService {

    static final String DN = "dn";
    static final String UID = "uid";
    static String[] UID_ARRAY = new String[] { "uid" };
    static final String USER_PASSWORD = "userPassword";
    static final String OBJECT_CLASS = "objectClass";

    static final String SUCCESS = "success";

    LdapConnectionProvider getConnectionProvider();

    void setConnectionProvider(LdapConnectionProvider connectionProvider);

    LdapConnectionProvider getBindConnectionProvider();

    void setBindConnectionProvider(LdapConnectionProvider bindConnectionProvider);

    LDAPConnectionPool getConnectionPool();

    LDAPConnection getConnection() throws LDAPException;

    void releaseConnection(LDAPConnection connection);

    <T> PagedResult<EntryData> search(String dn, Filter filter, SearchScope scope, LdapBatchOperationWraper<T> batchOperationWraper, int start,
                            int count, int pageSize, Control[] controls, String... attributes) throws SearchException;

    <T> PagedResult<EntryData> searchPagedEntries(String dn, Filter filter, SearchScope scope, int startIndex,
                                                        int count, int pageSize, String sortBy, SortOrder sortOrder,
                                                        String... attributes) throws Exception;

    /**
     * Lookup entry in the directory
     *
     * @param dn
     * @param attributes
     * @return EntryData
     * @throws ConnectionException
     */
    EntryData lookup(String dn, String... attributes) throws ConnectionException, SearchException;

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
