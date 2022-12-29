/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.annotation.*;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.InvalidArgumentException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.extension.PersistenceExtension;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.LocalizedString;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.orm.reflect.property.Getter;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.property.Setter;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterProcessor;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.jans.orm.model.base.LocalizedString.*;

/**
 * Abstract Entry Manager
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public abstract class BaseEntryManager<O extends PersistenceOperationService> implements PersistenceEntryManager {

	private static final Logger LOG = LoggerFactory.getLogger(BaseEntryManager.class);

	private static final Class<?>[] LDAP_ENTRY_TYPE_ANNOTATIONS = { DataEntry.class, SchemaEntry.class,
			ObjectClass.class };
	private static final Class<?>[] LDAP_ENTRY_PROPERTY_ANNOTATIONS = { AttributeName.class, AttributesList.class,
			JsonObject.class, LanguageTag.class };
	private static final Class<?>[] LDAP_CUSTOM_OBJECT_CLASS_PROPERTY_ANNOTATION = { CustomObjectClass.class };
	private static final Class<?>[] LDAP_DN_PROPERTY_ANNOTATION = { DN.class };
	private static final Class<?>[] LDAP_EXPIRATION_PROPERTY_ANNOTATION = { Expiration.class };

	public static final String OBJECT_CLASS = "objectClass";
	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Class<?>[] GROUP_BY_ALLOWED_DATA_TYPES = { String.class, Date.class, Integer.class,
			AttributeEnum.class };
	private static final Class<?>[] SUM_BY_ALLOWED_DATA_TYPES = { int.class, Integer.class, float.class, Float.class,
			double.class, Double.class };

	private final Map<String, List<PropertyAnnotation>> classAnnotations = new HashMap<String, List<PropertyAnnotation>>();
	private final Map<String, Getter> classGetters = new HashMap<String, Getter>();
	private final Map<String, Setter> classSetters = new HashMap<String, Setter>();

	private static Object CLASS_ANNOTATIONS_LOCK = new Object();
	private static Object CLASS_SETTERS_LOCK = new Object();
	private static Object CLASS_GETTERS_LOCK = new Object();

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

	protected static final String[] NO_STRINGS = new String[0];
	protected static final Object[] NO_OBJECTS = new Object[0];

	protected static final Comparator<String> LINE_LENGHT_COMPARATOR = new LineLenghtComparator<String>(false);

	protected static final int DEFAULT_PAGINATION_SIZE = 100;

	protected O operationService = null;
	protected PersistenceExtension persistenceExtension = null;

	protected FilterProcessor filterProcessor = new FilterProcessor();

	@Override
	public void persist(Object entry) {
		if (entry == null) {
			throw new MappingException("Entry to persist is null");
		}

		// Check entry class
		Class<?> entryClass = entry.getClass();
		checkEntryClass(entryClass, false);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		Integer expirationValue = getExpirationValue(entry, entryClass, false);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);

		// Add object classes
		String[] objectClasses = getObjectClasses(entry, entryClass);
		attributes.add(new AttributeData(OBJECT_CLASS, objectClasses, true));

		LOG.debug(String.format("LDAP attributes for persist: %s", attributes));

		persist(dnValue.toString(), objectClasses, attributes, expirationValue);
	}

	protected abstract void persist(String dn, String[] objectClasses, List<AttributeData> attributes, Integer expiration);

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> findEntries(Object entry, int count) {
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

		return findEntries(dnValue.toString(), entryClass, searchFilter, SearchScope.SUB, null, 0, count,
				DEFAULT_PAGINATION_SIZE);
	}

	@Override
	public <T> List<T> findEntries(Object entry) {
		return findEntries(entry, 0);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter) {
		return findEntries(baseDN, entryClass, filter, SearchScope.SUB, null, null, 0, 0, 0);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, int count) {
		return findEntries(baseDN, entryClass, filter, SearchScope.SUB, null, null, 0, count, 0);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes) {
		return findEntries(baseDN, entryClass, filter, SearchScope.SUB, ldapReturnAttributes, null, 0, 0, 0);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes,
			int count) {
		return findEntries(baseDN, entryClass, filter, SearchScope.SUB, ldapReturnAttributes, null, 0, count, 0);
	}

	@Override
	public <T> List<T> findEntries(String baseDN, Class<T> entryClass, Filter filter, SearchScope scope,
			String[] ldapReturnAttributes, int start, int count, int chunkSize) {
		return findEntries(baseDN, entryClass, filter, scope, ldapReturnAttributes, null, start, count, chunkSize);
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

	@SuppressWarnings("unchecked")
	protected Void merge(Object entry, boolean isSchemaUpdate, boolean isConfigurationUpdate, AttributeModificationType schemaModificationType) {
		if (entry == null) {
			throw new MappingException("Entry for check if exists is null");
		}

		Class<?> entryClass = entry.getClass();
		checkEntryClass(entryClass, isSchemaUpdate);

		// Determine entry update method
		boolean forceUpdate = isUseEntryForceUpdate(entryClass);

		String[] objectClasses = getObjectClasses(entry, entryClass);

		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

		Object dnValue = getDNValue(entry, entryClass);

		Integer expirationValue = getExpirationValue(entry, entryClass, true);

		List<AttributeData> attributesToPersist = getAttributesListForPersist(entry, propertiesAnnotations);
		Map<String, AttributeData> attributesToPersistMap = getAttributesMap(attributesToPersist);

		// Load entry
		List<AttributeData> attributesFromLdap = null;
		if (isSchemaUpdate || forceUpdate) {
			// If it's schema modification request we don't need to load
			// attributes from LDAP
			attributesFromLdap = new ArrayList<AttributeData>();
		} else {
			List<String> currentLdapReturnAttributesList = buildAttributesListForUpdate(entry, objectClasses, propertiesAnnotations);
			if (!isConfigurationUpdate) {
				currentLdapReturnAttributesList.add("objectClass");
			}

			attributesFromLdap = find(dnValue.toString(), objectClasses, propertiesAnnotationsMap, currentLdapReturnAttributesList.toArray(EMPTY_STRING_ARRAY));
		}

		if (LOG.isTraceEnabled()) {
			dumpAttributes("attributesFromLdap", attributesFromLdap);
			dumpAttributes("attributesToPersist", attributesToPersist);
		}

		Map<String, AttributeData> attributesFromLdapMap = getAttributesMap(attributesFromLdap);

		// Prepare list of modifications

		// Process properties with Attribute annotation
		List<AttributeDataModification> attributeDataModifications = collectAttributeModifications(
				propertiesAnnotations, attributesToPersistMap, attributesFromLdapMap, isSchemaUpdate,
				schemaModificationType, forceUpdate);

		if (LOG.isTraceEnabled()) {
			dumpAttributeDataModifications("attributeDataModifications before updateMergeChanges", attributeDataModifications);
		}

		updateMergeChanges(dnValue.toString(), entry, isSchemaUpdate | isConfigurationUpdate, entryClass, attributesFromLdapMap, attributeDataModifications, forceUpdate);

		if (LOG.isTraceEnabled()) {
			dumpAttributeDataModifications("attributeDataModifications after updateMergeChanges", attributeDataModifications);
		}

		LOG.debug(String.format("LDAP attributes for merge: %s", attributeDataModifications));

		merge(dnValue.toString(), objectClasses, attributeDataModifications, expirationValue);

		return null;
	}

	protected List<String> buildAttributesListForUpdate(Object entry, String[] objectClasses, List<PropertyAnnotation> propertiesAnnotations) {
		return getAttributesList(entry, propertiesAnnotations, false);
	}

	protected abstract <T> void updateMergeChanges(String baseDn, T entry, boolean isConfigurationUpdate, Class<?> entryClass,
			Map<String, AttributeData> attributesFromLdapMap,
			List<AttributeDataModification> attributeDataModifications, boolean forceUpdate);

	protected List<AttributeDataModification> collectAttributeModifications(
			List<PropertyAnnotation> propertiesAnnotations, Map<String, AttributeData> attributesToPersistMap,
			Map<String, AttributeData> attributesFromLdapMap, boolean isSchemaUpdate,
			AttributeModificationType schemaModificationType, boolean forceUpdate) {
		List<AttributeDataModification> attributeDataModifications = new ArrayList<AttributeDataModification>();

		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();
			Annotation ldapAttribute;

			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
					AttributeName.class);
			if (ldapAttribute != null) {
				String ldapAttributeName = ((AttributeName) ldapAttribute).name();
				if (StringHelper.isEmpty(ldapAttributeName)) {
					ldapAttributeName = propertyName;
				}
				ldapAttributeName = ldapAttributeName.toLowerCase();

				AttributeData attributeToPersist = attributesToPersistMap.get(ldapAttributeName);
				AttributeData attributeFromLdap = attributesFromLdapMap.get(ldapAttributeName);

				// Remove processed attributes
				attributesToPersistMap.remove(ldapAttributeName);
				attributesFromLdapMap.remove(ldapAttributeName);

				AttributeName ldapAttributeAnnotation = (AttributeName) ldapAttribute;
				if (ldapAttributeAnnotation.ignoreDuringUpdate()) {
					continue;
				}

				if (attributeFromLdap != null && attributeToPersist != null) {
					// Modify DN entry attribute in DS
					if (!attributeFromLdap.equals(attributeToPersist)) {
						if (isEmptyAttributeValues(attributeToPersist) && !ldapAttributeAnnotation.updateOnly()) {
							attributeDataModifications.add(new AttributeDataModification(
									AttributeModificationType.REMOVE, null, attributeFromLdap));
						} else {
							attributeDataModifications.add(new AttributeDataModification(
									AttributeModificationType.REPLACE, attributeToPersist, attributeFromLdap));
						}
					}
				} else if ((attributeFromLdap == null) && (attributeToPersist != null)) {
					// Add entry attribute or change schema
					if (isSchemaUpdate && (attributeToPersist.getValue() == null
							&& Arrays.equals(attributeToPersist.getValues(), new Object[] {}))) {
						continue;
					}
					AttributeModificationType modType = isSchemaUpdate ? schemaModificationType
							: AttributeModificationType.ADD;
					if (AttributeModificationType.ADD == modType) {
						if (isEmptyAttributeValues(attributeToPersist)) {
							if (forceUpdate) {
								attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE,
										null, attributeToPersist));
							}
						} else {
							modType = forceUpdate ? AttributeModificationType.FORCE_UPDATE : modType;
							attributeDataModifications.add(
									new AttributeDataModification(modType, attributeToPersist));
						}
					} else {
						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE,
								null, attributeToPersist));
					}
				} else if ((attributeFromLdap != null) && (attributeToPersist == null)) {
					// Remove if attribute not marked as ignoreDuringRead = true
					// or updateOnly = true
					if (!ldapAttributeAnnotation.ignoreDuringRead() && !ldapAttributeAnnotation.updateOnly()) {
						if (isEmptyAttributeValues(attributeFromLdap) && isStoreFullEntry()) {
							// It's RDBS case. We don't need to set null to already empty table cell
							continue;
						}
						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE,
								null, attributeFromLdap));
					}
				} else if (forceUpdate && (attributeFromLdap == null) && (attributeToPersist == null)) {
					attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE,
							null, new AttributeData(ldapAttributeName, null)));
				}
			}
		}

		// Process properties with @AttributesList annotation
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			Annotation ldapAttribute;
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
					AttributesList.class);
			if (ldapAttribute != null) {
				Map<String, AttributeName> ldapAttributesConfiguration = new HashMap<String, AttributeName>();
				for (AttributeName ldapAttributeConfiguration : ((AttributesList) ldapAttribute)
						.attributesConfiguration()) {
					ldapAttributesConfiguration.put(ldapAttributeConfiguration.name(), ldapAttributeConfiguration);
				}

				// Prepare attributes for removal
				for (AttributeData attributeFromLdap : attributesFromLdapMap.values()) {
					String attributeName = attributeFromLdap.getName();
					if (OBJECT_CLASS.equalsIgnoreCase(attributeName)) {
						continue;
					}

					AttributeName ldapAttributeConfiguration = ldapAttributesConfiguration.get(attributeName);
					if ((ldapAttributeConfiguration != null) && ldapAttributeConfiguration.ignoreDuringUpdate()) {
						continue;
					}

					if (!attributesToPersistMap.containsKey(attributeName.toLowerCase())) {
						// Remove if attribute not marked as ignoreDuringRead = true
						if ((ldapAttributeConfiguration == null) || ((ldapAttributeConfiguration != null)
								&& !ldapAttributeConfiguration.ignoreDuringRead())) {
							attributeDataModifications.add(new AttributeDataModification(
									AttributeModificationType.REMOVE, null, attributeFromLdap));
						}
					}
				}

				// Prepare attributes for adding and replace
				for (AttributeData attributeToPersist : attributesToPersistMap.values()) {
					String attributeName = attributeToPersist.getName();

					AttributeName ldapAttributeConfiguration = ldapAttributesConfiguration.get(attributeName);
					if ((ldapAttributeConfiguration != null) && ldapAttributeConfiguration.ignoreDuringUpdate()) {
						continue;
					}

					AttributeData attributeFromLdap = attributesFromLdapMap.get(attributeName.toLowerCase());
					if (attributeFromLdap == null) {
						// Add entry attribute or change schema
						AttributeModificationType modType = isSchemaUpdate ? schemaModificationType
								: AttributeModificationType.ADD;
						if (AttributeModificationType.ADD.equals(modType)) {
							if (!isEmptyAttributeValues(attributeToPersist)) {
								attributeDataModifications.add(new AttributeDataModification(
										AttributeModificationType.ADD, attributeToPersist));
							}
						} else {
							attributeDataModifications.add(new AttributeDataModification(
									AttributeModificationType.REMOVE, null, attributeToPersist));
						}
					} else if ((attributeFromLdap != null) && isEmptyAttributeValues(attributeToPersist)) {
						if (isEmptyAttributeValues(attributeFromLdap) && isStoreFullEntry()) {
							// It's RDBS case. We don't need to set null to already empty table cell
							continue;
						}

						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE,
								null, attributeFromLdap));
					} else {
						if (!attributeFromLdap.equals(attributeToPersist)) {
							if (isEmptyAttributeValues(attributeToPersist)
									&& (ldapAttributeConfiguration == null || !ldapAttributeConfiguration.updateOnly())) {
								if (isEmptyAttributeValues(attributeFromLdap) && isStoreFullEntry()) {
									// It's RDBS case. We don't need to set null to already empty table cell
									continue;
								}

								attributeDataModifications.add(new AttributeDataModification(
										AttributeModificationType.REMOVE, null, attributeFromLdap));
							} else {
								attributeDataModifications.add(new AttributeDataModification(
										AttributeModificationType.REPLACE, attributeToPersist, attributeFromLdap));
							}
						}
					}
				}

			}
		}

		return attributeDataModifications;
	}

	protected boolean isStoreFullEntry() {
		return false;
	}

	protected boolean isEmptyAttributeValues(AttributeData attributeData) {
		Object[] attributeToPersistValues = attributeData.getValues();

		return ArrayHelper.isEmpty(attributeToPersistValues)
				|| ((attributeToPersistValues.length == 1) && StringHelper.isEmpty(String.valueOf(attributeToPersistValues[0])));
	}

	protected abstract void merge(String dn, String[] objectClasses, List<AttributeDataModification> attributeDataModifications, Integer expiration);

	public abstract <T> void removeByDn(String dn, String[] objectClasses);

	@Deprecated
	public void remove(String primaryKey) {
		removeByDn(primaryKey, null);
	}

	@Override
	public <T> void remove(String primaryKey, Class<T> entryClass) {
		String[] objectClasses = null;

		if (entryClass != null) {
			// Check entry class
			checkEntryClass(entryClass, false);
			objectClasses = getTypeObjectClasses(entryClass);
		}

		removeByDn(primaryKey, objectClasses);
	}

	public abstract <T> void removeRecursivelyFromDn(String primaryKey, String[] objectClasses);

	@Deprecated
	public void removeRecursively(String primaryKey) {
		removeRecursivelyFromDn(primaryKey, null);
	}

	@Override
	public <T> void removeRecursively(String primaryKey, Class<T> entryClass) {
		String[] objectClasses = null;

		if (entryClass != null) {
			// Check entry class
			checkEntryClass(entryClass, false);
			objectClasses = getTypeObjectClasses(entryClass);
		}

		removeRecursivelyFromDn(primaryKey, objectClasses);
	}

	@Override
	public boolean contains(Object entry) {
		if (entry == null) {
			throw new MappingException("Entry for check if exists is null");
		}

		// Check entry class
		Class<?> entryClass = entry.getClass();
		checkEntryClass(entryClass, false);
		String[] objectClasses = getObjectClasses(entry, entryClass);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);

		String[] ldapReturnAttributes = getAttributes(null, propertiesAnnotations, false);

		return contains(dnValue.toString(), entryClass, propertiesAnnotations, attributes, objectClasses, ldapReturnAttributes);
	}

	protected <T> boolean contains(Class<T> entryClass, String primaryKey, String[] ldapReturnAttributes) {
		if (StringHelper.isEmptyString(primaryKey)) {
			throw new MappingException("DN to find entry is null");
		}

		checkEntryClass(entryClass, true);
		String[] objectClasses = getTypeObjectClasses(entryClass);

		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

        try {
			List<AttributeData> results = find(primaryKey, objectClasses, propertiesAnnotationsMap, ldapReturnAttributes);
			return (results != null) && (results.size() > 0);
		} catch (EntryPersistenceException ex) {
			return false;
		}
	}

	@Override
	public <T> boolean contains(String baseDN, Class<T> entryClass, Filter filter) {
		// Check entry class
		checkEntryClass(entryClass, false);
		String[] objectClasses = getTypeObjectClasses(entryClass);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
		String[] ldapReturnAttributes = getAttributes(null, propertiesAnnotations, false);

		return contains(baseDN, objectClasses, entryClass, propertiesAnnotations, filter, ldapReturnAttributes);
	}

	protected <T> boolean contains(String baseDN, Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, List<AttributeData> attributes, String[] objectClasses,
			String... ldapReturnAttributes) {
		Filter[] attributesFilters = createAttributesFilter(attributes);
		Filter attributesFilter = null;
		if (attributesFilters != null) {
			attributesFilter = Filter.createANDFilter(attributesFilters);
		}

		return contains(baseDN, objectClasses, entryClass, propertiesAnnotations, attributesFilter, ldapReturnAttributes);
	}

	protected abstract <T> boolean contains(String baseDN, String[] objectClasses, Class<T> entryClass,
			List<PropertyAnnotation> propertiesAnnotations, Filter filter, String[] ldapReturnAttributes);

	@Override
	public <T> boolean contains(String primaryKey, Class<T> entryClass) {
		return contains(entryClass, primaryKey, (String[]) null);
	}

	@Override
	public <T> T find(Class<T> entryClass, Object primaryKey) {
		return find(primaryKey, entryClass, null);
	}

	@Override
	public <T> T find(Object primaryKey, Class<T> entryClass, String[] ldapReturnAttributes) {
		if (StringHelper.isEmptyString(primaryKey)) {
			throw new MappingException("DN to find entry is null");
		}

		checkEntryClass(entryClass, true);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = prepareEntryPropertiesTypes(entryClass, propertiesAnnotations);

		return find(entryClass, primaryKey, ldapReturnAttributes, propertiesAnnotations, propertiesAnnotationsMap);
	}

	protected <T> String[] getAttributes(T entry, List<PropertyAnnotation> propertiesAnnotations,
			boolean isIgnoreAttributesList) {
		List<String> attributes = getAttributesList(entry, propertiesAnnotations, isIgnoreAttributesList);

		if (attributes == null) {
			return null;
		}

		return attributes.toArray(new String[0]);
	}

	protected <T> String[] getAttributes(Map<String, PropertyAnnotation> attributesMap) {
		if (attributesMap == null) {
			return null;
		}
		return attributesMap.keySet().toArray(new String[0]);
	}

	protected <T> List<String> getAttributesList(T entry, List<PropertyAnnotation> propertiesAnnotations,
			boolean isIgnoreAttributesList) {
		Map<String, PropertyAnnotation> attributesMap = getAttributesMap(entry, propertiesAnnotations, isIgnoreAttributesList);

		if (attributesMap == null) {
			return null;
		}

		return new ArrayList<String>(attributesMap.keySet());
	}

	protected <T> Map<String, PropertyAnnotation> getAttributesMap(T entry, List<PropertyAnnotation> propertiesAnnotations,
			boolean isIgnoreAttributesList) {
		Map<String, PropertyAnnotation> attributes = new HashMap<String, PropertyAnnotation>();

		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();
			Annotation ldapAttribute;

			if (!isIgnoreAttributesList) {
				ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
						AttributesList.class);
				if (ldapAttribute != null) {
					if (entry == null) {
						return null;
					} else {
						List<AttributeData> attributesList = getAttributeDataListFromCustomAttributesList(entry,
								(AttributesList) ldapAttribute, propertyName);
						for (AttributeData attributeData : attributesList) {
							String ldapAttributeName = attributeData.getName();
							if (!attributes.containsKey(ldapAttributeName)) {
								attributes.put(ldapAttributeName, propertiesAnnotation);
							}
						}
					}
				}
			}

			// Process properties with AttributeName annotation
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
					AttributeName.class);
			if (ldapAttribute != null) {
				String ldapAttributeName = ((AttributeName) ldapAttribute).name();
				if (StringHelper.isEmpty(ldapAttributeName)) {
					ldapAttributeName = propertyName;
				}

				if (!attributes.containsKey(ldapAttributeName)) {
					attributes.put(ldapAttributeName, propertiesAnnotation);
				}
			}
		}

		if (attributes.size() == 0) {
			return null;
		}

		return attributes;
	}

	protected <T> Map<String, PropertyAnnotation> prepareEntryPropertiesTypes(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations) {
        Map<String, PropertyAnnotation> propertiesAnnotationsMap = getAttributesMap(null, propertiesAnnotations, true);
        if (propertiesAnnotationsMap== null) {
        	return new HashMap<String, PropertyAnnotation>(0);
        }

        preparePropertyAnnotationTypes(entryClass, propertiesAnnotationsMap);

        return propertiesAnnotationsMap;
	}

	protected <T> void preparePropertyAnnotationTypes(Class<T> entry, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotationsMap.values()) {
			String propertyName = propertiesAnnotation.getPropertyName();

			Class<?> parameterType = getSetterPropertyType(entry, propertyName);
			propertiesAnnotation.setParameterType(parameterType);
		}
	}

	private <T> Class<?> getSetterPropertyType(Class<T> entry, String propertyName) {
		Setter propertyValueSetter = getSetter(entry, propertyName);
		if (propertyValueSetter == null) {
			throw new MappingException("Entry should has setter for property " + propertyName);
		}

		Class<?> parameterType = ReflectHelper.getSetterType(propertyValueSetter);
		return parameterType;
	}

	private <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes,
			List<PropertyAnnotation> propertiesAnnotations, Map<String, PropertyAnnotation> propertiesAnnotationsMap) {
		Map<String, List<AttributeData>> entriesAttributes = new HashMap<String, List<AttributeData>>();

		String[] currentLdapReturnAttributes = ldapReturnAttributes;
		if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
			currentLdapReturnAttributes = getAttributes(null, propertiesAnnotations, false);
		}

		String[] objectClasses = getTypeObjectClasses(entryClass);
		List<AttributeData> ldapAttributes = find(primaryKey.toString(), objectClasses, propertiesAnnotationsMap, currentLdapReturnAttributes);

		entriesAttributes.put(String.valueOf(primaryKey), ldapAttributes);
		List<T> results = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
		return results.get(0);
	}

	protected abstract List<AttributeData> find(String dn, String[] objectClasses, Map<String, PropertyAnnotation> propertiesAnnotationsMap, String... attributes);

	protected boolean checkEntryClass(Class<?> entryClass, boolean isAllowSchemaEntry) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		Annotation ldapSchemaEntry = ReflectHelper.getAnnotationByType(entryAnnotations, SchemaEntry.class);
		Annotation ldapEntry = ReflectHelper.getAnnotationByType(entryAnnotations, DataEntry.class);
		if (isAllowSchemaEntry) {
			if ((ldapSchemaEntry == null) && (ldapEntry == null)) {
				throw new MappingException(String.format("Entry should has DataEntry or SchemaEntry annotation", entryClass));
			}
		} else {
			if (ldapEntry == null) {
				throw new MappingException(String.format("Entry '%s' should has DataEntry annotation", entryClass));
			}
		}

		return true;
	}

	protected boolean isUseEntryForceUpdate(Class<?> entryClass) {
		if (!isSupportForceUpdate()) {
			return false;
		}

		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		Annotation dataEntry = ReflectHelper.getAnnotationByType(entryAnnotations, DataEntry.class);
		if (dataEntry == null) {
			return false;
		}

		return ((DataEntry) dataEntry).forceUpdate();
	}

	protected boolean isSupportForceUpdate() {
		return false;
	}

	protected boolean isSchemaEntry(Class<?> entryClass) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		return ReflectHelper.getAnnotationByType(entryAnnotations, SchemaEntry.class) != null;
	}

	protected boolean isConfigurationEntry(Class<?> entryClass) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		DataEntry dataEntry = (DataEntry) ReflectHelper.getAnnotationByType(entryAnnotations, DataEntry.class);
		if (dataEntry == null) {
			return false;
		}

		return dataEntry.configurationDefinition();
	}

	protected String[] getEntrySortByProperties(Class<?> entryClass) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);
		Annotation annotation = ReflectHelper.getAnnotationByType(entryAnnotations, DataEntry.class);

		if (annotation == null) {
			return null;
		}

		return ((DataEntry) annotation).sortBy();
	}

	protected String[] getEntrySortByNames(Class<?> entryClass) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);
		Annotation annotation = ReflectHelper.getAnnotationByType(entryAnnotations, DataEntry.class);

		if (annotation == null) {
			return null;
		}

		return ((DataEntry) annotation).sortByName();
	}

	@Override
	public String[] getObjectClasses(Object entry, Class<?> entryClass) {
		String[] typeObjectClasses = getTypeObjectClasses(entryClass);
		String[] customObjectClasses = getCustomObjectClasses(entry, entryClass);

		if (ArrayHelper.isEmpty(typeObjectClasses)) {
			return customObjectClasses;
		}

		String[] mergedArray = ArrayHelper.arrayMerge(typeObjectClasses, customObjectClasses);
		Set<String> objecClassSet = new HashSet<String>();
		objecClassSet.addAll(Arrays.asList(mergedArray));
		return objecClassSet.toArray(new String[0]);
	}

	protected String[] getTypeObjectClasses(Class<?> entryClass) {
		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		// Get object classes
		Annotation ldapObjectClass = ReflectHelper.getAnnotationByType(entryAnnotations, ObjectClass.class);
		if (ldapObjectClass == null) {
			return EMPTY_STRING_ARRAY;
		}

		if (StringHelper.isEmpty(((ObjectClass) ldapObjectClass).value())) {
			return EMPTY_STRING_ARRAY;
		}

		return new String[] { ((ObjectClass) ldapObjectClass).value() };
	}

	protected String[] getCustomObjectClasses(Object entry, Class<?> entryClass) {
		List<String> result = new ArrayList<String>();
		List<PropertyAnnotation> customObjectAnnotations = getEntryCustomObjectClassAnnotations(entryClass);

		for (PropertyAnnotation propertiesAnnotation : customObjectAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();

			Getter getter = getGetter(entryClass, propertyName);
			if (getter == null) {
				throw new MappingException("Entry should has getter for property " + propertyName);
			}

			Class<?> parameterType = getSetterPropertyType(entryClass, propertyName);
			boolean multiValued = isMultiValued(parameterType);

			AttributeData attribute = getAttributeData(propertyName, propertyName, getter, entry, multiValued, false);
			if (attribute != null) {
				for (String objectClass : attribute.getStringValues()) {
					if (objectClass != null) {
						result.add(objectClass);
					}
				}
			}
			break;
		}

		return result.toArray(new String[0]);
	}

	protected void setCustomObjectClasses(Object entry, Class<?> entryClass, String[] objectClasses) {
		List<PropertyAnnotation> customObjectAnnotations = getEntryCustomObjectClassAnnotations(entryClass);

		for (PropertyAnnotation propertiesAnnotation : customObjectAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();

			Setter setter = getSetter(entryClass, propertyName);
			if (setter == null) {
				throw new MappingException("Entry should has setter for property " + propertyName);
			}

			AttributeData attribute = new AttributeData(propertyName, objectClasses);

			setPropertyValue(propertyName, setter, entry, attribute, false);
			break;
		}
	}

	protected String getDNPropertyName(Class<?> entryClass) {
		List<PropertyAnnotation> propertiesAnnotations = getEntryDnAnnotations(entryClass);
		if (propertiesAnnotations.size() == 0) {
			throw new MappingException("Entry should has property with annotation DN");
		}

		if (propertiesAnnotations.size() > 1) {
			throw new MappingException("Entry should has only one property with annotation DN");
		}

		return propertiesAnnotations.get(0).getPropertyName();
	}

	protected PropertyAnnotation getExpirationProperty(Class<?> entryClass) {
		List<PropertyAnnotation> propertiesAnnotations = getEntryExpirationAnnotations(entryClass);
		if (propertiesAnnotations.size() == 0) {
			return null;
		}

		if (propertiesAnnotations.size() > 1) {
			throw new MappingException("Entry should has only one property with annotation Expiration");
		}

		return propertiesAnnotations.get(0);
	}

	protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
			Map<String, List<AttributeData>> entriesAttributes) {
		return createEntities(entryClass, propertiesAnnotations, entriesAttributes, true);
	}

	protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations,
			Map<String, List<AttributeData>> entriesAttributes, boolean doSort) {
		// Check if entry has DN property
		String dnProperty = getDNPropertyName(entryClass);

		// Get DN value
		Setter dnSetter = getSetter(entryClass, dnProperty);
		if (dnSetter == null) {
			throw new MappingException("Entry should has getter for property " + dnProperty);
		}

		// Type object classes
		String[] typeObjectClasses = getTypeObjectClasses(entryClass);
		Arrays.sort(typeObjectClasses);

		List<T> results = new ArrayList<T>(entriesAttributes.size());
		for (Entry<String, List<AttributeData>> entryAttributes : entriesAttributes.entrySet()) {
			String dn = entryAttributes.getKey();
			List<AttributeData> attributes = entryAttributes.getValue();
			Map<String, AttributeData> attributesMap = getAttributesMap(attributes);

			T entry;
			List<String> customObjectClasses = null;
			try {
				Class<?> declaringClass = entryClass.getDeclaringClass();
				if (declaringClass == null) {
					entry = ReflectHelper.createObjectByDefaultConstructor(entryClass);
				} else {
					entry = (T) ReflectHelper.getConstructor(entryClass, declaringClass).newInstance((Object) null);
				}
			} catch (Exception ex) {
				throw new MappingException(String.format("Entry %s should has default constructor", entryClass));
			}
			results.add(entry);

			dnSetter.set(entry, dn);

			// Remove processed DN attribute
			attributesMap.remove(dnProperty);

			// Set loaded properties to entry

			// Process properties with AttributeName annotation
			for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
				String propertyName = propertiesAnnotation.getPropertyName();
				Annotation ldapAttribute;

				ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
						AttributeName.class);
                Annotation languageTag = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LanguageTag.class);
				if (ldapAttribute != null) {
					String ldapAttributeName = ((AttributeName) ldapAttribute).name();
					if (StringHelper.isEmpty(ldapAttributeName)) {
						ldapAttributeName = propertyName;
					}

                    if (languageTag != null) {
                        Getter getter = getGetter(entryClass, propertyName);
                        if (getter == null) {
                            throw new MappingException("Entry should has getter for property " + propertyName);
                        }

                        Object propertyValue = getter.get(entry);
                        if (propertyValue == null) {
                            return null;
                        }

                        if (!(propertyValue instanceof LocalizedString)) {
                            throw new MappingException("Entry property should be LocalizedString");
                        }

                        LocalizedString localizedString = (LocalizedString) propertyValue;
                        final String finalLdapAttributeName = ldapAttributeName.replace(LOCALIZED, EMPTY_LANG_TAG);
                        Map<String, AttributeData> filteredAttrs = attributesMap.entrySet().stream()
                                .filter(x -> x.getKey().toLowerCase().startsWith(finalLdapAttributeName.toLowerCase()))
                                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                        loadLocalizedString(attributesMap, localizedString, filteredAttrs);

                        continue;
                    }

					ldapAttributeName = ldapAttributeName.toLowerCase();

					AttributeData attributeData = attributesMap.get(ldapAttributeName);

					// Remove processed attributes
					attributesMap.remove(ldapAttributeName);

					if (((AttributeName) ldapAttribute).ignoreDuringRead()) {
						continue;
					}

					Setter setter = getSetter(entryClass, propertyName);
					if (setter == null) {
						throw new MappingException("Entry should has setter for property " + propertyName);
					}

					Annotation ldapJsonObject = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
							JsonObject.class);
					boolean jsonObject = ldapJsonObject != null;

					setPropertyValue(propertyName, setter, entry, attributeData, jsonObject);
				}
			}

			// Process properties with @AttributesList annotation
			for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
				String propertyName = propertiesAnnotation.getPropertyName();
				Annotation ldapAttribute;

				ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
						AttributesList.class);
				if (ldapAttribute != null) {
					Map<String, AttributeName> ldapAttributesConfiguration = new HashMap<String, AttributeName>();
					for (AttributeName ldapAttributeConfiguration : ((AttributesList) ldapAttribute)
							.attributesConfiguration()) {
						ldapAttributesConfiguration.put(ldapAttributeConfiguration.name(), ldapAttributeConfiguration);
					}

					// Process objectClass first
					for (Iterator<Entry<String, AttributeData>> it = attributesMap.entrySet().iterator(); it.hasNext();) {
						Entry<String, AttributeData> attributeEntry = it.next();

						AttributeData entryAttribute = attributeEntry.getValue(); 
						if (OBJECT_CLASS.equalsIgnoreCase(entryAttribute.getName())) {
							it.remove();

							String[] objectClasses = entryAttribute.getStringValues();
							if (ArrayHelper.isEmpty(objectClasses)) {
								continue;
							}

							if (customObjectClasses == null) {
								customObjectClasses = new ArrayList<String>();
							}

							for (String objectClass : objectClasses) {
								int idx = Arrays.binarySearch(typeObjectClasses, objectClass, new Comparator<String>() {
									public int compare(String o1, String o2) {
										return o1.toLowerCase().compareTo(o2.toLowerCase());
									}
								});

								if (idx < 0) {
									customObjectClasses.add(objectClass);
								}
							}
						}
					}

					List<Object> propertyValue = getCustomAttributesListFromAttributeData(entryClass, (AttributesList) ldapAttribute, propertyName,
							attributesMap.values(), ldapAttributesConfiguration);

					Setter setter = getSetter(entryClass, propertyName);
					if (setter == null) {
						throw new MappingException("Entry should has setter for property " + propertyName);
					}

					if (doSort) {
						Class<?> entryItemType = ReflectHelper.getListType(setter);
						if (entryItemType == null) {
							throw new MappingException(
									"Entry property " + propertyName + " should has setter with specified element type");
						}
						sortAttributesListIfNeeded((AttributesList) ldapAttribute, entryItemType,
								propertyValue);
					}
					setter.set(entry, propertyValue);
				}
			}

			if ((customObjectClasses != null) && (customObjectClasses.size() > 0)) {
				setCustomObjectClasses(entry, entryClass, customObjectClasses.toArray(new String[0]));
			}
		}

		return results;
	}

	private <T> List<Object> getCustomAttributesListFromAttributeData(Class<T> entryClass, AttributesList attributesList,
			String propertyName, Collection<AttributeData> attributes, Map<String, AttributeName> ldapAttributesConfiguration) {
		List<Object> resultList = new ArrayList<Object>();

		Setter setter = getSetter(entryClass, propertyName);
		if (setter == null) {
			throw new MappingException("Entry should has setter for property " + propertyName);
		}

		Class<?> entryItemType = ReflectHelper.getListType(setter);
		if (entryItemType == null) {
			throw new MappingException(
					"Entry property " + propertyName + " should has setter with specified element type");
		}

		String entryPropertyName = attributesList.name();
		Setter entryPropertyNameSetter = getSetter(entryItemType, entryPropertyName);
		if (entryPropertyNameSetter == null) {
			throw new MappingException(
					"Entry should has setter for property " + propertyName + "." + entryPropertyName);
		}

		String entryPropertyValue = attributesList.value();
		Setter entryPropertyValueSetter = getSetter(entryItemType, entryPropertyValue);
		if (entryPropertyValueSetter == null) {
			throw new MappingException(
					"Entry should has getter for property " + propertyName + "." + entryPropertyValue);
		}

		String entryPropertyMultivalued = attributesList.multiValued();
		Setter entryPropertyMultivaluedSetter = null;
		if (StringHelper.isNotEmpty(entryPropertyMultivalued)) {
			entryPropertyMultivaluedSetter = getSetter(entryItemType, entryPropertyMultivalued);
		}

		if (entryPropertyMultivaluedSetter != null) {
			Class<?> parameterType = ReflectHelper.getSetterType(entryPropertyMultivaluedSetter);
			if (!parameterType.equals(Boolean.TYPE)) {
				throw new MappingException(
						"Entry should has getter for property " + propertyName + "." + entryPropertyMultivalued + " with boolean type");
			}
		}

		for (AttributeData entryAttribute : attributes) {
			if (ldapAttributesConfiguration != null) {
				AttributeName ldapAttributeConfiguration = ldapAttributesConfiguration
						.get(entryAttribute.getName());
				if ((ldapAttributeConfiguration != null) && ldapAttributeConfiguration.ignoreDuringRead()) {
					continue;
				}
			}

			Object listItem = getListItem(propertyName, entryPropertyNameSetter, entryPropertyValueSetter,
					entryPropertyMultivaluedSetter, entryItemType, entryAttribute);
			if (listItem != null) {
				resultList.add(listItem);
			}
		}
		
		return resultList;
	}

	public Class<?> getCustomAttributesListItemType(Object entry, AttributesList attributesList, String propertyName) {
		Class<?> entryClass = entry.getClass();
		Setter setter = getSetter(entryClass, propertyName);
		if (setter == null) {
			throw new MappingException("Entry should has setter for property " + propertyName);
		}

		return ReflectHelper.getListType(setter);
	}

    protected void loadLocalizedString(Map<String, AttributeData> attributesMap, LocalizedString localizedString, Map<String, AttributeData> filteredAttrs) {
        filteredAttrs.forEach((key, value) -> {
            AttributeData data = attributesMap.get(key);
			if (data.getValues() != null && data.getValues().length == 1) {
				if (data.getValues()[0] instanceof Map<?, ?>) {
					Map<?, ?> values = (Map<?, ?>) data.getValues()[0];
					values.forEach((languageTag, val) -> {
						if (languageTag instanceof String && val instanceof String) {
							localizedString.setValue((String) val, Locale.forLanguageTag((String) languageTag));
						}
					});
				} else if (data.getValues()[0] instanceof String) {
					String jsonStr = (String) data.getValues()[0];
					JSONObject jsonObject = new JSONObject(jsonStr);

					localizedString.loadFromJson(jsonObject, key);
				}
            }
        });
    }

	@Override
	public <T> List<T> createEntities(Class<T> entryClass, Map<String, List<AttributeData>> entriesAttributes) {
		checkEntryClass(entryClass, true);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		return createEntities(entryClass, propertiesAnnotations, entriesAttributes);
	}

	@SuppressWarnings("unchecked")
	private <T> void sortAttributesListIfNeeded(AttributesList ldapAttribute, Class<T> entryItemType,
			List<?> list) {
		if (!ldapAttribute.sortByName()) {
			return;
		}

		sortListByProperties(entryItemType, (List<T>) list, ldapAttribute.name());
	}

	protected <T> void sortEntriesIfNeeded(Class<T> entryClass, List<T> entries) {
		String[] sortByProperties = getEntrySortByProperties(entryClass);

		if (ArrayHelper.isEmpty(sortByProperties)) {
			return;
		}

		sortListByProperties(entryClass, entries, sortByProperties);
	}

    @Override
    public <T> void importEntry(String dn, Class<T> entryClass, List<AttributeData> data) {
    	// Check entry class
		checkEntryClass(entryClass, false);
		String[] objectClasses = getTypeObjectClasses(entryClass);

		persist(dn, objectClasses, data, 0);
    }

	@Override
	public <T> void sortListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive,
			String... sortByProperties) {
		// Check input parameters
		if (entries == null) {
			throw new MappingException("Entries list to sort is null");
		}

		if (entries.size() == 0) {
			return;
		}

		if ((sortByProperties == null) || (sortByProperties.length == 0)) {
			throw new InvalidArgumentException(
					"Invalid list of sortBy properties " + Arrays.toString(sortByProperties));
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
			if (!((propertyType == String.class) || (propertyType == Date.class) || (propertyType == Integer.class)
					|| (propertyType == Integer.TYPE))) {
				throw new MappingException("Entry properties should has String, Date or Integer type. Property: '"
						+ tmpProperties[tmpProperties.length - 1] + "'");
			}
		}

		PropertyComparator<T> comparator = new PropertyComparator<T>(propertyGetters, caseSensetive);
		Collections.sort(entries, comparator);
	}

	protected <T> void sortListByProperties(Class<T> entryClass, List<T> entries, String... sortByProperties) {
		sortListByProperties(entryClass, entries, false, sortByProperties);
	}

	@Override
	public <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, boolean caseSensetive,
			String groupByProperties, String sumByProperties) {
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
		Getter[] groupPropertyGetters = getEntryPropertyGetters(entryClass, groupByProperties,
				GROUP_BY_ALLOWED_DATA_TYPES);
		Setter[] groupPropertySetters = getEntryPropertySetters(entryClass, groupByProperties,
				GROUP_BY_ALLOWED_DATA_TYPES);
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
				throw new MappingException(
						"Entry property getter should has next data types " + Arrays.toString(allowedTypes));
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
				throw new MappingException(
						"Entry property setter should has next data types " + Arrays.toString(allowedTypes));
			}
		}

		return propertySetters;
	}

	protected <T> Map<T, List<T>> groupListByProperties(Class<T> entryClass, List<T> entries, String groupByProperties,
			String sumByProperties) {
		return groupListByProperties(entryClass, entries, false, groupByProperties, sumByProperties);
	}

	private <T> Map<T, List<T>> groupListByPropertiesImpl(Class<T> entryClass, List<T> entries, boolean caseSensetive,
			Getter[] groupPropertyGetters, Setter[] groupPropertySetters, Getter[] sumProperyGetters,
			Setter[] sumPropertySetter) {
		Map<String, T> keys = new HashMap<String, T>();
		Map<T, List<T>> groups = new IdentityHashMap<T, List<T>>();

		for (T entry : entries) {
			String key = getEntryKey(entry, caseSensetive, groupPropertyGetters);

			T entryKey = keys.get(key);
			if (entryKey == null) {
				try {
					entryKey = ReflectHelper.createObjectByDefaultConstructor(entryClass);
				} catch (Exception ex) {
					throw new MappingException(String.format("Entry %s should has default constructor", entryClass),
							ex);
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

	private Map<String, AttributeData> getAttributesMap(List<AttributeData> attributes) {
		Map<String, AttributeData> attributesMap = new HashMap<String, AttributeData>(attributes.size());
		for (AttributeData attribute : attributes) {
			attributesMap.put(attribute.getName().toLowerCase(), attribute);
		}

		return attributesMap;
	}

	private AttributeData getAttributeData(String propertyName, String ldapAttributeName, Getter propertyValueGetter,
			Object entry, boolean multiValued, boolean jsonObject) {
		Object propertyValue = propertyValueGetter.get(entry);
		if (propertyValue == null) {
			return null;
		}

		Object[] attributeValues = getAttributeValues(propertyName, jsonObject, propertyValue, multiValued);

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Property: %s, LdapProperty: %s, PropertyValue: %s", propertyName,
					ldapAttributeName, Arrays.toString(attributeValues)));
		}

		if (attributeValues.length == 0) {
			attributeValues = new String[] {};
		} else if ((attributeValues.length == 1) && (attributeValues[0] == null)) {
			return null;
		}

		return new AttributeData(ldapAttributeName, attributeValues, multiValued);
	}

	private Object[] getAttributeValues(String propertyName, boolean jsonObject, Object propertyValue, boolean multiValued) {
		Object[] attributeValues = new Object[1];

		boolean nativeType = getNativeAttributeValue(propertyValue, attributeValues, multiValued);
		if (nativeType) {
			// We do conversion in getNativeAttributeValue method already
		} else if (propertyValue instanceof AttributeEnum) {
			attributeValues[0] = ((AttributeEnum) propertyValue).getValue();
		} else if (propertyValue instanceof AttributeEnum[]) {
			AttributeEnum[] propertyValues = (AttributeEnum[]) propertyValue;
			attributeValues = new String[propertyValues.length];
			for (int i = 0; i < propertyValues.length; i++) {
				attributeValues[i] = (propertyValues[i] == null) ? null : propertyValues[i].getValue();
			}
		} else if (propertyValue instanceof String[]) {
			attributeValues = (String[]) propertyValue;
		} else if (propertyValue instanceof Object[]) {
			attributeValues = (Object[]) propertyValue;
		} else if (propertyValue instanceof List<?>) {
			attributeValues = new Object[((List<?>) propertyValue).size()];
			int index = 0;
			Object nativeAttributeValue[] = new Object[1];
			for (Object tmpPropertyValue : (List<?>) propertyValue) {
				if (jsonObject) {
					attributeValues[index++] = convertValueToJson(tmpPropertyValue);
				} else {
					if (getNativeAttributeValue(tmpPropertyValue, nativeAttributeValue, multiValued)) {
						attributeValues[index++] = nativeAttributeValue[0];
					} else {
						attributeValues[index++] = StringHelper.toString(tmpPropertyValue);
					}
				}
			}
		} else if (jsonObject) {
			attributeValues[0] = convertValueToJson(propertyValue);
		} else {
			throw new MappingException("Entry property '" + propertyName
					+ "' should has getter with String, String[], Boolean, Integer, Long, Date, List<String>, AttributeEnum or AttributeEnum[]"
					+ " return type or has annotation JsonObject");
		}
		return attributeValues;
	}

	/*
	 * This method doesn't produces new object to avoid extra garbage creation
	 */
	private boolean getNativeAttributeValue(Object propertyValue, Object[] resultValue, boolean multiValued) {
		// Clean result
		resultValue[0] = null;

		if (propertyValue instanceof String) {
			resultValue[0] = StringHelper.toString(propertyValue);
		} else if (propertyValue instanceof Boolean) {
			resultValue[0] = propertyValue;
		} else if (propertyValue instanceof Integer) {
			resultValue[0] = propertyValue;
		} else if (propertyValue instanceof Long) {
			resultValue[0] = propertyValue;
		} else if (propertyValue instanceof Date) {
			if (multiValued) {
				resultValue[0] = getNativeDateMultiAttributeValue((Date) propertyValue);
			} else {
				resultValue[0] = getNativeDateAttributeValue((Date) propertyValue);
			}
		} else {
			return false;
		}

		return true;
	}

	protected abstract Object getNativeDateAttributeValue(Date dateValue);

	protected Object getNativeDateMultiAttributeValue(Date dateValue) {
		return getNativeDateAttributeValue(dateValue);
	}

	protected boolean isAttributeMultivalued(Object[] values) {
		if (values.length > 1) {
			return true;

		}

		return false;
	}

	protected Object convertValueToJson(Object propertyValue) {
		try {
			String value = JSON_OBJECT_MAPPER.writeValueAsString(propertyValue);

			return value;
		} catch (Exception ex) {
			LOG.error("Failed to convert '{}' to json value:", propertyValue, ex);
			throw new MappingException(String.format("Failed to convert '%s' to json value", propertyValue));
		}
	}

	protected List<AttributeData> getAttributesListForPersist(Object entry,
			List<PropertyAnnotation> propertiesAnnotations) {
		// Prepare list of properties to persist
		List<AttributeData> attributes = new ArrayList<AttributeData>();
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();
			Annotation ldapAttribute;
            Annotation languageTag;

			// Process properties with AttributeName annotation
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
					AttributeName.class);
            languageTag = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
                    LanguageTag.class);
			if (ldapAttribute != null) {
                if (languageTag != null) {
					addAttributeDataFromLocalizedString(entry, ldapAttribute, propertyName, attributes);
                } else {
                    AttributeData attribute = getAttributeDataFromAttribute(entry, ldapAttribute, propertiesAnnotation,
                            propertyName);
                    if (attribute != null) {
                        attributes.add(attribute);
                    }
                }

				continue;
			}

			// Process properties with @AttributesList annotation
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
					AttributesList.class);
			if (ldapAttribute != null) {
				List<AttributeData> listAttributes = getAttributeDataListFromCustomAttributesList(entry, (AttributesList) ldapAttribute,
						propertyName);
				if (listAttributes != null) {
					attributes.addAll(listAttributes);
				}

				continue;
			}
		}

		return attributes;
	}

	private AttributeData getAttributeDataFromAttribute(Object entry, Annotation ldapAttribute,
			PropertyAnnotation propertiesAnnotation, String propertyName) {
		Class<?> entryClass = entry.getClass();

		String ldapAttributeName = ((AttributeName) ldapAttribute).name();
		if (StringHelper.isEmpty(ldapAttributeName)) {
			ldapAttributeName = propertyName;
		}

		Getter getter = getGetter(entryClass, propertyName);
		if (getter == null) {
			throw new MappingException("Entry should has getter for property " + propertyName);
		}

		Class<?> parameterType = getSetterPropertyType(entryClass, propertyName);
		boolean multiValued = isMultiValued(parameterType);

		Annotation ldapJsonObject = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
				JsonObject.class);
		boolean jsonObject = ldapJsonObject != null;
		AttributeData attribute = getAttributeData(propertyName, ldapAttributeName, getter, entry, multiValued, jsonObject);

		return attribute;
	}

	protected void addAttributeDataFromLocalizedString(Object entry, Annotation ldapAttribute, String propertyName, List<AttributeData> attributes) {
		Class<?> entryClass = entry.getClass();

		Getter getter = getGetter(entryClass, propertyName);
		if (getter == null) {
			throw new MappingException("Entry should has getter for property " + propertyName);
		}

		Object propertyValue = getter.get(entry);
		if (propertyValue == null) {
			return;
		}

		if (!(propertyValue instanceof LocalizedString)) {
			throw new MappingException("Entry property should be LocalizedString");
		}

		LocalizedString localizedString = (LocalizedString) propertyValue;
		String ldapAttributeName = ((AttributeName) ldapAttribute).name();

		AttributeData attributeDataLocalized = getAttributeDataFromLocalizedString(ldapAttributeName, localizedString);
		if (attributeDataLocalized != null) {
			attributes.add(attributeDataLocalized);
		}
	}

	protected AttributeData getAttributeDataFromLocalizedString(String ldapAttributeName, LocalizedString localizedString) {
		AttributeData attributeData = new AttributeData(ldapAttributeName, new String[1]);

		JSONObject jsonObject = new JSONObject();

		localizedString.getLanguageTags().forEach(languageTag -> {
			String key = localizedString.addLdapLanguageTag(ldapAttributeName, languageTag);
			String value = localizedString.getValue(languageTag);

			jsonObject.put(key, value);
		});

		attributeData.getValues()[0] = jsonObject.toString();

		return attributeData;
	}

	public List<AttributeData> getAttributeDataListFromCustomAttributesList(Object entry, AttributesList attributesList,
			String propertyName) {
		Class<?> entryClass = entry.getClass();
		List<AttributeData> listAttributes = new ArrayList<AttributeData>();

		Getter getter = getGetter(entryClass, propertyName);
		if (getter == null) {
			throw new MappingException("Entry should has getter for property " + propertyName);
		}

		Object propertyValue = getter.get(entry);
		if (propertyValue == null) {
			return null;
		}

		if (!(propertyValue instanceof List<?>)) {
			throw new MappingException("Entry property should has List base type");
		}

		Class<?> elementType = ReflectHelper.getListType(getter);

		String entryPropertyName = attributesList.name();
		Getter entryPropertyNameGetter = getGetter(elementType, entryPropertyName);
		if (entryPropertyNameGetter == null) {
			throw new MappingException(
					"Entry should has getter for property " + propertyName + "." + entryPropertyName);
		}

		String entryPropertyValue = attributesList.value();
		Getter entryPropertyValueGetter = getGetter(elementType, entryPropertyValue);
		if (entryPropertyValueGetter == null) {
			throw new MappingException(
					"Entry should has getter for property " + propertyName + "." + entryPropertyValue);
		}

		String entryPropertyMultivalued = attributesList.multiValued();
		Getter entryPropertyMultivaluedGetter = null;
		if (StringHelper.isNotEmpty(entryPropertyMultivalued)) {
			entryPropertyMultivaluedGetter = getGetter(elementType, entryPropertyMultivalued);
		}
		if (entryPropertyMultivaluedGetter != null) {
			Class<?> propertyType = entryPropertyMultivaluedGetter.getReturnType();
			if (!propertyType.equals(Boolean.TYPE)) {
				throw new MappingException(
						"Entry should has getter for property " + propertyName + "." + entryPropertyMultivalued + " with boolean type");
			}
		}

		for (Object entryAttribute : (List<?>) propertyValue) {
			Boolean multiValued = null;
			if (entryPropertyMultivaluedGetter != null) {
				multiValued = (boolean) entryPropertyMultivaluedGetter.get(entryAttribute);
			}

			AttributeData attribute = getAttributeData(propertyName, entryPropertyNameGetter, entryPropertyValueGetter,
					entryAttribute, Boolean.TRUE.equals(multiValued), false);
			if (attribute != null) {
				if (multiValued == null) {
					// Detect if attribute has more than one value
					multiValued = attribute.getValues().length > 1;
				}
				attribute.setMultiValued(multiValued);

				listAttributes.add(attribute);
			}
		}

		return listAttributes;
	}

	public List<Object> getCustomAttributesListFromAttributeDataList(Object entry, AttributesList attributesList,
			String propertyName, Collection<AttributeData> attributes) {
		Class<?> entryClass = entry.getClass();
		return getCustomAttributesListFromAttributeData(entryClass, attributesList, propertyName, attributes, null);
	}

	public <T> List<PropertyAnnotation> getEntryPropertyAnnotations(Class<T> entryClass) {
        final List<PropertyAnnotation> annotations = getEntryClassAnnotations(entryClass, "property_", LDAP_ENTRY_PROPERTY_ANNOTATIONS);
//        KeyShortcuter.initIfNeeded(entryClass, annotations);
        return annotations;
	}

	protected <T> List<PropertyAnnotation> getEntryDnAnnotations(Class<T> entryClass) {
		return getEntryClassAnnotations(entryClass, "dn_", LDAP_DN_PROPERTY_ANNOTATION);
	}

	protected <T> List<PropertyAnnotation> getEntryExpirationAnnotations(Class<T> entryClass) {
		return getEntryClassAnnotations(entryClass, "exp_", LDAP_EXPIRATION_PROPERTY_ANNOTATION);
	}

	protected <T> List<PropertyAnnotation> getEntryCustomObjectClassAnnotations(Class<T> entryClass) {
		return getEntryClassAnnotations(entryClass, "custom_", LDAP_CUSTOM_OBJECT_CLASS_PROPERTY_ANNOTATION);
	}

	protected <T> List<PropertyAnnotation> getEntryClassAnnotations(Class<T> entryClass, String keyCategory,
			Class<?>[] annotationTypes) {
		String key = keyCategory + entryClass.getName();

		List<PropertyAnnotation> annotations = classAnnotations.get(key);
		if (annotations == null) {
			synchronized (CLASS_ANNOTATIONS_LOCK) {
				annotations = classAnnotations.get(key);
				if (annotations == null) {
					Map<String, List<Annotation>> annotationsMap = ReflectHelper.getPropertiesAnnotations(entryClass,
							annotationTypes);
					annotations = convertToPropertyAnnotationList(annotationsMap);
					classAnnotations.put(key, annotations);
				}
			}
		}

		return annotations;
	}

	private List<PropertyAnnotation> convertToPropertyAnnotationList(Map<String, List<Annotation>> annotations) {
		List<PropertyAnnotation> result = new ArrayList<PropertyAnnotation>(annotations.size());
		for (Entry<String, List<Annotation>> entry : annotations.entrySet()) {
			result.add(new PropertyAnnotation(entry.getKey(), entry.getValue()));
		}

		Collections.sort(result);

		return result;
	}

	protected <T> Getter getGetter(Class<T> entryClass, String propertyName) {
		String key = entryClass.getName() + "." + propertyName;

		Getter getter = classGetters.get(key);
		if (getter == null) {
			synchronized (CLASS_GETTERS_LOCK) {
				getter = classGetters.get(key);
				if (getter == null) {
					getter = ReflectHelper.getGetter(entryClass, propertyName);
					classGetters.put(key, getter);
				}
			}
		}

		return getter;
	}

	protected <T> Setter getSetter(Class<T> entryClass, String propertyName) {
		String key = entryClass.getName() + "." + propertyName;

		Setter setter = classSetters.get(key);
		if (setter == null) {
			synchronized (CLASS_SETTERS_LOCK) {
				setter = classSetters.get(key);
				if (setter == null) {
					setter = ReflectHelper.getSetter(entryClass, propertyName);
					classSetters.put(key, setter);
				}
			}
		}

		return setter;
	}

	private AttributeData getAttributeData(String propertyName, Getter propertyNameGetter, Getter propertyValueGetter,
			Object entry, boolean multiValued, boolean jsonObject) {
		Object ldapAttributeName = propertyNameGetter.get(entry);
		if (ldapAttributeName == null) {
			return null;
		}

		return getAttributeData(propertyName, ldapAttributeName.toString(), propertyValueGetter, entry, multiValued, jsonObject);
	}

	private void setPropertyValue(String propertyName, Setter propertyValueSetter, Object entry,
			AttributeData attribute, boolean jsonObject) {
		if (attribute == null) {
			return;
		}

		LOG.debug(String.format("LdapProperty: %s, AttributeName: %s, AttributeValue: %s", propertyName,
				attribute.getName(), Arrays.toString(attribute.getValues())));

		Class<?> parameterType = ReflectHelper.getSetterType(propertyValueSetter);
		if (parameterType.equals(String.class)) {
			Object value = attribute.getValue();
			if (value instanceof Date) {
				value = encodeTime((Date) value);
			}
			propertyValueSetter.set(entry, String.valueOf(value));
		} else if (parameterType.equals(Boolean.class) || parameterType.equals(Boolean.TYPE)) {
			propertyValueSetter.set(entry, toBooleanValue(attribute));
		} else if (parameterType.equals(Integer.class) || parameterType.equals(Integer.TYPE)) {
			propertyValueSetter.set(entry, toIntegerValue(attribute));
		} else if (parameterType.equals(Long.class) || parameterType.equals(Long.TYPE)) {
			propertyValueSetter.set(entry, toLongValue(attribute));
		} else if (parameterType.equals(Date.class)) {
			if (attribute.getValue() == null) {
				propertyValueSetter.set(entry, null);
			} else {
				propertyValueSetter.set(entry, attribute.getValue() instanceof Date ? (Date) attribute.getValue() : decodeTime(String.valueOf(attribute.getValue())));
			}
		} else if (parameterType.equals(String[].class)) {
			propertyValueSetter.set(entry, attribute.getStringValues());
		} else if (ReflectHelper.assignableFrom(parameterType, List.class)) {
			if (jsonObject) {
				Object[] values = attribute.getValues();
				List<Object> jsonValues = new ArrayList<Object>(values.length);

				for (Object value : values) {
					Object jsonValue = convertJsonToValue(ReflectHelper.getListType(propertyValueSetter), value);
					jsonValues.add(jsonValue);
				}
				propertyValueSetter.set(entry, jsonValues);
			} else {
				List<?> resultValues = attributeToTypedList(ReflectHelper.getListType(propertyValueSetter), attribute);
				propertyValueSetter.set(entry, resultValues);
			}
		} else if (ReflectHelper.assignableFrom(parameterType, AttributeEnum.class)) {
			try {
				propertyValueSetter.set(entry, parameterType.getMethod("resolveByValue", String.class)
						.invoke(parameterType.getEnumConstants()[0], attribute.getValue()));
			} catch (Exception ex) {
				throw new MappingException("Failed to resolve Enum '" + parameterType + "' by value '" + attribute.getValue() + "'", ex);
			}
		} else if (ReflectHelper.assignableFrom(parameterType, AttributeEnum[].class)) {
			Class<?> itemType = parameterType.getComponentType();
			Method enumResolveByValue;
			try {
				enumResolveByValue = itemType.getMethod("resolveByValue", String.class);
			} catch (Exception ex) {
				throw new MappingException("Failed to resolve Enum '" + parameterType + "' by value '" + Arrays.toString(attribute.getValues()) + "'",
						ex);
			}

			Object[] attributeValues = attribute.getValues();
			AttributeEnum[] ldapEnums = (AttributeEnum[]) ReflectHelper.createArray(itemType, attributeValues.length);
			for (int i = 0; i < attributeValues.length; i++) {
				try {
					ldapEnums[i] = (AttributeEnum) enumResolveByValue.invoke(itemType.getEnumConstants()[0],
							attributeValues[i]);
				} catch (Exception ex) {
					throw new MappingException(
							"Failed to resolve Enum '" + parameterType + "' by value '" + Arrays.toString(attribute.getValues()) + "'", ex);
				}
			}
			propertyValueSetter.set(entry, ldapEnums);
		} else if (jsonObject) {
			Object stringValue = attribute.getValue();
			Object jsonValue = convertJsonToValue(parameterType, stringValue);
			propertyValueSetter.set(entry, jsonValue);
		} else {
			throw new MappingException("Entry property '" + propertyName
					+ "' should has setter with String, Boolean, Integer, Long, Date, String[], List<String>, AttributeEnum or AttributeEnum[]"
					+ " parameter type or has annotation JsonObject");
		}
	}

	private List<?> attributeToTypedList(Class<?> listType, AttributeData attributeData) {
		if (listType.equals(String.class)) {
			ArrayList<String> result = new ArrayList<String>();
			for (Object value : attributeData.getValues()) {
				String resultValue;
				if (value instanceof Date) {
					resultValue = encodeTime((Date) value);
				} else {
					resultValue = String.valueOf(value);
				}

				result.add(resultValue);
			}

			return result;
		}

		return Arrays.asList(attributeData.getValues());
	}

	private Boolean toBooleanValue(AttributeData attribute) {
		Boolean propertyValue = null;
		Object propertyValueObject = attribute.getValue();
		if (propertyValueObject != null) {
			if (propertyValueObject instanceof Boolean) {
				propertyValue = (Boolean) propertyValueObject;
			} else {
				propertyValue = Boolean.valueOf(String.valueOf(propertyValueObject));
			}
		}
		return propertyValue;
	}

	private Integer toIntegerValue(AttributeData attribute) {
		Integer propertyValue = null;
		Object propertyValueObject = attribute.getValue();
		if (propertyValueObject != null) {
			if (propertyValueObject instanceof Integer) {
				propertyValue = (Integer) propertyValueObject;
			} else if (propertyValueObject instanceof Long) {
					propertyValue = ((Long) propertyValueObject).intValue();
			} else {
				propertyValue = Integer.valueOf(String.valueOf(propertyValueObject));
			}
		}
		return propertyValue;
	}

	private Long toLongValue(AttributeData attribute) {
		Long propertyValue = null;
		Object propertyValueObject = attribute.getValue();
		if (propertyValueObject != null) {
			if (propertyValueObject instanceof Long) {
				propertyValue = (Long) propertyValueObject;
			} else if (propertyValueObject instanceof Integer) {
				propertyValue = ((Integer) propertyValueObject).longValue();
			} else {
				propertyValue = Long.valueOf(String.valueOf(propertyValueObject));
			}
		}
		return propertyValue;
	}

	protected Object convertJsonToValue(Class<?> parameterType, Object propertyValue) {
		try {
			Object jsonValue = JSON_OBJECT_MAPPER.readValue(String.valueOf(propertyValue), parameterType);
			return jsonValue;
		} catch (Exception ex) {
			LOG.error("Failed to convert json value '{}' to object `{}`", propertyValue, parameterType, ex);
			throw new MappingException(String.format("Failed to convert json value '%s' to object of type %s",
					propertyValue, parameterType));
		}
	}

	private Object getListItem(String propertyName, Setter propertyNameSetter, Setter propertyValueSetter,
			Setter entryPropertyMultivaluedSetter, Class<?> classType, AttributeData attribute) {
		if (attribute == null) {
			return null;
		}

		Object result;
		try {
			result = ReflectHelper.createObjectByDefaultConstructor(classType);
		} catch (Exception ex) {
			throw new MappingException(String.format("Entry %s should has default constructor", classType));
		}
		propertyNameSetter.set(result, attribute.getName());
		setPropertyValue(propertyName, propertyValueSetter, result, attribute, false);

		if ((entryPropertyMultivaluedSetter != null) && (attribute.getMultiValued() != null)) {
			entryPropertyMultivaluedSetter.set(result, attribute.getMultiValued());
		}

		return result;
	}

	protected <T> Object getDNValue(Object entry) {
		Class<?> entryClass = entry.getClass();

		return getDNValue(entry, entryClass);
	}

	protected <T> Object getDNValue(Object entry, Class<T> entryClass) {
		// Check if entry has DN property
		String dnProperty = getDNPropertyName(entryClass);

		// Get DN value
		Getter dnGetter = getGetter(entryClass, dnProperty);
		if (dnGetter == null) {
			throw new MappingException("Entry should has getter for property " + dnProperty);
		}

		Object dnValue = dnGetter.get(entry);
		if (StringHelper.isEmptyString(dnValue)) {
			throw new MappingException("Entry should has not null base DN property value");
		}

		return dnValue;
	}

	protected <T> Integer getExpirationValue(Object entry, Class<T> entryClass, boolean merge) {
		// Check if entry has Expiration property
		PropertyAnnotation expirationProperty = getExpirationProperty(entryClass);
		if (expirationProperty == null) {
			return null;
		}

		String expirationPropertyName = expirationProperty.getPropertyName();

		Expiration expirationAnnotation = (Expiration) ReflectHelper.getAnnotationByType(expirationProperty.getAnnotations(),
				Expiration.class);

		if (merge && expirationAnnotation.ignoreDuringUpdate()) {
			return null;
		}

		if (expirationPropertyName == null) {
			// No entry expiration property
			return null;
		}

		// Get Expiration value
		Getter expirationGetter = getGetter(entryClass, expirationPropertyName);
		if (expirationGetter == null) {
			throw new MappingException("Entry should has getter for property " + expirationGetter);
		}

		Class<?> propertyType = expirationGetter.getReturnType();
		if (!((propertyType == Integer.class) || (propertyType == Integer.TYPE))) {
			throw new MappingException("Entry expiration property should has Integer type. Property: '"
					+ expirationGetter + "'");
		}

		Object expirationValue = expirationGetter.get(entry);
		if (expirationValue == null) {
			// No entry expiration or null
			return null;
		}

		Integer resultExpirationValue;
		if (expirationValue instanceof Integer) {
			resultExpirationValue = (Integer) expirationValue;
		} else {
			resultExpirationValue = Integer.valueOf((int) expirationValue);
		}

		// TTL can't be negative
		if (resultExpirationValue < 0) {
			resultExpirationValue = 0;
		}

        return resultExpirationValue;
	}

	private <T> String getEntryKey(T entry, boolean caseSensetive, Getter[] propertyGetters) {
		StringBuilder sb = new StringBuilder("key");
		for (Getter getter : propertyGetters) {
			sb.append("__").append(getter.get(entry));
		}

		if (caseSensetive) {
			return sb.toString();
		} else {
			return sb.toString().toLowerCase();
		}
	}

	private Map<String, AttributeData> getAttributesDataMap(List<AttributeData> attributesData) {
		Map<String, AttributeData> result = new HashMap<String, AttributeData>();

		for (AttributeData attributeData : attributesData) {
			result.put(attributeData.getName(), attributeData);
		}

		return result;
	}

	private String getEntryKey(Object dnValue, boolean caseSensetive, List<PropertyAnnotation> propertiesAnnotations,
			List<AttributeData> attributesData) {
		StringBuilder sb = new StringBuilder("_HASH__").append((String.valueOf(dnValue)).toLowerCase()).append("__");

		List<String> processedProperties = new ArrayList<String>();
		Map<String, AttributeData> attributesDataMap = getAttributesDataMap(attributesData);
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			Annotation ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
					AttributeName.class);
			if (ldapAttribute == null) {
				continue;
			}

			String ldapAttributeName = ((AttributeName) ldapAttribute).name();
			if (StringHelper.isEmpty(ldapAttributeName)) {
				ldapAttributeName = propertiesAnnotation.getPropertyName();
			}

			processedProperties.add(ldapAttributeName);

			String[] values = null;

			AttributeData attributeData = attributesDataMap.get(ldapAttributeName);
			if ((attributeData != null) && (attributeData.getValues() != null)) {
				values = attributeData.getStringValues();
				Arrays.sort(values);
			}

			addPropertyWithValuesToKey(sb, ldapAttributeName, values);
		}

		for (AttributeData attributeData : attributesData) {
			if (processedProperties.contains(attributeData.getName())) {
				continue;
			}

			addPropertyWithValuesToKey(sb, attributeData.getName(), attributeData.getStringValues());
		}

		if (caseSensetive) {
			return sb.toString();
		} else {
			return sb.toString().toLowerCase();
		}
	}

	private void addPropertyWithValuesToKey(StringBuilder sb, String propertyName, String[] values) {
		sb.append(':').append(propertyName).append('=');
		if (values == null) {
			sb.append("null");
		} else {
			if (values.length == 1) {
				sb.append(values[0]);
			} else {
				String[] tmpValues = values.clone();
				Arrays.sort(tmpValues);

				for (int i = 0; i < tmpValues.length; i++) {
					sb.append(tmpValues[i]);
					if (i < tmpValues.length - 1) {
						sb.append(';');
					}
				}
			}
		}
	}

	@Override
	public int getHashCode(Object entry) {
		if (entry == null) {
			throw new MappingException("Entry to persist is null");
		}

		// Check entry class
		Class<?> entryClass = entry.getClass();
		checkEntryClass(entryClass, false);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);
		String key = getEntryKey(dnValue, false, propertiesAnnotations, attributes);
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Entry key HashCode is: %s", key.hashCode()));
		}

		return key.hashCode();
	}

	protected byte[][] toBinaryValues(String[] attributeValues) {
		byte[][] binaryValues = new byte[attributeValues.length][];

		for (int i = 0; i < attributeValues.length; i++) {
			binaryValues[i] = Base64.decodeBase64(attributeValues[i]);
		}
		return binaryValues;
	}

	protected <T> Filter createFilterByEntry(Object entry, Class<T> entryClass, List<AttributeData> attributes) {
		Filter[] attributesFilters = createAttributesFilter(attributes);
		Filter attributesFilter = null;
		if (attributesFilters != null) {
			attributesFilter = Filter.createANDFilter(attributesFilters);
		}

		String[] objectClasses = getObjectClasses(entry, entryClass);
		return addObjectClassFilter(attributesFilter, objectClasses);
	}

	protected Filter excludeObjectClassFilters(Filter genericFilter) {
		return filterProcessor.excludeFilter(genericFilter, FilterProcessor.OBJECT_CLASS_EQUALITY_FILTER, FilterProcessor.OBJECT_CLASS_PRESENCE_FILTER);
	}

	protected Filter[] createAttributesFilter(List<AttributeData> attributes) {
		if ((attributes == null) || (attributes.size() == 0)) {
			return null;
		}

		List<Filter> results = new ArrayList<Filter>(attributes.size());

		for (AttributeData attribute : attributes) {
			String attributeName = attribute.getName();
			for (Object value : attribute.getValues()) {
				Filter filter = Filter.createEqualityFilter(attributeName, value);
				if (attribute.getMultiValued() != null) {
					filter.multiValued(attribute.getMultiValued());
				}

				results.add(filter);
			}
		}

		return results.toArray(new Filter[results.size()]);
	}

	protected Filter addObjectClassFilter(Filter filter, String[] objectClasses) {
		if (objectClasses.length == 0) {
			return filter;
		}

		Filter[] objectClassFilter = new Filter[objectClasses.length];
		for (int i = 0; i < objectClasses.length; i++) {
			objectClassFilter[i] = Filter.createEqualityFilter(OBJECT_CLASS, objectClasses[i]).multiValued();
		}
		Filter searchFilter = Filter.createANDFilter(objectClassFilter);
		if (filter != null) {
			searchFilter = Filter.createANDFilter(Filter.createANDFilter(objectClassFilter), filter);
		}
		return searchFilter;
	}

	protected boolean isMultiValued(Class<?> parameterType) {
		if (parameterType == null) {
			return false;
		}

		boolean isMultiValued = parameterType.equals(String[].class) || ReflectHelper.assignableFrom(parameterType, List.class) || ReflectHelper.assignableFrom(parameterType, AttributeEnum[].class);

		return isMultiValued;
	}

	protected abstract Date decodeTime(String date);

	protected abstract String encodeTime(Date date);

	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		this.persistenceExtension = persistenceExtension;

		if (this.operationService != null) {
			this.operationService.setPersistenceExtension(persistenceExtension);
		}
	}

	@Override
	public <T> AttributeType getAttributeType(String primaryKey, Class<T> entryClass, String propertyName) {
        throw new UnsupportedOperationException("Method not implemented.");
	}

	protected static final class PropertyComparator<T> implements Comparator<T>, Serializable {

		private static final long serialVersionUID = 574848841116711467L;
		private Getter[][] propertyGetters;
		private boolean caseSensetive;

		private PropertyComparator(Getter[][] propertyGetters, boolean caseSensetive) {
			this.propertyGetters = propertyGetters;
			this.caseSensetive = caseSensetive;
		}

		@Override
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

	protected static final class LineLenghtComparator<T> implements Comparator<T>, Serializable {

		private static final long serialVersionUID = 575848841116711467L;
		private boolean ascending;

		public LineLenghtComparator(boolean ascending) {
			this.ascending = ascending;
		}

		@Override
		public int compare(T entry1, T entry2) {
			if ((entry1 == null) && (entry2 == null)) {
				return 0;
			}
			if ((entry1 == null) && (entry2 != null)) {
				return -1;
			} else if ((entry1 != null) && (entry2 == null)) {
				return 1;
			}

			int result = entry1.toString().length() - entry2.toString().length();
			if (ascending) {
				return result;
			} else {
				return -result;
			}
		}
	}

	protected void dumpAttributes(String variableName, List<AttributeData> attributesToPersist) {
		System.out.println("\n" + variableName + ": START");

		for (AttributeData attribute : attributesToPersist) {
			System.out.println(String.format("%s\t\t%s\t%b", attribute.getName(), Arrays.toString(attribute.getValues()), attribute.getMultiValued()));
		}

		System.out.println(variableName + ": END");
	}

	protected void dumpAttributeDataModifications(String variableName, List<AttributeDataModification> attributeDataModifications) {
		System.out.println("\n" + variableName + ": START");

		for (AttributeDataModification modification : attributeDataModifications) {
			String newValues = "[]";
			String oldValues = "[]";

			if ((modification.getAttribute() != null) && (modification.getAttribute().getValues() != null)) {
				newValues = Arrays.toString(modification.getAttribute().getValues());
			}

			if ((modification.getOldAttribute() != null) && (modification.getOldAttribute().getValues() != null)) {
				oldValues = Arrays.toString(modification.getOldAttribute().getValues());
			}

			AttributeData attribute = modification.getAttribute();
			if (attribute == null) {
				attribute = modification.getOldAttribute();
			}

			System.out.println(String.format("%s\t\t%s\t%b\t\t%s\t->\t%s", attribute.getName(), modification.getModificationType().name(), attribute.getMultiValued(), oldValues, newValues));
		}

		System.out.println(variableName + ": END");
	}

}
