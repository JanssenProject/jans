/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service;

import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.model.GluuAttribute;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.BaseCacheService;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @version May 30, 2018
 */
public abstract class AttributeService extends io.jans.service.AttributeService {

    private static final long serialVersionUID = -990409035168814270L;

    @Inject
    private Logger logger;

    @Inject
    private StaticConfiguration staticConfiguration;

    /**
     * returns GluuAttribute by Dn
     *
     * @return GluuAttribute
     */
    public GluuAttribute getAttributeByDn(String dn) {
        BaseCacheService usedCacheService = getCacheService();

        return usedCacheService.getWithPut(dn, () -> persistenceEntryManager.find(GluuAttribute.class, dn), 60);
    }

    public GluuAttribute getByLdapName(String name) {
        BaseCacheService usedCacheService = getCacheService();
        return usedCacheService.getWithPut(OxConstants.CACHE_ATTRIBUTE_DB_NAME + "_" + name, () -> {
            List<GluuAttribute> gluuAttributes = getAttributesByAttribute("jansAttrName", name, staticConfiguration.getBaseDn().getAttributes());
            if (gluuAttributes.size() > 0) {
                for (GluuAttribute gluuAttribute : gluuAttributes) {
                    if (gluuAttribute.getName() != null && gluuAttribute.getName().equals(name)) {
                        return gluuAttribute;
                    }
                }
            }

            return null;
        }, 30);
    }

    public GluuAttribute getByClaimName(String name) {
        BaseCacheService usedCacheService = getCacheService();
        return usedCacheService.getWithPut(OxConstants.CACHE_ATTRIBUTE_CLAIM_NAME + "_" + name, () -> {
            List<GluuAttribute> gluuAttributes = getAttributesByAttribute("jansClaimName", name, staticConfiguration.getBaseDn().getAttributes());
            if (gluuAttributes.size() > 0) {
                for (GluuAttribute gluuAttribute : gluuAttributes) {
                    if (gluuAttribute.getClaimName() != null && gluuAttribute.getClaimName().equals(name)) {
                        return gluuAttribute;
                    }
                }
            }
            return null;
        }, 30);
    }

    public String generateInumForNewAttribute() {
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForAttribute(newInum);
        } while (containsAttribute(newDn));

        return newInum;
    }

    public boolean containsAttribute(String dn) {
        return persistenceEntryManager.contains(dn, GluuAttribute.class);
    }

    public List<GluuAttribute> getAllAttributes() {
        return getAllAttributes(staticConfiguration.getBaseDn().getAttributes());
    }

    public String getDnForAttribute(String inum) {
        String attributesDn = staticConfiguration.getBaseDn().getAttributes();
        if (StringHelper.isEmpty(inum)) {
            return attributesDn;
        }
        return String.format("inum=%s,%s", inum, attributesDn);
    }

    public List<String> getAttributesDn(List<String> claimNames) {
        List<String> claims = new ArrayList<String>();

        for (String claimName : claimNames) {
            GluuAttribute gluuAttribute = getByClaimName(claimName);
            if (gluuAttribute != null) {
                claims.add(gluuAttribute.getDn());
            }
        }
        return claims;
    }

    protected BaseCacheService getCacheService() {
        if (isUseLocalCache()) {
            return localCacheService;
        }
        return cacheService;
    }

    public List<GluuAttribute> searchAttributes(String pattern, int sizeLimit) throws Exception {
        String baseDn = getDnForAttribute(null);

        String[] targetArray = new String[]{pattern};

        boolean useLowercaseFilter = !PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceEntryManager.getPersistenceType(baseDn));
        Filter searchFilter;
        if (useLowercaseFilter) {
            Filter displayNameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter(AttributeConstants.DISPLAY_NAME), null, targetArray, null);
            Filter descriptionFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter(AttributeConstants.DESCRIPTION), null, targetArray, null);
            Filter nameFilter = Filter.createSubstringFilter(Filter.createLowercaseFilter(AttributeConstants.JANS_ATTR_NAME), null, targetArray, null);
            searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter);
        } else {
            Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray, null);
            Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray, null);
            Filter nameFilter = Filter.createSubstringFilter(AttributeConstants.JANS_ATTR_NAME, null, targetArray, null);
            searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter);
        }

        return persistenceEntryManager.findEntries(baseDn, GluuAttribute.class,
                searchFilter, sizeLimit);
    }

    public List<GluuAttribute> searchAttributes(int sizeLimit) throws Exception {
        return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
                null, sizeLimit);
    }

    public List<GluuAttribute> searchAttributes(int sizeLimit, boolean active) throws Exception {
        Filter activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "active");
        if (!active) {
            activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "inactive");
        }
        return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
                activeFilter, sizeLimit);

    }

    public List<GluuAttribute> findAttributes(String pattern, int sizeLimit, boolean active) throws Exception {
        Filter activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "active");
        if (!active) {
            activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "inactive");
        }
        String[] targetArray = new String[]{pattern};
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray, null);
        Filter nameFilter = Filter.createSubstringFilter(AttributeConstants.JANS_ATTR_NAME, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter);
        return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
                Filter.createANDFilter(searchFilter, activeFilter), sizeLimit);
    }

    public GluuAttribute getAttributeByInum(String inum) {
        GluuAttribute result = null;
        try {
            result = persistenceEntryManager.find(GluuAttribute.class, getDnForAttribute(inum));
        } catch (Exception ex) {
            logger.error("Failed to load client entry", ex);
        }
        return result;
    }

    public void removeAttribute(GluuAttribute attribute) {
        logger.trace("Removing attribute {}", attribute.getDisplayName());
        persistenceEntryManager.remove(attribute);
    }

    public void addAttribute(GluuAttribute attribute) {
        persistenceEntryManager.persist(attribute);
    }

    public void updateAttribute(GluuAttribute attribute) {
        persistenceEntryManager.merge(attribute);
    }

    protected abstract boolean isUseLocalCache();

}