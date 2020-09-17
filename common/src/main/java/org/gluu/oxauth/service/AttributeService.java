/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.gluu.model.GluuAttribute;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.util.OxConstants;
import org.gluu.search.filter.Filter;
import org.gluu.service.BaseCacheService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @version May 30, 2018
 */
@ApplicationScoped
public class AttributeService extends org.gluu.service.AttributeService {

    /**
     *
     */
    private static final long serialVersionUID = -990409035168814270L;

    @Inject
    private Logger logger;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

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
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("gluuAttributeName", name, staticConfiguration.getBaseDn().getAttributes());
        if (gluuAttributes.size() > 0) {
            for (GluuAttribute gluuAttribute : gluuAttributes) {
                if (gluuAttribute.getName() != null && gluuAttribute.getName().equals(name)) {
                    return gluuAttribute;
                }
            }
        }
        return null;
    }

    public GluuAttribute getByClaimName(String name) {
        List<GluuAttribute> gluuAttributes = getAttributesByAttribute("oxAuthClaimName", name, staticConfiguration.getBaseDn().getAttributes());
        if (gluuAttributes.size() > 0) {
            for (GluuAttribute gluuAttribute : gluuAttributes) {
                if (gluuAttribute.getOxAuthClaimName() != null && gluuAttribute.getOxAuthClaimName().equals(name)) {
                    return gluuAttribute;
                }
            }
        }
        return null;
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
        if (appConfiguration.getUseLocalCache()) {
            return localCacheService;
        }
        return cacheService;
    }

    public List<GluuAttribute> searchAttributes(String pattern, int sizeLimit) throws Exception {
        String[] targetArray = new String[]{pattern};
        Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.description, null, targetArray, null);
        Filter nameFilter = Filter.createSubstringFilter(OxConstants.attributeName, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter);
        return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
                searchFilter, sizeLimit);
    }

    public List<GluuAttribute> searchAttributes(int sizeLimit) throws Exception {
        return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
                null, sizeLimit);
    }

    public List<GluuAttribute> searchAttributes(int sizeLimit, boolean active) throws Exception {
        Filter activeFilter = Filter.createEqualityFilter(OxConstants.gluuStatus, "active");
        if (!active) {
            activeFilter = Filter.createEqualityFilter(OxConstants.gluuStatus, "inactive");
        }
        return persistenceEntryManager.findEntries(getDnForAttribute(null), GluuAttribute.class,
                activeFilter, sizeLimit);

    }

    public List<GluuAttribute> findAttributes(String pattern, int sizeLimit, boolean active) throws Exception {
        Filter activeFilter = Filter.createEqualityFilter(OxConstants.gluuStatus, "active");
        if (!active) {
            activeFilter = Filter.createEqualityFilter(OxConstants.gluuStatus, "inactive");
        }
        String[] targetArray = new String[]{pattern};
        Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.description, null, targetArray, null);
        Filter nameFilter = Filter.createSubstringFilter(OxConstants.attributeName, null, targetArray, null);
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

}