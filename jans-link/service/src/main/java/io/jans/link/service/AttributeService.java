/*
 * Janssen Project is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Janssen Project
 */

package io.jans.link.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.unboundid.ldap.sdk.LDAPException;

import io.jans.link.constants.JansConstants;
import io.jans.model.GluuAttributeUsageType;
import io.jans.model.GluuUserRole;
import io.jans.model.JansAttribute;
import io.jans.model.JansCustomAttribute;
import io.jans.model.attribute.AttributeDataType;
import io.jans.model.user.UserRole;
import io.jans.orm.search.filter.Filter;
import io.jans.service.BaseCacheService;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import jakarta.inject.Inject;

/**
 * Provides operations with attributes
 * 
 * @author Yuriy Movchan Date: 10.13.2010
 */
public abstract class AttributeService extends io.jans.service.AttributeService {

    private GluuUserRole[] attributeEditTypes = new GluuUserRole[] { GluuUserRole.ADMIN, GluuUserRole.USER };

    private static final long serialVersionUID = 8223624816948822765L;

    @Inject
    private OrganizationService organizationService;


    public static final String CUSTOM_ATTRIBUTE_OBJECTCLASS_PREFIX = "ox-";

    /**
     * Get all person attributes
     * 
     * @param gluuUserRole
     *            User role
     * @return List of person attributes
     */
    @SuppressWarnings("unchecked")
    public List<JansAttribute> getAllPersonAttributes(GluuUserRole gluuUserRole) {
        String key = JansConstants.CACHE_ATTRIBUTE_PERSON_KEY_LIST + "_" + gluuUserRole.getValue();
        List<JansAttribute> attributeList = (List<JansAttribute>) cacheService.get(key);
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
    private List<JansAttribute> getAllPersonAtributesImpl(GluuUserRole gluuUserRole,
            Collection<JansAttribute> attributes) {
        List<JansAttribute> attributeList = new ArrayList<JansAttribute>();
        String[] objectClassTypes = getPersonObjectClassTypes();
        log.debug("objectClassTypes={}", Arrays.toString(objectClassTypes));
        for (JansAttribute attribute : attributes) {
            if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), getPersonCustomObjectClass())
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
    public List<JansAttribute> getAllActiveAttributes(GluuUserRole gluuUserRole) {
        String key = JansConstants.CACHE_ATTRIBUTE_PERSON_KEY_LIST + "_" + gluuUserRole.getValue();
        List<JansAttribute> attributeList = (List<JansAttribute>) cacheService.get(key);
        if (attributeList == null) {
            attributeList = getAllPersonAtributes(gluuUserRole, getAllAttributes());
            cacheService.put(key, attributeList);
        }
        return attributeList;
    }

    public boolean attributeWithSameNameDontExist(String name) {
        Filter nameFilter = Filter.createEqualityFilter("name", name);
        List<JansAttribute> result = persistenceEntryManager.findEntries(getDnForAttribute(null), JansAttribute.class,
                nameFilter, null);
        return (result != null && !result.isEmpty()) ? false : true;
    }

    private List<JansAttribute> getAllPersonAtributes(GluuUserRole gluuUserRole, Collection<JansAttribute> attributes) {
        List<JansAttribute> attributeList = new ArrayList<JansAttribute>();
        String[] objectClassTypes = getPersonObjectClassTypes();
        log.debug("objectClassTypes={}", Arrays.toString(objectClassTypes));
        for (JansAttribute attribute : attributes) {
            if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), getPersonCustomObjectClass())
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
    public List<JansAttribute> getAllContactAttributes(GluuUserRole gluuUserRole) {
        String key = JansConstants.CACHE_ATTRIBUTE_CONTACT_KEY_LIST + "_" + gluuUserRole.getValue();
        List<JansAttribute> attributeList = (List<JansAttribute>) cacheService.get(key);
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
    private List<JansAttribute> getAllContactAtributesImpl(GluuUserRole gluuUserRole,
            Collection<JansAttribute> attributes) {
        List<JansAttribute> returnAttributeList = new ArrayList<JansAttribute>();
        String[] objectClassTypes = getPersonObjectClassTypes();
        for (JansAttribute attribute : attributes) {
            if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), getPersonCustomObjectClass())
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
                .get(JansConstants.CACHE_ATTRIBUTE_ORIGIN_KEY_LIST);
        if (attributeOriginList == null) {
            attributeOriginList = getAllAttributeOrigins(getAllAttributes());
            cacheService.put(JansConstants.CACHE_ATTRIBUTE_ORIGIN_KEY_LIST, attributeOriginList);
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
    public List<String> getAllAttributeOrigins(Collection<JansAttribute> attributes) {
        List<String> attributeOriginList = new ArrayList<String>();
        for (JansAttribute attribute : attributes) {
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
    public List<JansAttribute> getCustomAttributes() {
        List<JansAttribute> attributeList = (List<JansAttribute>) cacheService
                .get(JansConstants.CACHE_ATTRIBUTE_CUSTOM_KEY_LIST);
        if (attributeList == null) {
            attributeList = new ArrayList<JansAttribute>();
            for (JansAttribute attribute : getAllAttributes()) {
                if (attribute.isCustom()) {
                    attributeList.add(attribute);
                }
            }
            cacheService.put(JansConstants.CACHE_ATTRIBUTE_CUSTOM_KEY_LIST, attributeList);
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
    public JansAttribute getAttributeByInum(String inum) {
        return getAttributeByInum(inum, getAllAtributesImpl(getDnForAttribute(null)));
    }

    public JansAttribute getAttributeByInum(String inum, List<JansAttribute> attributes) {
        for (JansAttribute attribute : attributes) {
            if (attribute.getInum().equals(inum)) {
                return attribute;
            }
        }
        return null;
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

    public boolean containsAttribute(JansAttribute attribute) {
        return persistenceEntryManager.contains(attribute);
    }

    public boolean containsAttribute(String dn) {
        return persistenceEntryManager.contains(dn, JansAttribute.class);
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
        return inum.replace(".", "").replace(JansConstants.inumDelimiter, "").replace("@", "");
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
        return getPersonCustomObjectClass();
    }

    public abstract String getPersonCustomObjectClass();

    public abstract String[] getPersonObjectClassTypes();

    @Override
    protected List<JansAttribute> getAllAtributesImpl(String baseDn) {
        List<JansAttribute> attributeList = persistenceEntryManager.findEntries(baseDn, JansAttribute.class, null);
        String customOrigin = getCustomOrigin();
        for (JansAttribute attribute : attributeList) {
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
    public void setAttributeMetadata(List<JansCustomAttribute> customAttributes, List<JansAttribute> attributes) {
        if ((customAttributes == null) || (attributes == null)) {
            return;
        }

        for (JansCustomAttribute personAttribute : customAttributes) {
            JansAttribute tmpAttribute = getAttributeByName(personAttribute.getName(), attributes);
            if (tmpAttribute == null) {
                log.warn("Failed to find attribute '{}' metadata", personAttribute.getName());
            }
            personAttribute.setMetadata(tmpAttribute);
        }
    }

    /**
     * Get custom attributes by attribute DNs
     *
     */
    public List<JansCustomAttribute> getCustomAttributesByAttributeDNs(List<String> attributeDNs,
            HashMap<String, JansAttribute> attributesByDNs) {
        List<JansCustomAttribute> customAttributes = new ArrayList<JansCustomAttribute>();
        if (attributeDNs == null) {
            return customAttributes;
        }
        for (String releasedAttributeDn : attributeDNs) {
            JansAttribute attribute = attributesByDNs.get(releasedAttributeDn);
            if (attribute != null) {
                JansCustomAttribute customAttribute = new JansCustomAttribute(attribute.getName(), releasedAttributeDn);
                customAttribute.setMetadata(attribute);
                customAttributes.add(customAttribute);
            }
        }
        return customAttributes;
    }

    public HashMap<String, JansAttribute> getAttributeMapByDNs(List<JansAttribute> attributes) {
        HashMap<String, JansAttribute> attributeDns = new HashMap<String, JansAttribute>();
        for (JansAttribute attribute : attributes) {
            attributeDns.put(attribute.getDn(), attribute);
        }
        return attributeDns;
    }

    public void sortCustomAttributes(List<JansCustomAttribute> customAttributes, String sortByProperties) {
        persistenceEntryManager.sortListByProperties(JansCustomAttribute.class, customAttributes, false,
                sortByProperties);
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
    public List<JansAttribute> getAllActivePersonAttributes(GluuUserRole admin) {
        List<JansAttribute> activeAttributeList = (List<JansAttribute>) cacheService
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
    private List<JansAttribute> getAllActiveAtributesImpl(GluuUserRole gluuUserRole) {
        Filter filter = Filter.createEqualityFilter("gluuStatus", "active");
        List<JansAttribute> attributeList = persistenceEntryManager.findEntries(getDnForAttribute(null),
                JansAttribute.class, filter);
        String customOrigin = getCustomOrigin();
        String[] objectClassTypes = getPersonObjectClassTypes();
        log.debug("objectClassTypes={}", Arrays.toString(objectClassTypes));
        List<JansAttribute> returnAttributeList = new ArrayList<JansAttribute>();
        for (JansAttribute attribute : attributeList) {
            if (StringHelper.equalsIgnoreCase(attribute.getOrigin(), getPersonCustomObjectClass())
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
    public List<JansAttribute> searchAttributes(String pattern, int sizeLimit) throws Exception {
        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(JansConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(JansConstants.description, null, targetArray, null);
        Filter nameFilter = Filter.createSubstringFilter(JansConstants.attributeName, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter);
        List<JansAttribute> result = persistenceEntryManager.findEntries(getDnForAttribute(null), JansAttribute.class,
                searchFilter, sizeLimit);
        String customOrigin = getCustomOrigin();
        for (JansAttribute attribute : result) {
            attribute.setCustom(customOrigin.equals(attribute.getOrigin()));
        }

        return result;
    }

    public List<JansAttribute> searchPersonAttributes(String pattern, int sizeLimit) throws Exception {
        String[] objectClassTypes = getPersonObjectClassTypes();
        String[] targetArray = new String[] { pattern };
        List<Filter> originFilters = new ArrayList<Filter>();
        Filter displayNameFilter = Filter.createSubstringFilter(JansConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(JansConstants.description, null, targetArray, null);
        for (String objectClassType : objectClassTypes) {
            Filter originFilter = Filter.createEqualityFilter(JansConstants.origin, objectClassType);
            originFilters.add(originFilter);
        }
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
        Filter originFilter = Filter.createORFilter(originFilters.toArray(new Filter[0]));
        Filter filter = Filter.createANDFilter(searchFilter, originFilter);
        return persistenceEntryManager.findEntries(getDnForAttribute(null), JansAttribute.class, filter, sizeLimit);
    }

    public JansAttribute getAttributeByDn(String Dn) throws Exception {
        return persistenceEntryManager.find(JansAttribute.class, Dn);
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
    
    public String getPersistenceType() {
		return persistenceEntryManager.getPersistenceType();
	}


}
