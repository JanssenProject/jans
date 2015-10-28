/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.exception.ConnectionException;
import org.gluu.site.ldap.persistence.AttributeDataModification.AttributeModificationType;
import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.gluu.site.ldap.persistence.exception.AuthenticationException;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.gluu.site.ldap.persistence.exception.InvalidArgumentException;
import org.gluu.site.ldap.persistence.exception.MappingException;
import org.gluu.site.ldap.persistence.property.Getter;
import org.gluu.site.ldap.persistence.property.Setter;
import org.gluu.site.ldap.persistence.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.ArrayHelper;
import org.xdi.util.StringHelper;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.util.StaticUtils;

/**
 * LDAP Entry Manager
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class LdapEntryManager extends AbstractEntryManager implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2544614410981223105L;

	private static final Logger log = LoggerFactory.getLogger(LdapEntryManager.class);

	private static final Class<?>[] GROUP_BY_ALLOWED_DATA_TYPES = { String.class, Date.class, Integer.class, LdapEnum.class };
	private static final Class<?>[] SUM_BY_ALLOWED_DATA_TYPES = { int.class, Integer.class, float.class, Float.class, double.class,
			Double.class };
	private static final String[] NO_STRINGS = new String[0];

	private transient OperationsFacade ldapOperationService;
	private transient List<DeleteNotifier> subscribers;

	public LdapEntryManager(OperationsFacade ldapOperationService) {
		this.ldapOperationService = ldapOperationService;
		subscribers = new LinkedList<DeleteNotifier>();
	}

	public boolean destroy() {
		boolean destroyResult = this.ldapOperationService.destroy();
		
		return destroyResult;
	}

	public OperationsFacade getLdapOperationService() {
		return ldapOperationService;
	}

	public void addDeleteSubscriber(DeleteNotifier subscriber) {
		subscribers.add(subscriber);
	}

	public void removerDeleteSubscriber(DeleteNotifier subscriber) {
		subscribers.remove(subscriber);
	}

	@Override
	protected void persist(String dn, List<AttributeData> attributes) {
		List<Attribute> ldapAttributes = new ArrayList<Attribute>(attributes.size());
		for (AttributeData attribute : attributes) {
			if (ArrayHelper.isNotEmpty(attribute.getValues()) && StringHelper.isNotEmpty(attribute.getValues()[0])) {
				ldapAttributes.add(new Attribute(attribute.getName(), attribute.getValues()));
			}
		}

		// Persist entry
		try {
			boolean result = this.ldapOperationService.addEntry(dn, ldapAttributes);
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
			List<Modification> modifications = new ArrayList<Modification>(attributeDataModifications.size());
			for (AttributeDataModification attributeDataModification : attributeDataModifications) {
				AttributeData attribute = attributeDataModification.getAttribute();
				AttributeData oldAttribute = attributeDataModification.getOldAttribute();
				Modification modification = null;
				if (AttributeModificationType.ADD.equals(attributeDataModification.getModificationType())) {
					modification = new Modification(ModificationType.ADD, attribute.getName(), attribute.getValues());
				} else if (AttributeModificationType.REMOVE.equals(attributeDataModification.getModificationType())) {
					modification = new Modification(ModificationType.DELETE, oldAttribute.getName(), oldAttribute.getValues());
				} else if (AttributeModificationType.REPLACE.equals(attributeDataModification.getModificationType())) {
					if ((attribute.getValues().length == 1) || (OBJECT_CLASS.equalsIgnoreCase(attribute.getName()))) {
						modification = new Modification(ModificationType.REPLACE, attribute.getName(), attribute.getValues());
					} else {
						String[] oldValues = oldAttribute.getValues().clone();
						String[] newValues = attribute.getValues().clone();

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
							Modification removeModification = new Modification(ModificationType.DELETE, attribute.getName(),
									removeValues.toArray(new String[removeValues.size()]));
							modifications.add(removeModification);
						}

						if (addValues.size() > 0) {
							Modification addModification = new Modification(ModificationType.ADD, attribute.getName(),
									addValues.toArray(new String[addValues.size()]));
							modifications.add(addModification);
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
		} catch (LDAPException e) {
            throw new EntryPersistenceException(String.format("Failed to update entry: %s", dn), e);
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
    public void removeWithSubtree(String dn) {
        try {
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onBeforeRemove(dn);
            }
            this.ldapOperationService.deleteWithSubtree(dn);
            for (DeleteNotifier subscriber : subscribers) {
                subscriber.onAfterRemove(dn);
            }
        } catch (Exception ex) {
            throw new EntryPersistenceException(String.format("Failed to remove entry: %s", dn), ex);
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

	public <T> List<T> findEntries(Object entry) {
		return findEntries(entry, 0);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findEntries(Object entry, int sizeLimit) {
		if (entry == null) {
			throw new MappingException("Entry to find is null");
		}

		// Check entry class
		Class<T> entryClass = (Class<T>) entry.getClass();
		checkEntryClass(entryClass, false);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);
		Filter searchFilter = createFilterByEntry(entry, entryClass, attributes);

		return findEntries(dnValue.toString(), entryClass, searchFilter, null, sizeLimit);
	}

	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter) {
		return findEntries(baseDN, entryClass, filter, null, 0);
	}

	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, String[] ldapReturnAttributes, Filter filter) {
		return findEntries(baseDN, entryClass, filter, ldapReturnAttributes, 0);
	}

	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, int sizeLimit) {
		return findEntries(baseDN, entryClass, filter, null, sizeLimit);
	}

	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, int sizeLimit) {
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
			searchResult = this.ldapOperationService.search(baseDN, searchFilter, sizeLimit, null, currentLdapReturnAttributes);
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

	public <T> boolean contains(String baseDN, Class<T> entryClass, Filter filter) {
		// Check entry class
		checkEntryClass(entryClass, false);
		String[] objectClasses = getTypeObjectClasses(entryClass);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
		String[] ldapReturnAttributes = getLdapAttributes(null, propertiesAnnotations, false);

		return contains(baseDN, filter, objectClasses, ldapReturnAttributes);
	}

	protected boolean contains(String baseDN, List<AttributeData> attributes, String[] objectClasses, String... ldapReturnAttributes) {
		Filter[] attributesFilters = createAttributesFilter(attributes);
		Filter attributesFilter = null;
		if (attributesFilters != null) {
			attributesFilter = Filter.createANDFilter(attributesFilters);
		}

		return contains(baseDN, attributesFilter, objectClasses, ldapReturnAttributes);
	}

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
			searchResult = this.ldapOperationService.search(baseDN, searchFilter, 1, null, ldapReturnAttributes);
			if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
				throw new EntryPersistenceException(String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter));
			}
		} catch (LDAPSearchException ex) {
			if (!ResultCode.NO_SUCH_OBJECT.equals(ex.getResultCode())) {
				throw new EntryPersistenceException(
						String.format("Failed to find entry with baseDN: %s, filter: %s", baseDN, searchFilter), ex);
			}
		}

		return (searchResult != null) && (searchResult.getEntryCount() > 0);
	}

	private Filter addObjectClassFilter(Filter filter, String[] objectClasses) {
		if (objectClasses.length == 0) {
			return filter;
		}

		Filter[] objectClassFilter = new Filter[objectClasses.length];
		for (int i = 0; i < objectClasses.length; i++) {
			objectClassFilter[i] = Filter.createEqualityFilter(OBJECT_CLASS, objectClasses[i]);
		}
		Filter searchFilter = Filter.createANDFilter(objectClassFilter);
		if (filter != null) {
			searchFilter = Filter.createANDFilter(Filter.createANDFilter(objectClassFilter), filter);
		}
		return searchFilter;
	}

	private <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
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

	private Filter[] createAttributesFilter(List<AttributeData> attributes) {
		if ((attributes == null) || (attributes.size() == 0)) {
			return null;
		}

		List<Filter> results = new ArrayList<Filter>(attributes.size());

		for (AttributeData attribute : attributes) {
			String attributeName = attribute.getName();
			for (String value : attribute.getValues()) {
				Filter filter = Filter.createEqualityFilter(attributeName, value);
				results.add(filter);
			}
		}

		return results.toArray(new Filter[results.size()]);
	}

	private List<AttributeData> getAttributeDataList(SearchResultEntry entry) {
		if (entry == null) {
			return null;
		}

		List<AttributeData> result = new ArrayList<AttributeData>();
		for (Attribute attribute : entry.getAttributes()) {
			String[] attributeValueStrings = NO_STRINGS;
			String attributeName = attribute.getName();

			if (attribute.needsBase64Encoding() && ldapOperationService.isBinaryAttribute(StringHelper.toLowerCase(attributeName))) {
				byte[][] attributeValues = attribute.getValueByteArrays();
				if (attributeValues != null) {
					attributeValueStrings = new String[attributeValues.length];
					for (int i = 0; i < attributeValues.length; i++) {
						attributeValueStrings[i] = Base64.encodeBase64String(attributeValues[i]);
					}
				}
			} else {
				attributeValueStrings = attribute.getValues();
			}
			
			AttributeData tmpAttribute = new AttributeData(attribute.getName(), attributeValueStrings);
			result.add(tmpAttribute);
		}

		return result;
	}

	public boolean authenticate(String userName, String password, String baseDN) {
		try {
			return ldapOperationService.authenticate(userName, password, baseDN);
		} catch (ConnectionException ex) {
			throw new AuthenticationException(String.format("Failed to authenticate user: %s", userName), ex);
		}
	}

	public boolean authenticate(String bindDn, String password) {
		try {
			return ldapOperationService.authenticate(bindDn, password);
		} catch (ConnectionException ex) {
			throw new AuthenticationException(String.format("Failed to authenticate dn: %s", bindDn), ex);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> int countEntries(Object entry) {
		if (entry == null) {
			throw new MappingException("Entry to count is null");
		}

		// Check entry class
		Class<T> entryClass = (Class<T>) entry.getClass();
		checkEntryClass(entryClass, false);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);
		Filter searchFilter = createFilterByEntry(entry, entryClass, attributes);

		return countEntries(dnValue.toString(), entryClass, searchFilter);
	}

	public <T> int countEntries(String baseDN, Class<T> entryClass, Filter filter) {
		if (StringHelper.isEmptyString(baseDN)) {
			throw new MappingException("Base DN to find entries is null");
		}

		// Check entry class
		checkEntryClass(entryClass, false);
		String[] objectClasses = getTypeObjectClasses(entryClass);
		String[] ldapReturnAttributes = new String[] { "" }; // Don't load
																// attributes

		// Find entries
		Filter searchFilter;
		if (objectClasses.length > 0) {
			searchFilter = addObjectClassFilter(filter, objectClasses);
		} else {
			searchFilter = filter;
		}

		int countEntries = 0;
		ASN1OctetString cookie = null;
		SearchResult searchResult = null;
		do {
			Control[] controls = new Control[] { new SimplePagedResultsControl(100, cookie) };
			try {
				searchResult = this.ldapOperationService.search(baseDN, searchFilter, 0, controls, ldapReturnAttributes);
				if (!ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
					throw new EntryPersistenceException(String.format("Failed to calculate count entries with baseDN: %s, filter: %s",
							baseDN, searchFilter));
				}
			} catch (Exception ex) {
				throw new EntryPersistenceException(String.format("Failed to calculate count entries with baseDN: %s, filter: %s", baseDN,
						searchFilter), ex);
			}

			countEntries += searchResult.getEntryCount();
			// Break infinite loop since cookie isn't empty after reaches end of
			// list
			if ((countEntries == 0) || ((countEntries % 100) != 0)) {
				break;
			}

			cookie = null;
			for (Control control : searchResult.getResponseControls()) {
				if (control instanceof SimplePagedResultsControl) {
					cookie = ((SimplePagedResultsControl) control).getCookie();
					break;
				}
			}
		} while (cookie != null);

		return countEntries;
	}

	private <T> Filter createFilterByEntry(Object entry, Class<T> entryClass, List<AttributeData> attributes) {
		Filter[] attributesFilters = createAttributesFilter(attributes);
		Filter attributesFilter = null;
		if (attributesFilters != null) {
			attributesFilter = Filter.createANDFilter(attributesFilters);
		}

		String[] objectClasses = getCustomObjectClasses(entry, entryClass);
        return addObjectClassFilter(attributesFilter, objectClasses);
	}

	public <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String... sortByProperties) {
		// Check input parameters
		if (entries == null) {
			throw new MappingException("Entries list to sort is null");
		}

		if (entries.size() == 0) {
			return;
		}

		if ((sortByProperties == null) || (sortByProperties.length == 0)) {
			throw new InvalidArgumentException("Invalid list of sortBy properties " + Arrays.toString(sortByProperties));
		}

		// Get getters for all properties
		Getter[][] propertyGetters = new Getter[sortByProperties.length][];
		for (int i = 0; i < sortByProperties.length; i++) {
			String[] tmpProperties = sortByProperties[i].split("\\.");
			propertyGetters[i] = new Getter[tmpProperties.length];
			Class<?> currentEntryClass = entryClass;
			for (int j = 0; j < tmpProperties.length; j++) {
				if (j > 0) {
					currentEntryClass = propertyGetters[i][j - 1].getReturnType();
				}
				propertyGetters[i][j] = getGetter(currentEntryClass, tmpProperties[j]);
			}

			if (propertyGetters[i][tmpProperties.length - 1] == null) {
				throw new MappingException("Entry should has getteres for all properties " + sortByProperties[i]);
			}

                        Class<?> propertyType = propertyGetters[i][tmpProperties.length - 1].getReturnType();
			if (!((propertyType == String.class) || (propertyType == Date.class) || (propertyType == Integer.class) || (propertyType == Integer.TYPE))) {
				throw new MappingException("Entry properties should has String, Date or Integer type. Property: '" + tmpProperties[tmpProperties.length - 1] + "'");
			}
		}

		PropertyComparator<T> comparator = new PropertyComparator<T>(propertyGetters, caseSensetive);
		Collections.sort(entries, comparator);
	}

	public <T> void sortListByProperties(Class<T> entryClass, List<T> entries, String... sortByProperties) {
		sortListByProperties(entryClass, entries, false, sortByProperties);
	}

	private static final class PropertyComparator<T> implements Comparator<T>, Serializable {

		private static final long serialVersionUID = 574848841116711467L;
		private Getter[][] propertyGetters;
		private boolean caseSensetive;

		private PropertyComparator(Getter[][] propertyGetters, boolean caseSensetive) {
			this.propertyGetters = propertyGetters;
			this.caseSensetive = caseSensetive;
		}

		public int compare(T entry1, T entry2) {
			if ((entry1 == null) && (entry2 == null)) {
				return 0;
			}
			if ((entry1 == null) && (entry2 != null)) {
				return -1;
			} else if ((entry1 != null) && (entry2 == null)) {
				return 1;
			}

			int result = 0;
			for (Getter[] curPropertyGetters : propertyGetters) {
				result = compare(entry1, entry2, curPropertyGetters);
				if (result != 0) {
					break;
				}
			}

			return result;
		}

		public int compare(T entry1, T entry2, Getter[] propertyGetters) {
			Object value1 = ReflectHelper.getPropertyValue(entry1, propertyGetters);
			Object value2 = ReflectHelper.getPropertyValue(entry2, propertyGetters);

			if ((value1 == null) && (value2 == null)) {
				return 0;
			}
			if ((value1 == null) && (value2 != null)) {
				return -1;
			} else if ((value1 != null) && (value2 == null)) {
				return 1;
			}

			if (value1 instanceof Date) {
				return ((Date) value1).compareTo((Date) value2);
			} else if (value1 instanceof Integer) {
				return ((Integer) value1).compareTo((Integer) value2);
			} else if (value1 instanceof String && value2 instanceof String) {
				if (caseSensetive) {
					return ((String) value1).compareTo((String) value2);
				} else {
					return ((String) value1).toLowerCase().compareTo(((String) value2).toLowerCase());
				}
			}
            return 0;
		}
	}

	public <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive, String groupByProperties,
			String sumByProperties) {
		// Check input parameters
		if (entries == null) {
			throw new MappingException("Entries list to group is null");
		}

		if (entries.size() == 0) {
			return new HashMap<T, List<T>>(0);
		}

		if (StringHelper.isEmpty(groupByProperties)) {
			throw new InvalidArgumentException("List of groupBy properties is null");
		}

		// Get getters for all properties
		Getter[] groupPropertyGetters = getEntryPropertyGetters(entryClass, groupByProperties, GROUP_BY_ALLOWED_DATA_TYPES);
		Setter[] groupPropertySetters = getEntryPropertySetters(entryClass, groupByProperties, GROUP_BY_ALLOWED_DATA_TYPES);
		Getter[] sumPropertyGetters = getEntryPropertyGetters(entryClass, sumByProperties, SUM_BY_ALLOWED_DATA_TYPES);
		Setter[] sumPropertySetter = getEntryPropertySetters(entryClass, sumByProperties, SUM_BY_ALLOWED_DATA_TYPES);

		return groupListByPropertiesImpl(entryClass, entries, caseSensetive, groupPropertyGetters, groupPropertySetters,
				sumPropertyGetters, sumPropertySetter);
	}

	private <T> Getter[] getEntryPropertyGetters(Class<T> entryClass, String properties, Class<?>[] allowedTypes) {
		if (StringHelper.isEmpty(properties)) {
			return null;
		}

		String[] tmpProperties = properties.split("\\,");
		Getter[] propertyGetters = new Getter[tmpProperties.length];
		for (int i = 0; i < tmpProperties.length; i++) {
			propertyGetters[i] = getGetter(entryClass, tmpProperties[i].trim());

			if (propertyGetters[i] == null) {
				throw new MappingException("Entry should has getter for property " + tmpProperties[i]);
			}

			Class<?> returnType = propertyGetters[i].getReturnType();
			boolean found = false;
			for (Class<?> clazz : allowedTypes) {
				if (ReflectHelper.assignableFrom(returnType, clazz)) {
					found = true;
					break;
				}
			}

			if (!found) {
				throw new MappingException("Entry property getter should has next data types " + Arrays.toString(allowedTypes));
			}
		}

		return propertyGetters;
	}

	private <T> Setter[] getEntryPropertySetters(Class<T> entryClass, String properties, Class<?>[] allowedTypes) {
		if (StringHelper.isEmpty(properties)) {
			return null;
		}

		String[] tmpProperties = properties.split("\\,");
		Setter[] propertySetters = new Setter[tmpProperties.length];
		for (int i = 0; i < tmpProperties.length; i++) {
			propertySetters[i] = getSetter(entryClass, tmpProperties[i].trim());

			if (propertySetters[i] == null) {
				throw new MappingException("Entry should has setter for property " + tmpProperties[i]);
			}

			Class<?> paramType = ReflectHelper.getSetterType(propertySetters[i]);
			boolean found = false;
			for (Class<?> clazz : allowedTypes) {
				if (ReflectHelper.assignableFrom(paramType, clazz)) {
					found = true;
					break;
				}
			}

			if (!found) {
				throw new MappingException("Entry property setter should has next data types " + Arrays.toString(allowedTypes));
			}
		}

		return propertySetters;
	}

	public <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, String groupByProperties, String sumByProperties) {
		return groupListByProperties(entryClass, entries, false, groupByProperties, sumByProperties);
	}

	private <T> Map<T, List<T>> groupListByPropertiesImpl(Class<T> entryClass, List<T> entries, boolean caseSensetive,
			Getter[] groupPropertyGetters, Setter[] groupPropertySetters, Getter[] sumProperyGetters, Setter[] sumPropertySetter) {
		Map<String, T> keys = new HashMap<String, T>();
		Map<T, List<T>> groups = new IdentityHashMap<T, List<T>>();

		for (T entry : entries) {
			String key = getEntryKey(entry, caseSensetive, groupPropertyGetters);

			T entryKey = keys.get(key);
			if (entryKey == null) {
				try {
					entryKey = ReflectHelper.createObjectByDefaultConstructor(entryClass);
				} catch (Exception ex) {
					throw new MappingException(String.format("Entry %s should has default constructor", entryClass), ex);
				}
				try {
					ReflectHelper.copyObjectPropertyValues(entry, entryKey, groupPropertyGetters, groupPropertySetters);
				} catch (Exception ex) {
					throw new MappingException("Failed to set values in group Entry", ex);
				}
				keys.put(key, entryKey);
			}

			List<T> groupValues = groups.get(entryKey);
			if (groupValues == null) {
				groupValues = new ArrayList<T>();
				groups.put(entryKey, groupValues);
			}

			try {
				if (sumProperyGetters != null) {
					ReflectHelper.sumObjectPropertyValues(entryKey, entry, sumProperyGetters, sumPropertySetter);
				}
			} catch (Exception ex) {
				throw new MappingException("Failed to sum values in group Entry", ex);
			}

			groupValues.add(entry);
		}

		return groups;
	}

	public String encodeGeneralizedTime(Date date) {
		if (date == null) {
			return null;
		}

		return StaticUtils.encodeGeneralizedTime(date);
	}

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

	public List<String[]> getLDIF(String dn, String[] attributes) {
		SearchResult searchResult;
		try {
			searchResult = this.ldapOperationService.search(dn, Filter.create("objectclass=*"), SearchScope.BASE, -1, null, attributes);
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

	public List<String[]> getLDIFTree(String baseDN, Filter searchFilter, String... attributes) {
		SearchResult searchResult;
		try {
			searchResult = this.ldapOperationService.search(baseDN, searchFilter, -1, null, attributes);
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

	/**
	 * @param dnForPerson
	 * @param class1
	 * @param attribute
	 */
	public <T> void  removeAttributeFromEntries(String baseDN, Class<T> entryClass, String attributeName) {
		try {
			SearchResult searchResult = this.ldapOperationService.search(baseDN, null, 0, null, "dn");
		} catch (LDAPSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
