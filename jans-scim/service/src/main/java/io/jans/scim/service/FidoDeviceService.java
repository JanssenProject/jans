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

import org.apache.commons.lang.StringUtils;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;

import org.slf4j.Logger;

import io.jans.scim.model.fido.GluuCustomFidoDevice;

/**
 * @author Val Pecaoco Updated by jgomer on 2017-10-22
 */
@ApplicationScoped
public class FidoDeviceService implements Serializable {

	private static final long serialVersionUID = -206231314840676189L;

	@Inject
	private Logger log;

	@Inject
	private PersonService personService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	public String getDnForFidoDevice(String userId, String id) {
		String baseDn;
		if (userId != null && !userId.isEmpty()) {
			baseDn = "ou=fido," + personService.getDnForPerson(userId);
			if (id != null && !id.isEmpty()) {
				baseDn = "jansId=" + id + "," + baseDn;
			}
		} else {
			baseDn = personService.getDnForPerson(null);
		}

		return baseDn;
	}

	public GluuCustomFidoDevice getGluuCustomFidoDeviceById(String userId, String id) {
		GluuCustomFidoDevice gluuCustomFidoDevice = null;

		try {
			String dn = getDnForFidoDevice(userId, id);
			if (StringUtils.isNotEmpty(userId))
				gluuCustomFidoDevice = ldapEntryManager.find(GluuCustomFidoDevice.class, dn);
			else {
				Filter filter = Filter.createEqualityFilter("jansId", id);
				gluuCustomFidoDevice = ldapEntryManager.findEntries(dn, GluuCustomFidoDevice.class, filter).get(0);
			}
		} catch (Exception e) {
			log.error("Failed to find device by id " + id, e);
		}

		return gluuCustomFidoDevice;
	}

	public GluuCustomFidoDevice searchFidoDevice(Filter filter, String userId, String id) throws Exception {
		GluuCustomFidoDevice gluuCustomFidoDevice = null;

		List<GluuCustomFidoDevice> gluuCustomFidoDevices = ldapEntryManager.findEntries(getDnForFidoDevice(userId, id),
				GluuCustomFidoDevice.class, filter, 1);
		if (gluuCustomFidoDevices != null && !gluuCustomFidoDevices.isEmpty()) {
			gluuCustomFidoDevice = gluuCustomFidoDevices.get(0);
		}

		return gluuCustomFidoDevice;
	}

	public void updateGluuCustomFidoDevice(GluuCustomFidoDevice gluuCustomFidoDevice) {
		ldapEntryManager.merge(gluuCustomFidoDevice);
	}

	public void removeGluuCustomFidoDevice(GluuCustomFidoDevice gluuCustomFidoDevice) {
		ldapEntryManager.removeRecursively(gluuCustomFidoDevice.getDn(), GluuCustomFidoDevice.class);
	}

	public List<GluuCustomFidoDevice> searchFidoDevices(String userInum, String... returnAttributes) {
		try {
			Filter equalityFilter = Filter.createEqualityFilter("personInum", userInum);
			return ldapEntryManager.findEntries(getDnForFidoDevice(userInum, null), GluuCustomFidoDevice.class,
					equalityFilter, returnAttributes);
		} catch (Exception e) {
			log.error("", e);
			return new ArrayList<>();
		}
	}
}
