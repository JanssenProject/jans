/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.persist.PersistenceEntryManager;
import org.oxauth.persistence.model.appliance.GluuAppliance;
import org.slf4j.Logger;
import org.xdi.model.SmtpConfiguration;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter.EncryptionException;

/**
 * GluuAppliance service
 *
 * @author Reda Zerrad Date: 08.10.2012
 */
@Stateless
@Named
public class ApplianceService {

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private StaticConfiguration staticConfiguration;
    
    @Inject
    private EncryptionService encryptionService;

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

	public void decryptSmtpPassword(SmtpConfiguration smtpConfiguration) {
		if (smtpConfiguration == null) {
			return;
		}

		String password = smtpConfiguration.getPassword();
		if (StringHelper.isNotEmpty(password)) {
			try {
				smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(password));
			} catch (EncryptionException ex) {
				log.error("Failed to decrypt SMTP user password", ex);
			}
		}
	}

}

