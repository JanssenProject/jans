/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gluu.persist.couchbase.model.ParsedKey;
import org.gluu.persist.couchbase.operation.CouchbaseOperationService;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.couchbase.operation.impl.CouchbaseOperationsServiceImpl;
import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.exception.mapping.EntryPersistenceException;
import org.gluu.persist.exception.mapping.MappingException;
import org.gluu.persist.exception.operation.AuthenticationException;
import org.gluu.persist.exception.operation.KeyConversionException;
import org.gluu.persist.exception.operation.SearchException;
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

import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.subdoc.MutationSpec;

/**
 * Couchbase Entry Manager
 *
 * @author Yuriy Movchan Date: 05/14/2018
 */
// TODO: Review meta_doc_id. We must have it in every JsonDocument
public class CouchbaseEntryManager extends BaseEntryManager implements Serializable {

    private static final long serialVersionUID = 2127241817126412574L;

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private static final CouchbaseFilterConverter FILTER_CONVERTER = new CouchbaseFilterConverter();
    private static final CouchbaseKeyConverter KEY_CONVERTER = new CouchbaseKeyConverter();

    private CouchbaseOperationsServiceImpl operationService;
    private List<DeleteNotifier> subscribers;

    protected CouchbaseEntryManager(CouchbaseOperationsServiceImpl operationService) {
        this.operationService = operationService;
        subscribers = new LinkedList<DeleteNotifier>();
    }

