/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.impl;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.EntryDeleteException;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.impl.BaseEntryManager;
import io.jans.orm.impl.GenericKeyConverter;
import io.jans.orm.impl.model.ParsedKey;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.EntryData;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterProcessor;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.model.SearchReturnDataType;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

/**
 * SQL Entry Manager
 *
 * @author Yuriy Movchan Date: 01/12/2020
 */
public class SqlEntryManager extends BaseEntryManager implements Serializable {

	private static final long serialVersionUID = 2127241817126412574L;

    private static final Logger LOG = LoggerFactory.getLogger(SqlEntryManager.class);

    private static final String JSON_DATA_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @Inject
    private Logger log;

    private final SqlFilterConverter filterConverter;
	private FilterProcessor filterProcessor;

	private static final GenericKeyConverter KEY_CONVERTER = new GenericKeyConverter(false);

    private List<DeleteNotifier> subscribers;

    protected SqlEntryManager(SqlOperationService operationService) {
        this.operationService = operationService;
        this.filterConverter = new SqlFilterConverter(operationService);
        this.filterProcessor = new FilterProcessor();
        subscribers = new LinkedList<DeleteNotifier>();
    }

    @Override
    public boolean destroy() {
        if (this.operationService == null) {
            return true;
        }

        return ((SqlOperationService) this.operationService).destroy();
    }

