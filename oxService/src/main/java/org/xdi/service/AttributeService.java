/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 *//**
 * 
 */
package org.xdi.service;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;
import org.xdi.model.GluuAttribute;
import org.xdi.model.SchemaEntry;
import org.xdi.util.OxConstants;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;

/**
 * Provides operations with attributes
 * 
 * @author Oleksiy Tataryn
 * @author Yuriy Movchan Date: 01/06/2015
 */
public  @Data class AttributeService {
	@Logger
	private Log log;

	@In
    private LdapEntryManager ldapEntryManager;
	
	@In
	private SchemaService schemaService;

	@In
	private CacheService cacheService;
	
    public List<GluuAttribute> getAttributesByAttribute(String attributeName, String attributeValue, String baseDn) {
    	  String[] targetArray = new String[] { attributeValue };
    	  Filter filter = Filter.createSubstringFilter(attributeName, null, targetArray, null);
    	  List<GluuAttribute> result = getLdapEntryManager().findEntries(baseDn, GluuAttribute.class, filter, 0);
    	  return result;
    }
    
    public String getDefaultSaml2Uri(String name){
    	SchemaEntry schemaEntry = schemaService.getSchema();
		List<String> attributeNames = new ArrayList<String>();
		attributeNames.add(name);
		List<AttributeTypeDefinition> attributeTypes = schemaService.getAttributeTypeDefinitions(schemaEntry, attributeNames);
		AttributeTypeDefinition attributeTypeDefinition = schemaService.getAttributeTypeDefinition(attributeTypes, name);

		return String.format("urn:oid:%s", attributeTypeDefinition.getOID());
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
		List<GluuAttribute> attributeList = getLdapEntryManager().findEntries(baseDn, GluuAttribute.class, null);

		return attributeList;
	}

}
