package org.gluu.oxauth.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

//import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.oxauth.model.GluuOrganization;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.CacheService;
import org.gluu.util.OxConstants;

@Stateless
@Named("organizationService")
public class OrganizationService extends org.gluu.service.OrganizationService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8966940469789981584L;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private CacheService cacheService;

	/**
	 * Update organization entry
	 * 
	 * @param organization
	 *            Organization
	 */
	public void updateOrganization(GluuOrganization organization) {
		ldapEntryManager.merge(organization);

	}

	/**
	 * Check if LDAP server contains organization with specified attributes
	 * 
	 * @return True if organization with specified attributes exist
	 */
	public boolean containsOrganization(GluuOrganization organization) {
		return ldapEntryManager.contains(organization);
	}

	public GluuOrganization getOrganization() {
		String key = OxConstants.CACHE_ORGANIZATION_KEY;
		GluuOrganization organization = (GluuOrganization) cacheService.get(key);
		if (organization == null) {
			String orgDn = getDnForOrganization();
			organization = ldapEntryManager.find(GluuOrganization.class, orgDn);
			cacheService.put(key, organization);
		}

		return organization;
	}

	public String getDnForOrganization() {
		return "o=gluu";
	}

}