    public SqlOperationService getOperationService() {
        return ((SqlOperationService) operationService);
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
            throw new UnsupportedOperationException("Server doesn't support dynamic schema modifications");
        } else {
            return merge(entry, false, false, null);
        }
    }

    @Override
    protected <T> void updateMergeChanges(String baseDn, T entry, boolean isConfigurationUpdate, Class<?> entryClass, Map<String, AttributeData> attributesFromDbMap,
            List<AttributeDataModification> attributeDataModifications, boolean forceUpdate) {
    	if (forceUpdate) {
    		// SQL ORM can't update objectClass because it select table by objectClass name  
    		return;
    	}

    	// Update object classes if entry contains custom object classes
        if (!isConfigurationUpdate) {
            String[] objectClasses = getObjectClasses(entry, entryClass);
            if (ArrayHelper.isEmpty(objectClasses)) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses to persist! Entry is invalid: '%s'", entry));
            }

            AttributeData objectClassAttributeData = attributesFromDbMap.get(OBJECT_CLASS.toLowerCase());
            if (objectClassAttributeData == null) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses in DB! Entry is invalid: '%s'", entry));
            }

            String[] objectClassesFromDb = objectClassAttributeData.getStringValues();
            if (ArrayHelper.isEmpty(objectClassesFromDb)) {
                throw new UnsupportedOperationException(String.format("There is no attribute with objectClasses in DB! Entry is invalid: '%s'", entry));
            }
            
            // We need to check only first element of each array because objectCLass in SQL is single value attribute
            if (!StringHelper.equals(objectClassesFromDb[0], objectClasses[0])) {
            	throw new UnsupportedOperationException(String.format("It's not possible to change objectClasses of already persisted entry! Entry is invalid: '%s'", entry));
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

        remove(dnValue.toString(), entryClass);
    }

    @Override
    protected void persist(String dn, String[] objectClasses, List<AttributeData> attributes, Integer expiration) {
    	ArrayList<AttributeData> resultAttributes = new ArrayList<>(attributes.size() + 1);
        for (AttributeData attribute : attributes) {
            String attributeName = attribute.getName();
            Object[] attributeValues = attribute.getValues();
            Boolean multiValued = attribute.getMultiValued();

            if (ArrayHelper.isNotEmpty(attributeValues) && (attributeValues[0] != null)) {
            	Object[] realValues = attributeValues;

            	// We need to store only one objectClass value in SQL
                if (StringHelper.equalsIgnoreCase(SqlOperationService.OBJECT_CLASS, attributeName)) {
                	if (!ArrayHelper.isEmpty(realValues)) {
                		realValues = new Object[] { realValues[0] };
                		multiValued = false;
                	}
                }

            	// Process userPassword 
                if (StringHelper.equalsIgnoreCase(SqlOperationService.USER_PASSWORD, attributeName)) {
                    realValues = getOperationService().createStoragePassword(StringHelper.toStringArray(attributeValues));
                }

                escapeValues(realValues);

                AttributeData resultAttributeData;
                if (Boolean.TRUE.equals(multiValued)) {
                	resultAttributeData = new AttributeData(toInternalAttribute(attributeName), realValues, multiValued);
                } else {
                	resultAttributeData = new AttributeData(toInternalAttribute(attributeName), realValues[0]);
                }

                resultAttributes.add(resultAttributeData);
            }
        }

        // Persist entry
        try {
        	ParsedKey parsedKey = toSQLKey(dn);
            resultAttributes.add(new AttributeData(SqlOperationService.DN, dn));
            resultAttributes.add(new AttributeData(SqlOperationService.DOC_ID, parsedKey.getKey()));

            boolean result = getOperationService().addEntry(parsedKey.getKey(), objectClasses[0], resultAttributes);
            if (!result) {
                throw new EntryPersistenceException(String.format("Failed to persist entry: '%s'", dn));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to persist entry: '%s'", dn), ex);
        }
    }

    @Override
    public void merge(String dn, String[] objectClasses, List<AttributeDataModification> attributeDataModifications, Integer expirationValue) {
        // Update entry
        try {
            List<AttributeDataModification> modifications = new ArrayList<AttributeDataModification>(attributeDataModifications.size());
            for (AttributeDataModification attributeDataModification : attributeDataModifications) {
                AttributeData attribute = attributeDataModification.getAttribute();
                AttributeData oldAttribute = attributeDataModification.getOldAttribute();

                String attributeName = null;
                Object[] attributeValues = null;
                Boolean multiValued = null;
                if (attribute != null) {
                    attributeName = attribute.getName();
                    attributeValues = attribute.getValues();
                    multiValued = attribute.getMultiValued();
                }

                String oldAttributeName = null;
                Object[] oldAttributeValues = null;
                if (oldAttribute != null) {
                    oldAttributeName = oldAttribute.getName();
                    oldAttributeValues = oldAttribute.getValues();
                }
                
                AttributeDataModification modification = null;
                AttributeModificationType modificationType = attributeDataModification.getModificationType();
				if ((AttributeModificationType.ADD == modificationType) ||
                	(AttributeModificationType.FORCE_UPDATE == modificationType)) {
                    modification = createModification(modificationType, toInternalAttribute(attributeName), multiValued, attributeValues);
                } else {
                    if ((AttributeModificationType.REMOVE == modificationType)) {
                		if ((attribute == null) && isEmptyAttributeValues(oldAttribute)) {
							// It's RDBS case. We don't need to set null to already empty table cell
                			continue;
                		}
                        modification = createModification(AttributeModificationType.REMOVE, toInternalAttribute(oldAttributeName), multiValued, oldAttributeValues);
                    } else if ((AttributeModificationType.REPLACE == modificationType)) {
                        modification = createModification(AttributeModificationType.REPLACE, toInternalAttribute(attributeName), multiValued, attributeValues);
                    }
                }

                if (modification != null) {
                    modifications.add(modification);
                }
            }

            if (modifications.size() > 0) {
                boolean result = getOperationService().updateEntry(toSQLKey(dn).getKey(), objectClasses[0], modifications);
                if (!result) {
                    throw new EntryPersistenceException(String.format("Failed to update entry: '%s'", dn));
                }
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to update entry: '%s'", dn), ex);
        }
    }

    @Override
    protected <T> void removeByDn(String dn, String[] objectClasses) {
    	if (ArrayHelper.isEmpty(objectClasses)) {
    		throw new UnsupportedOperationException("Entry class is manadatory for remove operation!");
    	}

		// Remove entry
        try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            getOperationService().delete(toSQLKey(dn).getKey(), objectClasses[0]);
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to remove entry: '%s'", dn), ex);
        }
    }

    @Override
    protected <T> void removeRecursivelyFromDn(String dn, String[] objectClasses) {
    	if (ArrayHelper.isEmpty(objectClasses)) {
    		throw new UnsupportedOperationException("Entry class is manadatory for recursive remove operation!");
    	}
		
    	removeByDn(dn, objectClasses);

		try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            getOperationService().deleteRecursively(toSQLKey(dn).getKey(), objectClasses[0]);
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to remove entry: '%s'", dn), ex);
        }
    }

    @Override
	public <T> int remove(String dn, Class<T> entryClass, Filter filter, int count) {
		if (StringHelper.isEmptyString(dn)) {
			throw new MappingException("Base DN to delete entries is null");
		}

		// Remove entries by filter
		return removeImpl(dn, entryClass, filter, count);
	}

    protected <T> int removeImpl(String dn, Class<T> entryClass, Filter filter, int count) {
        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);

        Filter searchFilter;
        if (objectClasses.length > 0) {
			LOG.trace("Filter: {}", filter);
			searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            throw new EntryDeleteException(String.format("Failed to delete entries with DN: '%s', filter: '%s' because objectClass is not specified", dn, filter));
        }

        // Find entries
        LOG.trace("-------------------------------------------------------");
        LOG.trace("Filter: {}", filter);
        LOG.trace("objectClasses count: {} ", objectClasses.length);
        LOG.trace("objectClasses: {}", objectClasses.toString());
        LOG.trace("Search filter: {}", searchFilter);

		// Prepare properties types to allow build filter properly
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ParsedKey keyWithInum = toSQLKey(dn);
        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toSqlFilterWithEmptyAlias(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryDeleteException(String.format("Failed to convert filter '%s' to expression", searchFilter), ex);
		}
        
        try {
        	int processed = (int) getOperationService().delete(keyWithInum.getKey(), objectClasses[0], convertedExpression, count);
        	
        	return processed;
        } catch (Exception ex) {
            throw new EntryDeleteException(String.format("Failed to delete entries with key: '%s', expression: '%s'", keyWithInum.getKey(), convertedExpression), ex);
        }
    }

	@Override
    protected List<AttributeData> find(String dn, String[] objectClasses, Map<String, PropertyAnnotation> propertiesAnnotationsMap, String... ldapReturnAttributes) {
        try {
            // Load entry
            ParsedKey keyWithInum = toSQLKey(dn);
            List<AttributeData> result = getOperationService().lookup(keyWithInum.getKey(), objectClasses[0], toInternalAttributes(ldapReturnAttributes));
            if (result != null) {
                return result;
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: '%s'", dn), ex);
        }

        throw new EntryPersistenceException(String.format("Failed to find entry: '%s'", dn));
    }

    @Override
    public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                                   BatchOperation<T> batchOperation, int start, int count, int chunkSize) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        PagedResult<EntryData> searchResult = findEntriesImpl(baseDN, entryClass, filter, scope, ldapReturnAttributes, null, null, batchOperation,
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

        PagedResult<EntryData> searchResult = findEntriesImpl(baseDN, entryClass, filter, SearchScope.SUB, ldapReturnAttributes, sortBy, sortOrder,
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

    protected <T> PagedResult<EntryData> findEntriesImpl(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope,
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

        Filter searchFilter;
        if (objectClasses.length > 0) {
        	 LOG.trace("Filter: {}", filter);
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

        // Find entries
        LOG.trace("-------------------------------------------------------");
        LOG.trace("Filter: {}", filter);
        LOG.trace("objectClasses count: {} ", objectClasses.length);
        LOG.trace("objectClasses: {}", ArrayHelper.toString(objectClasses));
        LOG.trace("Search filter: {}", searchFilter);

        // Prepare default sort
        OrderSpecifier<?>[] defaultSort = getDefaultSort(entryClass);

        if (StringHelper.isNotEmpty(sortBy)) {
            OrderSpecifier<?> requestedSort = buildSort(sortBy, sortOrder);

            if (ArrayHelper.isEmpty(defaultSort)) {
                defaultSort = new OrderSpecifier[] { requestedSort };
            } else { 
                defaultSort = ArrayHelper.arrayMerge(new OrderSpecifier[] { requestedSort }, defaultSort);
            }
        }

		// Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);
        ParsedKey keyWithInum = toSQLKey(baseDN);
        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toSqlFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter '%s' to expression", searchFilter));
		}

        PagedResult<EntryData> searchResult = null;
        try {
            SqlBatchOperationWraper<T> batchOperationWraper = null;
            if (batchOperation != null) {
                batchOperationWraper = new SqlBatchOperationWraper<T>(batchOperation, this, entryClass, propertiesAnnotations);
            }
            searchResult = searchImpl(keyWithInum.getKey(), objectClasses[0], convertedExpression, scope, currentLdapReturnAttributes,
                    defaultSort, batchOperationWraper, returnDataType, start, count, chunkSize);

            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entries with key: '%s', expression: '%s'", keyWithInum.getKey(), convertedExpression));
            }

            return searchResult;
        } catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with key: '%s'", keyWithInum.getKey()), ex);
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entries with key: '%s', expression: '%s'", keyWithInum.getKey(), convertedExpression), ex);
        }
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

		// Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toSqlFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter '%s' to expression", searchFilter));
		}

        PagedResult<EntryData> searchResult = null;
        try {
            ParsedKey keyWithInum = toSQLKey(baseDN);
            searchResult = searchImpl(keyWithInum.getKey(), objectClasses[0], convertedExpression, SearchScope.SUB, ldapReturnAttributes, null,
                    null, SearchReturnDataType.SEARCH, 0, 1, 0);
            if (searchResult == null) {
                throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: '%s', filter: '%s'", baseDN, searchFilter));
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: '%s', filter: '%s'", baseDN, searchFilter), ex);
        }

        return (searchResult != null) && (searchResult.getEntriesCount() > 0);
    }

	private <O> PagedResult<EntryData> searchImpl(String key, String objectClass, ConvertedExpression expression, SearchScope scope, String[] attributes, OrderSpecifier<?>[] orderBy,
            SqlBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
		return getOperationService().search(key, objectClass, expression, scope, toInternalAttributes(attributes), orderBy, batchOperationWraper, returnDataType, start, count, pageSize);
	}

    protected <T> List<T> createEntities(String baseDN, Class<T> entryClass, PagedResult<EntryData> searchResult) {
        ParsedKey keyWithInum = toSQLKey(baseDN);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        List<T> entries = createEntities(entryClass, propertiesAnnotations, keyWithInum,
                searchResult.getEntries().toArray(new EntryData[searchResult.getEntriesCount()]));

        return entries;
    }

    protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, ParsedKey baseDn,
    		EntryData ... searchResultEntries) {
        List<T> result = new ArrayList<T>(searchResultEntries.length);
        Map<String, List<AttributeData>> entriesAttributes = new LinkedHashMap<String, List<AttributeData>>(100);

        int count = 0;
        for (int i = 0; i < searchResultEntries.length; i++) {
            count++;
            EntryData entryData = searchResultEntries[i];
            
            AttributeData attributeDataDn = entryData.getAttributeDate(SqlOperationService.DN);
            if ((attributeDataDn == null) || (attributeDataDn.getValue() == null)) {
                throw new MappingException("Failed to convert EntryData to Entry because DN is missing");
            }

            entriesAttributes.put(attributeDataDn.getValue().toString(), entryData.getAttributeData());

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

    @Override
    public <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password) {
        if (StringHelper.isEmptyString(baseDN)) {
            throw new MappingException("Base DN to find entries is null");
        }

        // Check entry class
        checkEntryClass(entryClass, false);
        String[] objectClasses = getTypeObjectClasses(entryClass);
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        
        // Find entries
        Filter searchFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter(SqlOperationService.UID), StringHelper.toLowerCase(userName));
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(searchFilter, objectClasses);
        }

        // Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toSqlFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter '%s' to expression", searchFilter));
		}

		try {
            PagedResult<EntryData> searchResult = searchImpl(toSQLKey(baseDN).getKey(), objectClasses[0], convertedExpression,
                    SearchScope.SUB, SqlOperationService.UID_ARRAY, null, null, SearchReturnDataType.SEARCH, 0, 1, 1);
            if ((searchResult == null) || (searchResult.getEntriesCount() != 1)) {
                return false;
            }

            AttributeData attributeData = searchResult.getEntries().get(0).getAttributeDate(SqlOperationService.DN);
            if ((attributeData == null) || (attributeData.getValue() == null)) {
                throw new AuthenticationException("Failed to find user DN in entry: '%s'");
            }

            String bindDn = attributeData.getValue().toString();

            return authenticate(bindDn, entryClass, password);
        } catch (SearchException ex) {
            throw new AuthenticationException(String.format("Failed to find user DN: '%s'", userName), ex);
        } catch (Exception ex) {
            throw new AuthenticationException(String.format("Failed to authenticate user: '%s'", userName), ex);
        }
    }

    @Override
    @Deprecated
    public boolean authenticate(String bindDn, String password) {
    	return authenticate(bindDn, null, password);
    }

    @Override
    public <T> boolean authenticate(String bindDn, Class<T> entryClass, String password) {
    	if (entryClass == null) {
    		throw new UnsupportedOperationException("Entry class is manadatory for authenticate operation!");
    	}

    	// Check entry class
		checkEntryClass(entryClass, false);
		String[] objectClasses = getTypeObjectClasses(entryClass);

    	try {
            return getOperationService().authenticate(toSQLKey(bindDn).getKey(), escapeValue(password), objectClasses[0]);
        } catch (Exception ex) {
            throw new AuthenticationException(String.format("Failed to authenticate DN: '%s'", bindDn), ex);
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
        List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        
        // Find entries
        Filter searchFilter;
        if (objectClasses.length > 0) {
            searchFilter = addObjectClassFilter(filter, objectClasses);
        } else {
            searchFilter = filter;
        }

		// Prepare properties types to allow build filter properly
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        ConvertedExpression convertedExpression;
		try {
			convertedExpression = toSqlFilter(searchFilter, propertiesAnnotationsMap);
		} catch (SearchException ex) {
            throw new EntryPersistenceException(String.format("Failed to convert filter '%s' to expression", searchFilter));
		}

        PagedResult<EntryData> searchResult;
        try {
            searchResult = searchImpl(toSQLKey(baseDN).getKey(), objectClasses[0], convertedExpression, scope, null, null,
                    null, SearchReturnDataType.COUNT, 0, 0, 0);
        } catch (Exception ex) {
            throw new EntryPersistenceException(
                    String.format("Failed to calculate the number of entries with baseDN: '%s', filter: '%s'", baseDN, searchFilter), ex);
        }

        return searchResult.getTotalEntriesCount();
    }

    private AttributeDataModification createModification(final AttributeModificationType type, final String attributeName, final Boolean multiValued, final Object... attributeValues) {
        String realAttributeName = attributeName;

        Object[] realValues = attributeValues;
        if (StringHelper.equalsIgnoreCase(SqlOperationService.USER_PASSWORD, realAttributeName)) {
            realValues = getOperationService().createStoragePassword(StringHelper.toStringArray(attributeValues));
        }

        escapeValues(realValues);
        
        if (Boolean.TRUE.equals(multiValued)) {
            return new AttributeDataModification(type, new AttributeData(realAttributeName, realValues, multiValued));
        } else {
        	if ((realValues == null) || (realValues.length == 0)) {
                return new AttributeDataModification(type, new AttributeData(realAttributeName, null));
        	}
            return new AttributeDataModification(type, new AttributeData(realAttributeName, realValues[0]));
        }
    }

    protected OrderSpecifier<?> buildSort(String sortBy, SortOrder sortOrder) {
    	OrderSpecifier<?> requestedSort = null;
        if (SortOrder.DESCENDING == sortOrder) {
            requestedSort = new OrderSpecifier(Order.DESC, Expressions.stringPath(sortBy));
        } else if (SortOrder.ASCENDING == sortOrder) {
            requestedSort = new OrderSpecifier(Order.ASC, Expressions.stringPath(sortBy));
        } else {
            requestedSort = new OrderSpecifier(Order.ASC, Expressions.stringPath(sortBy));
        }
        return requestedSort;
    }

    protected <T> OrderSpecifier[] getDefaultSort(Class<T> entryClass) {
        String[] sortByProperties = getEntrySortByNames(entryClass);

        if (ArrayHelper.isEmpty(sortByProperties)) {
        	// Fall back to sortBy property name 
            sortByProperties = getEntrySortByProperties(entryClass);
            if (ArrayHelper.isEmpty(sortByProperties)) {
            	return null;
            }
        }

        OrderSpecifier[] sort = new OrderSpecifier[sortByProperties.length];
        for (int i = 0; i < sortByProperties.length; i++) {
            sort[i] = new OrderSpecifier(Order.ASC, Expressions.stringPath(sortByProperties[i]));
        }

        return sort;
    }

    @Override
    public List<AttributeData> exportEntry(String dn) {
        try {
            // Load entry
            ParsedKey keyWithInum = toSQLKey(dn);
            List<AttributeData> entry = getOperationService().lookup(keyWithInum.getKey(), null);

            if (entry != null) {
                return entry;
            }
            
            return null;
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to find entry: '%s'", dn), ex);
        }
    }

    private ConvertedExpression toSqlFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
        return filterConverter.convertToSqlFilter(excludeObjectClassFilters(genericFilter), propertiesAnnotationsMap);
    }

    private ConvertedExpression toSqlFilterWithEmptyAlias(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap) throws SearchException {
        return filterConverter.convertToSqlFilter(excludeObjectClassFilters(genericFilter), propertiesAnnotationsMap, true);
    }

    private ConvertedExpression toSqlFilter(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
        return filterConverter.convertToSqlFilter(excludeObjectClassFilters(genericFilter), propertiesAnnotationsMap, processor);
    }
    private ConvertedExpression toSqlFilterWithEmptyAlias(Filter genericFilter, Map<String, PropertyAnnotation> propertiesAnnotationsMap, Function<? super Filter, Boolean> processor) throws SearchException {
        return filterConverter.convertToSqlFilter(excludeObjectClassFilters(genericFilter), propertiesAnnotationsMap, processor, true);
    }

	private Filter excludeObjectClassFilters(Filter genericFilter) {
		return filterProcessor.excludeFilter(genericFilter, FilterProcessor.OBJECT_CLASS_EQUALITY_FILTER, FilterProcessor.OBJECT_CLASS_PRESENCE_FILTER);
	}

    private ParsedKey toSQLKey(String dn) {
        return KEY_CONVERTER.convertToKey(dn);
    }

    @Override
	protected Filter addObjectClassFilter(Filter filter, String[] objectClasses) {
    	return filter;
/*
    	if (objectClasses.length == 0) {
			return filter;
		}
		
		// In SQL implementation we need to use first one as entry type
		Filter searchFilter = Filter.createEqualityFilter(OBJECT_CLASS, objectClasses[0]);
		if (filter != null) {
			searchFilter = Filter.createANDFilter(Filter.createANDFilter(searchFilter), filter);
		}

		return searchFilter;
*/
	}

    @Override
    public String encodeTime(String baseDN, Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat jsonDateFormat = new SimpleDateFormat(SqlOperationService.SQL_DATA_FORMAT);
        return jsonDateFormat.format(date);
    }

    @Override
	protected String encodeTime(Date date) {
		return encodeTime(null, date);
	}

    @Override
    public Date decodeTime(String date) {
		return decodeTime(null, date);
	}

    @Override
    public Date decodeTime(String baseDN, String date) {
    	return decodeTime(baseDN, date, false);
    }

    protected Date decodeTime(String baseDN, String date, boolean silent) {
        if (StringHelper.isEmpty(date)) {
            return null;
        }

        // Add ending Z if necessary
        String dateZ = date.endsWith("Z") ? date : date + "Z";
        try {
            return new Date(Instant.parse(dateZ).toEpochMilli());
        } catch (DateTimeParseException ex) {
        	if (!silent) {
	            LOG.error("Failed to decode generalized time '{}'", date, ex);
        	}

        	return null;
        }
    }

	@Override
	public boolean hasBranchesSupport(String dn) {
		return false;
	}

	@Override
	public boolean hasExpirationSupport(String primaryKey) {
		return false;
	}

	@Override
	public String getPersistenceType() {
		return SqlEntryManagerFactory.PERSISTENCE_TYPE;
	}

    @Override
	public String getPersistenceType(String primaryKey) {
		return SqlEntryManagerFactory.PERSISTENCE_TYPE;
	}

	@Override
	public PersistenceEntryManager getPersistenceEntryManager(String persistenceType) {
		if (SqlEntryManagerFactory.PERSISTENCE_TYPE.equals(persistenceType)) {
			return this;
		}
		
		return null;
	}

	@Override
	protected Object convertJsonToValue(Class<?> parameterType, Object propertyValue) {
    	return super.convertJsonToValue(parameterType, propertyValue);
	}

    @Override
	protected Object getNativeDateAttributeValue(Date dateValue) {
		return dateValue;
    }

    @Override
    protected Object getNativeDateMultiAttributeValue(Date dateValue) {
        SimpleDateFormat jsonDateFormat = new SimpleDateFormat(JSON_DATA_FORMAT);
		return jsonDateFormat.format(dateValue);
	}

    @Override
	protected boolean isStoreFullEntry() {
		return true;
	}

	private String escapeValue(String value) {
		return ((SqlOperationService) operationService).escapeValue(value);
	}

	private void escapeValues(Object[] realValues) {
		((SqlOperationService) operationService).escapeValues(realValues);
	}

	private String unescapeValue(String value) {
		return ((SqlOperationService) operationService).unescapeValue(value);
	}

	private void unescapeValues(Object[] realValues) {
		((SqlOperationService) operationService).unescapeValues(realValues);
	}

	public String toInternalAttribute(String attributeName) {
		return ((SqlOperationService) operationService).toInternalAttribute(attributeName);
	}

	public String[] toInternalAttributes(String[] attributeNames) {
		return ((SqlOperationService) operationService).toInternalAttributes(attributeNames);
	}

	public String fromInternalAttribute(String internalAttributeName) {
		return ((SqlOperationService) operationService).fromInternalAttribute(internalAttributeName);
	}

	public String[] fromInternalAttributes(String[] internalAttributeNames) {
		return ((SqlOperationService) operationService).fromInternalAttributes(internalAttributeNames);
	}

	protected boolean isSupportForceUpdate() {
		return true;
	}

}
