package org.gluu.oxauth.service;

import org.gluu.oxauth.model.GluuOrganization;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import org.gluu.service.BaseCacheService;
import org.gluu.service.CacheService;
import org.gluu.service.LocalCacheService;
import org.gluu.util.OxConstants;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
@Named("organizationService")
public class OrganizationService extends org.gluu.service.OrganizationService {

    private static final long serialVersionUID = -8966940469789981584L;
    public static final int ONE_MINUTE_IN_SECONDS = 60;

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
        return usedCacheService.getWithPut(OxConstants.CACHE_ORGANIZATION_KEY, () -> ldapEntryManager.find(GluuOrganization.class, getDnForOrganization()), ONE_MINUTE_IN_SECONDS);
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
