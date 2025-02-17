/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import io.jans.scim.model.JansCustomPerson;
import io.jans.scim.model.JansFido2Device;

@ApplicationScoped
public class Fido2DeviceService implements Serializable {

	private static final long serialVersionUID = 5874835162873627676L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private OrganizationService organizationService;

	public boolean removeFido2(JansCustomPerson person, String deviceID) {
		try {
			String finalDn = String.format("jansId=%s,ou=fido2_register,", deviceID);
			finalDn = finalDn.concat(person.getDn());
			ldapEntryManager.removeRecursively(finalDn, JansFido2Device.class);
			return true;
		} catch (Exception e) {
			log.error("", e);
			return false;
		}
	}

	public String getDnForFido2Device(String oxid, String personInum) {
		String orgDn = organizationService.getDnForOrganization();
		if (!StringHelper.isEmpty(personInum) && StringHelper.isEmpty(oxid)) {
			return String.format("ou=fido2_register,inum=%s,ou=people,%s", personInum, orgDn);
		}
		if (!StringHelper.isEmpty(oxid) && !StringHelper.isEmpty(personInum)) {
			return String.format("jansId=%s,ou=fido2_register,inum=%s,ou=people,%s", oxid, personInum, orgDn);
		}
		return String.format("ou=people,%s", orgDn);
	}

	public List<JansFido2Device> findAllFido2Devices(JansCustomPerson person) {
		try {
			String baseDnForU2fDevices = getDnForFido2Device(null, person.getInum());
			Filter inumFilter = Filter.createEqualityFilter("personInum", person.getInum());
			return ldapEntryManager.findEntries(baseDnForU2fDevices, JansFido2Device.class, inumFilter);
		} catch (EntryPersistenceException e) {
			log.warn("No fido2 devices enrolled for " + person.getDisplayName());
			return new ArrayList<>();
		}
	}

	public JansFido2Device getFido2DeviceById(String userId, String id) {
		JansFido2Device f2d = null;
		try {
			String dn = getDnForFido2Device(id, userId);
			if (StringUtils.isNotEmpty(userId)) {
				f2d = ldapEntryManager.find(JansFido2Device.class, dn);
			} else {
				Filter filter = Filter.createEqualityFilter("jansId", id);
				f2d = ldapEntryManager.findEntries(dn, JansFido2Device.class, filter).get(0);
			}
		} catch (Exception e) {
			log.error("Failed to find Fido 2 device with id " + id, e);
		}
		return f2d;

	}

	public void updateFido2Device(JansFido2Device fido2Device) {
		ldapEntryManager.merge(fido2Device);
	}

	public void removeFido2Device(JansFido2Device fido2Device) {
		ldapEntryManager.removeRecursively(fido2Device.getDn(), JansFido2Device.class);
	}

	public JansFido2Device getJansCustomFidoDeviceById(String id, String userId) {
		JansFido2Device gluuCustomFidoDevice = null;
		try {
			String dn = getDnForFido2Device(id, userId);
			if (StringUtils.isNotEmpty(userId))
				gluuCustomFidoDevice = ldapEntryManager.find(JansFido2Device.class, dn);
			else {
				Filter filter = Filter.createEqualityFilter("jansId", id);
				gluuCustomFidoDevice = ldapEntryManager.findEntries(dn, JansFido2Device.class, filter).get(0);
			}
		} catch (Exception e) {
			log.error("Failed to find device by id " + id, e);
		}

		return gluuCustomFidoDevice;
	}

}
