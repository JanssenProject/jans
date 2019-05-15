/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gluu.persist.couchbase.model.SearchReturnDataType;
import org.gluu.persist.couchbase.operation.CouchbaseOperationService;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.couchbase.operation.impl.CouchbaseOperationsServiceImpl;
import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.exception.AuthenticationException;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.exception.MappingException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.impl.BaseEntryManager;
import org.gluu.persist.key.impl.GenericKeyConverter;
import org.gluu.persist.key.impl.model.ParsedKey;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.AttributeDataModification;
import org.gluu.persist.model.AttributeDataModification.AttributeModificationType;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.DefaultBatchOperation;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.reflect.property.PropertyAnnotation;
import org.gluu.search.filter.Filter;
import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class CouchbaseEntryManager extends BaseEntryManager implements Serializable {

	private static final long serialVersionUID = 2127241817126412574L;

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private static final CouchbaseFilterConverter FILTER_CONVERTER = new CouchbaseFilterConverter();
    private static final GenericKeyConverter KEY_CONVERTER = new GenericKeyConverter();

    private SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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
        if (isSchemaEntry(entryClass)) {
            throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
        } else {
            return merge(entry, false, null);
        }
    }

    @Override
    protected <T> void updateMergeChanges(String baseDn, T entry, boolean isSchemaUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromDbMap,
            List<AttributeDataModification> attributeDataModifications) {
        // Update object classes if entry contains custom object classes
        if (!isSchemaUpdate) {
            String[] objectClasses = getObjectClasses(entry, entryClass);
            AttributeData objectClassAttributeData = attributesFromDbMap.get(OBJECT_CLASS.toLowerCase());
            if (objectClassAttributeData == null) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses list! Entry is invalid: '%s'", entry));
            }

            String[] objectClassesFromDb = objectClassAttributeData.getStringValues();

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
        if (isSchemaEntry(entryClass)) {
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
            Object[] attributeValues = attribute.getValues();

            if (ArrayHelper.isNotEmpty(attributeValues) && (attributeValues[0] != null)) {
                Object[] realValues = attributeValues;
                if (StringHelper.equals(CouchbaseOperationService.USER_PASSWORD, attributeName)) {
                    realValues = operationService.createStoragePassword(StringHelper.toStringArray(attributeValues));
                }
                if (realValues.length > 1) {
                    jsonObject.put(attributeName, JsonArray.from(realValues));
                } else {
                    jsonObject.put(attributeName, realValues[0]);
                }
            }
        }
        jsonObject.put(CouchbaseOperationService.DN, dn);

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
                Object[] attributeValues = null;
                if (attribute != null) {
                    attributeName = attribute.getName();
                    attributeValues = attribute.getValues();
                }

                String oldAttributeName = null;
                Object[] oldAttributeValues = null;
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
                        modification = createModification(Mutation.REPLACE, attributeName, attributeValues);
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
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
            BatchOperation<T> batchOperation, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        PagedResult<JsonObject> searchResult = findEntriesImpl(baseDN, entryClass, filter, scope, ldapReturnAttributes, null, null, batchOperation,
        		SearchReturnDataType.SEARCH, start, count, chunkSize);
        if (searchResult.getEntriesCount() == 0) {
            return new ArrayList<T>(0);
        }

        List<T> entries = createEntities(baseDN, entryClass, searchResult);

        return entries;
    }

    @Override
    public <T> PagedResult<T> findPagedEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
            SortOrder sortOrder, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        PagedResult<JsonObject> searchResult = findEntriesImpl(baseDN, entryClass, filter, SearchScope.SUB, ldapReturnAttributes, sortBy, sortOrder,
                null, SearchReturnDataType.SEARCH_COUNT, start, count, chunkSize);

        PagedResult<T> result = new PagedResult<T>();
        result.setEntriesCount(searchResult.getEntriesCount());
        result.setStart(searchResult.getStart());
        result.setTotalEntriesCount(searchResult.getTotalEntriesCount());

        if (searchResult.getEntriesCount() == 0) {
            result.setEntries(new ArrayList<T>(0));
            return result;
        }

        List<T> entries = createEntities(baseDN, entryClass, searchResult);
        result.setEntries(entries);

        return result;
    }

    protected <T> PagedResult<JsonObject> findEntriesImpl(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope,
            String[] ldapReturnAttributes, String sortBy, SortOrder sortOrder, BatchOperation<T> batchOperation, SearchReturnDataType returnDataType, int start,
            int count, int chunkSize) {
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

        PagedResult<JsonObject> searchResult = null;
        try {
            CouchbaseBatchOperationWraper<T> batchOperationWraper = null;
            if (batchOperation != null) {
                batchOperationWraper = new CouchbaseBatchOperationWraper<T>(batchOperation, this, entryClass, propertiesAnnotations);
            }
            ParsedKey keyWithInum = toCouchbaseKey(baseDN);
            searchResult = operationService.search(keyWithInum.getKey(), toCouchbaseFilter(searchFilter), scope, currentLdapReturnAttributes,
                    defaultSort, batchOperationWraper, returnDataType, start, count, chunkSize);

            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
            }

            return searchResult;
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }
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

        PagedResult<JsonObject> searchResult = null;
        try {
            ParsedKey keyWithInum = toCouchbaseKey(baseDN);
            searchResult = operationService.search(keyWithInum.getKey(), toCouchbaseFilter(searchFilter), SearchScope.SUB, ldapReturnAttributes, null,
                    null, SearchReturnDataType.SEARCH, 1, 1, 0);
            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return (searchResult != null) && (searchResult.getEntriesCount() > 0);
    }

    protected <T> List<T> createEntities(String baseDN, Class<T> entryClass, PagedResult<JsonObject> searchResult) {
        ParsedKey keyWithInum = toCouchbaseKey(baseDN);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        List<T> entries = createEntities(entryClass, propertiesAnnotations, keyWithInum,
                searchResult.getEntries().toArray(new JsonObject[searchResult.getEntriesCount()]));

        return entries;
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

            Object[] attributeValueObjects;
            if (attributeObject == null) {
                attributeValueObjects = NO_OBJECTS;
            }
            if (attributeObject instanceof JsonArray) {
            	JsonArray jsonArray = (JsonArray) attributeObject;
            	ArrayList<Object> resultList = new ArrayList<Object>(jsonArray.size());
            	for (Iterator<Object> it = jsonArray.iterator(); it.hasNext();) {
            		resultList.add(it.next());
				}
                attributeValueObjects = resultList.toArray(NO_OBJECTS);
            } else {
            	if ((attributeObject instanceof Boolean) || (attributeObject instanceof Integer) || (attributeObject instanceof Long) ||
            		(attributeObject instanceof JsonObject)) {
                    attributeValueObjects = new Object[] { attributeObject };
            	} else {
            		attributeValueObjects = new Object[] { attributeObject.toString() };
            	}
            }

            AttributeData tmpAttribute = new AttributeData(attributeName, attributeValueObjects);
            result.add(tmpAttribute);
        }

        return result;
    }

    @Override
    public boolean authenticate(String baseDN, String userName, String password) {
        try {
            Filter filter = Filter.createEqualityFilter(CouchbaseOperationService.UID, userName);
            PagedResult<JsonObject> searchResult = operationService.search(toCouchbaseKey(baseDN).getKey(), toCouchbaseFilter(filter),
                    SearchScope.SUB, null, null, null, SearchReturnDataType.SEARCH, 0, 1, 1);
            if ((searchResult == null) || (searchResult.getEntriesCount() != 1)) {
                return false;
            }

            String bindDn = searchResult.getEntries().get(0).getString(CouchbaseOperationService.DN);

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
        return countEntries(baseDN, entryClass, filter, SearchScope.SUB);
    }

    @Override
    public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
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

        PagedResult<JsonObject> searchResult;
        try {
            searchResult = operationService.search(toCouchbaseKey(baseDN).getKey(), toCouchbaseFilter(searchFilter), scope, null, null,
                    null, SearchReturnDataType.COUNT, 0, 0, 0);
        } catch (Exception ex) {
            throw new EntryPersistenceException(
                    String.format("Failed to calucalte count of entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
        }

        return searchResult.getTotalEntriesCount();
    }

    private MutationSpec createModification(final Mutation type, final String attributeName, final Object... attributeValues) {
        String realAttributeName = attributeName;

        Object[] realValues = attributeValues;
        if (StringHelper.equals(CouchbaseOperationService.USER_PASSWORD, realAttributeName)) {
            realValues = operationService.createStoragePassword(StringHelper.toStringArray(attributeValues));
        }

        if (realValues.length == 1) {
            return new MutationSpec(type, realAttributeName, realValues[0]);
        }

        return new MutationSpec(type, realAttributeName, realValues);
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
        return FILTER_CONVERTER.convertToCouchbaseFilter(genericFilter);
    }

    private ParsedKey toCouchbaseKey(String dn) {
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
    public String encodeTime(String baseDN, Date date) {
        if (date == null) {
            return null;
        }

        return jsonDateFormat.format(date);
    }

    @Override
	protected String encodeTime(Date date) {
		return encodeTime(null, date);
	}

    @Override
    public Date decodeTime(String baseDN, String date) {
        if (StringHelper.isEmpty(date)) {
            return null;
        }

        Date decodedDate;
        try {
            decodedDate = jsonDateFormat.parse(date);
        } catch (ParseException ex) {
            LOG.error("Failed to decode generalized time '{}'", date, ex);

            return null;
        }

        return decodedDate;
    }

    @Override
    public Date decodeTime(String date) {
		return decodeTime(null, date);
	}

	@Override
	public boolean hasBranchesSupport(String dn) {
		return false;
	}

    @Override
	protected Object convertValueToJson(Object propertyValue) {
    	String jsonStringPropertyValue = (String) super.convertValueToJson(propertyValue);
    	return JsonObject.fromJson(jsonStringPropertyValue);
	}

	@Override
	protected Object convertJsonToValue(Class<?> parameterType, Object propertyValue) {
    	Object jsonStringPropertyValue = propertyValue;
    	if (propertyValue instanceof JsonObject) {
    		JsonObject jsonObject = (JsonObject) propertyValue;
    		jsonStringPropertyValue = jsonObject.toString();
    	}

    	return super.convertJsonToValue(parameterType, jsonStringPropertyValue);
	}

}
