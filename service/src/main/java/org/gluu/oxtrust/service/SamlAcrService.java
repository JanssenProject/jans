package org.gluu.oxtrust.service;

import java.io.Serializable;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxtrust.model.SamlAcr;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

@ApplicationScoped
public class SamlAcrService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5692082015849025306L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private OrganizationService organizationService;

	public String getDn(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=samlAcrs,%s", orgDn);
		}

		return String.format("inum=%s,ou=samlAcrs,%s", inum, orgDn);
	}

	public SamlAcr getByInum(String inum) {
		SamlAcr samlAcr = null;
		try {
			samlAcr = persistenceEntryManager.find(SamlAcr.class, getDn(inum));
		} catch (Exception e) {
			log.error("Failed to find samlAcr by Inum " + inum, e);
		}
		return samlAcr;
	}

	public void update(SamlAcr samlAcr) {
		persistenceEntryManager.merge(samlAcr);
	}

	public void add(SamlAcr samlAcr) {
		persistenceEntryManager.persist(samlAcr);
	}

	public void remove(SamlAcr samlAcr) {
		if (samlAcr.getDn() == null) {
			samlAcr.setDn(getDn(samlAcr.getInum()));
		}
		persistenceEntryManager.remove(samlAcr);
	}

	public SamlAcr[] getAll() {
		return persistenceEntryManager.findEntries(getDn(null), SamlAcr.class, null).stream()
				.toArray(size -> new SamlAcr[size]);
	}

	public boolean contains(String dn) {
		boolean result = false;
		try {
			result = persistenceEntryManager.contains(dn, SamlAcr.class);
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		}
		return result;
	}

	public String generateInumForSamlAcr() {
		SamlAcr samlAcr = null;
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumImpl();
			newDn = getDn(newInum);
			samlAcr = new SamlAcr();
			samlAcr.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, SamlAcr.class));
		return newInum;
	}

	private String generateInumImpl() {
		return UUID.randomUUID().toString();
	}

}
