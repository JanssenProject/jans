/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.model.GluuAttribute;
import org.gluu.model.GluuAttributeUsageType;
import org.gluu.model.GluuUserRole;
import org.gluu.model.attribute.AttributeDataType;
import org.gluu.model.user.UserRole;
import org.gluu.oxtrust.model.GluuCustomAttribute;
import org.gluu.oxtrust.service.cdi.event.EventType;
import org.gluu.oxtrust.service.cdi.event.EventTypeQualifier;
import org.gluu.oxtrust.service.cdi.event.Events;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.search.filter.Filter;
import org.gluu.service.BaseCacheService;
import org.gluu.util.OxConstants;
import org.gluu.util.StringHelper;

import com.unboundid.ldap.sdk.LDAPException;

/**
 * Provides operations with attributes
 * 
 * @author Yuriy Movchan Date: 10.13.2010
 */
@ApplicationScoped
public class AttributeService extends org.gluu.service.AttributeService {

	private GluuUserRole[] attributeEditTypes = new GluuUserRole[] { GluuUserRole.ADMIN, GluuUserRole.USER };

	/**
	 * 
	 */
	private static final long serialVersionUID = 8223624816948822765L;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private OrganizationService organizationService;

	@Inject
	@Any
	private Event<Events> event;

	public static final String CUSTOM_ATTRIBUTE_OBJECTCLASS_PREFIX = "ox-";

	/**
	 * Get all person attributes
	 * 
	 * @param gluuUserRole
	 *            User role
	 * @return List of person attributes
	 */
	@SuppressWarnings("unchecked")
	public List<GluuAttribute> getAllPersonAttributes(GluuUserRole gluuUserRole) {
		String key = OxTrustConstants.CACHE_ATTRIBUTE_PERSON_KEY_LIST + "_" + gluuUserRole.getValue();
		List<GluuAttribute> attributeList = (List<GluuAttribute>) cacheService.get(key);
		if (attributeList == null) {
			attributeList = getAllPersonAtributesImpl(gluuUserRole, getAllAttributes());
			cacheService.put(key, attributeList);
		}
		return attributeList;
	}

