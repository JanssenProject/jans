/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.io.Serializable;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.couchbase.operation.impl.CouchbaseOperationsServiceImpl;
import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.exception.mapping.EntryPersistenceException;
import org.gluu.persist.exception.mapping.MappingException;
import org.gluu.persist.exception.operation.AuthenticationException;
import org.gluu.persist.exception.operation.ConnectionException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.exception.operation.SearchScopeException;
import org.gluu.persist.impl.BaseEntryManager;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.AttributeDataModification;
import org.gluu.persist.model.AttributeDataModification.AttributeModificationType;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.DefaultBatchOperation;
import org.gluu.persist.model.ListViewResponse;
import org.gluu.persist.model.PropertyAnnotation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.ArrayHelper;
import org.xdi.util.StringHelper;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * Couchbase Entry Manager
 *
 * @author Yuriy Movchan Date: 05/14/2018
 */
public class CouchbaseEntryManager extends BaseEntryManager implements Serializable {

    @Override
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int startIndex, int sizeLimit, int chunkSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> ListViewResponse<T> findListViewResponse(String baseDN, Class<T> entryClass, Filter filter, int startIndex, int count, int chunkSize,
            String sortBy, SortOrder sortOrder, String[] ldapReturnAttributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean authenticate(String bindDn, String password) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean authenticate(String userName, String password, String baseDN) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean destroy() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addDeleteSubscriber(DeleteNotifier subscriber) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeDeleteSubscriber(DeleteNotifier subscriber) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String[] getLDIF(String dn) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String[]> getLDIF(String dn, String[] attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String[]> getLDIFTree(String baseDN, Filter searchFilter, String... attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void persist(String dn, List<AttributeData> attributes) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> T merge(T entry) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected <T> void updateMergeChanges(T entry, boolean isSchemaUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromLdapMap,
            List<AttributeDataModification> attributeDataModifications) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void merge(String dn, List<AttributeDataModification> attributeDataModifications) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove(Object entry) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void remove(String dn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeRecursively(String dn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean contains(String baseDN, Filter filter, String[] objectClasses, String[] ldapReturnAttributes) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected List<AttributeData> find(String dn, String... attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String encodeGeneralizedTime(Date date) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date decodeGeneralizedTime(String date) {
        // TODO Auto-generated method stub
        return null;
    }
    protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
            JsonObject ... searchResultEntries) {
        return null;
    }
/*
    private static final long serialVersionUID = -2554615610981223105L;

    private static final Logger log = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private static final CouchbaseFilterConverter FILTER_CONVERTER = new CouchbaseFilterConverter();
    private static final CouchbaseSearchScopeConverter SEARCH_SCOPE_CONVERTER = new CouchbaseSearchScopeConverter();

    private transient CouchbaseOperationsServiceImpl operationService;
    private transient List<DeleteNotifier> subscribers;

    public CouchbaseEntryManager() {
    }

    protected CouchbaseEntryManager(CouchbaseOperationsServiceImpl operationService) {
        this.operationService = operationService;
        subscribers = new LinkedList<DeleteNotifier>();
    }

    @Override
    public boolean destroy() {
        boolean destroyResult = false;
        if (this.operationService != null) {
            destroyResult = this.operationService.destroy();
        } else {
            destroyResult = true;
        }

        return destroyResult;
    }

    public CouchbaseOperationsServiceImpl getOperationService() {
        return operationService;
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
    public <T> T merge(T entry) {
        Class<?> entryClass = entry.getClass();
        checkEntryClass(entryClass, true);
        if (isLdapSchemaEntry(entryClass)) {
            if (getSupportedLDAPVersion() > 2) {
                return merge(entry, true, AttributeModificationType.ADD);
            } else {
                throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
            }
        } else {
            return merge(entry, false, null);
        }
    }

    @Override
    protected <T> void updateMergeChanges(T entry, boolean isSchemaUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromLdapMap,
            List<AttributeDataModification> attributeDataModifications) {
        // Update object classes if entry contains custom object classes
        if (getSupportedLDAPVersion() > 2) {
            if (!isSchemaUpdate) {
                String[] objectClasses = getObjectClasses(entry, entryClass);
                String[] objectClassesFromLdap = attributesFromLdapMap.get(OBJECT_CLASS.toLowerCase()).getValues();

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
        if (isLdapSchemaEntry(entryClass)) {
            if (getSupportedLDAPVersion() > 2) {
                merge(entry, true, AttributeModificationType.REMOVE);
            } else {
                throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
            }
            return;
        }

        Object dnValue = getDNValue(entry, entryClass);

        log.debug(String.format("LDAP entry to remove: %s", dnValue.toString()));

        remove(dnValue.toString());
    }

    @Override
    protected void persist(String dn, List<AttributeData> attributes) {
        List<Attribute> ldapAttributes = new ArrayList<Attribute>(attributes.size());
        for (AttributeData attribute : attributes) {
            String attributeName = attribute.getName();
            String[] attributeValues = attribute.getValues();

            if (ArrayHelper.isNotEmpty(attributeValues) && StringHelper.isNotEmpty(attributeValues[0])) {
                if (operationService.isCertificateAttribute(attributeName)) {
                    byte[][] binaryValues = toBinaryValues(attributeValues);

                    ldapAttributes.add(new Attribute(attributeName + ";binary", binaryValues));
                } else {
                    ldapAttributes.add(new Attribute(attributeName, attributeValues));
                }
            }
        }

        // Persist entry
        try {
            boolean result = this.operationService.addEntry(dn, ldapAttributes);
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
    public void merge(String dn, List<AttributeDataModification> attributeDataModifications) {
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
                    attributeValues = attribute.getValues();
                }

                String oldAttributeName = null;
                String[] oldAttributeValues = null;
                if (oldAttribute != null) {
                    oldAttributeName = oldAttribute.getName();
                    oldAttributeValues = oldAttribute.getValues();
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
                boolean result = this.operationService.updateEntry(dn, modifications);
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
    protected void remove(String dn) {
        // Remove entry
        try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            this.operationService.delete(dn);
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    @Override
    public void removeRecursively(String dn) {
        try {
            if (this.operationService.getConnectionProvider().isSupportsSubtreeDeleteRequestControl()) {
                for (DeleteNotifier subscriber : subscribers) {
                    subscriber.onBeforeRemove(dn);
                }
                this.operationService.deleteRecursively(dn);
                for (DeleteNotifier subscriber : subscribers) {
                    subscriber.onAfterRemove(dn);
                }
            } else {
                removeSubtreeThroughIteration(dn);
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    private void removeSubtreeThroughIteration(String dn) {
        SearchResult searchResult = null;
        try {
            searchResult = this.operationService.search(dn, toLdapFilter(Filter.createPresenceFilter("objectClass")), 0, 0, null, "dn");
            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find sub-entries of entry '%s' for removal", dn));
            }
        } catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to find sub-entries of entry '%s' for removal", dn), ex);
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
    protected List<AttributeData> find(String dn, String... ldapReturnAttributes) {
        // Load entry

        try {
            SearchResultEntry entry = this.operationService.lookup(dn, ldapReturnAttributes);
            List<AttributeData> result = getAttributeDataList(entry);
            if (result != null) {
                return result;
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn), ex);
        }

        throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn));
    }
*/
/*

    @Override
    public JsonObject lookup(String key) throws SearchException {
        return lookup(key, (String[]) null);
    }

    @Override
    public List<JsonObject> search(String key, Expression expression, int pageLimit, int count) throws SearchException {
        return search(key, expression, pageLimit, count, (String[]) null);
    }

    @Override
    public List<JsonObject> search(String key, Expression expression, int pageLimit, int count, String... attributes) throws SearchException {
        return search(key, expression, SearchScope.SUB, pageLimit, count, attributes);
    }

    @Override
    public List<JsonObject> search(String key, Expression expression, SearchScope scope, int pageLimit, int count,
            String... attributes) throws SearchException {
        return search(key, expression, scope, null, 0, pageLimit, count, attributes);
    }
*/
/*
    @Override
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int startIndex, int sizeLimit, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        String[] currentLdapReturnAttributes = ldapReturnAttributes;
        if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
            currentLdapReturnAttributes = getLdapAttributes(null, propertiesAnnotations, false);
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
            searchResult = this.operationService.search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(scope), batchOperationWraper,
                    startIndex, chunkSize, sizeLimit, null, currentLdapReturnAttributes);

            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
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
    public <T> ListViewResponse<T> findListViewResponse(String baseDN, Class<T> entryClass, Filter filter, int startIndex, int count, int chunkSize,
            String sortBy, SortOrder sortOrder, String[] ldapReturnAttributes) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        String[] currentLdapReturnAttributes = ldapReturnAttributes;
        if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
            currentLdapReturnAttributes = getLdapAttributes(null, propertiesAnnotations, false);
        }

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        SearchResult searchResult = null;
        ListViewResponse<T> vlvResponse = new ListViewResponse<T>();
        try {

            searchResult = this.operationService.searchSearchResult(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB),
                    startIndex, count, chunkSize, sortBy, sortOrder, vlvResponse, currentLdapReturnAttributes);

            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }

        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        if (searchResult.getEntryCount() == 0) {
            vlvResponse.setResult(new ArrayList<T>(0));
            return vlvResponse;
        }

        List<T> entries = createEntitiesVirtualListView(entryClass, propertiesAnnotations,
                searchResult.getSearchEntries().toArray(new SearchResultEntry[searchResult.getSearchEntries().size()]));
        vlvResponse.setResult(entries);

        return vlvResponse;
    }

    @Override
    protected boolean contains(String baseDN, Filter filter, String[] objectClasses, String[] ldapReturnAttributes) {
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

        SearchResult searchResult = null;
        try {
            searchResult = this.operationService.search(baseDN, toLdapFilter(searchFilter), 1, 1, null, ldapReturnAttributes);
            if ((searchResult == null) || !ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (SearchException ex) {
            if (!(ResultCode.NO_SUCH_OBJECT_INT_VALUE == ex.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
            }
        }

        return (searchResult != null) && (searchResult.getEntryCount() > 0);
    }

    protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
            JsonObject ... searchResultEntries) {
        List<T> result = new ArrayList<T>(searchResultEntries.length);
        Map<String, List<AttributeData>> entriesAttributes = new HashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {
            count++;
            JsonDocument entry = searchResultEntries[i];
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
            JsonDocument ... searchResultEntries) {

        List<T> result = new LinkedList<T>();
        Map<String, List<AttributeData>> entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {

            count++;

            JsonDocument entry = searchResultEntries[i];

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

    private List<AttributeData> getAttributeDataList(JsonDocument entry) {
        if (entry == null) {
            return null;
        }

        List<AttributeData> result = new ArrayList<AttributeData>();
        JsonObject content = entry.content();
        for (String attributeName : content.getNames()) {
            Object attributeObject = content.get(attributeName);

            String[] attributeValueStrings;
            if (attributeObject == null) {
                attributeValueStrings = NO_STRINGS;
            } if (attributeObject instanceof JsonArray) {
                attributeValueStrings = ((JsonArray) attributeObject).toList().toArray(NO_STRINGS);
            } else {
                attributeValueStrings = new String[] { attributeObject.toString() };
            }

            AttributeData tmpAttribute = new AttributeData(attributeName, attributeValueStrings);
            result.add(tmpAttribute);
        }

        return result;
    }

    @Override
    public boolean authenticate(String userName, String password, String baseDN) {
        try {
            return operationService.authenticate(userName, password, baseDN);
        } catch (ConnectionException ex) {
            throw new AuthenticationException(String.format("Failed to authenticate user: %s", userName), ex);
        } catch (SearchException ex) {
            throw new AuthenticationException(String.format("Failed to find user DN: %s", userName), ex);
        }
    }

    @Override
    public boolean authenticate(String bindDn, String password) {
        try {
            return operationService.authenticate(bindDn, password);
        } catch (ConnectionException ex) {
            throw new AuthenticationException(String.format("Failed to authenticate DN: %s", bindDn), ex);
        }
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        String[] ldapReturnAttributes = new String[] {""}; // Don't load attributes

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        CountBatchOperation<T> batchOperation = new CountBatchOperation<T>();

        try {
            LdapBatchOperationWraper<T> batchOperationWraper = new LdapBatchOperationWraper<T>(batchOperation);
            operationService.search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB), batchOperationWraper, 0, 100, 0, null,
                    ldapReturnAttributes);
        } catch (Exception ex) {
            throw new EntryPersistenceException(
                    String.format("Failed to calucalte count of entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return batchOperation.getCountEntries();
    }

    @Override
    public String[] getLDIF(String dn) {
        String[] ldif = null;
        try {
            ldif = this.operationService.lookup(dn).toLDIF();
        } catch (ConnectionException e) {
            log.error("Failed get ldif from " + dn, e);
        }

        return ldif;
    }

    @Override
    public List<String[]> getLDIF(String dn, String[] attributes) {
        SearchResult searchResult;
        try {
            searchResult = this.operationService.search(dn, toLdapFilter(Filter.create("objectclass=*")), toLdapSearchScope(SearchScope.BASE), -1,
                    0, null, attributes);
            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s", dn));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", dn, null), ex);
        }

        List<String[]> result = new ArrayList<String[]>();

        if (searchResult.getEntryCount() == 0) {
            return result;
        }

        for (SearchResultEntry searchResultEntry : searchResult.getSearchEntries()) {
            result.add(searchResultEntry.toLDIF());
        }

        return result;
    }

    @Override
    public List<String[]> getLDIFTree(String baseDN, Filter searchFilter, String... attributes) {
        SearchResult searchResult;
        try {
            searchResult = this.operationService.search(baseDN, toLdapFilter(searchFilter), -1, 0, null, attributes);
            if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        List<String[]> result = new ArrayList<String[]>();

        if (searchResult.getEntryCount() == 0) {
            return result;
        }

        for (SearchResultEntry searchResultEntry : searchResult.getSearchEntries()) {
            result.add(searchResultEntry.toLDIF());
        }

        return result;
    }

    private Modification createModification(final ModificationType modificationType, final String attributeName, final String... attributeValues) {
        String realAttributeName = attributeName;
        if (operationService.isCertificateAttribute(realAttributeName)) {
            realAttributeName += ";binary";
            byte[][] binaryValues = toBinaryValues(attributeValues);

            return new Modification(modificationType, realAttributeName, binaryValues);
        }

        return new Modification(modificationType, realAttributeName, attributeValues);
    }

    private com.unboundid.ldap.sdk.Filter toLdapFilter(Filter genericFilter) throws SearchException {
        return FILTER_CONVERTER.convertToLdapFilter(genericFilter);
    }

    private com.unboundid.ldap.sdk.SearchScope toLdapSearchScope(SearchScope scope) throws SearchScopeException {
        return SEARCH_SCOPE_CONVERTER.convertToLdapSearchScope(scope);
    }

    private static final class CountBatchOperation<T> extends DefaultBatchOperation<T> {

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
*/
}
