/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;

import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;

import io.jans.model.GluuAttribute;
import io.jans.model.SchemaEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;

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
    protected PersistenceEntryManager persistenceEntryManager;

    @Inject
    protected SchemaService schemaService;

    @Inject
    protected CacheService cacheService;

    @Inject
    protected LocalCacheService localCacheService;

    public List<GluuAttribute> getAttributesByAttribute(String attributeName, String attributeValue, String baseDn) {
        Filter filter = Filter.createEqualityFilter(attributeName, attributeValue);
        List<GluuAttribute> result = persistenceEntryManager.findEntries(baseDn, GluuAttribute.class, filter);

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

	/**
	 * Get attribute by inum
	 * 
	 * @param inum Inum
	 * @return Attribute
	 */
	public GluuAttribute getAttributeByInum(String inum) {
		return getAttributeByInum(inum, getAllAtributesImpl(getDnForAttribute(null)));
	}

	public GluuAttribute getAttributeByInum(String inum, List<GluuAttribute> attributes) {
		for (GluuAttribute attribute : attributes) {
			if (attribute.getInum().equals(inum)) {
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
    	BaseCacheService usedCacheService = getCacheService();

    	List<GluuAttribute> attributeList = (List<GluuAttribute>) usedCacheService.get(OxConstants.CACHE_ATTRIBUTE_CACHE_NAME,
                OxConstants.CACHE_ATTRIBUTE_KEY_LIST);
        if (attributeList == null) {
            attributeList = getAllAtributesImpl(baseDn);
            usedCacheService.put(OxConstants.CACHE_ATTRIBUTE_CACHE_NAME, OxConstants.CACHE_ATTRIBUTE_KEY_LIST, attributeList);
        }

        return attributeList;
    }

    public Map<String, GluuAttribute> getAllAttributesMap() {
        return getAllAttributesMap(getDnForAttribute(null));
    }

    @SuppressWarnings("unchecked")
    public Map<String, GluuAttribute> getAllAttributesMap(String baseDn) {
    	BaseCacheService usedCacheService = getCacheService();

    	Map<String, GluuAttribute> attributeMap = (Map<String, GluuAttribute>) usedCacheService.get(OxConstants.CACHE_ATTRIBUTE_CACHE_NAME,
                OxConstants.CACHE_ATTRIBUTE_KEY_MAP);
        if (attributeMap == null) {
        	attributeMap = getAllAttributesMapImpl(baseDn);

            usedCacheService.put(OxConstants.CACHE_ATTRIBUTE_CACHE_NAME, OxConstants.CACHE_ATTRIBUTE_KEY_MAP, attributeMap);
        }

        return attributeMap;
    }

	private Map<String, GluuAttribute> getAllAttributesMapImpl(String baseDn) {
		List<GluuAttribute> attributeList = getAllAttributes(baseDn);

		Map<String, GluuAttribute> attributeMap = new HashMap<>();
		for (GluuAttribute attribute : attributeList) {
			attributeMap.put(StringHelper.toLowerCase(attribute.getName()), attribute);
		}

		return attributeMap;
	}

    protected List<GluuAttribute> getAllAtributesImpl(String baseDn) {
        List<GluuAttribute> attributeList = persistenceEntryManager.findEntries(baseDn, GluuAttribute.class, null);

        return attributeList;
    }

    protected abstract BaseCacheService getCacheService();

    public abstract String getDnForAttribute(String inum);

}
