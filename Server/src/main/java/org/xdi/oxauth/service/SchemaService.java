package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.exception.InvalidSchemaUpdateException;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.ldap.SchemaEntry;
import org.xdi.oxauth.model.register.RegisterRequestParam;
import org.xdi.util.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Schema service
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.1, 10.14.2010
 */
@Scope(ScopeType.APPLICATION)
@Name("schemaService")
@AutoCreate
@Startup
public class SchemaService {

    // save attribute in this file, because OX related object classes are saved there, otherwise error occurs
    public static final String MODIFICATION_FILE_NAME = "101-ox.ldif";

    private static final String ATTR_VALUE_TEMPLATE = "( %1$s-oid NAME '%1$s' SUBSTR caseIgnoreSubstringsMatch EQUALITY caseIgnoreMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-ORIGIN 'Gluu - dynamic registration custom attribute' X-SCHEMA-FILE '" + SchemaService.MODIFICATION_FILE_NAME + "' )";
    private static final String OPTIONAL_ATTR_IDENTIFIER = "MAY (";

    //    private static final String schemaAddAttributeDefinition = "( %s-oid NAME '%s' EQUALITY caseIgnoreMatch ORDERING caseIgnoreOrderingMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 USAGE userApplications X-SCHEMA-FILE '" + MODIFICATION_FILE_NAME + "' X-ORIGIN 'gluu' )";
    private static final String schemaAddObjectClassWithoutAttributeTypesDefinition = "( %s-oid NAME '%s' SUP top STRUCTURAL MUST objectClass X-SCHEMA-FILE '" + MODIFICATION_FILE_NAME + "' X-ORIGIN 'gluu' )";
    private static final String schemaAddObjectClassWithAttributeTypesDefinition = "( %s-oid NAME '%s' SUP top STRUCTURAL MUST objectClass MAY ( %s ) X-SCHEMA-FILE '" + MODIFICATION_FILE_NAME + "' X-ORIGIN 'gluu' )";

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    public void createCustomAttributes() {
        try {
            final List<String> list = ConfigurationFactory.getConfiguration().getDynamicRegistrationCustomAttributes();

            if (list != null && !list.isEmpty()) {
                for (String attr : list) {
                    if (RegisterRequestParam.isCustomParameterValid(attr)) {
                        addStringAttribute(attr);
                        addAttributeTypeToObjectClass("oxAuthClient", attr);
                    }
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
    }

//    public void addAttributeToSchemaAndObjectClass(String p_attribute) {
//        final LDAPConnectionPool pool = ldapEntryManager.getLdapOperationService().getConnectionPool();
//        if (pool != null && StringUtils.isNotBlank(p_attribute)) {
//            try {
//                final String attrValue = String.format(ATTR_VALUE_TEMPLATE, p_attribute);
//                log.debug("Try to add custom attribute: " + attrValue);
//                final LDAPResult addAttrResult = pool.modify("cn=schema", new Modification(
//                        ModificationType.ADD, "attributeTypes", attrValue
//                ));
//
//                if (addAttrResult != null && addAttrResult.getResultCode() == ResultCode.SUCCESS) {
//                    final ObjectClassDefinition objectClass = pool.getSchema().getObjectClass("oxAuthClient");
//                    if (objectClass != null) {
//                        final String objectClassAsStr = objectClass.toString();
//                        if (StringUtils.isNotBlank(objectClassAsStr)) {
//                            final String[] optionalAttributes = objectClass.getOptionalAttributes();
//                            if (!ArrayUtils.isEmpty(optionalAttributes) && !Arrays.asList(optionalAttributes).contains(p_attribute)) {
//                                int index = objectClassAsStr.indexOf(OPTIONAL_ATTR_IDENTIFIER);
//                                if (index != -1) {
//                                    index = index + OPTIONAL_ATTR_IDENTIFIER.length();
//                                    final StringBuilder toInsert = new StringBuilder(" ");
//                                    toInsert.append(p_attribute).append(" $");
//
//                                    final StringBuilder sb = new StringBuilder(objectClassAsStr);
//                                    sb.insert(index, toInsert.toString());
//
//                                    final String newObjClassValue = sb.toString();
//
//                                    log.debug("Try to add custom attribute to object class: " + newObjClassValue);
//                                    pool.modify("cn=schema", new Modification(
//                                            ModificationType.ADD, "objectClasses", newObjClassValue
//                                    ));
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (LDAPException e) {
//                log.debug(e.getMessage(), e);
//            }
//        }
//    }

    /**
     * Load schema from DS
     *
     * @return Schema
     */
    public SchemaEntry getSchema() {
        return ldapEntryManager.find(SchemaEntry.class, getDnForSchema());
    }

    /**
     * Add new object class with specified attributes
     *
     * @param objectClass    Object class name
     * @param attributeTypes Attribute types
     */
    public void addObjectClass(String objectClass, String attributeTypes) {
        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());

        String objectClassDefinition;
        if (StringHelper.isEmpty(attributeTypes)) {
            objectClassDefinition = String.format(schemaAddObjectClassWithoutAttributeTypesDefinition, objectClass, objectClass);
        } else {
            objectClassDefinition = String.format(schemaAddObjectClassWithAttributeTypesDefinition, objectClass, objectClass, attributeTypes);
        }
        schemaEntry.addObjectClass(objectClassDefinition);

        log.debug("Adding new objectClass: {0}", schemaEntry);
        ldapEntryManager.merge(schemaEntry);
    }

    /**
     * Remove object class
     *
     * @param objectClass Object class name
     */
    public void removeObjectClass(String objectClass) {
        SchemaEntry schema = getSchema();

        String objectClassDefinition = getObjectClassDefinitionString(schema, objectClass);
        if (objectClassDefinition != null) {
            SchemaEntry schemaEntry = new SchemaEntry();
            schemaEntry.setDn(getDnForSchema());
            schemaEntry.addObjectClass(objectClassDefinition);

            log.debug("Removing objectClass: {0}", schemaEntry);
            ldapEntryManager.remove(schemaEntry);
        }
    }

    /**
     * Add attribute type to object class
     *
     * @param objectClass   Object class name
     * @param attributeType Attribute type name
     */
    public void addAttributeTypeToObjectClass(String objectClass, String attributeType) {
        final SchemaEntry schema = getSchema();

        final String objectClassDefinition = getObjectClassDefinitionString(schema, objectClass);
        if (objectClassDefinition == null) {
            throw new InvalidSchemaUpdateException(String.format("Can't add attributeType %s to objectClass %s because objectClass doesn't exist", attributeType, objectClass));
        }

        // check whether attribute is already in object class, if yes then skip
        try {
            ObjectClassDefinition definition = new ObjectClassDefinition(objectClassDefinition);
            final String[] optionalAttributes = definition.getOptionalAttributes();
            final String[] requiredAttributes = definition.getRequiredAttributes();
            if (requiredAttributes != null && Arrays.asList(requiredAttributes).contains(attributeType)) {
                log.debug("Skip add attribute to object class '{0}', attribute '{1}' is already in required attributes.", objectClass, attributeType);
                return;
            }
            if (optionalAttributes != null && Arrays.asList(optionalAttributes).contains(attributeType)) {
                log.debug("Skip add attribute to object class '{0}', attribute '{1}' is already in optional attributes.", objectClass, attributeType);
                return;
            }
        } catch (LDAPException e) {
            log.debug(e.getMessage(), e);
            return;
        }

        String newObjectClassDefinition = null;
        String attributeTypesStartPattern = "MAY ( ";
        int index = objectClassDefinition.indexOf(attributeTypesStartPattern);
        if (index != -1) {
            int index2 = objectClassDefinition.indexOf(")", index);
            newObjectClassDefinition = objectClassDefinition.substring(0, index2) + "$ " + attributeType + " " + objectClassDefinition.substring(index2);
        } else {
            attributeTypesStartPattern = "MUST objectClass ";
            index = objectClassDefinition.indexOf(attributeTypesStartPattern);
            if (index != -1) {
                int index2 = index + attributeTypesStartPattern.length();
                newObjectClassDefinition = objectClassDefinition.substring(0, index2) + "MAY ( " + attributeType + " ) " + objectClassDefinition.substring(index2);
            }
        }

        log.debug("Current object class definition:" + objectClassDefinition);
        log.debug("New object class definition:" + newObjectClassDefinition);

        if (newObjectClassDefinition == null) {
            throw new InvalidSchemaUpdateException(String.format("Invalid objectClass definition format"));
        }

        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());
        schemaEntry.addObjectClass(newObjectClassDefinition);

        log.debug("Adding attributeType to objectClass: {0}", schemaEntry);
        ldapEntryManager.merge(schemaEntry);
    }

    /**
     * Remove attribute type from object class
     *
     * @param objectClass   Object class name
     * @param attributeType Attribute type name
     */
    public void removeAttributeTypeFromObjectClass(String objectClass, String attributeType) {
        SchemaEntry schema = getSchema();

        String objectClassDefinition = getObjectClassDefinitionString(schema, objectClass);
        if (objectClassDefinition == null) {
            throw new InvalidSchemaUpdateException(String.format("Can't add attributeType %s to objectClass %s because objectClass doesn't exist", attributeType, objectClass));
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

        String newObjectClassDefinition = objectClassDefinition.substring(0, index) + objectClassDefinition.substring(index + attributeTypePattern.length());

        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());
        schemaEntry.addObjectClass(newObjectClassDefinition);

        log.debug("Removing attributeType from objectClass: {0}", schemaEntry);
        ldapEntryManager.merge(schemaEntry);
    }

    /**
     * Add new attribute type
     *
     * @param name attribute name
     */
    public void addStringAttribute(String name) {
        final SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setDn(getDnForSchema());
        schemaEntry.addAttributeType(String.format(ATTR_VALUE_TEMPLATE, name));

        log.debug("Adding new attributeType: {0}", schemaEntry);
        ldapEntryManager.merge(schemaEntry);
    }

    /**
     * Remove string attribute
     *
     * @param attributeType Attribute type name
     */
    public void removeStringAttribute(String attributeType) {
        SchemaEntry schema = getSchema();

        String attributeTypeDefinition = getAttributeTypeDefinition(schema, attributeType);
        if (attributeTypeDefinition != null) {
            SchemaEntry schemaEntry = new SchemaEntry();
            schemaEntry.setDn(getDnForSchema());
            schemaEntry.addAttributeType(attributeTypeDefinition);

            log.debug("Removing attributeType: {0}", schemaEntry);
            ldapEntryManager.remove(schemaEntry);
        }
    }

    /**
     * Get attribute type schema definition string
     *
     * @param schemaEntry   Schema
     * @param attributeType Attribute type name
     * @return Attribute type schema definition string
     */
    public String getAttributeTypeDefinition(SchemaEntry schemaEntry, String attributeType) {
        if ((schemaEntry == null) || (attributeType == null)) {
            return null;
        }

        List<AttributeTypeDefinition> attributeTypes = getAttributeTypeDefinitions(schemaEntry, Arrays.asList(attributeType));
        AttributeTypeDefinition attributeTypeDefinition = getAttributeTypeDefinition(attributeTypes, attributeType);

        return (attributeTypeDefinition == null) ? null : attributeTypeDefinition.toString();
    }

    /**
     * Get attribute type schema definition string
     *
     * @param attributeTypes attribute type definitions
     * @param attributeType  Attribute type name
     * @return Attribute type schema definition string
     */
    public AttributeTypeDefinition getAttributeTypeDefinition(List<AttributeTypeDefinition> attributeTypes, String attributeType) {
        if ((attributeTypes == null) || (attributeType == null)) {
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
                if (attributeTypeDefinition.toLowerCase().contains(name)) { // Optimization to reduce number of objects
                    try {
                        result.add(new AttributeTypeDefinition(attributeTypeDefinition));
                    } catch (Exception ex) {
                        log.error("Failed to get attribute type definition by string {0}", ex, attributeTypeDefinition);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get object class schema definition string
     *
     * @param schemaEntry Schema
     * @param objectClass Object class name
     * @return Object class schema definition string
     */
    public String getObjectClassDefinitionString(SchemaEntry schemaEntry, String objectClass) {
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
                log.error(ex.getMessage(), ex);
            }
        }

        return null;
    }

    /**
     * Check if schema contains specified attribute
     *
     * @param attributeType Attribute type name
     * @return True if schema contains specified attribute
     */
    public boolean containsAttributeTypeInSchema(String attributeType) {
        SchemaEntry schema = getSchema();

        return getAttributeTypeDefinition(schema, attributeType) != null;
    }

    /**
     * Build DN string for DS schema
     *
     * @return DN string for DS schema
     */
    public String getDnForSchema() {
        return "cn=schema";
    }

    /**
     * Get schemaService instance
     *
     * @return SchemaService instance
     */
    public static SchemaService instance() {
        return (SchemaService) Component.getInstance(SchemaService.class);
    }

}