    @Override
    public boolean destroy() {
        if (this.operationService == null) {
            return true;
        }

        return this.operationService.destroy();
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
            throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
        } else {
            return merge(entry, false, null);
        }
    }

    @Override
    protected <T> void updateMergeChanges(T entry, boolean isSchemaUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromDbMap,
            List<AttributeDataModification> attributeDataModifications) {
        // Update object classes if entry contains custom object classes
        if (!isSchemaUpdate) {
            String[] objectClasses = getObjectClasses(entry, entryClass);
            String[] objectClassesFromDb = attributesFromDbMap.get(OBJECT_CLASS.toLowerCase()).getValues();

            if (!Arrays.equals(objectClassesFromDb, objectClasses)) {
                attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE,
                        new AttributeData(OBJECT_CLASS, objectClasses), new AttributeData(OBJECT_CLASS, objectClassesFromDb)));
            }
        }
    }

    @Override
    public void remove(Object entry) {
        Class<?> entryClass = entry.getClass();
        checkEntryClass(entryClass, true);
        if (isLdapSchemaEntry(entryClass)) {
            throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
        }

        Object dnValue = getDNValue(entry, entryClass);

        LOG.debug("LDAP entry to remove: '{}'", dnValue.toString());

        remove(dnValue.toString());
    }

    @Override
    protected void persist(String dn, List<AttributeData> attributes) {
        JsonObject jsonObject = JsonObject.create();
        for (AttributeData attribute : attributes) {
            String attributeName = attribute.getName();
            String[] attributeValues = attribute.getValues();

            if (ArrayHelper.isNotEmpty(attributeValues) && StringHelper.isNotEmpty(attributeValues[0])) {
                jsonObject.put(attributeName, attributeValues);
            }
        }

        // Persist entry
        try {
            boolean result = operationService.addEntry(toCouchbaseKey(dn).getKey(), jsonObject);
            if (!result) {
                throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to persist entry: %s", dn), ex);
        }
    }

    @Override
    public void merge(String dn, List<AttributeDataModification> attributeDataModifications) {
        // Update entry
        try {
            List<MutationSpec> modifications = new ArrayList<MutationSpec>(attributeDataModifications.size());
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

                MutationSpec modification = null;
                if (AttributeModificationType.ADD.equals(attributeDataModification.getModificationType())) {
                    modification = createModification(Mutation.DICT_ADD, attributeName, attributeValues);
                } else {
                    if (AttributeModificationType.REMOVE.equals(attributeDataModification.getModificationType())) {
                        modification = createModification(Mutation.DELETE, oldAttributeName, oldAttributeValues);
                    } else if (AttributeModificationType.REPLACE.equals(attributeDataModification.getModificationType())) {
                        if (attributeValues.length == 1) {
                            modification = createModification(Mutation.REPLACE, attributeName, attributeValues);
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
                                MutationSpec removeModification = createModification(Mutation.DELETE, attributeName,
                                        removeValues.toArray(new String[removeValues.size()]));
                                modifications.add(removeModification);
                            }

                            if (addValues.size() > 0) {
                                MutationSpec addModification = createModification(Mutation.DICT_ADD, attributeName,
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
                boolean result = operationService.updateEntry(toCouchbaseKey(dn).getKey(), modifications);
                if (!result) {
                    throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn));
                }
            }
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
            operationService.delete(toCouchbaseKey(dn).getKey());
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
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            operationService.deleteRecursively(toCouchbaseKey(dn).getKey());
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to remove entry: %s", dn), ex);
        }
    }

    @Override
    protected List<AttributeData> find(String dn, String... ldapReturnAttributes) {
        try {
            // Load entry
            ParsedKey keyWithInum = toCouchbaseKey(dn);
            JsonObject entry = operationService.lookup(keyWithInum.getKey(), ldapReturnAttributes);
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
    // TODO: Reuse findListViewResponse after changing method signature
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int startIndex, int count, int pageLimit) {
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

        // Prepare default sort
        Sort[] defaultSort = getDefaultSort(entryClass);

        ListViewResponse<JsonObject> searchResult = null;
        ParsedKey keyWithInum = null;
        try {
            CouchbaseBatchOperationWraper<T> batchOperationWraper = new CouchbaseBatchOperationWraper<T>(batchOperation, this, entryClass,
                    propertiesAnnotations);
            keyWithInum = toCouchbaseKey(baseDN);
            searchResult = operationService.search(keyWithInum.getKey(), toCouchbaseFilter(searchFilter), scope, startIndex, pageLimit, count,
                    defaultSort, batchOperationWraper, false, currentLdapReturnAttributes);

            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        if (searchResult.getItemsPerPage() == 0) {
            return new ArrayList<T>(0);
        }

        List<T> entries = createEntities(entryClass, propertiesAnnotations, keyWithInum,
                searchResult.getResult().toArray(new JsonObject[searchResult.getItemsPerPage()]));

        return entries;
    }

    @Override
    public <T> ListViewResponse<T> findListViewResponse(String baseDN, Class<T> entryClass, Filter filter, int startIndex, int count, int pageLimit,
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

        // Prepare default sort
        Sort[] defaultSort = getDefaultSort(entryClass);

        if (StringHelper.isNotEmpty(sortBy)) {
            Sort requestedSort = buildSort(sortBy, sortOrder);

            if (ArrayHelper.isEmpty(defaultSort)) {
                defaultSort = new Sort[] { requestedSort };
            } else {
                defaultSort = ArrayHelper.arrayMerge(new Sort[] { requestedSort }, defaultSort);
            }
        }

        ListViewResponse<JsonObject> searchResult = null;
        ParsedKey keyWithInum = null;
        try {
            keyWithInum = toCouchbaseKey(baseDN);
            searchResult = operationService.search(keyWithInum.getKey(), toCouchbaseFilter(searchFilter), SearchScope.SUB, startIndex, pageLimit,
                    count, defaultSort, null, true, currentLdapReturnAttributes);

            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        ListViewResponse<T> result = new ListViewResponse<T>();
        result.setItemsPerPage(searchResult.getItemsPerPage());
        result.setStartIndex(searchResult.getStartIndex());
        result.setTotalResults(searchResult.getTotalResults());

        if (searchResult.getItemsPerPage() == 0) {
            result.setResult(new ArrayList<T>(0));
            return result;
        }

        List<T> entries = createEntities(entryClass, propertiesAnnotations, keyWithInum,
                searchResult.getResult().toArray(new JsonObject[searchResult.getItemsPerPage()]));
        result.setResult(entries);

        return result;
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

        ListViewResponse<JsonObject> searchResult = null;
        try {
            ParsedKey keyWithInum = toCouchbaseKey(baseDN);
            searchResult = operationService.search(keyWithInum.getKey(), toCouchbaseFilter(searchFilter), SearchScope.SUB, 1, 0, 1, null, null, false,
                    ldapReturnAttributes);
            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return (searchResult != null) && (searchResult.getItemsPerPage() > 0);
    }

    protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, ParsedKey baseDn,
            JsonObject... searchResultEntries) {
        List<T> result = new ArrayList<T>(searchResultEntries.length);
        Map<String, List<AttributeData>> entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {
            count++;
            JsonObject entry = searchResultEntries[i];
            // String key = entry.getString(CouchbaseOperationService.META_DOC_ID);
            String dn = entry.getString(CouchbaseOperationService.DN);
            entriesAttributes.put(dn, getAttributeDataList(entry));

            // Remove reference to allow java clean up object
            searchResultEntries[i] = null;

            // Allow java to clean up temporary objects
            if (count >= 100) {
                List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
                result.addAll(currentResult);

                entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);
                count = 0;
            }
        }

        List<T> currentResult = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
        result.addAll(currentResult);

        return result;
    }

    private List<AttributeData> getAttributeDataList(JsonObject entry) {
        if (entry == null) {
            return null;
        }

        List<AttributeData> result = new ArrayList<AttributeData>();
        for (String attributeName : entry.getNames()) {
            Object attributeObject = entry.get(attributeName);

            String[] attributeValueStrings;
            if (attributeObject == null) {
                attributeValueStrings = NO_STRINGS;
            }
            if (attributeObject instanceof JsonArray) {
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
            Filter filter = Filter.createEqualityFilter(CouchbaseOperationService.UID, userName);
            ListViewResponse<JsonObject> searchResult = operationService.search(toCouchbaseKey(baseDN).getKey(), toCouchbaseFilter(filter),
                    SearchScope.SUB, 0, 0, 1, null, null, false);
            if ((searchResult == null) || (searchResult.getItemsPerPage() != 1)) {
                return false;
            }

            String bindDn = searchResult.getResult().get(0).getString(CouchbaseOperationService.DN);

            return authenticate(bindDn, password);
        } catch (SearchException ex) {
            throw new AuthenticationException(String.format("Failed to find user DN: %s", userName), ex);
        } catch (Exception ex) {
            throw new AuthenticationException(String.format("Failed to authenticate user: %s", userName), ex);
        }
    }

    @Override
    public boolean authenticate(String bindDn, String password) {
        try {
            return operationService.authenticate(toCouchbaseKey(bindDn).getKey(), password);
        } catch (Exception ex) {
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
        String[] ldapReturnAttributes = new String[] { "" }; // Don't load attributes

        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        CountBatchOperation<T> batchOperation = new CountBatchOperation<T>();

        try {
            CouchbaseBatchOperationWraper<T> batchOperationWraper = new CouchbaseBatchOperationWraper<T>(batchOperation);
            operationService.search(toCouchbaseKey(baseDN).getKey(), toCouchbaseFilter(searchFilter), SearchScope.SUB, 0, 100, 0, null,
                    batchOperationWraper, false, ldapReturnAttributes);
        } catch (Exception ex) {
            throw new EntryPersistenceException(
                    String.format("Failed to calucalte count of entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return batchOperation.getCountEntries();
    }

    private MutationSpec createModification(final Mutation type, final String attributeName, final String... attributeValues) {
        String realAttributeName = attributeName;

        return new MutationSpec(type, realAttributeName, attributeValues);
    }

    protected Sort buildSort(String sortBy, SortOrder sortOrder) {
        Sort requestedSort = null;
        if (SortOrder.DESCENDING == sortOrder) {
            requestedSort = Sort.desc(Expression.path(sortBy));
        } else if (SortOrder.ASCENDING == sortOrder) {
            requestedSort = Sort.asc(Expression.path(sortBy));
        } else {
            requestedSort = Sort.def(Expression.path(sortBy));
        }
        return requestedSort;
    }

    protected <T> Sort[] getDefaultSort(Class<T> entryClass) {
        String[] sortByProperties = getEntrySortBy(entryClass);

        if (ArrayHelper.isEmpty(sortByProperties)) {
            return null;
        }

        Sort[] sort = new Sort[sortByProperties.length];
        for (int i = 0; i < sortByProperties.length; i++) {
            sort[i] = Sort.def(Expression.path(sortByProperties[i]));
        }

        return sort;
    }

    @Override
    public String[] exportEntry(String dn) {
        try {
            // Load entry
            ParsedKey keyWithInum = toCouchbaseKey(dn);
            JsonObject entry = operationService.lookup(keyWithInum.getKey());
            Map<String, Object> map = entry.toMap();
            List<String> result = new ArrayList<String>(map.size());
            for (Entry<String, Object> attr : map.entrySet()) {
                result.add(attr.getKey() + ": " + attr.getValue());
            }
            
            return result.toArray(new String[result.size()]);
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: %s", dn), ex);
        }
    }

    private Expression toCouchbaseFilter(Filter genericFilter) throws SearchException {
        return FILTER_CONVERTER.convertToLdapFilter(genericFilter);
    }

    private ParsedKey toCouchbaseKey(String dn) throws KeyConversionException {
        return KEY_CONVERTER.convertToKey(dn);
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

}
