package org.gluu.oxauth.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

//import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.oxauth.model.GluuOrganization;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.BaseCacheService;
import org.gluu.service.CacheService;
import org.gluu.service.LocalCacheService;
import org.gluu.util.OxConstants;

@Stateless
@Named("organizationService")
public class OrganizationService extends org.gluu.service.OrganizationService {

	private static final long serialVersionUID = -8966940469789981584L;

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private CacheService cacheService;

    @Inject
    private LocalCacheService localCacheService;

	/**
	 * Update organization entry
	 * 
	 * @param organization
	 *            Organization
	 */
	public void updateOrganization(GluuOrganization organization) {
		ldapEntryManager.merge(organization);

	}

	public GluuOrganization getOrganization() {
    	BaseCacheService usedCacheService = getCacheService();
		String key = OxConstants.CACHE_ORGANIZATION_KEY;
		GluuOrganization organization = (GluuOrganization) usedCacheService.get(key);
		if (organization == null) {
			String orgDn = getDnForOrganization();
			organization = ldapEntryManager.find(GluuOrganization.class, orgDn);
			usedCacheService.put(key, organization);
		}

		return organization;
	}

	public String getDnForOrganization() {
		return "o=gluu";
	}

    private BaseCacheService getCacheService() {
    	if (appConfiguration.getUseLocalCache()) {
    		return localCacheService;
    	}
    	
    	return cacheService;
    }

}
