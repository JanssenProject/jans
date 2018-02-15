/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.ldap.impl;

import java.io.Serializable;
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
import org.gluu.persist.event.DeleteNotifier;
import org.gluu.persist.exception.mapping.EntryPersistenceException;
import org.gluu.persist.exception.mapping.MappingException;
import org.gluu.persist.exception.operation.AuthenticationException;
import org.gluu.persist.exception.operation.ConnectionException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.exception.operation.SearchScopeException;
import org.gluu.persist.impl.BaseEntryManager;
import org.gluu.persist.ldap.operation.impl.LdapOperationsServiceImpl;
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

	private static final Logger log = LoggerFactory.getLogger(LdapEntryManager.class);

	private static final LdapFilterConverter ldapFilterConverter = new LdapFilterConverter();
	private static final LdapSearchScopeConverter ldapSearchScopeConverter = new LdapSearchScopeConverter();

	private transient LdapOperationsServiceImpl ldapOperationService;
	private transient List<DeleteNotifier> subscribers;

	public LdapEntryManager() {}

	protected LdapEntryManager(LdapOperationsServiceImpl ldapOperationService) {
		this.ldapOperationService = ldapOperationService;
		subscribers = new LinkedList<DeleteNotifier>();
	}

	@Override
	public boolean destroy() {
		boolean destroyResult = this.ldapOperationService.destroy();
		
		return destroyResult;
	}

	public LdapOperationsServiceImpl getOperationService() {
		return ldapOperationService;
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
	protected <T> void updateMergeChanges(T entry, boolean isSchemaUpdate, Class<?> entryClass,
			Map<String, AttributeData> attributesFromLdapMap,
			List<AttributeDataModification> attributeDataModifications) {
		// Update object classes if entry contains custom object classes
		if (getSupportedLDAPVersion() > 2) {
			if (!isSchemaUpdate) {
				String[] objectClasses = getObjectClasses(entry, entryClass);
				String[] objectClassesFromLdap = attributesFromLdapMap.get(OBJECT_CLASS.toLowerCase()).getValues();

				if (!Arrays.equals(objectClassesFromLdap, objectClasses)) {
					attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE, new AttributeData(
							OBJECT_CLASS, objectClasses), new AttributeData(OBJECT_CLASS, objectClassesFromLdap)));
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
			String attributeValues[] = attribute.getValues();

            if (ArrayHelper.isNotEmpty(attributeValues) && StringHelper.isNotEmpty(attributeValues[0])) {
                if (ldapOperationService.isCertificateAttribute(attributeName)) {
					byte[][] binaryValues = toBinaryValues(attributeValues);

					ldapAttributes.add(new Attribute(attributeName + ";binary", binaryValues));
				} else {
					ldapAttributes.add(new Attribute(attributeName, attributeValues));
				}
			}
		}

		// Persist entry
		try {
			boolean result = this.ldapOperationService.addEntry(dn, ldapAttributes);
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
				boolean result = this.ldapOperationService.updateEntry(dn, modifications);
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
			this.ldapOperationService.delete(dn);
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
            if (this.ldapOperationService.getConnectionProvider().isSupportsSubtreeDeleteRequestControl()) {
	            for (DeleteNotifier subscriber : subscribers) {
	                subscriber.onBeforeRemove(dn);
	            }
	            	this.ldapOperationService.deleteWithSubtree(dn);
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
    		searchResult = this.ldapOperationService.search(dn, toLdapFilter(Filter.createPresenceFilter("objectClass")), 0, 0, null, "dn");
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
	protected List<AttributeData> find(String dn, String ... ldapReturnAttributes) {
		// Load entry

		try {
			SearchResultEntry entry = this.ldapOperationService.lookup(dn, ldapReturnAttributes);
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
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes, BatchOperation<T> batchOperation, int startIndex, int sizeLimit, int chunkSize) {
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
			LdapBatchOperationWraper<T> batchOperationWraper = new LdapBatchOperationWraper<T>(batchOperation, this, entryClass, propertiesAnnotations);
			searchResult = this.ldapOperationService.search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(scope),  batchOperationWraper, startIndex, chunkSize, sizeLimit, null, currentLdapReturnAttributes);

			if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
				throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
			}
		} catch (Exception ex) {
			throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter),
					ex);
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
	public <T> ListViewResponse<T> findListViewResponse(String baseDN, Class<T> entryClass, Filter filter, int startIndex, int count, int chunkSize, String sortBy, SortOrder sortOrder, String[] ldapReturnAttributes) {
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

			searchResult = this.ldapOperationService.searchSearchResult(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB), startIndex, count, chunkSize, sortBy, sortOrder, vlvResponse, currentLdapReturnAttributes);

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

		List<T> entries = createEntitiesVirtualListView(entryClass, propertiesAnnotations, searchResult.getSearchEntries().toArray(new SearchResultEntry[searchResult.getSearchEntries().size()]));
		vlvResponse.setResult(entries);

		return vlvResponse;
	}

	@Deprecated
	public <T> List<T> findEntriesVirtualListView(String baseDN, Class<T> entryClass, Filter filter, int startIndex, int count, String sortBy, SortOrder sortOrder, ListViewResponse vlvResponse, String[] ldapReturnAttributes) {

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

			searchResult = this.ldapOperationService.searchVirtualListView(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB), startIndex, count, sortBy, sortOrder, vlvResponse, currentLdapReturnAttributes);

			if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
				throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
			}

		} catch (Exception ex) {
			throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
		}

		if (searchResult.getEntryCount() == 0) {
			return new ArrayList<T>(0);
		}

		List<T> entries = createEntitiesVirtualListView(entryClass, propertiesAnnotations, searchResult.getSearchEntries().toArray(new SearchResultEntry[searchResult.getSearchEntries().size()]));

		return entries;
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
			searchResult = this.ldapOperationService.search(baseDN, toLdapFilter(searchFilter), 1, 1, null, ldapReturnAttributes);
			if ((searchResult == null) || !ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
				throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
			}
		} catch (SearchException ex) {
			if (!(ResultCode.NO_SUCH_OBJECT_INT_VALUE == ex.getResultCode())) {
				throw new EntryPersistenceException(
						String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
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
	private <T> List<T> createEntitiesVirtualListView(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, SearchResultEntry... searchResultEntries) {

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
			if (log.isTraceEnabled()) {
				if (attribute.needsBase64Encoding()) {
					log.trace("Found binary attribute: " + attributeName + ". Is defined in LDAP config: " + ldapOperationService.isBinaryAttribute(attributeName));
				}
			}

			attributeValueStrings = attribute.getValues();
			if (attribute.needsBase64Encoding()) {
				boolean binaryAttribute = ldapOperationService.isBinaryAttribute(attributeName);
				boolean certificateAttribute =  ldapOperationService.isCertificateAttribute(attributeName);

				if (binaryAttribute || certificateAttribute) {
					byte[][] attributeValues = attribute.getValueByteArrays();
					if (attributeValues != null) {
						attributeValueStrings = new String[attributeValues.length];
						for (int i = 0; i < attributeValues.length; i++) {
							attributeValueStrings[i] = Base64.encodeBase64String(attributeValues[i]);
	                        log.trace("Binary attribute: " + attribute.getName() + " value (hex): " + org.apache.commons.codec.binary.Hex.encodeHexString(attributeValues[i]) +
	                                 " value (base64): " + attributeValueStrings[i]);
						}
					}
				}
				if (certificateAttribute) {
					attributeName = ldapOperationService.getCertificateAttributeName(attributeName);
				}
			}
			
			AttributeData tmpAttribute = new AttributeData(attributeName, attributeValueStrings);
			result.add(tmpAttribute);
		}

		return result;
	}

	@Override
	public boolean authenticate(String userName, String password, String baseDN) {
		try {
			return ldapOperationService.authenticate(userName, password, baseDN);
		} catch (ConnectionException ex) {
			throw new AuthenticationException(String.format("Failed to authenticate user: %s", userName), ex);
		} catch (SearchException ex) {
			throw new AuthenticationException(String.format("Failed to find user DN: %s", userName), ex);
		}
	}

	@Override
	public boolean authenticate(String bindDn, String password) {
		try {
			return ldapOperationService.authenticate(bindDn, password);
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
			LdapBatchOperationWraper<T> batchOperationWraper = new LdapBatchOperationWraper<T>(batchOperation);
			ldapOperationService.search(baseDN, toLdapFilter(searchFilter), toLdapSearchScope(SearchScope.SUB), batchOperationWraper, 0, 100, 0, null, ldapReturnAttributes);
		} catch (Exception ex) {
			throw new EntryPersistenceException(String.format("Failed to calucalte count of entries with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
		}

		return batchOperation.getCountEntries();
	}

	@Override
	public String encodeGeneralizedTime(Date date) {
		if (date == null) {
			return null;
		}

		return StaticUtils.encodeGeneralizedTime(date);
	}

	@Override
	public Date decodeGeneralizedTime(String date) {
		if (date == null) {
			return null;
		}

		try {
			return StaticUtils.decodeGeneralizedTime(date);
		} catch (ParseException ex) {
			log.error("Failed to parse generalized time {}", date, ex);
		}

		return null;
	}

	public boolean loadLdifFileContent(String ldifFileContent) {
		LDAPConnection connection = null;
		try {
			connection = ldapOperationService.getConnection();
			ResultCode result = LdifDataUtility.instance().importLdifFileContent(connection, ldifFileContent);
			return ResultCode.SUCCESS.equals(result);
		} catch (Exception ex) {
			log.error("Failed to load ldif file", ex);
			return false;
		} finally {
			if (connection != null) {
				ldapOperationService.releaseConnection(connection);
			}
		}
	}

	@Override
	public String[] getLDIF(String dn) {
		String[] ldif = null;
		try {
			ldif = this.ldapOperationService.lookup(dn).toLDIF();
		} catch (ConnectionException e) {
			log.error("Failed get ldif from " + dn, e);
		}
		;
		return ldif;
	}

	@Override
	public List<String[]> getLDIF(String dn, String[] attributes) {
		SearchResult searchResult;
		try {
			searchResult = this.ldapOperationService.search(dn, toLdapFilter(Filter.create("objectclass=*")), toLdapSearchScope(SearchScope.BASE), -1, 0, null, attributes);
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
			searchResult = this.ldapOperationService.search(baseDN, toLdapFilter(searchFilter), -1, 0, null, attributes);
			if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
				throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter));
			}
		} catch (Exception ex) {
			throw new EntryPersistenceException(String.format("Failed to find entries with baseDN: %s, filter: %s", baseDN, searchFilter),
					ex);
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

	public int getSupportedLDAPVersion() {
		return this.ldapOperationService.getSupportedLDAPVersion();
	}

	private Modification createModification(final ModificationType modificationType, final String attributeName,
			final String... attributeValues) {
		String realAttributeName = attributeName;
		if (ldapOperationService.isCertificateAttribute(realAttributeName)) {
			realAttributeName += ";binary";
			byte[][] binaryValues = toBinaryValues(attributeValues);

			return new Modification(modificationType, realAttributeName, binaryValues);
		}

		return new Modification(modificationType, realAttributeName, attributeValues);
	}

	private com.unboundid.ldap.sdk.Filter toLdapFilter(Filter genericFilter) throws SearchException {
		return ldapFilterConverter.convertToLdapFilter(genericFilter);
	}

	private com.unboundid.ldap.sdk.SearchScope toLdapSearchScope(SearchScope scope) throws SearchScopeException {
		return ldapSearchScopeConverter.convertToLdapSearchScope(scope);
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

}
