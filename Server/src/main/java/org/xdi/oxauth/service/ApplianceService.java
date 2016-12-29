/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.util.StringHelper;

import java.util.List;

/**
 * GluuAppliance service
 *
 * @author Reda Zerrad Date: 08.10.2012
 */
@Scope(ScopeType.STATELESS)
@Name("applianceService")
@AutoCreate
public class ApplianceService {

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

    @In
    private AppConfiguration appConfiguration;

	@In
	private StaticConf staticConfiguration;
	/**
	 * Add new appliance
	 * @param appliance Appliance
	 */
	public void addAppliance(GluuAppliance appliance) {
		ldapEntryManager.persist(appliance);
	}

	/**
	 * Update appliance entry
	 * @param appliance GluuAppliance
	 */
	public void updateAppliance(GluuAppliance appliance) {
		ldapEntryManager.merge(appliance);
	}

	/**
	 * Check if LDAP server contains appliance with specified attributes
	 * @return True if appliance with specified attributes exist
	 */
	public boolean containsAppliance(GluuAppliance appliance) {
		return ldapEntryManager.contains(appliance);
	}

	/**
	 * Get appliance by inum
	 * @param inum Appliance Inum
	 * @return Appliance
	 * @throws Exception 
	 */
	public GluuAppliance getApplianceByInum(String inum) {
		return ldapEntryManager.find(GluuAppliance.class, getDnForAppliance(inum));
	}

	/**
	 * Get appliance
	 * @return Appliance
	 * @throws Exception 
	 */
	public GluuAppliance getAppliance() {
		String applianceInum = getApplianceInum();
		if (StringHelper.isEmpty(applianceInum)) {
			return null;
		}

		return ldapEntryManager.find(GluuAppliance.class, getDnForAppliance(getApplianceInum()));
	}

	/**
	 * Get all appliances
	 * @return List of attributes
	 * @throws Exception 
	 */
	public List<GluuAppliance> getAppliances() {
		List<GluuAppliance> applianceList = ldapEntryManager.findEntries(getDnForAppliance(null), GluuAppliance.class, null);
		return applianceList;
	}

	/**
	 * Build DN string for appliance
	 * @param inum Inum
	 * @return DN string for specified appliance or DN for appliances branch if inum is null
	 * @throws Exception 
	 */
	public String getDnForAppliance(String inum) {
		String baseDn = staticConfiguration.getBaseDn().getAppliance();
		if (StringHelper.isEmpty(inum)) {
			return baseDn;
		}

		return String.format("inum=%s,%s", inum, baseDn);
	}

	/**
	 * Build DN string for appliance
	 * @return DN string for appliance
	 * @throws Exception 
	 */
	public String getDnForAppliance() {
		return getDnForAppliance(getApplianceInum());
	}

	public String getApplianceInum() {
		return appConfiguration.getApplianceInum();
	}

	/**
	 * Get applianceService instance
	 * @return ApplianceService instance
	 */
	public static ApplianceService instance() {
		return (ApplianceService) Component.getInstance(ApplianceService.class);
	}

}

