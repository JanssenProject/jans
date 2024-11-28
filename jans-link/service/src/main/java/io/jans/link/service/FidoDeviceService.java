package io.jans.link.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//import javax.inject.Inject;

import io.jans.link.model.GluuCustomFidoDevice;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * @author Val Pecaoco Updated by jgomer on 2017-10-22
 */
@ApplicationScoped
public class FidoDeviceService implements IFidoDeviceService, Serializable {

	private static final long serialVersionUID = -206231314840676189L;

	@Inject
	private Logger log;

	@Inject
	private IPersonService personService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Override
	public String getDnForFidoDevice(String userId, String id) {
		String baseDn;
		if (userId != null && !userId.isEmpty()) {
			baseDn = "ou=fido," + personService.getDnForPerson(userId);
			if (id != null && !id.isEmpty()) {
				baseDn = "oxId=" + id + "," + baseDn;
			}
		} else {
			baseDn = personService.getDnForPerson(null);
		}

		return baseDn;
	}

	@Override
	public GluuCustomFidoDevice getGluuCustomFidoDeviceById(String userId, String id) {
		GluuCustomFidoDevice gluuCustomFidoDevice = null;

		try {
			String dn = getDnForFidoDevice(userId, id);
			if (StringUtils.isNotEmpty(userId))
				gluuCustomFidoDevice = ldapEntryManager.find(GluuCustomFidoDevice.class, dn);
			else {
				Filter filter = Filter.createEqualityFilter("oxId", id);
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

	@Override
	public void updateGluuCustomFidoDevice(GluuCustomFidoDevice gluuCustomFidoDevice) {
		ldapEntryManager.merge(gluuCustomFidoDevice);
	}

	@Override
	public void removeGluuCustomFidoDevice(GluuCustomFidoDevice gluuCustomFidoDevice) {
		ldapEntryManager.removeRecursively(gluuCustomFidoDevice.getDn(), GluuCustomFidoDevice.class);
	}

	@Override
	public List<GluuCustomFidoDevice> searchFidoDevices(String userInum, String... returnAttributes) {
		try {
			Filter equalityFilter = Filter.createEqualityFilter("personInum", userInum);
			return ldapEntryManager.findEntries(getDnForFidoDevice(userInum, null), GluuCustomFidoDevice.class,
					equalityFilter, returnAttributes);
		} catch (Exception e) {
			log.warn("", e);
			return new ArrayList<>();
		}
	}
}
