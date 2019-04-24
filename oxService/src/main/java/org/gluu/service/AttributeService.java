/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gluu.model.GluuAttribute;
import org.gluu.model.SchemaEntry;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.OxConstants;
import org.slf4j.Logger;

import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;

/**
 * Provides operations with attributes
 *
 * @author Oleksiy Tataryn
 * @author Yuriy Movchan Date: 01/06/2015
 */
public abstract class AttributeService implements Serializable {

    private static final long serialVersionUID = -1311784648561611479L;

    @Inject
    protected Logger log;

    @Inject
    protected PersistenceEntryManager ldapEntryManager;

    @Inject
    protected SchemaService schemaService;

    @Inject
    protected CacheService cacheService;

    public List<GluuAttribute> getAttributesByAttribute(String attributeName, String attributeValue, String baseDn) {
        String[] targetArray = new String[] { attributeValue };
        Filter filter = Filter.createSubstringFilter(attributeName, null, targetArray, null);
        List<GluuAttribute> result = ldapEntryManager.findEntries(baseDn, GluuAttribute.class, filter);

        return result;
    }

    public String getDefaultSaml2Uri(String name) {
        SchemaEntry schemaEntry = schemaService.getSchema();
        if (schemaEntry == null) {
	        List<String> attributeNames = new ArrayList<String>();
	        attributeNames.add(name);
	        List<AttributeTypeDefinition> attributeTypes = schemaService.getAttributeTypeDefinitions(schemaEntry, attributeNames);
	        AttributeTypeDefinition attributeTypeDefinition = schemaService.getAttributeTypeDefinition(attributeTypes, name);
	        if (attributeTypeDefinition != null) {
	            return String.format("urn:oid:%s", attributeTypeDefinition.getOID());
	        }
        }

        return "";
    }

    /**
     * Get attribute by name
     *
     * @return Attribute
     */
    public GluuAttribute getAttributeByName(String name) {
        return getAttributeByName(name, getAllAttributes());
    }

    /**
     * Get attribute by name
     *
     * @return Attribute
     */
    public GluuAttribute getAttributeByName(String name, List<GluuAttribute> attributes) {
        for (GluuAttribute attribute : attributes) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }

        return null;
    }

    public List<GluuAttribute> getAllAttributes() {
        return getAllAttributes(getDnForAttribute(null));
    }

    @SuppressWarnings("unchecked")
    public List<GluuAttribute> getAllAttributes(String baseDn) {
        List<GluuAttribute> attributeList = (List<GluuAttribute>) cacheService.get(OxConstants.CACHE_ATTRIBUTE_NAME,
                OxConstants.CACHE_ATTRIBUTE_KEY_LIST);
        if (attributeList == null) {
            attributeList = getAllAtributesImpl(baseDn);
            cacheService.put(OxConstants.CACHE_ATTRIBUTE_NAME, OxConstants.CACHE_ATTRIBUTE_KEY_LIST, attributeList);
        }

        return attributeList;
    }

    protected List<GluuAttribute> getAllAtributesImpl(String baseDn) {
        List<GluuAttribute> attributeList = ldapEntryManager.findEntries(baseDn, GluuAttribute.class, null);

        return attributeList;
    }

    public abstract String getDnForAttribute(String inum);

}
