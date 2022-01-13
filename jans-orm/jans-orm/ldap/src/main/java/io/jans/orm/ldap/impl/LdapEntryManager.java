/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.impl;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.EntryDeleteException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.exception.operation.SearchScopeException;
import io.jans.orm.impl.BaseEntryManager;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.orm.ldap.operation.impl.LdapOperationServiceImpl;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.DefaultBatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.util.StaticUtils;

/**
 * LDAP Entry Manager
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class LdapEntryManager extends BaseEntryManager implements Serializable {

    private static final long serialVersionUID = -2544614410981223105L;

    private static final Logger LOG = LoggerFactory.getLogger(LdapEntryManager.class);

    private static final LdapFilterConverter LDAP_FILTER_CONVERTER = new LdapFilterConverter();
    private static final LdapSearchScopeConverter LDAP_SEARCH_SCOPE_CONVERTER = new LdapSearchScopeConverter();

    private List<DeleteNotifier> subscribers;

    public LdapEntryManager() {
    }

    public LdapEntryManager(LdapOperationServiceImpl operationService) {
        this.operationService = operationService;
        this.subscribers = new LinkedList<DeleteNotifier>();
    }

    @Override
    public boolean destroy() {
        if (this.operationService == null) {
            return true;
        }

        return getOperationService().destroy();
    }

    public LdapOperationServiceImpl getOperationService() {
        return (LdapOperationServiceImpl) operationService;
    }

    @Override
    public void addDeleteSubscriber(DeleteNotifier subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeDeleteSubscriber(DeleteNotifier subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public Void merge(Object entry) {
        Class<?> entryClass = entry.getClass();
        checkEntryClass(entryClass, true);
        if (isSchemaEntry(entryClass)) {
            if (getSupportedLDAPVersion() > 2) {
                return merge(entry, true, false, AttributeModificationType.ADD);
            } else {
                throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
            }
        } else {
        	boolean configurationEntry = isConfigurationEntry(entryClass);
            return merge(entry, false, configurationEntry, null);
        }
    }

    @Override
    protected <T> void updateMergeChanges(String baseDn, T entry, boolean isConfigurationUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromLdapMap,
            List<AttributeDataModification> attributeDataModifications, boolean forceUpdate) {
        // Update object classes if entry contains custom object classes
        if (getSupportedLDAPVersion() > 2) {
            if (!isConfigurationUpdate) {
                String[] objectClasses = getObjectClasses(entry, entryClass);
                String[] objectClassesFromLdap = attributesFromLdapMap.get(OBJECT_CLASS.toLowerCase()).getStringValues();

                if (!Arrays.equals(objectClassesFromLdap, objectClasses)) {
                    attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE,
                            new AttributeData(OBJECT_CLASS, objectClasses), new AttributeData(OBJECT_CLASS, objectClassesFromLdap)));
                }
            }
        }
    }

    @Override
    public void remove(Object entry) {
        Class<?> entryClass = entry.getClass();
        checkEntryClass(entryClass, true);
        if (isSchemaEntry(entryClass)) {
            if (getSupportedLDAPVersion() > 2) {
                merge(entry, true, false, AttributeModificationType.REMOVE);
            } else {
                throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
            }
            return;
        }

        Object dnValue = getDNValue(entry, entryClass);

        LOG.debug(String.format("LDAP entry to remove: %s", dnValue.toString()));

        remove(dnValue.toString());
    }

    @Override
    protected void persist(String dn, String[] objectClasses, List<AttributeData> attributes, Integer expiration) {
        List<Attribute> ldapAttributes = new ArrayList<Attribute>(attributes.size());
        for (AttributeData attribute : attributes) {
            String attributeName = attribute.getName();
            String[] attributeValues = attribute.getStringValues();

            if (ArrayHelper.isNotEmpty(attributeValues) && StringHelper.isNotEmpty(attributeValues[0])) {
                if (getOperationService().isCertificateAttribute(attributeName)) {
                    byte[][] binaryValues = toBinaryValues(attributeValues);

                    ldapAttributes.add(new Attribute(attributeName + ";binary", binaryValues));
                } else {
                    ldapAttributes.add(new Attribute(attributeName, attributeValues));
                }
            }
        }

        // Persist entry
        try {
            boolean result = getOperationService().addEntry(dn, ldapAttributes);
            if (!result) {
                throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn));
            }
        } catch (ConnectionException ex) {
            throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn), ex.getCause());
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn), ex);
        }
    }

    @Override
    public void merge(String dn, String[] objectClasses, List<AttributeDataModification> attributeDataModifications, Integer expiration) {
        // Update entry
        try {
            List<Modification> modifications = new ArrayList<Modification>(attributeDataModifications.size());
            for (AttributeDataModification attributeDataModification : attributeDataModifications) {
                AttributeData attribute = attributeDataModification.getAttribute();
                AttributeData oldAttribute = attributeDataModification.getOldAttribute();

                String attributeName = null;
                String[] attributeValues = null;
                if (attribute != null) {
                    attributeName = attribute.getName();
                    attributeValues = attribute.getStringValues();
                }

                String oldAttributeName = null;
                String[] oldAttributeValues = null;
                if (oldAttribute != null) {
                    oldAttributeName = oldAttribute.getName();
                    oldAttributeValues = oldAttribute.getStringValues();
                }

                Modification modification = null;
                if (AttributeModificationType.ADD.equals(attributeDataModification.getModificationType())) {
                    modification = createModification(ModificationType.ADD, attributeName, attributeValues);
                } else {
                    if (AttributeModificationType.REMOVE.equals(attributeDataModification.getModificationType())) {
                        modification = createModification(ModificationType.DELETE, oldAttributeName, oldAttributeValues);
                    } else if (AttributeModificationType.REPLACE.equals(attributeDataModification.getModificationType())) {
                        if (attributeValues.length == 1) {
                            modification = createModification(ModificationType.REPLACE, attributeName, attributeValues);
                        } else {
                            String[] oldValues = ArrayHelper.arrayClone(oldAttributeValues);
                            String[] newValues = ArrayHelper.arrayClone(attributeValues);

                            Arrays.sort(oldValues);
                            Arrays.sort(newValues);

                            boolean[] retainOldValues = new boolean[oldValues.length];
                            Arrays.fill(retainOldValues, false);

                            List<String> addValues = new ArrayList<String>();
                            List<String> removeValues = new ArrayList<String>();

                            // Add new values
                            for (String value : newValues) {
                                int idx = Arrays.binarySearch(oldValues, value, new Comparator<String>() {
                                    @Override
                                    public int compare(String o1, String o2) {
                                        return o1.toLowerCase().compareTo(o2.toLowerCase());
                                    }
                                });
                                if (idx >= 0) {
                                    // Old values array contains new value. Retain
                                    // old value
                                    retainOldValues[idx] = true;
                                } else {
                                    // This is new value
                                    addValues.add(value);
                                }
                            }

                            // Remove values which we don't have in new values
                            for (int i = 0; i < oldValues.length; i++) {
                                if (!retainOldValues[i]) {
                                    removeValues.add(oldValues[i]);
                                }
                            }

                            if (removeValues.size() > 0) {
                                Modification removeModification = createModification(ModificationType.DELETE, attributeName,
                                        removeValues.toArray(new String[removeValues.size()]));
                                modifications.add(removeModification);
                            }

                            if (addValues.size() > 0) {
                                Modification addModification = createModification(ModificationType.ADD, attributeName,
                                        addValues.toArray(new String[addValues.size()]));
                                modifications.add(addModification);
                            }
                        }
                    }
                }

                if (modification != null) {
                    modifications.add(modification);
                }
            }

            if (modifications.size() > 0) {
                boolean result = getOperationService().updateEntry(dn, modifications);
                if (!result) {
                    throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn));
                }
            }
        } catch (ConnectionException ex) {
            throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn), ex.getCause());
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn), ex);
        }
    }

    @Override
    public <T> void removeByDn(String dn, String[] objectClasses) {
        // Remove entry
        try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            getOperationService().delete(dn);
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    @Override
	public <T> int remove(String baseDN, Class<T> entryClass, Filter filter, int count) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        DeleteBatchOperation batchOperation = new DeleteBatchOperation<T>(this);
        SearchResult searchResult = null;
        try {
            LdapBatchOperationWraper<T> batchOperationWraper = new LdapBatchOperationWraper<T>(batchOperation, this, entryClass,
                    propertiesAnnotations);
            searchResult = getOperationService().search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB), batchOperationWraper,
                    0, 100, count, null, LdapOperationService.DN);

        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to delete entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
            throw new EntryDeleteException(String.format("Failed to delete entries with baseDN: %s, filter: %s", baseDN, searchFilter));
        }

        return batchOperation.getCountEntries();
    }

	@Override
    public <T> void removeRecursivelyFromDn(String dn, String[] objectClasses) {
        try {
            if (getOperationService().getConnectionProvider().isSupportsSubtreeDeleteRequestControl()) {
                for (DeleteNotifier subscriber : subscribers) {
                    subscriber.onBeforeRemove(dn);
                }
                getOperationService().deleteRecursively(dn);
                for (DeleteNotifier subscriber : subscribers) {
                    subscriber.onAfterRemove(dn);
                }
            } else {
                removeSubtreeThroughIteration(dn);
            }
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    private void removeSubtreeThroughIteration(String dn) {
    	SearchScope scope = SearchScope.SUB;

    	SearchResult searchResult = null;
        try {
            searchResult = getOperationService().search(dn, toLdapFilter(Filter.createPresenceFilter("objectClass")), toLdapSearchScope(scope), null, 0, 0, 0, null, "dn");
            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find sub-entries of entry '%s' for removal", dn));
            }
        } catch (SearchScopeException ex) {
            throw new AuthenticationException(String.format("Failed to convert scope: %s", scope), ex);
        } catch (SearchException ex) {
            throw new EntryDeleteException(String.format("Failed to find sub-entries of entry '%s' for removal", dn), ex);
        }

        List<String> removeEntriesDn = new ArrayList<String>(searchResult.getEntryCount());
        for (SearchResultEntry searchResultEntry : searchResult.getSearchEntries()) {
            removeEntriesDn.add(searchResultEntry.getDN());
        }

        Collections.sort(removeEntriesDn, LINE_LENGHT_COMPARATOR);

        for (String removeEntryDn : removeEntriesDn) {
            remove(removeEntryDn);
        }
    }

	@Override
    protected List<AttributeData> find(String dn, String[] objectClasses, Map<String, PropertyAnnotation> propertiesAnnotationsMap, String... ldapReturnAttributes) {
        try {
            // Load entry
            SearchResultEntry entry = getOperationService().lookup(dn, ldapReturnAttributes);
            List<AttributeData> result = getAttributeDataList(entry);
            if (result != null) {
                return result;
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn), ex);
        }

        throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn));
    }

    @Override
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        String[] currentLdapReturnAttributes = ldapReturnAttributes;
        if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
            currentLdapReturnAttributes = getAttributes(null, propertiesAnnotations, false);
        }

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }
        SearchResult searchResult = null;
        try {
            LdapBatchOperationWraper<T> batchOperationWraper = new LdapBatchOperationWraper<T>(batchOperation, this, entryClass,
                    propertiesAnnotations);
            searchResult = getOperationService().search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(scope), batchOperationWraper,
                    start, chunkSize, count, null, currentLdapReturnAttributes);
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
        }

        if (searchResult.getEntryCount() == 0) {
            return new ArrayList<T>(0);
        }

        List<T> entries = createEntities(entryClass, propertiesAnnotations,
                searchResult.getSearchEntries().toArray(new SearchResultEntry[searchResult.getSearchEntries().size()]));

        // Default sort if needed
        sortEntriesIfNeeded(entryClass, entries);

        return entries;
    }

    @Override
    public <T> PagedResult<T> findPagedEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
            SortOrder sortOrder, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        String[] currentLdapReturnAttributes = ldapReturnAttributes;
        if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
            currentLdapReturnAttributes = getAttributes(null, propertiesAnnotations, false);
        }

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        List<SearchResultEntry> searchResultEntries;
        PagedResult<T> vlvResponse = new PagedResult<T>();
        try {
            searchResultEntries = getOperationService().searchSearchResultEntryList(baseDN, toLdapFilter(searchFilter),
                    toLdapSearchScope(SearchScope.SUB), start, count, chunkSize, sortBy, sortOrder, vlvResponse, currentLdapReturnAttributes);
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        List<T> entries = new ArrayList<T>(0);
        if (searchResultEntries.size() > 0) {
            entries = createEntitiesVirtualListView(entryClass, propertiesAnnotations, searchResultEntries.toArray(new SearchResultEntry[]{}));
        }
        vlvResponse.setEntries(entries);

        return vlvResponse;

    }

    @Deprecated
    public <T> List<T> findEntriesVirtualListView(String baseDN, Class<T> entryClass, Filter filter, int start, int count, String sortBy,
            SortOrder sortOrder, PagedResult vlvResponse, String[] ldapReturnAttributes) {

        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        String[] currentLdapReturnAttributes = ldapReturnAttributes;
        if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
            currentLdapReturnAttributes = getAttributes(null, propertiesAnnotations, false);
        }

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        SearchResult searchResult = null;
        try {

            searchResult = getOperationService().searchVirtualListView(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB),
                    start, count, sortBy, sortOrder, vlvResponse, currentLdapReturnAttributes);

            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }

        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        if (searchResult.getEntryCount() == 0) {
            return new ArrayList<T>(0);
        }

        List<T> entries = createEntitiesVirtualListView(entryClass, propertiesAnnotations,
                searchResult.getSearchEntries().toArray(new SearchResultEntry[searchResult.getSearchEntries().size()]));

        return entries;
    }

    @Override
    protected <T> boolean contains(String baseDN, String[] objectClasses, Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, Filter filter, String[] ldapReturnAttributes) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to check contain entries is null");
        }

        // Create filter
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        SearchScope scope = SearchScope.SUB;

        SearchResult searchResult = null;
        try {
            searchResult = getOperationService().search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(scope), null, 0, 1, 1, null, ldapReturnAttributes);
            if ((searchResult == null) || !ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (SearchScopeException ex) {
            throw new AuthenticationException(String.format("Failed to convert scope: %s", scope), ex);
        } catch (SearchException ex) {
            if (!(ResultCode.NO_SUCH_OBJECT_INT_VALUE == ex.getErrorCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
            }
        }

        return (searchResult != null) && (searchResult.getEntryCount() > 0);
    }

    protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
            SearchResultEntry... searchResultEntries) {
        List<T> result = new ArrayList<T>(searchResultEntries.length);
        Map<String, List<AttributeData>> entriesAttributes = new HashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {
            count++;
            SearchResultEntry entry = searchResultEntries[i];
            entriesAttributes.put(entry.getDN(), getAttributeDataList(entry));

            // Remove reference to allow java clean up object
            searchResultEntries[i] = null;

            // Allow java to clean up temporary objects
            if (count >= 100) {
                List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
                result.addAll(currentResult);

                entriesAttributes = new HashMap<String, List<AttributeData>>(100);
                count = 0;
            }
        }

        List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
        result.addAll(currentResult);

        return result;
    }

    @Deprecated
    private <T> List<T> createEntitiesVirtualListView(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
            SearchResultEntry... searchResultEntries) {

        List<T> result = new LinkedList<T>();
        Map<String, List<AttributeData>> entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {

            count++;

            SearchResultEntry entry = searchResultEntries[i];

            LinkedList<AttributeData> attributeDataLinkedList = new LinkedList<AttributeData>();
            attributeDataLinkedList.addAll(getAttributeDataList(entry));
            entriesAttributes.put(entry.getDN(), attributeDataLinkedList);

            // Remove reference to allow java clean up object
            searchResultEntries[i] = null;

            // Allow java to clean up temporary objects
            if (count >= 100) {

                List<T> currentResult = new LinkedList<T>();
                currentResult.addAll(createEntities(entryClass, propertiesAnnotations, entriesAttributes, false));
                result.addAll(currentResult);

                entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);
                count = 0;
            }
        }

        List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes, false);
        result.addAll(currentResult);

        return result;
    }

    private List<AttributeData> getAttributeDataList(SearchResultEntry entry) {
        if (entry == null) {
            return null;
        }

        List<AttributeData> result = new ArrayList<AttributeData>();
        for (Attribute attribute : entry.getAttributes()) {
            String[] attributeValueStrings = NO_STRINGS;
            String attributeName = attribute.getName();
            if (LOG.isTraceEnabled()) {
                if (attribute.needsBase64Encoding()) {
                    LOG.trace("Found binary attribute: " + attributeName + ". Is defined in LDAP config: "
                            + getOperationService().isBinaryAttribute(attributeName));
                }
            }

            attributeValueStrings = attribute.getValues();
            if (attribute.needsBase64Encoding()) {
                boolean binaryAttribute = getOperationService().isBinaryAttribute(attributeName);
                boolean certificateAttribute = getOperationService().isCertificateAttribute(attributeName);

                if (binaryAttribute || certificateAttribute) {
                    byte[][] attributeValues = attribute.getValueByteArrays();
                    if (attributeValues != null) {
                        attributeValueStrings = new String[attributeValues.length];
                        for (int i = 0; i < attributeValues.length; i++) {
                            attributeValueStrings[i] = Base64.encodeBase64String(attributeValues[i]);
                            LOG.trace("Binary attribute: " + attribute.getName() + " value (hex): "
                                    + org.apache.commons.codec.binary.Hex.encodeHexString(attributeValues[i]) + " value (base64): "
                                    + attributeValueStrings[i]);
                        }
                    }
                }
                if (certificateAttribute) {
                    attributeName = getOperationService().getCertificateAttributeName(attributeName);
                }
            }

            boolean multiValued = attributeValueStrings.length > 1;
            AttributeData tmpAttribute = new AttributeData(attributeName, attributeValueStrings, multiValued);
            result.add(tmpAttribute);
        }

        return result;
    }

    @Override
    public <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to count entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);

        // Find entries
        Filter searchFilter = Filter.createEqualityFilter(LdapOperationService.UID, userName);
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(searchFilter, objectClasses);
        }

    	SearchScope scope = SearchScope.SUB;
        try {
            SearchResult searchResult = getOperationService().search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(scope), null, 0, 1, 1, null, LdapOperationService.UID_ARRAY);
            if ((searchResult == null) || (searchResult.getEntryCount() != 1)) {
                return false;
            }

            String bindDn = searchResult.getSearchEntries().get(0).getDN();

            return getOperationService().authenticate(bindDn, password, null);
        } catch (ConnectionException ex) {
            throw new AuthenticationException(String.format("Failed to authenticate user: %s", userName), ex);
        } catch (SearchScopeException ex) {
            throw new AuthenticationException(String.format("Failed to convert scope: %s", scope), ex);
        } catch (SearchException ex) {
            throw new AuthenticationException(String.format("Failed to find user DN: %s", userName), ex);
        }
    }

    @Override
    @Deprecated
    public boolean authenticate(String bindDn, String password) {
    	return authenticate(bindDn, null, password);
    }

    @Override
    public <T> boolean authenticate(String bindDn, Class<T> entryClass, String password) {
        try {
            return getOperationService().authenticate(bindDn, password, null);
        } catch (Exception ex) {
            throw new AuthenticationException(String.format("Failed to authenticate DN: %s", bindDn), ex);
        }
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter) {
        return countEntries(baseDN, entryClass, filter, null);
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to count entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        SearchScope searchScope = scope;
        if (searchScope == null) {
            searchScope = SearchScope.SUB;
        }

        String[] ldapReturnAttributes;
        CountBatchOperation<T> batchOperation;
        if (SearchScope.BASE == searchScope) {
            ldapReturnAttributes = new String[] { "numsubordinates" }; // Don't load attributes
            batchOperation = null;
        } else {
            ldapReturnAttributes = new String[] { "" }; // Don't load attributes
            batchOperation = new CountBatchOperation<T>();
        }

        SearchResult searchResult;
        try {
            LdapBatchOperationWraper<T> batchOperationWraper = null;
            if (batchOperation != null) {
                batchOperationWraper = new LdapBatchOperationWraper<T>(batchOperation);
            }
            searchResult = getOperationService().search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(searchScope), batchOperationWraper, 0, 100, 0, null,
                    ldapReturnAttributes);
        } catch (Exception ex) {
            throw new EntryPersistenceException(
                    String.format("Failed to calculate the number of entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        if (SearchScope.BASE != searchScope) {
            return batchOperation.getCountEntries();
        }

        if (searchResult.getEntryCount() != 1) {
            throw new EntryPersistenceException(String.format("Failed to calculate the number of entries due to missing result entry with baseDN: %s, filter: %s", baseDN, searchFilter));
        }

        Long result = searchResult.getSearchEntries().get(0).getAttributeValueAsLong("numsubordinates");
        if (result == null) {
            throw new EntryPersistenceException(String.format("Failed to calculate the number of entries due to missing attribute 'numsubordinates' with baseDN: %s, filter: %s", baseDN, searchFilter));
        }

        return result.intValue();
    }

    @Override
    public String encodeTime(String baseDN, Date date) {
        if (date == null) {
            return null;
        }

        return StaticUtils.encodeGeneralizedTime(date);
    }

    @Override
	protected String encodeTime(Date date) {
		return encodeTime(null, date);
	}

    @Override
    public Date decodeTime(String baseDN, String date) {
        if (date == null) {
            return null;
        }

        try {
            return StaticUtils.decodeGeneralizedTime(date);
        } catch (ParseException ex) {
            LOG.error("Failed to parse generalized time {}", date, ex);
        }

        return null;
    }

    @Override
    protected Date decodeTime(String date) {
		return decodeTime(null, date);
    }

    public boolean loadLdifFileContent(String ldifFileContent) {
        LDAPConnection connection = null;
        try {
            connection = getOperationService().getConnection();
            ResultCode result = LdifDataUtility.instance().importLdifFileContent(connection, ldifFileContent);
            return ResultCode.SUCCESS.equals(result);
        } catch (Exception ex) {
            LOG.error("Failed to load ldif file", ex);
            return false;
        } finally {
            if (connection != null) {
            	getOperationService().releaseConnection(connection);
            }
        }
    }

    @Override
    public List<AttributeData> exportEntry(String dn) {
        try {
        	SearchResultEntry searchResultEntry = getOperationService().lookup(dn, (String[]) null);

            List<AttributeData> result = getAttributeDataList(searchResultEntry);
            if (result != null) {
                return result;
            }
            
            return null;
        } catch (ConnectionException | SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn), ex);
        }
    }

    public int getSupportedLDAPVersion() {
        return getOperationService().getSupportedLDAPVersion();
    }

    private Modification createModification(final ModificationType modificationType, final String attributeName, final String... attributeValues) {
        String realAttributeName = attributeName;
        if (getOperationService().isCertificateAttribute(realAttributeName)) {
            realAttributeName += ";binary";
            byte[][] binaryValues = toBinaryValues(attributeValues);

            return new Modification(modificationType, realAttributeName, binaryValues);
        }

        return new Modification(modificationType, realAttributeName, attributeValues);
    }

    private com.unboundid.ldap.sdk.Filter toLdapFilter(Filter genericFilter) throws SearchException {
        return LDAP_FILTER_CONVERTER.convertToLdapFilter(genericFilter);
    }

    private com.unboundid.ldap.sdk.SearchScope toLdapSearchScope(SearchScope scope) throws SearchScopeException {
        return LDAP_SEARCH_SCOPE_CONVERTER.convertToLdapSearchScope(scope);
    }

	@Override
	public boolean hasBranchesSupport(String dn) {
		return true;
	}

	@Override
	public boolean hasExpirationSupport(String primaryKey) {
		return false;
	}

	@Override
	public String getPersistenceType() {
		return LdapEntryManagerFactory.PERSISTENCE_TYPE;
	}

    @Override
	public String getPersistenceType(String primaryKey) {
		return LdapEntryManagerFactory.PERSISTENCE_TYPE;
	}

	@Override
	public PersistenceEntryManager getPersistenceEntryManager(String persistenceType) {
		if (LdapEntryManagerFactory.PERSISTENCE_TYPE.equals(persistenceType)) {
			return this;
		}
		
		return null;
	}

    @Override
	public String[] getObjectClasses(Object entry, Class<?> entryClass) {
		String[] ojectClasses = super.getObjectClasses(entry, entryClass);

		Set<String> objecClassSet = new HashSet<String>();

		// Add in LDAP implementation "top" by default
		objecClassSet.add("top");
		objecClassSet.addAll(Arrays.asList(ojectClasses));
		return objecClassSet.toArray(new String[0]);
	}

    @Override
	protected Object getNativeDateAttributeValue(Date dateValue) {
		return encodeTime(dateValue);
    }

	private static class CountBatchOperation<T> extends DefaultBatchOperation<T> {

        private int countEntries = 0;

        @Override
        public void performAction(List<T> entries) {
        }

        @Override
        public boolean collectSearchResult(int size) {
            countEntries += size;
            return false;
        }

        public int getCountEntries() {
            return countEntries;
        }
    }

	private static final class DeleteBatchOperation<T> extends DefaultBatchOperation<T> {

		private int countEntries = 0;
		private LdapEntryManager ldapEntryManager;

        public DeleteBatchOperation(LdapEntryManager ldapEntryManager) {
        	this.ldapEntryManager = ldapEntryManager;
		}

        @Override
		public void performAction(List<T> entries) {
			for (T entity : entries) {
				try {
					String dnValue = ldapEntryManager.getDNValue(entity).toString();
					if (ldapEntryManager.hasBranchesSupport(dnValue)) {
						ldapEntryManager.removeRecursively(dnValue);
					} else {
						ldapEntryManager.remove(dnValue);
					}
					LOG.trace("Removed {}", dnValue);
				} catch (Exception e) {
					LOG.error("Failed to remove entry, entity: " + entity, e);
				}
			}
		}

        @Override
        public boolean collectSearchResult(int size) {
            countEntries += size;
            return false;
        }

        public int getCountEntries() {
            return countEntries;
        }
	}

}
