package io.jans.link.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//import javax.inject.Inject;

import io.jans.link.constants.JansConstants;
import io.jans.link.model.GluuCustomPerson;
import io.jans.link.model.GluuFido2Device;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class Fido2DeviceService implements Serializable {

	private static final long serialVersionUID = 5874835162873627676L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private OrganizationService organizationService;

	public boolean removeFido2(GluuCustomPerson person, String deviceID) {
		try {
			String finalDn = String.format("oxId=%s,ou=fido2_register,", deviceID);
			finalDn = finalDn.concat(person.getDn());
			ldapEntryManager.removeRecursively(finalDn, GluuCustomPerson.class);
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
			return String.format("oxId=%s,ou=fido2_register,inum=%s,ou=people,%s", oxid, personInum, orgDn);
		}
		return String.format("ou=people,%s", orgDn);
	}

	public List<GluuFido2Device> findAllFido2Devices(GluuCustomPerson person) {
		try {
			String baseDnForU2fDevices = getDnForFido2Device(null, person.getInum());
			Filter inumFilter = Filter.createEqualityFilter(JansConstants.PERSON_INUM, person.getInum());
			return ldapEntryManager.findEntries(baseDnForU2fDevices, GluuFido2Device.class, inumFilter);
		} catch (EntryPersistenceException e) {
			log.warn("No fido2 devices enrolled for " + person.getDisplayName());
			return new ArrayList<>();
		}
	}

	public GluuFido2Device getFido2DeviceById(String userId, String id) {
		GluuFido2Device f2d = null;
		try {
			String dn = getDnForFido2Device(id, userId);
			if (StringUtils.isNotEmpty(userId)) {
				f2d = ldapEntryManager.find(GluuFido2Device.class, dn);
			} else {
				Filter filter = Filter.createEqualityFilter("oxId", id);
				f2d = ldapEntryManager.findEntries(dn, GluuFido2Device.class, filter).get(0);
			}
		} catch (Exception e) {
			log.error("Failed to find Fido 2 device with id " + id, e);
		}
		return f2d;

	}

	public void updateFido2Device(GluuFido2Device fido2Device) {
		ldapEntryManager.merge(fido2Device);
	}

	public void removeFido2Device(GluuFido2Device fido2Device) {
		ldapEntryManager.removeRecursively(fido2Device.getDn(), GluuFido2Device.class);
	}

	public GluuFido2Device getGluuCustomFidoDeviceById(String id, String userId) {
		GluuFido2Device gluuCustomFidoDevice = null;
		try {
			String dn = getDnForFido2Device(id, userId);
			if (StringUtils.isNotEmpty(userId))
				gluuCustomFidoDevice = ldapEntryManager.find(GluuFido2Device.class, dn);
			else {
				Filter filter = Filter.createEqualityFilter("oxId", id);
				gluuCustomFidoDevice = ldapEntryManager.findEntries(dn, GluuFido2Device.class, filter).get(0);
			}
		} catch (Exception e) {
			log.error("Failed to find device by id " + id, e);
		}

		return gluuCustomFidoDevice;
	}

}
