/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.jans.model.SchemaEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.hybrid.impl.HybridEntryManagerFactory;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidSchemaUpdateException;
import org.slf4j.Logger;

import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;

/**
 * Provides operations with LDAP schema
 *
 * @author Yuriy Movchan Date: 10.14.2010
 */
@ApplicationScoped
@Named
public class SchemaService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;


    @Inject
    private DataSourceTypeService dataSourceTypeService;

    @Inject
    private AttributeService attributeService;

    /**
     * Load schema from DS
     *
     * @return Schema
     */
    public SchemaEntry getSchema() {
    	String shemaDn = getDnForSchema();
    	if (StringHelper.isNotEmpty(shemaDn)) { 
        	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
    		SchemaEntry schemaEntry = ldapPersistenceEntryManager.find(getDnForSchema(), SchemaEntry.class, null);
            return schemaEntry;
    	}
    	
    	return null;

    }

    /**
     * Add new object class with specified attributes
     *
     * @param objectClass
     *            Object class name
     * @param attributeTypes
     *            Attribute types
     */
    public void addObjectClass(String objectClass, String attributeTypes, String schemaAddObjectClassWithoutAttributeTypesDefinition,
            String schemaAddObjectClassWithAttributeTypesDefinition) {
        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());

        String objectClassDefinition;
        if (StringHelper.isEmpty(attributeTypes)) {
            objectClassDefinition = String.format(schemaAddObjectClassWithoutAttributeTypesDefinition, objectClass, objectClass);
        } else {
            objectClassDefinition = String.format(schemaAddObjectClassWithAttributeTypesDefinition, objectClass, objectClass, attributeTypes);
        }
        schemaEntry.addObjectClass(objectClassDefinition);

        log.debug("Adding new objectClass: {}", schemaEntry);
    	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
        ldapPersistenceEntryManager.merge(schemaEntry);
    }

    /**
     * Remove object class
     *
     * @param objectClass
     *            Object class name
     */
    public void removeObjectClass(String objectClass) {
        SchemaEntry schema = getSchema();

        String objectClassDefinition = getObjectClassDefinition(schema, objectClass);
        if (objectClassDefinition != null) {
            removeObjectClassWithDefinition(objectClassDefinition);
        }
    }

    private void removeObjectClassWithDefinition(String objectClassDefinition) {
        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());
        schemaEntry.addObjectClass(objectClassDefinition);

        log.debug("Removing objectClass: {}", schemaEntry);
    	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
    	ldapPersistenceEntryManager.remove(schemaEntry);
    }

    /**
     * Add attribute type to object class
     *
     * @param objectClass
     *            Object class name
     * @param attributeType
     *            Attribute type name
     * @throws Exception
     */
    public void addAttributeTypeToObjectClass(String objectClass, String attributeType) throws Exception {
        SchemaEntry schema = getSchema();

        String objectClassDefinition = getObjectClassDefinition(schema, objectClass);
        if (objectClassDefinition == null) {
            throw new InvalidSchemaUpdateException(
                    String.format("Can't add attributeType %s to objectClass %s because objectClass doesn't exist", attributeType, objectClass));
        }

        String newObjectClassDefinition = null;
        String attributeTypesStartPattern = "MAY ( ";
        int index = objectClassDefinition.indexOf(attributeTypesStartPattern);
        if (index != -1) {
            int index2 = objectClassDefinition.indexOf(")", index);
            newObjectClassDefinition = objectClassDefinition.substring(0, index2) + "$ " + attributeType + " "
                    + objectClassDefinition.substring(index2);
        } else {
            attributeTypesStartPattern = "MUST objectClass ";
            index = objectClassDefinition.indexOf(attributeTypesStartPattern);
            if (index != -1) {
                int index2 = index + attributeTypesStartPattern.length();
                newObjectClassDefinition = objectClassDefinition.substring(0, index2) + "MAY ( " + attributeType + " ) "
                        + objectClassDefinition.substring(index2);
            }
        }

        log.debug("Current object class definition:" + objectClassDefinition);
        log.debug("New object class definition:" + newObjectClassDefinition);

        if (newObjectClassDefinition == null) {
            throw new InvalidSchemaUpdateException(String.format("Invalid objectClass definition format"));
        }

        // Remove OC definition
        removeObjectClassWithDefinition(objectClassDefinition);

        // Add updated OC definition
        SchemaEntry newSchemaEntry = new SchemaEntry();
        newSchemaEntry.setDn(getDnForSchema());
        newSchemaEntry.addObjectClass(newObjectClassDefinition);

        log.debug("Adding attributeType to objectClass: {}", newSchemaEntry);
    	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
    	ldapPersistenceEntryManager.merge(newSchemaEntry);
    }

    /**
     * Remove attribute type from object class
     *
     * @param objectClass
     *            Object class name
     * @param attributeType
     *            Attribute type name
     * @throws Exception
     */
    public void removeAttributeTypeFromObjectClass(String objectClass, String attributeType) throws Exception {
        SchemaEntry schema = getSchema();

        String objectClassDefinition = getObjectClassDefinition(schema, objectClass);
        if (objectClassDefinition == null) {
            throw new InvalidSchemaUpdateException(
                    String.format("Can't add attributeType %s to objectClass %s because objectClass doesn't exist", attributeType, objectClass));
        }

        String attributeTypePattern = "$ " + attributeType + " ";
        int index = objectClassDefinition.indexOf(attributeTypePattern);
        if (index == -1) {
            attributeTypePattern = " " + attributeType + " $";
            index = objectClassDefinition.indexOf(attributeTypePattern);
            if (index == -1) {
                attributeTypePattern = " MAY ( " + attributeType + " )";
                index = objectClassDefinition.indexOf(attributeTypePattern);
                if (index == -1) {
                    throw new InvalidSchemaUpdateException(String.format("Invalid objectClass definition format"));
                }
            }
        }

        String newObjectClassDefinition = objectClassDefinition.substring(0, index)
                + objectClassDefinition.substring(index + attributeTypePattern.length());

        // Remove OC definition
        removeObjectClassWithDefinition(objectClassDefinition);

        // Add updated OC definition
        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());
        schemaEntry.addObjectClass(newObjectClassDefinition);

        log.debug("Removing attributeType from objectClass: {}", schemaEntry);
    	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
    	ldapPersistenceEntryManager.merge(schemaEntry);

    }

    /**
     * Add new attribute type
     */
    public void addStringAttribute(String oid, String name, String schemaAddAttributeDefinition) throws Exception {
        log.info("getting a new instance SchemaEntry ");
        SchemaEntry schemaEntry = new SchemaEntry();
        log.info("setting the DN ");
        schemaEntry.setDn(getDnForSchema());
        log.info("adding attribute name ");
        log.info("applicationConfiguration.getSchemaAddAttributeDefinition() : ", schemaAddAttributeDefinition);
        log.info("oid : ", oid);
        log.info("name : ", name);
        schemaEntry.addAttributeType(String.format(schemaAddAttributeDefinition, oid, name));
        log.debug("Adding new attributeType: {}", schemaEntry);
        log.info("merging data");
    	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
    	ldapPersistenceEntryManager.merge(schemaEntry);
    }

    /**
     * Remove string attribute
     *
     * @param attributeType
     *            Attribute type name
     * @throws Exception
     */
    public void removeStringAttribute(String attributeType) throws Exception {
        SchemaEntry schema = getSchema();

        String attributeTypeDefinition = getAttributeTypeDefinition(schema, attributeType);
        if (attributeTypeDefinition != null) {
            SchemaEntry schemaEntry = new SchemaEntry();
            schemaEntry.setDn(getDnForSchema());
            schemaEntry.addAttributeType(attributeTypeDefinition);

            log.debug("Removing attributeType: {}", schemaEntry);
        	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
        	ldapPersistenceEntryManager.remove(schemaEntry);

        }
    }

    /**
     * Get attribute type schema definition string
     *
     * @param schemaEntry
     *            Schema
     * @param attributeType
     *            Attribute type name
     * @return Attribute type schema definition string
     */
    public String getAttributeTypeDefinition(SchemaEntry schemaEntry, String attributeType) {
        if ((schemaEntry == null) || (attributeType == null)) {
            return null;
        }

        String lowerCaseAttributeType = StringHelper.toLowerCase(attributeType);

        List<AttributeTypeDefinition> attributeTypes = getAttributeTypeDefinitions(schemaEntry,
                Arrays.asList(new String[] { lowerCaseAttributeType }));
        AttributeTypeDefinition attributeTypeDefinition = getAttributeTypeDefinition(attributeTypes, lowerCaseAttributeType);

        return (attributeTypeDefinition == null) ? null : attributeTypeDefinition.toString();
    }

    /**
     * Get attribute type schema definition string
     *
     * @param attributeTypes
     * @param attributeType
     *            Attribute type name
     * @return Attribute type schema definition string
     */
    public AttributeTypeDefinition getAttributeTypeDefinition(List<AttributeTypeDefinition> attributeTypes, String attributeType) {
        if (attributeTypes == null || attributeType == null) {
            return null;
        }

        for (AttributeTypeDefinition definition : attributeTypes) {
            for (String name : definition.getNames()) {
                if (name.equalsIgnoreCase(attributeType)) {
                    return definition;
                }
            }
        }

        return null;
    }

    public List<AttributeTypeDefinition> getAttributeTypeDefinitions(SchemaEntry schemaEntry, List<String> attributeNames) {
        if (schemaEntry == null) {
            return null;
        }

        String[] attrs = attributeNames.toArray(new String[attributeNames.size()]);
        for (int i = 0; i < attrs.length; i++) {
            attrs[i] = "'" + attrs[i].toLowerCase() + "'";
        }

        List<AttributeTypeDefinition> result = new ArrayList<AttributeTypeDefinition>();
        for (String attributeTypeDefinition : schemaEntry.getAttributeTypes()) {
            for (String name : attrs) {
                if (attributeTypeDefinition.toLowerCase().contains(name)) {
                    try {
                        result.add(new AttributeTypeDefinition(attributeTypeDefinition));
                    } catch (Exception ex) {
                        log.error("Failed to get attribute type definition by string {}", ex, attributeTypeDefinition);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get object class schema definition string
     *
     * @param schemaEntry
     *            Schema
     * @param objectClass
     *            Object class name
     * @return Object class schema definition string
     */
    public String getObjectClassDefinition(SchemaEntry schemaEntry, String objectClass) {
        if ((schemaEntry == null) || (objectClass == null)) {
            return null;
        }

        for (String objectClassDefinition : schemaEntry.getObjectClasses()) {
            ObjectClassDefinition definition;
            try {
                definition = new ObjectClassDefinition(objectClassDefinition);

                for (String name : definition.getNames()) {
                    if (name.equalsIgnoreCase(objectClass)) {
                        return objectClassDefinition;
                    }
                }
            } catch (Exception ex) {
            }
        }

        return null;
    }

    /**
     * Get all attribute names by specified object classes
     *
     * @param schemaEntry
     *            Schema
     * @param OBJECT_CLASS
     *            Object class name
     * @return Object class schema definition string
     */
    public Set<String> getObjectClassesAttributes(SchemaEntry schemaEntry, String[] objectClasses) {
        if ((schemaEntry == null) || (objectClasses == null)) {
            return null;
        }

        Map<String, ObjectClassDefinition> objectClassDefinitions = new HashMap<String, ObjectClassDefinition>();
        for (String objectClassDefinition : schemaEntry.getObjectClasses()) {
            ObjectClassDefinition definition;
            try {
                definition = new ObjectClassDefinition(objectClassDefinition);

                for (String name : definition.getNames()) {
                    objectClassDefinitions.put(StringHelper.toLowerCase(name), definition);
                }
            } catch (Exception ex) {
                log.error("Failed to parse LDAP object class definition: '{}'", ex, objectClassDefinition);
            }
        }

        Set<ObjectClassDefinition> resultObjectClassDefinitions = getSuperiorClasses(objectClassDefinitions, objectClasses, true);
        Set<String> resultAttributes = getAttributes(resultObjectClassDefinitions, true, true);

        return resultAttributes;
    }

    private Set<String> getAttributes(Set<ObjectClassDefinition> objectClassDefinitions, boolean includeRequired, boolean includeOpional) {
        final LinkedHashSet<String> resultAttributes = new LinkedHashSet<String>();
        for (final ObjectClassDefinition objectClassDefinition : objectClassDefinitions) {
            if (includeRequired) {
                for (String attribute : objectClassDefinition.getRequiredAttributes()) {
                    resultAttributes.add(StringHelper.toLowerCase(attribute));
                }
            }

            if (includeOpional) {
                for (String attribute : objectClassDefinition.getOptionalAttributes()) {
                    resultAttributes.add(StringHelper.toLowerCase(attribute));
                }
            }
        }

        return resultAttributes;
    }

    public Set<ObjectClassDefinition> getSuperiorClasses(final Map<String, ObjectClassDefinition> objectClassDefinitions,
            final String[] superiorClasses, final boolean recursive) {
        final LinkedHashSet<ObjectClassDefinition> resultObjectClassDefinitions = new LinkedHashSet<ObjectClassDefinition>();
        for (final String superiorClass : superiorClasses) {
            final ObjectClassDefinition objectClassDefinition = objectClassDefinitions.get(StringHelper.toLowerCase(superiorClass));
            if (objectClassDefinition != null) {
                resultObjectClassDefinitions.add(objectClassDefinition);
                if (recursive) {
                    getSuperiorClasses(objectClassDefinitions, objectClassDefinition, resultObjectClassDefinitions);
                }
            }
        }

        return Collections.unmodifiableSet(resultObjectClassDefinitions);
    }

    private static void getSuperiorClasses(final Map<String, ObjectClassDefinition> objectClassDefinitions,
            final ObjectClassDefinition objectClassDefinition, final Set<ObjectClassDefinition> resultObjectClassDefinitions) {
        for (final String superiorClass : objectClassDefinition.getSuperiorClasses()) {
            final ObjectClassDefinition superiorObjectClassDefinition = objectClassDefinitions.get(StringHelper.toLowerCase(superiorClass));
            if (superiorObjectClassDefinition != null) {
                resultObjectClassDefinitions.add(superiorObjectClassDefinition);
                getSuperiorClasses(objectClassDefinitions, superiorObjectClassDefinition, resultObjectClassDefinitions);
            }
        }
    }

    /**
     * Check if schema contains specified attribute
     *
     * @param attributeType
     *            Attribute type name
     * @return True if schema contains specified attribute
     */
    public boolean containsAttributeTypeInSchema(String attributeType) {
        SchemaEntry schema = getSchema();

        return getAttributeTypeDefinition(schema, attributeType) != null;
    }

    /**
     * Determine object classes by attribute name
     *
     * @param schemaEntry
     *            Schema
     * @param OBJECT_CLASS
     *            Object class name
     * @return List of object classes
     */
    public Set<String> getObjectClassesByAttribute(SchemaEntry schemaEntry, String attributeType) {
        if ((schemaEntry == null) || StringHelper.isEmpty(attributeType)) {
            return null;
        }

        String lowerCaseAttributeType = StringHelper.toLowerCase(attributeType);

        Set<String> resultObjectClasses = new HashSet<String>();
        for (String objectClassDefinition : schemaEntry.getObjectClasses()) {
            ObjectClassDefinition definition;
            try {
                definition = new ObjectClassDefinition(objectClassDefinition);

                Set<String> objectClassAttributeTypes = new HashSet<String>();
                for (String name : definition.getOptionalAttributes()) {
                    objectClassAttributeTypes.add(StringHelper.toLowerCase(name));
                }
                for (String name : definition.getRequiredAttributes()) {
                    objectClassAttributeTypes.add(StringHelper.toLowerCase(name));
                }

                if (objectClassAttributeTypes.contains(lowerCaseAttributeType)) {
                    String objectClassType = definition.getNameOrOID();
                    resultObjectClasses.add(objectClassType);
                }
            } catch (Exception ex) {
                log.error("Failed to parse LDAP object class definition: '{}'", ex, objectClassDefinition);
            }
        }

        return resultObjectClasses;
    }

    /**
     * Build DN string for DS schema
     *
     * @return DN string for DS schema
     */
    public String getDnForSchema() {
    	PersistenceEntryManager ldapPersistenceEntryManager = getPersistenceEntryManager();
    	if (ldapPersistenceEntryManager != null) {
            return ((LdapOperationService) ldapPersistenceEntryManager.getOperationService()).getSubschemaSubentry();
    	} else {
    		return "";
    	}
    }
    
    private PersistenceEntryManager getPersistenceEntryManager() {
        if (dataSourceTypeService.isLDAP(attributeService.getDnForAttribute(null))) {
        	if (persistenceEntryManager.getPersistenceType().equals(HybridEntryManagerFactory.PERSISTENCE_TYPE)) {
        		return persistenceEntryManager.getPersistenceEntryManager(LdapEntryManagerFactory.PERSISTENCE_TYPE);
        	}
        	
        	return persistenceEntryManager;
        }
        
        return null;
    }

}
