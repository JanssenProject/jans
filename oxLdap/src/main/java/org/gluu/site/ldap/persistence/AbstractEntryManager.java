/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.site.ldap.persistence.AttributeDataModification.AttributeModificationType;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapAttributesList;
import org.gluu.site.ldap.persistence.annotation.LdapCustomObjectClass;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapEnum;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.gluu.site.ldap.persistence.annotation.LdapSchemaEntry;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.gluu.site.ldap.persistence.exception.MappingException;
import org.gluu.site.ldap.persistence.property.Getter;
import org.gluu.site.ldap.persistence.property.Setter;
import org.gluu.site.ldap.persistence.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.ArrayHelper;
import org.xdi.util.StringHelper;

/**
 * LDAP Entry Manager
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
public abstract class AbstractEntryManager implements EntityManager {

	private static final Logger log = LoggerFactory.getLogger(AbstractEntryManager.class);

	private static final Class<?>[] LDAP_ENTRY_TYPE_ANNOTATIONS = { LdapEntry.class, LdapSchemaEntry.class, LdapObjectClass.class };
	private static final Class<?>[] LDAP_ENTRY_PROPERTY_ANNOTATIONS = { LdapAttribute.class, LdapAttributesList.class, LdapJsonObject.class };
	private static final Class<?>[] LDAP_CUSTOM_OBJECT_CLASS_PROPERTY_ANNOTATION = { LdapCustomObjectClass.class };
	private static final Class<?>[] LDAP_DN_PROPERTY_ANNOTATION = { LdapDN.class };

	public static final String OBJECT_CLASS = "objectClass";
	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	private final Map<String, List<PropertyAnnotation>> classAnnotations = new HashMap<String, List<PropertyAnnotation>>();
	private final Map<String, Getter> classGetters = new HashMap<String, Getter>();
	private final Map<String, Setter> classSetters = new HashMap<String, Setter>();

	private static Object classAnnotationsLock = new Object();
	private static Object classSettersLock = new Object();
	private static Object classGettersLock = new Object();

	public static final String ATTRIBUTE_DN = "dn";
	
	private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

	public void persist(Object entry) {
		if (entry == null) {
			throw new MappingException("Entry to persist is null");
		}

		// Check entry class
		Class<?> entryClass = entry.getClass();
		checkEntryClass(entryClass, false);
		String[] objectClasses = getObjectClasses(entry, entryClass);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);

		// Add object classes
		attributes.add(new AttributeData(OBJECT_CLASS, objectClasses));

		log.debug(String.format("LDAP attributes for persist: %s", attributes));

		persist(dnValue.toString(), attributes);
	}

	protected abstract void persist(String dn, List<AttributeData> attributes);

	@SuppressWarnings("unchecked")
	private <T> T merge(T entry, boolean isSchemaUpdate, AttributeModificationType schemaModificationType) {
		if (entry == null) {
			throw new MappingException("Entry to persist is null");
		}

		Class<?> entryClass = entry.getClass();
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributesToPersist = getAttributesListForPersist(entry, propertiesAnnotations);
		Map<String, AttributeData> attributesToPersistMap = getAttributesMap(attributesToPersist);

		// Load entry
		List<AttributeData> attributesFromLdap;
		if (isSchemaUpdate) {
			// If it's schema modification request we don't need to load
			// attributes from LDAP
			attributesFromLdap = new ArrayList<AttributeData>();
		} else {
			String[] currentLdapReturnAttributes = null;
			currentLdapReturnAttributes = getLdapAttributes(entry, propertiesAnnotations, false);
			attributesFromLdap = find(dnValue.toString(), currentLdapReturnAttributes);
		}
		Map<String, AttributeData> attributesFromLdapMap = getAttributesMap(attributesFromLdap);

		// Prepare list of modifications

		// Process properties with LdapAttribute annotation
		List<AttributeDataModification> attributeDataModifications = new ArrayList<AttributeDataModification>();
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();
			Annotation ldapAttribute;

			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttribute.class);
			if (ldapAttribute != null) {
				String ldapAttributeName = ((LdapAttribute) ldapAttribute).name();
				if (StringHelper.isEmpty(ldapAttributeName)) {
					ldapAttributeName = propertyName;
				}
				ldapAttributeName = ldapAttributeName.toLowerCase();

				AttributeData attributeToPersist = attributesToPersistMap.get(ldapAttributeName);
				AttributeData attributeFromLdap = attributesFromLdapMap.get(ldapAttributeName);

				// Remove processed attributes
				attributesToPersistMap.remove(ldapAttributeName);
				attributesFromLdapMap.remove(ldapAttributeName);

				LdapAttribute ldapAttributeAnnotation = (LdapAttribute) ldapAttribute;
				if (ldapAttributeAnnotation.ignoreDuringUpdate()) {
					continue;
				}

                if (attributeFromLdap != null && attributeToPersist != null) {
                    // Modify DN entry attribute in DS
                    if (!attributeFromLdap.equals(attributeToPersist)) {
                        attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE, attributeToPersist,
                                attributeFromLdap));
                    }
                } else if ((attributeFromLdap == null) && (attributeToPersist != null)) {
                    // Add entry attribute or change schema
					if (isSchemaUpdate
							&& (attributeToPersist.getValue() == null && Arrays.equals(attributeToPersist.getValues(),
									new String[] { null }))) {
						continue;
					}
					AttributeModificationType modType = isSchemaUpdate ? schemaModificationType : AttributeModificationType.ADD;
					if (AttributeModificationType.ADD.equals(modType)) {
						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.ADD, attributeToPersist));
					} else {
						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE, null,
								attributeToPersist));
					}
				} else if ((attributeFromLdap != null) && (attributeToPersist == null)) {
					// Remove if attribute not marked as ignoreDuringRead = true
					// or updateOnly = true
					if (!ldapAttributeAnnotation.ignoreDuringRead() && !ldapAttributeAnnotation.updateOnly()) {
						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE, null,
								attributeFromLdap));
					}
				}
			}
		}

		// Process properties with LdapAttributesList annotation
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			Annotation ldapAttribute;
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttributesList.class);
			if (ldapAttribute != null) {
				Map<String, LdapAttribute> ldapAttributesConfiguration = new HashMap<String, LdapAttribute>();
				for (LdapAttribute ldapAttributeConfiguration : ((LdapAttributesList) ldapAttribute).attributesConfiguration()) {
					ldapAttributesConfiguration.put(ldapAttributeConfiguration.name(), ldapAttributeConfiguration);
				}

				// Prepare attributes for removal
				for (AttributeData attributeFromLdap : attributesFromLdapMap.values()) {
					String attributeName = attributeFromLdap.getName();
					if (OBJECT_CLASS.equalsIgnoreCase(attributeName)) {
						continue;
					}

					LdapAttribute ldapAttributeConfiguration = ldapAttributesConfiguration.get(attributeName);
					if ((ldapAttributeConfiguration != null) && ldapAttributeConfiguration.ignoreDuringUpdate()) {
						continue;
					}

					if (!attributesToPersistMap.containsKey(attributeName.toLowerCase())) {
						// Remove if attribute not marked as ignoreDuringRead =
						// true
						if ((ldapAttributeConfiguration == null)
								|| ((ldapAttributeConfiguration != null) && !ldapAttributeConfiguration.ignoreDuringRead())) {
							attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE, null,
									attributeFromLdap));
						}
					}
				}

				// Prepare attributes for adding and replace
				for (AttributeData attributeToPersist : attributesToPersistMap.values()) {
					String attributeName = attributeToPersist.getName();
					// if (OBJECT_CLASS.equalsIgnoreCase(attributeName)) {
					// continue;
					// }

					LdapAttribute ldapAttributeConfiguration = ldapAttributesConfiguration.get(attributeName);
					if ((ldapAttributeConfiguration != null) && ldapAttributeConfiguration.ignoreDuringUpdate()) {
						continue;
					}

					AttributeData attributeFromLdap = attributesFromLdapMap.get(attributeName.toLowerCase());
					if (attributeFromLdap == null) {
						// Add entry attribute or change schema
						AttributeModificationType modType = isSchemaUpdate ? schemaModificationType : AttributeModificationType.ADD;
						if (AttributeModificationType.ADD.equals(modType)) {
							String[] attributeToPersistValues = attributeToPersist.getValues();
							if (!(ArrayHelper.isEmpty(attributeToPersistValues) || ((attributeToPersistValues.length == 1) && StringHelper.isEmpty(attributeToPersistValues[0])))) {
								attributeDataModifications
										.add(new AttributeDataModification(AttributeModificationType.ADD, attributeToPersist));
							}
						} else {
							attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE, null,
									attributeToPersist));
						}
					} else if ((attributeFromLdap != null) && (Arrays.equals(attributeToPersist.getValues(), new String[] { null }))) {

						attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REMOVE, null,
								attributeFromLdap));
					} else {
						if (!attributeFromLdap.equals(attributeToPersist)) {
							attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE,
									attributeToPersist, attributeFromLdap));
						}
					}
				}

			}
		}

		// Update object classes if entry contains custom object classes
		if (getSupportedLDAPVersion() > 2) {
			if (!isSchemaUpdate && (getCustomObjectClasses(entry, entryClass).length > 0)) {
				String[] objectClasses = getObjectClasses(entry, entryClass);
				if (objectClasses.length > 0) {
					attributeDataModifications.add(new AttributeDataModification(AttributeModificationType.REPLACE, new AttributeData(
							OBJECT_CLASS, objectClasses), new AttributeData(OBJECT_CLASS, objectClasses)));
				}
			}
		}

		log.debug(String.format("LDAP attributes for merge: %s", attributeDataModifications));

		merge(dnValue.toString(), attributeDataModifications);

		return (T) find(entryClass, dnValue.toString(), null, propertiesAnnotations);
	}

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

	protected abstract void merge(String dn, List<AttributeDataModification> attributeDataModifications);

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

	protected abstract void remove(String dn);

    protected abstract void removeWithSubtree(String dn);

	public boolean contains(Object entry) {
		if (entry == null) {
			throw new MappingException("Entry to persist is null");
		}

		// Check entry class
		Class<?> entryClass = entry.getClass();
		checkEntryClass(entryClass, false);
		String[] objectClasses = getObjectClasses(entry, entryClass);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		Object dnValue = getDNValue(entry, entryClass);

		List<AttributeData> attributes = getAttributesListForPersist(entry, propertiesAnnotations);

		String[] ldapReturnAttributes = getLdapAttributes(null, propertiesAnnotations, false);

		return contains(dnValue.toString(), attributes, objectClasses, ldapReturnAttributes);
	}

	protected abstract boolean contains(String baseDN, List<AttributeData> attributes, String[] objectClasses,
			String... ldapReturnAttributes);

	public <T> boolean contains(Class<T> entryClass, String primaryKey, String[] ldapReturnAttributes) {
		if (StringHelper.isEmptyString(primaryKey)) {
			throw new MappingException("DN to find entry is null");
		}

		checkEntryClass(entryClass, true);

		try {
			List<AttributeData> results = find(primaryKey, ldapReturnAttributes);
			return (results != null) && (results.size() > 0);
		} catch (EntryPersistenceException ex) {
			return false;
		}
	}

	public <T> boolean contains(Class<T> entryClass, String primaryKey) {
		return contains(entryClass, primaryKey, (String[]) null);
	}

	public <T> T find(Class<T> entryClass, Object primaryKey) {
		return find(entryClass, primaryKey, null);
	}

	public <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes) {
		if (StringHelper.isEmptyString(primaryKey)) {
			throw new MappingException("DN to find entry is null");
		}

		checkEntryClass(entryClass, true);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		return find(entryClass, primaryKey, ldapReturnAttributes, propertiesAnnotations);
	}

	protected <T> String[] getLdapAttributes(T entry, List<PropertyAnnotation> propertiesAnnotations, boolean isIgnoreLdapAttributesList) {
		List<String> attributes = new ArrayList<String>();
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();
			Annotation ldapAttribute;

			if (!isIgnoreLdapAttributesList) {
				ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttributesList.class);
				if (ldapAttribute != null) {
					if (entry == null) {
						return null;
					} else {
						List<AttributeData> ldapAttributesList = getAttributesFromLdapAttributesList(entry, ldapAttribute, propertyName);
						for (AttributeData attributeData : ldapAttributesList) {
							String ldapAttributeName = attributeData.getName();
							attributes.add(ldapAttributeName);
						}
					}
				}
			}

			// Process properties with LdapAttribute annotation
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttribute.class);
			if (ldapAttribute != null) {
				String ldapAttributeName = ((LdapAttribute) ldapAttribute).name();
				if (StringHelper.isEmpty(ldapAttributeName)) {
					ldapAttributeName = propertyName;
				}

				attributes.add(ldapAttributeName);
			}
		}

		if (attributes.size() == 0) {
			return null;
		}

		return attributes.toArray(new String[0]);
	}

	private <T> T find(Class<T> entryClass, Object primaryKey, String[] ldapReturnAttributes, List<PropertyAnnotation> propertiesAnnotations) {
		Map<String, List<AttributeData>> entriesAttributes = new HashMap<String, List<AttributeData>>();

		String[] currentLdapReturnAttributes = ldapReturnAttributes;
		if (ArrayHelper.isEmpty(currentLdapReturnAttributes)) {
			currentLdapReturnAttributes = getLdapAttributes(null, propertiesAnnotations, false);
		}

		List<AttributeData> ldapAttributes = find(primaryKey.toString(), currentLdapReturnAttributes);

		entriesAttributes.put(primaryKey.toString(), ldapAttributes);
		List<T> results = createEntities(entryClass, propertiesAnnotations, entriesAttributes);
		return results.get(0);
	}

	protected abstract List<AttributeData> find(String dn, String... attributes);

	protected boolean checkEntryClass(Class<?> entryClass, boolean isAllowSchemaEntry) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		Annotation ldapSchemaEntry = ReflectHelper.getAnnotationByType(entryAnnotations, LdapSchemaEntry.class);
		Annotation ldapEntry = ReflectHelper.getAnnotationByType(entryAnnotations, LdapEntry.class);
		if (isAllowSchemaEntry) {
			if ((ldapSchemaEntry == null) && (ldapEntry == null)) {
				throw new MappingException("Entry should has LdapEntry or LdapSchemaEntry annotation");
			}
		} else {
			if (ldapEntry == null) {
				throw new MappingException("Entry should has LdapEntry annotation");
			}
		}

		return true;
	}

	protected boolean isLdapSchemaEntry(Class<?> entryClass) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);

		return ReflectHelper.getAnnotationByType(entryAnnotations, LdapSchemaEntry.class) != null;
	}

	protected String[] getEntrySortBy(Class<?> entryClass) {
		if (entryClass == null) {
			throw new MappingException("Entry class is null");
		}

		// Check if entry is LDAP Entry
		List<Annotation> entryAnnotations = ReflectHelper.getClassAnnotations(entryClass, LDAP_ENTRY_TYPE_ANNOTATIONS);
		Annotation annotation = ReflectHelper.getAnnotationByType(entryAnnotations, LdapEntry.class);

		if (annotation == null) {
			return null;
		}

		return ((LdapEntry) annotation).sortBy();
	}

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
		Annotation ldapObjectClass = ReflectHelper.getAnnotationByType(entryAnnotations, LdapObjectClass.class);
		if (ldapObjectClass == null) {
			return EMPTY_STRING_ARRAY;
		}

		return ((LdapObjectClass) ldapObjectClass).values();
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

			AttributeData attribute = getAttribute(propertyName, propertyName, getter, entry, false);
			if (attribute != null) {
				for (String objectClass : attribute.getValues()) {
					result.add(objectClass);
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
			throw new MappingException("Entry should has property with annotation LdapDN");
		}

		if (propertiesAnnotations.size() > 1) {
			throw new MappingException("Entry should has only one property with annotation LdapDN");
		}

		return propertiesAnnotations.get(0).getPropertyName();
	}

	protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, Map<String, List<AttributeData>> entriesAttributes) {
		return createEntities(entryClass, propertiesAnnotations, entriesAttributes, true);
	}

	protected <T> List<T> createEntities(Class<T> entryClass, List<PropertyAnnotation> propertiesAnnotations, Map<String, List<AttributeData>> entriesAttributes, boolean doSort) {

		// Check if entry has DN property
		String dnProperty = getDNPropertyName(entryClass);

		// Get DN value
		Setter dnSetter = getSetter(entryClass, dnProperty);
		if (dnSetter == null) {
			throw new MappingException("Entry should has getter for property " + dnProperty);
		}

		List<T> results = new ArrayList<T>(entriesAttributes.size());
		for (Entry<String, List<AttributeData>> entryAttributes : entriesAttributes.entrySet()) {
			String dn = entryAttributes.getKey();
			List<AttributeData> attributes = entryAttributes.getValue();
			Map<String, AttributeData> attributesMap = getAttributesMap(attributes);

			T entry;
			List<String> customObjectClasses = null;
			try {
				entry = ReflectHelper.createObjectByDefaultConstructor(entryClass);
			} catch (Exception ex) {
				throw new MappingException(String.format("Entry %s should has default constructor", entryClass));
			}
			results.add(entry);

			dnSetter.set(entry, dn);

			// Set loaded properties to entry

			// Process properties with LdapAttribute annotation
			for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
				String propertyName = propertiesAnnotation.getPropertyName();
				Annotation ldapAttribute;

				ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttribute.class);
				if (ldapAttribute != null) {
					String ldapAttributeName = ((LdapAttribute) ldapAttribute).name();
					if (StringHelper.isEmpty(ldapAttributeName)) {
						ldapAttributeName = propertyName;
					}
					ldapAttributeName = ldapAttributeName.toLowerCase();

					AttributeData attributeData = attributesMap.get(ldapAttributeName);

					// Remove processed attributes
					attributesMap.remove(ldapAttributeName);

					if (((LdapAttribute) ldapAttribute).ignoreDuringRead()) {
						continue;
					}

					Setter setter = getSetter(entryClass, propertyName);
					if (setter == null) {
						throw new MappingException("Entry should has setter for property " + propertyName);
					}

					Annotation ldapJsonObject = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapJsonObject.class);
					boolean jsonObject = ldapJsonObject != null;

					setPropertyValue(propertyName, setter, entry, attributeData, jsonObject);
				}
			}

			// Process properties with LdapAttributesList annotation
			for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
				String propertyName = propertiesAnnotation.getPropertyName();
				Annotation ldapAttribute;

				ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttributesList.class);
				if (ldapAttribute != null) {
					Map<String, LdapAttribute> ldapAttributesConfiguration = new HashMap<String, LdapAttribute>();
					for (LdapAttribute ldapAttributeConfiguration : ((LdapAttributesList) ldapAttribute).attributesConfiguration()) {
						ldapAttributesConfiguration.put(ldapAttributeConfiguration.name(), ldapAttributeConfiguration);
					}

					Setter setter = getSetter(entryClass, propertyName);
					if (setter == null) {
						throw new MappingException("Entry should has setter for property " + propertyName);
					}

					List<Object> propertyValue = new ArrayList<Object>();
					setter.set(entry, propertyValue);

					Class<?> entryItemType = ReflectHelper.getListType(setter);
					if (entryItemType == null) {
						throw new MappingException("Entry property " + propertyName + " should has setter with specified element type");
					}

					String entryPropertyName = ((LdapAttributesList) ldapAttribute).name();
					Setter entryPropertyNameSetter = getSetter(entryItemType, entryPropertyName);
					if (entryPropertyNameSetter == null) {
						throw new MappingException("Entry should has setter for property " + propertyName + "." + entryPropertyName);
					}

					String entryPropertyValue = ((LdapAttributesList) ldapAttribute).value();
					Setter entryPropertyValueSetter = getSetter(entryItemType, entryPropertyValue);
					if (entryPropertyValueSetter == null) {
						throw new MappingException("Entry should has getter for property " + propertyName + "." + entryPropertyValue);
					}

					for (AttributeData entryAttribute : attributesMap.values()) {
						if (OBJECT_CLASS.equalsIgnoreCase(entryAttribute.getName())) {
							String[] objectClasses = entryAttribute.getValues();
							if (ArrayHelper.isEmpty(objectClasses)) {
								continue;
							}

							if (customObjectClasses == null) {
								customObjectClasses = new ArrayList<String>();
							}

							for (String objectClass : objectClasses) {
								customObjectClasses.add(objectClass);
							}

							continue;
						}

						LdapAttribute ldapAttributeConfiguration = ldapAttributesConfiguration.get(entryAttribute.getName());
						if ((ldapAttributeConfiguration != null) && ldapAttributeConfiguration.ignoreDuringRead()) {
							continue;
						}

						Object listItem = getListItem(propertyName, entryPropertyNameSetter, entryPropertyValueSetter, entryItemType,
								entryAttribute);
						if (listItem != null) {
							propertyValue.add(listItem);
						}
					}

					if (doSort) {
						sortLdapAttributesListIfNeeded((LdapAttributesList) ldapAttribute, entryItemType, propertyValue);
					}
				}
			}

			if ((customObjectClasses != null) && (customObjectClasses.size() > 0)) {
				setCustomObjectClasses(entry, entryClass, customObjectClasses.toArray(new String[0]));
			}
		}

		return results;
	}

	public <T> List<T> createEntities(Class<T> entryClass, Map<String, List<AttributeData>> entriesAttributes) {
		checkEntryClass(entryClass, true);
		List<PropertyAnnotation> propertiesAnnotations = getEntryPropertyAnnotations(entryClass);

		return createEntities(entryClass, propertiesAnnotations, entriesAttributes);
	}

	@SuppressWarnings("unchecked")
	private <T> void sortLdapAttributesListIfNeeded(LdapAttributesList ldapAttribute, Class<T> entryItemType, List<?> list) {
		if (!ldapAttribute.sortByName()) {
			return;
		}

		sortListByProperties(entryItemType, (List<T>) list, ldapAttribute.name());
	}

	protected <T> void sortEntriesIfNeeded(Class<T> entryClass, List<T> entries) {
		String[] sortByProperties = getEntrySortBy(entryClass);

		if (ArrayHelper.isEmpty(sortByProperties)) {
			return;
		}

		sortListByProperties(entryClass, entries, sortByProperties);
	}

	protected abstract <T> void sortListByProperties(Class<T> entryClass, List<T> entries, String... sortByProperties);

	private Map<String, AttributeData> getAttributesMap(List<AttributeData> attributes) {
		Map<String, AttributeData> attributesMap = new HashMap<String, AttributeData>(attributes.size());
		for (AttributeData attribute : attributes) {
			attributesMap.put(attribute.getName().toLowerCase(), attribute);
		}

		return attributesMap;
	}

	private AttributeData getAttribute(String propertyName, String ldapAttributeName, Getter propertyValueGetter, Object entry, boolean jsonObject) {
		Object propertyValue = propertyValueGetter.get(entry);
		if (propertyValue == null) {
			return null;
		}

		String[] attributeValues = new String[1];
		if (propertyValue instanceof String) {
			attributeValues[0] = StringHelper.toString(propertyValue);
		} else if (propertyValue instanceof Boolean) {
			attributeValues[0] = propertyValue.toString();
		} else if (propertyValue instanceof Integer) {
			attributeValues[0] = propertyValue.toString();
		} else if (propertyValue instanceof Long) {
			attributeValues[0] = propertyValue.toString();
		} else if (propertyValue instanceof Date) {
			attributeValues[0] = encodeGeneralizedTime((Date) propertyValue);
		} else if (propertyValue instanceof String[]) {
			attributeValues = (String[]) propertyValue;
		} else if (propertyValue instanceof List<?>) {
			attributeValues = new String[((List<?>) propertyValue).size()];
			int index = 0;
			for (Object tmpPropertyValue : (List<?>) propertyValue) {
				if (jsonObject) {
					attributeValues[index++] = convertJsonToString(tmpPropertyValue);
				} else {
					attributeValues[index++] = StringHelper.toString(tmpPropertyValue);
				}
			}
		} else if (propertyValue instanceof LdapEnum) {
			attributeValues[0] = ((LdapEnum) propertyValue).getValue();
		} else if (propertyValue instanceof LdapEnum[]) {
			LdapEnum[] propertyValues = (LdapEnum[]) propertyValue;
			attributeValues = new String[propertyValues.length];
			for (int i = 0; i < propertyValues.length; i++) {
				attributeValues[i] = (propertyValues[i] == null) ? null : propertyValues[i].getValue();
			}
		} else if (jsonObject) {
			attributeValues[0] = convertJsonToString(propertyValue);
		} else {
			throw new MappingException(
					"Entry property '" + propertyName + "' should has getter with String, String[], Boolean, Integer, Long, Date, List, LdapEnum or LdapEnum[] return type or has annotation LdapJsonObject");
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Property: %s, LdapProperty: %s, PropertyValue: %s", propertyName, ldapAttributeName,
					Arrays.toString(attributeValues)));
		}

		if (attributeValues.length == 0) {
			attributeValues = new String[] { null };
		} else if ((attributeValues.length == 1) && StringHelper.isEmpty(attributeValues[0])) {
			return null;
		}

		return new AttributeData(ldapAttributeName, attributeValues);
	}

	private String convertJsonToString(Object propertyValue) {
		try {
			String value = jsonObjectMapper.writeValueAsString(propertyValue);
			
			return value;
		} catch (Exception ex) {
			log.error("Failed to convert '{}' to json value:", propertyValue, ex);
			throw new MappingException(String.format("Failed to convert '%s' to json value", propertyValue));
		}
	}

	protected List<AttributeData> getAttributesListForPersist(Object entry, List<PropertyAnnotation> propertiesAnnotations) {
		// Prepare list of properties to persist
		List<AttributeData> attributes = new ArrayList<AttributeData>();
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			String propertyName = propertiesAnnotation.getPropertyName();
			Annotation ldapAttribute;

			// Process properties with LdapAttribute annotation
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttribute.class);
			if (ldapAttribute != null) {
				AttributeData attribute = getAttributeFromLdapAttribute(entry, ldapAttribute, propertiesAnnotation, propertyName);
				if (attribute != null) {
					attributes.add(attribute);
				}

				continue;
			}

			// Process properties with LdapAttributesList annotation
			ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttributesList.class);
			if (ldapAttribute != null) {
				
				List<AttributeData> listAttributes = getAttributesFromLdapAttributesList(entry, ldapAttribute, propertyName);
				if (listAttributes != null) {
					attributes.addAll(listAttributes);
				}

				continue;
			}
		}

		return attributes;
	}

	private AttributeData getAttributeFromLdapAttribute(Object entry, Annotation ldapAttribute,
			PropertyAnnotation propertiesAnnotation, String propertyName) {
		Class<?> entryClass = entry.getClass();

		String ldapAttributeName = ((LdapAttribute) ldapAttribute).name();
		if (StringHelper.isEmpty(ldapAttributeName)) {
			ldapAttributeName = propertyName;
		}

		Getter getter = getGetter(entryClass, propertyName);
		if (getter == null) {
			throw new MappingException("Entry should has getter for property " + propertyName);
		}

		Annotation ldapJsonObject = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapJsonObject.class);
		boolean jsonObject = ldapJsonObject != null;
		AttributeData attribute = getAttribute(propertyName, ldapAttributeName, getter, entry, jsonObject);

		return attribute;
	}

	private List<AttributeData> getAttributesFromLdapAttributesList(Object entry, Annotation ldapAttribute, String propertyName) {
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

		String entryPropertyName = ((LdapAttributesList) ldapAttribute).name();
		Getter entryPropertyNameGetter = getGetter(elementType, entryPropertyName);
		if (entryPropertyNameGetter == null) {
			throw new MappingException("Entry should has getter for property " + propertyName + "." + entryPropertyName);
		}

		String entryPropertyValue = ((LdapAttributesList) ldapAttribute).value();
		Getter entryPropertyValueGetter = getGetter(elementType, entryPropertyValue);
		if (entryPropertyValueGetter == null) {
			throw new MappingException("Entry should has getter for property " + propertyName + "." + entryPropertyValue);
		}

		for (Object entryAttribute : (List<?>) propertyValue) {
			AttributeData attribute = getAttribute(propertyName, entryPropertyNameGetter, entryPropertyValueGetter, entryAttribute, false);
			if (attribute != null) {
				listAttributes.add(attribute);
			}
		}
		
		return listAttributes;
	}

	protected <T> List<PropertyAnnotation> getEntryPropertyAnnotations(Class<T> entryClass) {
		return getEntryClassAnnotations(entryClass, "property_", LDAP_ENTRY_PROPERTY_ANNOTATIONS);
	}

	protected <T> List<PropertyAnnotation> getEntryDnAnnotations(Class<T> entryClass) {
		return getEntryClassAnnotations(entryClass, "dn_", LDAP_DN_PROPERTY_ANNOTATION);
	}

	protected <T> List<PropertyAnnotation> getEntryCustomObjectClassAnnotations(Class<T> entryClass) {
		return getEntryClassAnnotations(entryClass, "custom_", LDAP_CUSTOM_OBJECT_CLASS_PROPERTY_ANNOTATION);
	}

	protected <T> List<PropertyAnnotation> getEntryClassAnnotations(Class<T> entryClass, String keyCategory, Class<?>[] annotationTypes) {
		String key = keyCategory + entryClass.getName();

		List<PropertyAnnotation> annotations = classAnnotations.get(key);
		if (annotations == null) {
			synchronized (classAnnotationsLock) {
				annotations = classAnnotations.get(key);
				if (annotations == null) {
					Map<String, List<Annotation>> annotationsMap = ReflectHelper.getPropertiesAnnotations(entryClass, annotationTypes);
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
			synchronized (classGettersLock) {
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
			synchronized (classSettersLock) {
				setter = classSetters.get(key);
				if (setter == null) {
					setter = ReflectHelper.getSetter(entryClass, propertyName);
					classSetters.put(key, setter);
				}
			}
		}

		return setter;
	}

	private AttributeData getAttribute(String propertyName, Getter propertyNameGetter, Getter propertyValueGetter, Object entry, boolean jsonObject) {
		Object ldapAttributeName = propertyNameGetter.get(entry);
		if (ldapAttributeName == null) {
			return null;
		}

		return getAttribute(propertyName, ldapAttributeName.toString(), propertyValueGetter, entry, jsonObject);
	}

	private void setPropertyValue(String propertyName, Setter propertyValueSetter, Object entry, AttributeData attribute, boolean jsonObject) {
		if (attribute == null) {
			return;
		}

		log.debug(String.format("LdapProperty: %s, AttributeName: %s, AttributeValue: %s", propertyName, attribute.getName(),
				Arrays.toString(attribute.getValues())));

		Class<?> parameterType = ReflectHelper.getSetterType(propertyValueSetter);
		if (parameterType.equals(String.class)) {
			propertyValueSetter.set(entry, attribute.getValue());
		} else if (parameterType.equals(Boolean.class) || parameterType.equals(Boolean.TYPE)) {
			propertyValueSetter.set(entry, attribute.getValue() == null ? null : Boolean.valueOf(attribute.getValue()));
		} else if (parameterType.equals(Integer.class) || parameterType.equals(Integer.TYPE)) {
			propertyValueSetter.set(entry, attribute.getValue() == null ? null : Integer.valueOf(attribute.getValue()));
		} else if (parameterType.equals(Long.class) || parameterType.equals(Long.TYPE)) {
			propertyValueSetter.set(entry, attribute.getValue() == null ? null : Long.valueOf(attribute.getValue()));
		} else if (parameterType.equals(Date.class)) {
			propertyValueSetter.set(entry, decodeGeneralizedTime(attribute.getValue()));
		} else if (parameterType.equals(String[].class)) {
			propertyValueSetter.set(entry, attribute.getValues());
		} else if (ReflectHelper.assignableFrom(parameterType, List.class)) {
			if (jsonObject) {
				String[] stringValues = attribute.getValues();
				List<Object> jsonValues = new ArrayList<Object>(stringValues.length);
				
				for (String stringValue : stringValues) {
					Object jsonValue = convertStringToJson(entry, ReflectHelper.getListType(propertyValueSetter), stringValue);
					jsonValues.add(jsonValue);
				}
				propertyValueSetter.set(entry, jsonValues);
			} else {
				propertyValueSetter.set(entry, Arrays.asList(attribute.getValues()));
			}
		} else if (ReflectHelper.assignableFrom(parameterType, LdapEnum.class)) {
			try {
				propertyValueSetter.set(
						entry,
						parameterType.getMethod("resolveByValue", String.class).invoke(parameterType.getEnumConstants()[0],
								attribute.getValue()));
			} catch (Exception ex) {
				throw new MappingException("Failed to resolve Enum by value " + attribute.getValue(), ex);
			}
		} else if (ReflectHelper.assignableFrom(parameterType, LdapEnum[].class)) {
			Class<?> itemType = parameterType.getComponentType();
			Method enumResolveByValue;
			try {
				enumResolveByValue = itemType.getMethod("resolveByValue", String.class);
			} catch (Exception ex) {
				throw new MappingException("Failed to resolve Enum by value " + Arrays.toString(attribute.getValues()), ex);
			}

			String[] attributeValues = attribute.getValues();
			LdapEnum[] ldapEnums = (LdapEnum[]) ReflectHelper.createArray(itemType, attributeValues.length);
			for (int i = 0; i < attributeValues.length; i++) {
				try {
					ldapEnums[i] = (LdapEnum) enumResolveByValue.invoke(itemType.getEnumConstants()[0], attributeValues[i]);
				} catch (Exception ex) {
					throw new MappingException("Failed to resolve Enum by value " + Arrays.toString(attribute.getValues()), ex);
				}
			}
			propertyValueSetter.set(entry, ldapEnums);
		} else if (jsonObject) {
			String stringValue = attribute.getValue();
			Object jsonValue = convertStringToJson(entry, parameterType, stringValue);
			propertyValueSetter.set(entry, jsonValue);
		} else {
			throw new MappingException(
					"Entry property '" + propertyName + "' should has setter with String, Boolean, Integer, Long, Date, String[], List, LdapEnum or LdapEnum[] parameter type or has annotation LdapJsonObject");
		}
	}

	private Object convertStringToJson(Object entry, Class<?> parameterType, String stringValue) {
		try {
			Object jsonValue = jsonObjectMapper.readValue(stringValue, parameterType);

			return jsonValue;
		} catch (Exception ex) {
			log.error("Failed to convert json value '{}' to object: ", stringValue, ex);
			throw new MappingException(String.format("Failed to convert json value '%s' to object", stringValue));
		}
	}

	private Object getListItem(String propertyName, Setter propertyNameSetter, Setter propertyValueSetter, Class<?> classType,
			AttributeData attribute) {
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

		return result;
	}

	protected abstract String encodeGeneralizedTime(Date date);

	protected abstract Date decodeGeneralizedTime(String date);

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

	protected <T> String getEntryKey(T entry, boolean caseSensetive, Getter[] propertyGetters) {
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

	protected String getEntryKey(Object dnValue, boolean caseSensetive, List<PropertyAnnotation> propertiesAnnotations,
			List<AttributeData> attributesData) {
		StringBuilder sb = new StringBuilder("_HASH__").append(((String) dnValue).toLowerCase()).append("__");

		List<String> processedProperties = new ArrayList<String>();
		Map<String, AttributeData> attributesDataMap = getAttributesDataMap(attributesData);
		for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
			Annotation ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(), LdapAttribute.class);
			if (ldapAttribute == null) {
				continue;
			}

			String ldapAttributeName = ((LdapAttribute) ldapAttribute).name();
			if (StringHelper.isEmpty(ldapAttributeName)) {
				ldapAttributeName = propertiesAnnotation.getPropertyName();
			}

			processedProperties.add(ldapAttributeName);

			String values[] = null;

			AttributeData attributeData = attributesDataMap.get(ldapAttributeName);
			if ((attributeData != null) && (attributeData.getValues() != null)) {
				values = attributeData.getValues().clone();
				Arrays.sort(values);
			}

			addPropertyWithValuesToKey(sb, ldapAttributeName, values);
		}

		for (AttributeData attributeData : attributesData) {
			if (processedProperties.contains(attributeData.getName())) {
				continue;
			}

			addPropertyWithValuesToKey(sb, attributeData.getName(), attributeData.getValues());
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
		if (log.isDebugEnabled()) {
			log.debug(String.format("Entry key HashCode is: %s", key.hashCode()));
		}

		return key.hashCode();
	}

	public abstract int getSupportedLDAPVersion();

	public boolean isOpen() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void refresh(Object entry) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void clear() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void close() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public Query createNamedQuery(String name) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public Query createNativeQuery(String sqlString) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public Query createNativeQuery(String sqlString, @SuppressWarnings("rawtypes") Class resultClass) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public Query createQuery(String qlString) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void flush() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public Object getDelegate() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public FlushModeType getFlushMode() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public <T> T getReference(Class<T> entryClass, Object primaryKey) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public EntityTransaction getTransaction() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void joinTransaction() {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void lock(Object entry, LockModeType lockMode) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	public void setFlushMode(FlushModeType flushMode) {
		throw new UnsupportedOperationException("Method not implemented.");
	}

}
