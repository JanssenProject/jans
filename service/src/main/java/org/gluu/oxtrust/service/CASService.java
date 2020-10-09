/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.service;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.config.oxtrust.LdapShibbolethCASProtocolConfiguration;
import org.gluu.config.oxtrust.ShibbolethCASProtocolConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.INumGenerator;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * CAS LDAP configuration service.
 * 
 * @author Dmitry Ognyannikov, 2017
 */
@ApplicationScoped
public class CASService implements Serializable {

	private static final long serialVersionUID = -6130872937911013810L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	OrganizationService organizationService;

	@PostConstruct
	public void init() {
	}

	@PreDestroy
	public void destroy() {
	}

	public ShibbolethCASProtocolConfiguration loadCASConfiguration() {
		log.info("loadCASConfiguration() call");
		List<LdapShibbolethCASProtocolConfiguration> entries = persistenceEntryManager.findEntries(
				getDnForLdapShibbolethCASProtocolConfiguration(null), LdapShibbolethCASProtocolConfiguration.class,
				null);
		if (!entries.isEmpty())
			return entries.get(0).getCasProtocolConfiguration();
		else
			return null;
	}

	public void updateCASConfiguration(ShibbolethCASProtocolConfiguration entry) {
		log.info("updateCASConfiguration() call");
		LdapShibbolethCASProtocolConfiguration ldapEntry = persistenceEntryManager.find(
				LdapShibbolethCASProtocolConfiguration.class,
				getDnForLdapShibbolethCASProtocolConfiguration(entry.getInum()));
		ldapEntry.setInum(entry.getInum());
		ldapEntry.setCasProtocolConfiguration(entry);
		persistenceEntryManager.merge(ldapEntry);
	}

	public void addCASConfiguration(ShibbolethCASProtocolConfiguration entry) {
		log.info("addCASConfiguration() call");
		try {
			LdapShibbolethCASProtocolConfiguration ldapEntry = new LdapShibbolethCASProtocolConfiguration();
			ldapEntry.setCasProtocolConfiguration(entry);
			String inum = generateInum();
			log.info("getDnForLdapShibbolethCASProtocolConfiguration(inum) retsult: "
					+ getDnForLdapShibbolethCASProtocolConfiguration(inum));
			entry.setInum(inum);
			ldapEntry.setInum(inum);
			ldapEntry.setDn(getDnForLdapShibbolethCASProtocolConfiguration(inum));
			persistenceEntryManager.persist(ldapEntry);
		} catch (Exception e) {
			log.error("addIDPEntry() exception", e);
		}
	}

	private String getDnForLdapShibbolethCASProtocolConfiguration(String inum) {
		String organizationDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=cas,ou=oxidp,%s", organizationDn);
		}
		return String.format("inum=%s,ou=cas,ou=oxidp,%s", inum, organizationDn);
	}

	/**
	 * Generate new inum for Scope
	 * 
	 * @return New inum for Scope
	 * @throws Exception
	 */
	private static String generateInum() {
		return INumGenerator.generate(1);
	}

}