	/**
	 * Get all organization attributes
	 * 
	 * @param attributes
	 *            List of attributes
	 * @return List of organization attributes
	 */
	private List<GluuAttribute> getAllPersonAtributesImpl(GluuUserRole gluuUserRole,
			Collection<GluuAttribute> attributes) {
		List<GluuAttribute> attributeList = new ArrayList<GluuAttribute>();
		String[] objectClassTypes = appConfiguration.getPersonObjectClassTypes();
		log.debug("objectClassTypes={}", Arrays.toString(objectClassTypes));
		for (GluuAttribute attribute : attributes) {
			if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), appConfiguration.getPersonCustomObjectClass())
					&& (GluuUserRole.ADMIN == gluuUserRole)) {
				attribute.setCustom(true);
				attributeList.add(attribute);
				continue;
			}
			for (String objectClassType : objectClassTypes) {
				if (attribute.getOrigin().equals(objectClassType)
						&& ((attribute.allowViewBy(gluuUserRole) || attribute.allowEditBy(gluuUserRole)))) {
					attributeList.add(attribute);
					break;
				}
			}
		}
		return attributeList;
	}

	@SuppressWarnings("unchecked")
	public List<GluuAttribute> getAllActiveAttributes(GluuUserRole gluuUserRole) {
		String key = OxTrustConstants.CACHE_ATTRIBUTE_PERSON_KEY_LIST + "_" + gluuUserRole.getValue();
		List<GluuAttribute> attributeList = (List<GluuAttribute>) cacheService.get(key);
		if (attributeList == null) {
			attributeList = getAllPersonAtributes(gluuUserRole, getAllAttributes());
			cacheService.put(key, attributeList);
		}
		return attributeList;
	}

	private List<GluuAttribute> getAllPersonAtributes(GluuUserRole gluuUserRole, Collection<GluuAttribute> attributes) {
		List<GluuAttribute> attributeList = new ArrayList<GluuAttribute>();
		String[] objectClassTypes = appConfiguration.getPersonObjectClassTypes();
		log.debug("objectClassTypes={}", Arrays.toString(objectClassTypes));
		for (GluuAttribute attribute : attributes) {
			if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), appConfiguration.getPersonCustomObjectClass())
					&& (GluuUserRole.ADMIN == gluuUserRole)) {
				attribute.setCustom(true);
				attributeList.add(attribute);
				continue;
			}
			for (String objectClassType : objectClassTypes) {
				if (attribute.getOrigin().equals(objectClassType)) {
					attributeList.add(attribute);
					break;
				}
			}
		}
		return attributeList;
	}

	/**
	 * Get all contact attributes
	 * 
	 * @return List of contact attributes
	 */
	@SuppressWarnings("unchecked")
	public List<GluuAttribute> getAllContactAttributes(GluuUserRole gluuUserRole) {
		String key = OxTrustConstants.CACHE_ATTRIBUTE_CONTACT_KEY_LIST + "_" + gluuUserRole.getValue();
		List<GluuAttribute> attributeList = (List<GluuAttribute>) cacheService.get(key);
		if (attributeList == null) {
			attributeList = getAllContactAtributesImpl(gluuUserRole, getAllAttributes());
			cacheService.put(key, attributeList);
		}
		return attributeList;
	}

	/**
	 * Get all contact attributes
	 * 
	 * @param attributes
	 *            List of attributes
	 * @return List of contact attributes
	 */
	private List<GluuAttribute> getAllContactAtributesImpl(GluuUserRole gluuUserRole,
			Collection<GluuAttribute> attributes) {
		List<GluuAttribute> returnAttributeList = new ArrayList<GluuAttribute>();
		String[] objectClassTypes = appConfiguration.getContactObjectClassTypes();
		for (GluuAttribute attribute : attributes) {
			if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), appConfiguration.getPersonCustomObjectClass())
					&& (GluuUserRole.ADMIN == gluuUserRole)) {
				attribute.setCustom(true);
				returnAttributeList.add(attribute);
				continue;
			}

			for (String objectClassType : objectClassTypes) {
				if (attribute.getOrigin().equals(objectClassType)
						&& (attribute.allowViewBy(gluuUserRole) || attribute.allowEditBy(gluuUserRole))) {
					returnAttributeList.add(attribute);
					break;
				}
			}
		}
		return returnAttributeList;
	}

	/**
	 * Get all origins
	 * 
	 * @return List of origins
	 */
	@SuppressWarnings("unchecked")
	public List<String> getAllAttributeOrigins() {
		List<String> attributeOriginList = (List<String>) cacheService
				.get(OxTrustConstants.CACHE_ATTRIBUTE_ORIGIN_KEY_LIST);
		if (attributeOriginList == null) {
			attributeOriginList = getAllAttributeOrigins(getAllAttributes());
			cacheService.put(OxTrustConstants.CACHE_ATTRIBUTE_ORIGIN_KEY_LIST, attributeOriginList);
		}
		return attributeOriginList;
	}

	/**
	 * Get all origins
	 * 
	 * @param attributes
	 *            List of attributes
	 * @return List of origins
	 */
	public List<String> getAllAttributeOrigins(Collection<GluuAttribute> attributes) {
		List<String> attributeOriginList = new ArrayList<String>();
		for (GluuAttribute attribute : attributes) {
			String origin = attribute.getOrigin();
			if (!attributeOriginList.contains(origin)) {
				attributeOriginList.add(attribute.getOrigin());
			}
		}
		String customOrigin = getCustomOrigin();
		if (!attributeOriginList.contains(customOrigin)) {
			attributeOriginList.add(customOrigin);
		}
		return attributeOriginList;
	}

	/**
	 * Get origin display names
	 * 
	 * @param attributes
	 *            List of origins
	 * @param objectClassTypes
	 *            List of objectClasses
	 * @param objectClassDisplayNames
	 *            List of display names for objectClasses
	 * @return Map with key = origin and value = display name
	 */
	public Map<String, String> getAllAttributeOriginDisplayNames(List<String> attributeOriginList,
			String[] objectClassTypes, String[] objectClassDisplayNames) {
		Map<String, String> attributeOriginDisplayNameMap = new HashMap<String, String>();
		for (String origin : attributeOriginList) {
			attributeOriginDisplayNameMap.put(origin, origin);
		}
		if (objectClassTypes.length == objectClassDisplayNames.length) {
			for (int i = 0; i < objectClassTypes.length; i++) {
				String objectClass = objectClassTypes[i];
				if (attributeOriginDisplayNameMap.containsKey(objectClass)) {
					attributeOriginDisplayNameMap.put(objectClass, objectClassDisplayNames[i]);
				}
			}
		}
		return attributeOriginDisplayNameMap;
	}

	/**
	 * Get custom attributes
	 * 
	 * @return List of cusomt attributes
	 */
	@SuppressWarnings("unchecked")
	public List<GluuAttribute> getCustomAttributes() {
		List<GluuAttribute> attributeList = (List<GluuAttribute>) cacheService
				.get(OxTrustConstants.CACHE_ATTRIBUTE_CUSTOM_KEY_LIST);
		if (attributeList == null) {
			attributeList = new ArrayList<GluuAttribute>();
			for (GluuAttribute attribute : getAllAttributes()) {
				if (attribute.isCustom()) {
					attributeList.add(attribute);
				}
			}
			cacheService.put(OxTrustConstants.CACHE_ATTRIBUTE_CUSTOM_KEY_LIST, attributeList);
		}
		return attributeList;
	}

	/**
	 * Get attribute by inum
	 * 
	 * @param inum
	 *            Inum
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

	/**
	 * Get SCIM related attributes
	 * 
	 * @return Attribute
	 */
	public List<GluuAttribute> getSCIMRelatedAttributes() throws Exception {
		return getSCIMRelatedAttributesImpl(getAllAttributes());
	}

	/**
	 * Get SCIMAttributes
	 * 
	 * @param attributes
	 *            List of attributes
	 * @return list of Attributes
	 */
	public List<GluuAttribute> getSCIMRelatedAttributesImpl(List<GluuAttribute> attributes) throws Exception {
		List<GluuAttribute> result = new ArrayList<GluuAttribute>();
		for (GluuAttribute attribute : attributes) {
			boolean isEmpty = attribute.getOxSCIMCustomAttribute() == null;
			if (!isEmpty) {
				if ((attribute.getOxSCIMCustomAttribute() != null) && attribute.getOxSCIMCustomAttribute()) {
					result.add(attribute);
				}
			}
		}
		return result;
	}

	/**
	 * Add new custom attribute
	 * 
	 * @param attribute
	 *            Attribute
	 */
	public void addAttribute(GluuAttribute attribute) {
		persistenceEntryManager.persist(attribute);
		event.select(new EventTypeQualifier(Events.EVENT_CLEAR_ATTRIBUTES)).fire(Events.EVENT_CLEAR_ATTRIBUTES);
	}

	public void removeAttribute(String inum) {
		GluuAttribute attribute = new GluuAttribute();
		attribute.setDn(getDnForAttribute(inum));
		removeAttribute(attribute);
	}

	/**
	 * Remove attribute with specified Inum
	 * 
	 * @param inum
	 *            Inum
	 */
	public void removeAttribute(GluuAttribute attribute) {
		log.trace("Removing attribute {}", attribute.getDisplayName());
		persistenceEntryManager.remove(attribute);
		event.select(new EventTypeQualifier(Events.EVENT_CLEAR_ATTRIBUTES)).fire(Events.EVENT_CLEAR_ATTRIBUTES);
	}

	/**
	 * Update specified attribute
	 * 
	 * @param attribute
	 *            Attribute
	 */
	public void updateAttribute(GluuAttribute attribute) {
		persistenceEntryManager.merge(attribute);
		event.select(new EventTypeQualifier(Events.EVENT_CLEAR_ATTRIBUTES)).fire(Events.EVENT_CLEAR_ATTRIBUTES);
	}

	/**
	 * Clear attributes cache after receiving event that attributes were changed
	 */
	public void clearAttributesCache(@Observes @EventType(Events.EVENT_CLEAR_ATTRIBUTES) Events event) {
		log.debug("Removing attributes from cache");
		cacheService.clear();
	}

	public AttributeDataType[] getDataTypes() {
		return AttributeDataType.values();
	}

	public UserRole[] getAttributeUserRoles() {
		return new UserRole[] { UserRole.ADMIN, UserRole.USER };
	}

	public UserRole[] getViewTypes() {
		return new UserRole[] { UserRole.ADMIN, UserRole.USER };
	}

	public GluuAttributeUsageType[] getAttributeUsageTypes() {
		return new GluuAttributeUsageType[] { GluuAttributeUsageType.OPENID };
	}

	public boolean containsAttribute(GluuAttribute attribute) {
		return persistenceEntryManager.contains(attribute);
	}

	public boolean containsAttribute(String dn) {
		return persistenceEntryManager.contains(dn, GluuAttribute.class);
	}

	public String generateInumForNewAttribute() {
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumForNewAttributeImpl();
			newDn = getDnForAttribute(newInum);
		} while (containsAttribute(newDn));

		return newInum;
	}

	public String toInumWithoutDelimiters(String inum) {
		return inum.replace(".", "").replace(OxTrustConstants.inumDelimiter, "").replace("@", "");
	}

	public String generateRandomOid() {
		return Long.toString(System.currentTimeMillis());
	}

	private String generateInumForNewAttributeImpl() {
		return UUID.randomUUID().toString();
	}

	public String getDnForAttribute(String inum) {
		String organizationDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=attributes,%s", organizationDn);
		}

		return String.format("inum=%s,ou=attributes,%s", inum, organizationDn);
	}

	/**
	 * Return current custom origin
	 * 
	 * @return Current custom origin
	 */
	public String getCustomOrigin() {
		return appConfiguration.getPersonCustomObjectClass();
	}

	@Override
	protected List<GluuAttribute> getAllAtributesImpl(String baseDn) {
		List<GluuAttribute> attributeList = persistenceEntryManager.findEntries(baseDn, GluuAttribute.class, null);
		String customOrigin = getCustomOrigin();
		for (GluuAttribute attribute : attributeList) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}

		return attributeList;
	}

	/**
	 * Set metadata for every custom attribute
	 * 
	 * @param customAttributes
	 *            List of custom attributes
	 * @param attributes
	 *            List of attributes
	 */
	public void setAttributeMetadata(List<GluuCustomAttribute> customAttributes, List<GluuAttribute> attributes) {
		if ((customAttributes == null) || (attributes == null)) {
			return;
		}

		for (GluuCustomAttribute personAttribute : customAttributes) {
			GluuAttribute tmpAttribute = getAttributeByName(personAttribute.getName(), attributes);
			if (tmpAttribute == null) {
				log.warn("Failed to find attribute '{}' metadata", personAttribute.getName());
			}
			personAttribute.setMetadata(tmpAttribute);
		}
	}

	/**
	 * Get custom attributes by attribute DNs
	 * 
	 * @param customAttributes
	 *            List of attribute DNs
	 * @param attributes
	 *            List of custom attributes
	 */
	public List<GluuCustomAttribute> getCustomAttributesByAttributeDNs(List<String> attributeDNs,
			HashMap<String, GluuAttribute> attributesByDNs) {
		List<GluuCustomAttribute> customAttributes = new ArrayList<GluuCustomAttribute>();
		if (attributeDNs == null) {
			return customAttributes;
		}
		for (String releasedAttributeDn : attributeDNs) {
			GluuAttribute attribute = attributesByDNs.get(releasedAttributeDn);
			if (attribute != null) {
				GluuCustomAttribute customAttribute = new GluuCustomAttribute(attribute.getName(), releasedAttributeDn);
				customAttribute.setMetadata(attribute);
				customAttributes.add(customAttribute);
			}
		}
		return customAttributes;
	}

	public HashMap<String, GluuAttribute> getAttributeMapByDNs(List<GluuAttribute> attributes) {
		HashMap<String, GluuAttribute> attributeDns = new HashMap<String, GluuAttribute>();
		for (GluuAttribute attribute : attributes) {
			attributeDns.put(attribute.getDn(), attribute);
		}
		return attributeDns;
	}

	public void sortCustomAttributes(List<GluuCustomAttribute> customAttributes, String sortByProperties) {
		persistenceEntryManager.sortListByProperties(GluuCustomAttribute.class, customAttributes, false, sortByProperties);
	}

	/**
	 * Build DN string for group
	 * 
	 * @param inum
	 *            Group Inum
	 * @return DN string for specified group or DN for groups branch if inum is null
	 */
	public String getDnForGroup(String inum) throws Exception {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=groups,%s", orgDn);
		}

		return String.format("inum=%s,ou=groups,%s", inum, orgDn);
	}

	/**
	 * @param admin
	 * @return
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public List<GluuAttribute> getAllActivePersonAttributes(GluuUserRole admin) {
		List<GluuAttribute> activeAttributeList = (List<GluuAttribute>) cacheService
				.get(OxConstants.CACHE_ACTIVE_ATTRIBUTE_NAME, OxConstants.CACHE_ACTIVE_ATTRIBUTE_KEY_LIST);
		if (activeAttributeList == null) {
			activeAttributeList = getAllActiveAtributesImpl(admin);
			cacheService.put(OxConstants.CACHE_ATTRIBUTE_KEY_LIST, activeAttributeList);
		}
		return activeAttributeList;
	}

	/**
	 * @return
	 * @throws LDAPException
	 */
	private List<GluuAttribute> getAllActiveAtributesImpl(GluuUserRole gluuUserRole) {
		Filter filter = Filter.createEqualityFilter("gluuStatus", "active");
		List<GluuAttribute> attributeList = persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
				filter);
		String customOrigin = getCustomOrigin();
		String[] objectClassTypes = appConfiguration.getPersonObjectClassTypes();
		log.debug("objectClassTypes={}", Arrays.toString(objectClassTypes));
		List<GluuAttribute> returnAttributeList = new ArrayList<GluuAttribute>();
		for (GluuAttribute attribute : attributeList) {
			if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), appConfiguration.getPersonCustomObjectClass())
					&& (GluuUserRole.ADMIN == gluuUserRole)) {
				attribute.setCustom(true);
				returnAttributeList.add(attribute);
				continue;
			}
			for (String objectClassType : objectClassTypes) {
				if (attribute.getOrigin().equals(objectClassType)) {
					attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
					returnAttributeList.add(attribute);
					break;
				}
			}
		}
		return returnAttributeList;
	}

	/**
	 * Search groups by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of groups
	 * @throws Exception
	 */
	public List<GluuAttribute> searchAttributes(String pattern, int sizeLimit) throws Exception {
		String[] targetArray = new String[] { pattern };
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter descriptionFilter = Filter.createSubstringFilter(OxTrustConstants.description, null, targetArray, null);
		Filter nameFilter = Filter.createSubstringFilter(OxTrustConstants.attributeName, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter,nameFilter);
		List<GluuAttribute> result = persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
				searchFilter, sizeLimit);
		String customOrigin = getCustomOrigin();
		for (GluuAttribute attribute : result) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}
		return result;
	}

	public List<GluuAttribute> searchAttributes(int sizeLimit) throws Exception {
		List<GluuAttribute> result = persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
				null, sizeLimit);
		String customOrigin = getCustomOrigin();
		for (GluuAttribute attribute : result) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}
		return result;
	}



	public List<GluuAttribute> searchAttributes(int sizeLimit, boolean active) throws Exception {
		Filter activeFilter = Filter.createEqualityFilter(OxTrustConstants.gluuStatus, "active");
		if(!active){
			activeFilter = Filter.createEqualityFilter(OxTrustConstants.gluuStatus, "inactive");
		}
		List<GluuAttribute> result = persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
				activeFilter, sizeLimit);
		String customOrigin = getCustomOrigin();
		for (GluuAttribute attribute : result) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}
		return result;
	}

	public List<GluuAttribute> findAttributes(String pattern, int sizeLimit,boolean active) throws Exception {
		Filter activeFilter = Filter.createEqualityFilter(OxTrustConstants.gluuStatus, "active");
		if(!active){
			activeFilter = Filter.createEqualityFilter(OxTrustConstants.gluuStatus, "inactive");
		}
		String[] targetArray = new String[] { pattern };
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter descriptionFilter = Filter.createSubstringFilter(OxTrustConstants.description, null, targetArray, null);
		Filter nameFilter = Filter.createSubstringFilter(OxTrustConstants.attributeName, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter,nameFilter);
		List<GluuAttribute> result = persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
				Filter.createANDFilter(searchFilter, activeFilter), sizeLimit);
		String customOrigin = getCustomOrigin();
		for (GluuAttribute attribute : result) {
			attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
		}
		return result;
	}


	public List<GluuAttribute> searchPersonAttributes(String pattern, int sizeLimit) throws Exception {
		String[] objectClassTypes = appConfiguration.getPersonObjectClassTypes();
		String[] targetArray = new String[] { pattern };
		List<Filter> originFilters = new ArrayList<Filter>();
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter descriptionFilter = Filter.createSubstringFilter(OxTrustConstants.description, null, targetArray, null);
		for (String objectClassType : objectClassTypes) {
			Filter originFilter = Filter.createEqualityFilter(OxTrustConstants.origin, objectClassType);
			originFilters.add(originFilter);
		}
		Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
		Filter originFilter = Filter.createORFilter(originFilters.toArray(new Filter[0]));
		Filter filter = Filter.createANDFilter(searchFilter, originFilter);
		return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class, filter, sizeLimit);
	}

	public GluuAttribute getAttributeByDn(String Dn) throws Exception {
		return persistenceEntryManager.find(GluuAttribute.class, Dn);
	}

	public GluuUserRole[] getAttributeEditTypes() {
		return attributeEditTypes;
	}

	public void setAttributeEditTypes(GluuUserRole[] attributeEditTypes) {
		this.attributeEditTypes = attributeEditTypes;
	}

	@Override
	protected BaseCacheService getCacheService() {
		return cacheService;
	}

}
