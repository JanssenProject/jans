/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.operation.impl;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.jans.orm.ldap.exception.InvalidSimplePageControlException;
import io.jans.orm.ldap.impl.LdapBatchOperationWraper;
import io.jans.orm.ldap.operation.watch.OperationDurationUtil;
import org.apache.commons.lang.StringUtils;

import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.extension.PersistenceExtension;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.operation.auth.PasswordEncryptionHelper;
import io.jans.orm.operation.auth.PasswordEncryptionMethod;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.Pair;
import io.jans.orm.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldif.LDIFChangeRecord;

/**
 * OperationsFacade is the base class that performs all the ldap operations
 * using connectionpool
 *
 * @author Pankaj
 * @author Yuriy Movchan
 */
public class LdapOperationServiceImpl implements LdapOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(LdapOperationServiceImpl.class);

    private LdapConnectionProvider connectionProvider;
    private LdapConnectionProvider bindConnectionProvider;

	private PersistenceExtension persistenceExtension;

    private static Map<String, Class<?>> ATTRIBUTE_DATA_TYPES = new HashMap<String, Class<?>>();
    private static List<String> OBJECT_CLASSES = new ArrayList<String>();
    private static final Map<String, Class<?>> OID_SYNTAX_CLASS_MAPPING;

    static {
        //Populates the mapping of syntaxes that will support comparison of attribute values.
        //Only accounting for the most common and existing in Jans Schema
        OID_SYNTAX_CLASS_MAPPING = new HashMap<String, Class<?>>();
        //See RFC4517, section 3.3
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.7", Boolean.class);
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.11", String.class);   //Country String
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.15", String.class);   //Directory String
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.12", String.class);   //DN
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.22", String.class);   //Facsimile
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.24", Date.class);     //Generalized Time
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.26", String.class);   //IA5 String (used in email)
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.27", Integer.class);
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.36", String.class);   //Numeric string
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.41", String.class);   //Postal address
        OID_SYNTAX_CLASS_MAPPING.put("1.3.6.1.4.1.1466.115.121.1.50", String.class);   //Telephone number
    }

    @SuppressWarnings("unused")
    private LdapOperationServiceImpl() {
    }

    public LdapOperationServiceImpl(LdapConnectionProvider connectionProvider) {
        this(connectionProvider, null);
        populateAttributeDataTypesMapping(getSubschemaSubentry());
    }

    public LdapOperationServiceImpl(LdapConnectionProvider connectionProvider, LdapConnectionProvider bindConnectionProvider) {
        this.connectionProvider = connectionProvider;
        this.bindConnectionProvider = bindConnectionProvider;
        populateAttributeDataTypesMapping(getSubschemaSubentry());
    }

    @Override
    public LdapConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    @Override
    public void setConnectionProvider(LdapConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public LdapConnectionProvider getBindConnectionProvider() {
        return bindConnectionProvider;
    }

    @Override
    public void setBindConnectionProvider(LdapConnectionProvider bindConnectionProvider) {
        this.bindConnectionProvider = bindConnectionProvider;
    }

    @Override
    public LDAPConnectionPool getConnectionPool() {
        return connectionProvider.getConnectionPool();
    }

    @Override
    public LDAPConnection getConnection() throws LDAPException {
        return connectionProvider.getConnection();
    }

    @Override
    public void releaseConnection(LDAPConnection connection) {
        connectionProvider.releaseConnection(connection);
    }

    @Override
	public boolean authenticate(String bindDn, String password, String objectClass) throws ConnectionException, SearchException, AuthenticationException {
        try {
            return authenticateImpl(bindDn, password);
        } catch (LDAPException ex) {
            throw new ConnectionException("Failed to authenticate dn", ex);
        }
    }

    private boolean authenticateImpl(final String bindDn, final String password) throws LDAPException, ConnectionException, SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        boolean result = false;

        // Try to authenticate if the password was encrypted with additional mechanism
        List<PasswordEncryptionMethod> additionalPasswordMethods = this.connectionProvider.getAdditionalPasswordMethods();
        if ((persistenceExtension != null) || !additionalPasswordMethods.isEmpty()) {
            SearchResultEntry searchResult = lookup(bindDn, USER_PASSWORD);
            if (searchResult == null) {
                throw new ConnectionException("Failed to find use by dn");
            }

			String userPassword = searchResult.getAttribute(USER_PASSWORD).getValue();
	        if (userPassword != null) {
				if (persistenceExtension != null) {
					result = persistenceExtension.compareHashedPasswords(password, userPassword);
				} else {
					PasswordEncryptionMethod storedPasswordMethod = PasswordEncryptionHelper.findAlgorithm(userPassword);
					if (additionalPasswordMethods.contains(storedPasswordMethod)) {
						LOG.debug("Authenticating '{}' using internal authentication mechanism '{}'", bindDn, storedPasswordMethod);
						result = PasswordEncryptionHelper.compareCredentials(password, userPassword);
					}
				}
	        }
        } else {
	        if (this.bindConnectionProvider == null) {
	            result = authenticateConnectionPoolImpl(bindDn, password);
	        } else {
	            result = authenticateBindConnectionPoolImpl(bindDn, password);
	        }
        }

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: bind, duration: {}, dn: {}", duration, bindDn);

        return result;
    }

    private boolean authenticateConnectionPoolImpl(final String bindDn, final String password) throws LDAPException, ConnectionException {
        boolean loggedIn = false;

        if (bindDn == null) {
            return loggedIn;
        }

        boolean closeConnection = false;
        LDAPConnection connection = connectionProvider.getConnection();
        try {
            closeConnection = true;
            BindResult r = connection.bind(bindDn, password);
            if (r.getResultCode() == ResultCode.SUCCESS) {
                loggedIn = true;
            }
        } finally {
            connectionProvider.releaseConnection(connection);
            // We can't use connection which binded as ordinary user
            if (closeConnection) {
                connectionProvider.closeDefunctConnection(connection);
            }
        }

        return loggedIn;
    }

    private boolean authenticateBindConnectionPoolImpl(final String bindDn, final String password) throws LDAPException, ConnectionException {
        if (bindDn == null) {
            return false;
        }

        LDAPConnection connection = bindConnectionProvider.getConnection();
        try {
            BindResult r = connection.bind(bindDn, password);
            return r.getResultCode() == ResultCode.SUCCESS;
        } finally {
            bindConnectionProvider.releaseConnection(connection);
        }
    }

    @Override
    public <T> SearchResult search(String dn, Filter filter, SearchScope scope, LdapBatchOperationWraper<T> batchOperationWraper, int start,
                                   int searchLimit, int count, Control[] controls, String... attributes) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        SearchResult result = searchImpl(dn, filter, scope, batchOperationWraper, start, searchLimit, count, controls, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: search, duration: {}, dn: {}, filter: {}, scope: {}, batchOperationWraper: {}, start: {}, searchLimit: {}, count: {}, controls: {}, attributes: {}", duration, dn, filter, scope, batchOperationWraper, start, searchLimit, count, controls, attributes);

        return result;
    }

    private <T> SearchResult searchImpl(String dn, Filter filter, SearchScope scope, LdapBatchOperationWraper<T> batchOperationWraper, int start,
            int searchLimit, int count, Control[] controls, String... attributes) throws SearchException {
        SearchRequest searchRequest;

        BatchOperation<T> ldapBatchOperation = null;
        if (batchOperationWraper != null) {
            ldapBatchOperation = (BatchOperation<T>) batchOperationWraper.getBatchOperation();
        }

        if (LOG.isTraceEnabled()) {
            // Find whole tree search. This can be very slow
            if (StringHelper.equalsIgnoreCase(dn, "o=jans")) {
                LOG.trace("Search in whole LDAP tree", new Exception());
            }
        }

        if (attributes == null) {
            searchRequest = new SearchRequest(dn, scope, filter);
        } else {
            searchRequest = new SearchRequest(dn, scope, filter, attributes);
        }

        boolean useSizeLimit = count > 0;

        if (useSizeLimit) {
            // Use paged result to limit search
            searchLimit = count;
        }

        SearchResult searchResult = null;
        List<SearchResult> searchResultList = new ArrayList<SearchResult>();
        List<SearchResultEntry> searchResultEntries = new ArrayList<SearchResultEntry>();
        List<SearchResultReference> searchResultReferences = new ArrayList<SearchResultReference>();

        if ((searchLimit > 0) || (start > 0)) {
            if (searchLimit == 0) {
                // Default page size
                searchLimit = 100;
            }

            boolean collectSearchResult;

            LDAPConnection ldapConnection = null;
            try {
                ldapConnection = getConnectionPool().getConnection();
                ASN1OctetString cookie = null;
                SimplePagedResponse simplePagedResponse = null;
                if (start > 0) {
                    try {
                    	simplePagedResponse = scrollSimplePagedResultsControl(ldapConnection, dn, filter, scope, controls, start); 
                        cookie = simplePagedResponse.getCookie();
                    } catch (InvalidSimplePageControlException ex) {
                        throw new LDAPSearchException(ex.getResultCode(), "Failed to scroll to specified start", ex);
                    } catch (LDAPException ex) {
                        throw new LDAPSearchException(ex.getResultCode(), "Failed to scroll to specified start", ex);
                    }
                }
                
                if ((cookie != null) && (cookie.getValueLength() == 0)) {
                    SearchResult searchResultTemp = simplePagedResponse.getLastSearchResult();
                    return new SearchResult(searchResultTemp.getMessageID(), searchResultTemp.getResultCode(), searchResultTemp.getDiagnosticMessage(),
                            searchResultTemp.getMatchedDN(), searchResultTemp.getReferralURLs(), searchResultEntries, searchResultReferences,
                            searchResultEntries.size(), searchResultReferences.size(), searchResultTemp.getResponseControls());
                }

                do {
                    collectSearchResult = true;
                    searchRequest.setControls(new Control[] {new SimplePagedResultsControl(searchLimit, cookie)});
                    setControls(searchRequest, controls);
                    searchResult = ldapConnection.search(searchRequest);

                    if (ldapBatchOperation != null) {
                        collectSearchResult = ldapBatchOperation.collectSearchResult(searchResult.getEntryCount());
                    }
                    if (collectSearchResult) {
                        searchResultList.add(searchResult);
                        searchResultEntries.addAll(searchResult.getSearchEntries());
                        searchResultReferences.addAll(searchResult.getSearchReferences());
                    }

                    if (ldapBatchOperation != null) {
                        List<T> entries = batchOperationWraper.createEntities(searchResult);
                        ldapBatchOperation.performAction(entries);
                    }
                    cookie = null;
                    try {
                        SimplePagedResultsControl c = SimplePagedResultsControl.get(searchResult);
                        if (c != null) {
                            cookie = c.getCookie();
                        }
                    } catch (LDAPException ex) {
                        LOG.error("Error while accessing cookies" + ex.getMessage());
                    }

                    if (useSizeLimit) {
                        break;
                    }
                } while ((cookie != null) && (cookie.getValueLength() > 0));
            } catch (LDAPException ex) {
                throw new SearchException("Failed to scroll to specified start", ex, ex.getResultCode().intValue());
            } finally {
                if (ldapConnection != null) {
                    getConnectionPool().releaseConnection(ldapConnection);
                }
            }

            if (!collectSearchResult) {
                return new SearchResult(searchResult.getMessageID(), searchResult.getResultCode(), searchResult.getDiagnosticMessage(),
                        searchResult.getMatchedDN(), searchResult.getReferralURLs(), searchResultEntries, searchResultReferences,
                        searchResultEntries.size(), searchResultReferences.size(), searchResult.getResponseControls());
            }

            if (!searchResultList.isEmpty()) {
                SearchResult searchResultTemp = searchResultList.get(0);
                return new SearchResult(searchResultTemp.getMessageID(), searchResultTemp.getResultCode(), searchResultTemp.getDiagnosticMessage(),
                        searchResultTemp.getMatchedDN(), searchResultTemp.getReferralURLs(), searchResultEntries, searchResultReferences,
                        searchResultEntries.size(), searchResultReferences.size(), searchResultTemp.getResponseControls());
            }
        } else {
            setControls(searchRequest, controls);
            try {
                searchResult = getConnectionPool().search(searchRequest);
            } catch (LDAPSearchException ex) {
                throw new SearchException(ex.getMessage(), ex, ex.getResultCode().intValue());
            }
        }

        return searchResult;
    }

    private SimplePagedResponse scrollSimplePagedResultsControl(LDAPConnection ldapConnection, String dn, Filter filter, SearchScope scope,
            Control[] controls, int start) throws LDAPException, InvalidSimplePageControlException {
        SearchRequest searchRequest = new SearchRequest(dn, scope, filter, "dn");

        int currentStartIndex = start;
        ASN1OctetString cookie = null;
        SearchResult searchResult = null;
        do {
            int pageSize = Math.min(currentStartIndex, 100);
            searchRequest.setControls(new Control[] {new SimplePagedResultsControl(pageSize, cookie, true)});
            setControls(searchRequest, controls);
            searchResult = ldapConnection.search(searchRequest);

            currentStartIndex -= searchResult.getEntryCount();
            try {
                SimplePagedResultsControl c = SimplePagedResultsControl.get(searchResult);
                if (c != null) {
                    cookie = c.getCookie();
                }
            } catch (LDAPException ex) {
                LOG.error("Error while accessing cookie", ex);
                throw new InvalidSimplePageControlException(ex.getResultCode(), "Error while accessing cookie");
            }
        } while ((cookie != null) && (cookie.getValueLength() > 0) && (currentStartIndex > 0));

        return new SimplePagedResponse(cookie, searchResult);
    }

    @Override
    public List<SearchResultEntry> searchSearchResultEntryList(String dn, Filter filter, SearchScope scope, int startIndex,
                                                               int count, int pageSize, String sortBy, SortOrder sortOrder,
                                                               PagedResult vlvResponse, String... attributes) throws Exception {
        Instant startTime = OperationDurationUtil.instance().now();
        
        List<SearchResultEntry> result = searchSearchResultEntryListImpl(dn, filter, scope, startIndex, count, pageSize, sortBy, sortOrder, vlvResponse, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: search_result_list, duration: {}, dn: {}, filter: {}, scope: {}, startIndex: {}, count: {}, pageSize: {}, sortBy: {}, sortOrder: {}, vlvResponse: {}, attributes: {}", duration, dn, filter, scope, startIndex, count, pageSize, sortBy, sortOrder, vlvResponse, attributes);

        return result;
    }

    private List<SearchResultEntry> searchSearchResultEntryListImpl(String dn, Filter filter, SearchScope scope, int start, int count,
            int pageSize, String sortBy, SortOrder sortOrder, PagedResult vlvResponse, String... attributes) throws LDAPException, Exception {
        //This method does not assume that count <= pageSize as occurs in SCIM, but it's more general

        //Why this?
        if (StringHelper.equalsIgnoreCase(dn, "o=jans")) {
            (new Exception()).printStackTrace();
        }

        List<SearchResultEntry> searchEntries;
        ASN1OctetString resumeCookie = null;
        LDAPConnection conn = getConnection();
        SearchRequest searchRequest = new SearchRequest(dn, scope, filter, attributes);

        int totalResults = 0;

        do {
            //Keep searching while we reach start index...
            SearchResult searchResult = nextSearchResult(conn, searchRequest, pageSize, resumeCookie);
            searchEntries = searchResult.getSearchEntries();
            totalResults += searchEntries.size();

            resumeCookie = getSearchResultCookie(searchResult);
        } while (totalResults < start && resumeCookie != null);

        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();

        if (totalResults > start) {
            //Take the interesting ones, ie. skip [0, start) interval
            int lowerBound = searchEntries.size() - (totalResults - start);
            int upperBound = Math.min(searchEntries.size(), lowerBound + count);
            searchResultEntryList.addAll(searchEntries.subList(lowerBound, upperBound));
        }

        //Continue adding results till reaching count if needed
        while (resumeCookie != null && totalResults < count + start) {
            SearchResult searchResult = nextSearchResult(conn, searchRequest, pageSize, resumeCookie);
            searchEntries = searchResult.getSearchEntries();
            searchResultEntryList.addAll(searchEntries);
            totalResults += searchEntries.size();

            resumeCookie = getSearchResultCookie(searchResult);
        }

        if (totalResults > count + start) {
            //Remove the uninteresting tail
            searchResultEntryList = searchResultEntryList.subList(0, count);
        }

        //skip the rest and update the number of total results only
        while (resumeCookie != null) {
            SearchResult searchResult = nextSearchResult(conn, searchRequest, pageSize, resumeCookie);
            searchEntries = searchResult.getSearchEntries();
            totalResults += searchEntries.size();

            resumeCookie = getSearchResultCookie(searchResult);
        }

        if (StringUtils.isNotEmpty(sortBy)) {
            boolean ascending = sortOrder == null || sortOrder.equals(SortOrder.ASCENDING);
            searchResultEntryList = sortListByAttributes(searchResultEntryList, SearchResultEntry.class, false, ascending, sortBy);
        }

        // Get results info
        vlvResponse.setEntriesCount(searchResultEntryList.size());
        vlvResponse.setTotalEntriesCount(totalResults);
        vlvResponse.setStart(start);

        releaseConnection(conn);
        return searchResultEntryList;
    }

    private ASN1OctetString getSearchResultCookie(SearchResult searchResult) throws Exception {
        SimplePagedResultsControl responseControl = SimplePagedResultsControl.get(searchResult);
        return responseControl.moreResultsToReturn() ? responseControl.getCookie() : null;
    }

    private SearchResult nextSearchResult(LDAPConnection connection, SearchRequest searchRequest, int pageSize,
                                          ASN1OctetString resumeCookie) throws Exception {

        searchRequest.setControls(new SimplePagedResultsControl(pageSize, resumeCookie));
        SearchResult result = connection.search(searchRequest);

        if (!ResultCode.SUCCESS.equals(result.getResultCode())) {
            String msgErr = "Search operation returned: " + result.getResultCode();
            LOG.error(msgErr);
            throw new Exception(msgErr);
        }
        return result;

    }

    @Deprecated
    public SearchResult searchVirtualListView(String dn, Filter filter, SearchScope scope, int start, int count, String sortBy,
            SortOrder sortOrder, PagedResult vlvResponse, String... attributes) throws Exception {
        Instant startTime = OperationDurationUtil.instance().now();

        SearchResult result = searchVirtualListViewImpl(dn, filter, scope, start, count, sortBy, sortOrder, vlvResponse, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: search_virtual_list_view, duration: {}, dn: {}, filter: {}, scope: {}, start: {}, count: {}, sortBy: {}, sortOrder: {}, vlvResponse: {}, attributes: {}", duration, dn, filter, scope, start, count, sortBy, sortOrder, vlvResponse, attributes);

        return result;
    }

    private SearchResult searchVirtualListViewImpl(String dn, Filter filter, SearchScope scope, int start, int count, String sortBy,
            SortOrder sortOrder, PagedResult vlvResponse, String... attributes) throws LDAPSearchException, LDAPException {
        if (StringHelper.equalsIgnoreCase(dn, "o=jans")) {
            (new Exception()).printStackTrace();
        }

        SearchRequest searchRequest;

        if (attributes == null) {
            searchRequest = new SearchRequest(dn, scope, filter);
        } else {
            searchRequest = new SearchRequest(dn, scope, filter, attributes);
        }

        // start and count should be "cleansed" before arriving here
        int targetOffset = start;
        int beforeCount = 0;
        int afterCount = (count > 0) ? (count - 1) : 0;
        int contentCount = 0;

        boolean reverseOrder = false;
        if (sortOrder != null) {
            reverseOrder = sortOrder.equals(SortOrder.DESCENDING) ? true : false;
        }

        // Note that the VLV control always requires the server-side sort control.
        searchRequest.setControls(new ServerSideSortRequestControl(new SortKey(sortBy, reverseOrder)),
                new VirtualListViewRequestControl(targetOffset, beforeCount, afterCount, contentCount, null));

        SearchResult searchResult = getConnectionPool().search(searchRequest);

        /*
         * for (SearchResultEntry searchResultEntry : searchResult.getSearchEntries()) {
         * log.info("##### searchResultEntry = " + searchResultEntry.toString()); }
         */

        // LDAPTestUtils.assertHasControl(searchResult,
        // VirtualListViewResponseControl.VIRTUAL_LIST_VIEW_RESPONSE_OID);

        VirtualListViewResponseControl vlvResponseControl = VirtualListViewResponseControl.get(searchResult);

        // Get results info
        vlvResponse.setEntriesCount(searchResult.getEntryCount());
        vlvResponse.setTotalEntriesCount(vlvResponseControl.getContentCount());
        vlvResponse.setStart(vlvResponseControl.getTargetPosition());

        return searchResult;
    }

    private void setControls(SearchRequest searchRequest, Control... controls) {
        if (!ArrayHelper.isEmpty(controls)) {
            Control[] newControls;
            if (ArrayHelper.isEmpty(searchRequest.getControls())) {
                newControls = controls;
            } else {
                newControls = ArrayHelper.arrayMerge(searchRequest.getControls(), controls);
            }

            searchRequest.setControls(newControls);
        }
    }

    @Override
    public SearchResultEntry lookup(String dn, String... attributes) throws ConnectionException, SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        SearchResultEntry result = lookupImpl(dn, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: lookup, duration: {}, dn: {}, attributes: {}", duration, dn, attributes);

        return result;
    }

    private SearchResultEntry lookupImpl(String dn, String... attributes) throws SearchException {
        try {
        	SearchResultEntry result;
            if (attributes == null) {
            	result = getConnectionPool().getEntry(dn);
            } else {
            	result = getConnectionPool().getEntry(dn, attributes);
            }

            if (result != null) {
            	return result;
            }
        } catch (Exception ex) {
            throw new ConnectionException("Failed to lookup entry", ex);
        }

        throw new SearchException(String.format("Failed to lookup entry by DN: '%s'", dn));
    }

    @Override
    public boolean addEntry(String dn, Collection<Attribute> attributes) throws DuplicateEntryException, ConnectionException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        boolean result = addEntryImpl(dn, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: add, duration: {}, dn: {}, attributes: {}", duration, dn, attributes);
        
        return result;
    }

    private boolean addEntryImpl(String dn, Collection<Attribute> attributes) throws DuplicateEntryException {
    	if (this.persistenceExtension != null) {
    		updateUserPasswordAttribute(attributes);
    	}

    	try {
            LDAPResult result = getConnectionPool().add(dn, attributes);
            if (result.getResultCode().getName().equalsIgnoreCase(SUCCESS)) {
                return true;
            }
        } catch (final LDAPException ex) {
            int errorCode = ex.getResultCode().intValue();
            if (errorCode == ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE) {
                throw new DuplicateEntryException();
            }
            if (errorCode == ResultCode.INSUFFICIENT_ACCESS_RIGHTS_INT_VALUE) {
                throw new ConnectionException("LDAP config error: insufficient access rights.", ex);
            }
            if (errorCode == ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE) {
                throw new ConnectionException("LDAP Error: time limit exceeded", ex);
            }
            if (errorCode == ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE) {
                throw new ConnectionException("LDAP config error: schema violation contact LDAP admin.", ex);
            }

            throw new ConnectionException("Error adding entry to directory. LDAP error number " + errorCode, ex);
        }

        return false;
    }

    @Deprecated
    protected boolean updateEntry(String dn, Collection<Attribute> attrs) throws DuplicateEntryException, ConnectionException {
        List<Modification> mods = new ArrayList<Modification>();

        for (Attribute attribute : attrs) {
            String attributeName = attribute.getName();
            String attributeValue = attribute.getValue();
            if (attributeName.equalsIgnoreCase(OBJECT_CLASS)
                    || attributeName.equalsIgnoreCase(DN)
                    || attributeName.equalsIgnoreCase(USER_PASSWORD)) {
                continue;
            } else {
                if (attributeValue != null) {
                    mods.add(new Modification(ModificationType.REPLACE, attributeName, attributeValue));
                }
            }
        }

        return updateEntry(dn, mods);
    }

    @Override
    public boolean updateEntry(String dn, List<Modification> modifications) throws DuplicateEntryException, ConnectionException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        boolean result = updateEntryImpl(dn, modifications);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: modify, duration: {}, dn: {}, modifications: {}", duration, dn, modifications);

        return result;
    }

    private boolean updateEntryImpl(String dn, List<Modification> modifications) throws DuplicateEntryException {
    	if (this.persistenceExtension != null) {
    		updateUserPasswordModification(modifications);
    	}

    	ModifyRequest modifyRequest = new ModifyRequest(dn, modifications);
        return modifyEntry(modifyRequest);
    }

    /**
     * Use this method to add / replace / delete attribute from entry
     *
     * @param modifyRequest
     * @return true if modification is successful
     * @throws DuplicateEntryException
     * @throws ConnectionException
     */
    protected boolean modifyEntry(ModifyRequest modifyRequest) throws DuplicateEntryException, ConnectionException {
        LDAPResult modifyResult = null;
        try {
            modifyResult = getConnectionPool().modify(modifyRequest);
            return ResultCode.SUCCESS.equals(modifyResult.getResultCode());
        } catch (final LDAPException ex) {
            int errorCode = ex.getResultCode().intValue();
            if (errorCode == ResultCode.INSUFFICIENT_ACCESS_RIGHTS_INT_VALUE) {
                throw new ConnectionException("LDAP config error: insufficient access rights.", ex);
            }
            if (errorCode == ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE) {
                throw new ConnectionException("LDAP Error: time limit exceeded", ex);
            }
            if (errorCode == ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE) {
                throw new ConnectionException("LDAP config error: schema violation contact LDAP admin.", ex);
            }

            throw new ConnectionException("Error updating entry in directory. LDAP error number " + errorCode, ex);
        }
    }

    @Override
    public boolean delete(String dn) throws ConnectionException {
        Instant startTime = OperationDurationUtil.instance().now();

        boolean result = deleteImpl(dn);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: delete, duration: {}, dn: {}", duration, dn);

        return result;
    }

    private boolean deleteImpl(String dn) {
        try {
            LDAPResult result = getConnectionPool().delete(dn);
            
            return ResultCode.SUCCESS.equals(result.getResultCode());
        } catch (Exception ex) {
            throw new ConnectionException("Failed to delete entry", ex);
        }
    }

    @Override
    public boolean deleteRecursively(String dn) throws ConnectionException {
        Instant startTime = OperationDurationUtil.instance().now();

        boolean result = deleteRecursivelyImpl(dn);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("LDAP operation: delete_tree, duration: {}, dn: {}", duration, dn);

        return result;
    }

    protected boolean deleteRecursivelyImpl(String dn) {
        try {
            final DeleteRequest deleteRequest = new DeleteRequest(dn);
            deleteRequest.addControl(new SubtreeDeleteRequestControl());
            LDAPResult result = getConnectionPool().delete(deleteRequest);

            return ResultCode.SUCCESS.equals(result.getResultCode());
        } catch (Exception ex) {
            throw new ConnectionException("Failed to delete entry", ex);
        }
    }

    @Override
    public boolean processChange(LDIFChangeRecord ldifRecord) throws LDAPException {
        LDAPConnection connection = getConnection();
        try {
            LDAPResult ldapResult = ldifRecord.processChange(connection);

            return ResultCode.SUCCESS.equals(ldapResult.getResultCode());
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public int getSupportedLDAPVersion() {
        return this.connectionProvider.getSupportedLDAPVersion();
    }

    @Override
    public String getSubschemaSubentry() {
        return this.connectionProvider.getSubschemaSubentry();
    }

    @Override
    public boolean destroy() {
        boolean result = true;

        if (connectionProvider != null) {
            try {
                connectionProvider.closeConnectionPool();
            } catch (Exception ex) {
                LOG.error("Failed to close connection pool correctly");
                result = false;
            }
        }

        if (bindConnectionProvider != null) {
            try {
                bindConnectionProvider.closeConnectionPool();
            } catch (Exception ex) {
                LOG.error("Failed to close bind connection pool correctly");
                result = false;
            }
        }

        return result;
    }

    @Override
    public boolean isBinaryAttribute(String attributeName) {
        return this.connectionProvider.isBinaryAttribute(attributeName);
    }

    @Override
    public boolean isCertificateAttribute(String attributeName) {
        return this.connectionProvider.isCertificateAttribute(attributeName);
    }

    @Override
    public String getCertificateAttributeName(String attributeName) {
        return this.connectionProvider.getCertificateAttributeName(attributeName);
    }

    private void updateUserPasswordAttribute(Collection<Attribute> attributes) {
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
			Attribute attribute = (Attribute) it.next();
		    if (StringHelper.equalsIgnoreCase(LdapOperationService.USER_PASSWORD, attribute.getName())) {
		    	it.remove();
		    	Attribute newAttribute = new Attribute(attribute.getName(),
		    			createStoragePassword(attribute.getValues()));
		    	attributes.add(newAttribute);
		    	break;
		    }
		}
    }

	private void updateUserPasswordModification(List<Modification> modifications) {
		for (Iterator<Modification> it = modifications.iterator(); it.hasNext();) {
			Modification modification = (Modification) it.next();
		    if (StringHelper.equalsIgnoreCase(LdapOperationService.USER_PASSWORD, modification.getAttributeName())) {
		    	it.remove();
		    	Modification newModification = new Modification(modification.getModificationType(),
		    			modification.getAttributeName(),
		    			createStoragePassword(modification.getValues()));
		    	modifications.add(newModification);
		    	break;
		    }
		}
	}

    public String[] createStoragePassword(String[] passwords) {
        if (ArrayHelper.isEmpty(passwords)) {
            return passwords;
        }

        String[] results = new String[passwords.length];
        for (int i = 0; i < passwords.length; i++) {
			if (persistenceExtension != null) {
				results[i] = persistenceExtension.createHashedPassword(passwords[i]);
			}
        }

        return results;
    }

    @Override
    public <T> List<T> sortListByAttributes(List<T> searchResultEntries, Class<T> cls, boolean caseSensitive,
                                            boolean ascending, String... sortByAttributes) {
        // Check input parameters
        if (searchResultEntries == null) {
            throw new MappingException("Entries list to sort is null");
        }

        if (searchResultEntries.size() == 0) {
            return searchResultEntries;
        }

        SearchResultEntryComparator<T> comparator = new SearchResultEntryComparator<T>(sortByAttributes, caseSensitive, ascending);

        //The following line does not work because of type erasure
        //T array[]=(T[])searchResultEntries.toArray();

        //Converting the list to an array gets rid of unmodifiable list problem, see issue #68
        T[] dummyArr = (T[]) java.lang.reflect.Array.newInstance(cls, 0);
        T[] array = searchResultEntries.toArray(dummyArr);
        Arrays.sort(array, comparator);
        return Arrays.asList(array);

    }

    private void populateAttributeDataTypesMapping(String schemaEntryDn) {

        try {
            if (ATTRIBUTE_DATA_TYPES.size() == 0) {
                //schemaEntryDn="ou=schema";
                SearchResultEntry entry = lookup(schemaEntryDn, "attributeTypes");
                Attribute attrAttributeTypes = entry.getAttribute("attributeTypes");

                Map<String, Pair<String, String>> tmpMap = new HashMap<String, Pair<String, String>>();

                for (String strAttributeType : attrAttributeTypes.getValues()) {
                    AttributeTypeDefinition attrTypeDef = new AttributeTypeDefinition(strAttributeType);
                    String[] names = attrTypeDef.getNames();

                    if (names != null) {
                        for (String name : names) {
                            tmpMap.put(name, new Pair<String, String>(attrTypeDef.getBaseSyntaxOID(), attrTypeDef.getSuperiorType()));
                        }
                    }
                }

                //Fill missing values
                for (String name : tmpMap.keySet()) {
                    Pair<String, String> currPair = tmpMap.get(name);
                    String sup = currPair.getSecond();

                    if (currPair.getFirst() == null && sup != null) {     //No OID syntax?
                        //Try to lookup superior type
                        Pair<String, String> pair = tmpMap.get(sup);
                        if (pair != null) {
                            currPair.setFirst(pair.getFirst());
                        }
                    }
                }

                //Populate map of attribute names vs. Java classes
                for (String name : tmpMap.keySet()) {
                    String syntaxOID = tmpMap.get(name).getFirst();

                    if (syntaxOID != null) {
                        Class<?> cls = OID_SYNTAX_CLASS_MAPPING.get(syntaxOID);
                        if (cls != null) {
                            ATTRIBUTE_DATA_TYPES.put(name, cls);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static final class SearchResultEntryComparator<T> implements Comparator<T>, Serializable {

        private static final long serialVersionUID = 574848841116711467L;
        private String[] sortByAttributes;
        private boolean caseSensitive;
        private boolean ascending;

        private SearchResultEntryComparator(String[] sortByAttributes, boolean caseSensitive, boolean ascending) {
            this.sortByAttributes = sortByAttributes;
            this.caseSensitive = caseSensitive;
            this.ascending = ascending;
        }

        public int compare(T entry1, T entry2) {

            int result = 0;

            if (entry1 == null) {
                if (entry2 == null) {
                    result = 0;
                } else {
                    result = -1;
                }
            } else {
                if (entry2 == null) {
                    result = 1;
                } else {
                    for (String currSortByAttribute : sortByAttributes) {
                        result = compare(entry1, entry2, currSortByAttribute);
                        if (result != 0) {
                            break;
                        }
                    }
                }
            }

            if (!ascending) {
                result *= -1;
            }

            return result;

        }

        //This comparison assumes a default sort order of "ascending"
        public int compare(T entry1, T entry2, String attributeName) {

            int result = 0;
            try {

                if (entry1 instanceof SearchResultEntry) {

                    SearchResultEntry resultEntry1 = (SearchResultEntry) entry1;
                    SearchResultEntry resultEntry2 = (SearchResultEntry) entry2;

                    //Obtain a string representation first and do nulls treatments
                    String value1 = resultEntry1.getAttributeValue(attributeName);
                    String value2 = resultEntry2.getAttributeValue(attributeName);

                    if (value1 == null) {
                        if (value2 == null) {
                            result = 0;
                        } else {
                            result = -1;
                        }
                    } else {
                        if (value2 == null) {
                            result = 1;
                        } else {
                            Class<?> cls = ATTRIBUTE_DATA_TYPES.get(attributeName);

                            if (cls != null) {
                                if (cls.equals(String.class)) {
                                    if (caseSensitive) {
                                        result = value1.compareTo(value2);
                                    } else {
                                        result = value1.toLowerCase().compareTo(value2.toLowerCase());
                                    }
                                } else
                                if (cls.equals(Integer.class)) {
                                    result = resultEntry1.getAttributeValueAsInteger(attributeName)
                                            .compareTo(resultEntry2.getAttributeValueAsInteger(attributeName));
                                } else
                                if (cls.equals(Boolean.class)) {
                                    result = resultEntry1.getAttributeValueAsBoolean(attributeName)
                                            .compareTo(resultEntry2.getAttributeValueAsBoolean(attributeName));
                                } else
                                if (cls.equals(Date.class)) {
                                    result = resultEntry1.getAttributeValueAsDate(attributeName)
                                            .compareTo(resultEntry2.getAttributeValueAsDate(attributeName));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error occurred when comparing entries with SearchResultEntryComparator");
                LOG.error(e.getMessage(), e);
            }
            return result;

        }

    }

    @Override
    public boolean isConnected() {
        return connectionProvider.isConnected();
    }

	@Override
	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		this.persistenceExtension = persistenceExtension;
	}

	@Override
	public boolean isSupportObjectClass(String objectClass) {
		return true;
	}

    private class SimplePagedResponse {

		private ASN1OctetString cookie;
		private SearchResult lastSearchResult;

		public SimplePagedResponse(ASN1OctetString cookie, SearchResult lastSearchResult) {
			this.cookie = cookie;
			this.lastSearchResult = lastSearchResult;
		}

		public ASN1OctetString getCookie() {
			return cookie;
		}

		public SearchResult getLastSearchResult() {
			return lastSearchResult;
		}
    }

}

