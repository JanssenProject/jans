/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.fido.u2f;

import org.gluu.oxauth.model.fido.u2f.DeviceRegistration;
import org.gluu.oxauth.model.fido.u2f.DeviceRegistrationStatus;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.service.UserService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@Stateless
@Named
public class DeviceRegistrationService {

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private UserService userService;

	@Inject
	private StaticConfiguration staticConfiguration;

	public void addBranch(final String userInum) {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("fido");
		branch.setDn(getBaseDnForU2fUserDevices(userInum));

		ldapEntryManager.persist(branch);
	}

	public boolean containsBranch(final String userInum) {
		return ldapEntryManager.contains(getBaseDnForU2fUserDevices(userInum), SimpleBranch.class);
	}

	public void prepareBranch(final String userInum) {
		// Create U2F user device registrations branch if needed
		if (!containsBranch(userInum)) {
			addBranch(userInum);
		}
	}

	public DeviceRegistration findUserDeviceRegistration(String userInum, String deviceId, String... returnAttributes) {
		prepareBranch(userInum);

		String deviceDn = getDnForU2fDevice(userInum, deviceId);

		return ldapEntryManager.find(deviceDn, DeviceRegistration.class, returnAttributes);
	}

	public List<DeviceRegistration> findUserDeviceRegistrations(String userInum, String appId, String ... returnAttributes) {
		prepareBranch(userInum);

		String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
		Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);

		return ldapEntryManager.findEntries(baseDnForU2fDevices, DeviceRegistration.class, appIdFilter, returnAttributes);
	}

	public List<DeviceRegistration> findDeviceRegistrationsByKeyHandle(String appId, String keyHandle, String ... returnAttributes) {
		if (org.gluu.util.StringHelper.isEmpty(appId) || StringHelper.isEmpty(keyHandle)) {
			return new ArrayList<DeviceRegistration>(0);
		}

		byte[] keyHandleDecoded = Base64Util.base64urldecode(keyHandle);

		String baseDn = userService.getDnForUser(null);

		Filter deviceObjectClassFilter = Filter.createEqualityFilter("objectClass", "oxDeviceRegistration");
		Filter deviceHashCodeFilter = Filter.createEqualityFilter("oxDeviceHashCode", String.valueOf(getKeyHandleHashCode(keyHandleDecoded)));
		Filter deviceKeyHandleFilter = Filter.createEqualityFilter("oxDeviceKeyHandle", keyHandle);
		Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);

		Filter filter = Filter.createANDFilter(deviceObjectClassFilter, deviceHashCodeFilter, appIdFilter, deviceKeyHandleFilter);

		return ldapEntryManager.findEntries(baseDn, DeviceRegistration.class, filter, returnAttributes);
	}

	public DeviceRegistration findOneStepUserDeviceRegistration(String deviceId, String... returnAttributes) {
		String deviceDn = getDnForOneStepU2fDevice(deviceId);

		return ldapEntryManager.find(DeviceRegistration.class, deviceDn);
	}

	public void addUserDeviceRegistration(String userInum, DeviceRegistration deviceRegistration) {
		prepareBranch(userInum);
        deviceRegistration.setDeletable(false);
		ldapEntryManager.persist(deviceRegistration);
	}

	public boolean attachUserDeviceRegistration(String userInum, String oneStepDeviceId) {
		String oneStepDeviceDn = getDnForOneStepU2fDevice(oneStepDeviceId);

		// Load temporary stored device registration
		DeviceRegistration deviceRegistration = ldapEntryManager.find(DeviceRegistration.class, oneStepDeviceDn);
		if (deviceRegistration == null) {
			return false;
		}

		// Remove temporary stored device registration
		removeUserDeviceRegistration(deviceRegistration);

		// Attach user device registration to user
		String deviceDn = getDnForU2fDevice(userInum, deviceRegistration.getId());

		deviceRegistration.setDn(deviceDn);
		addUserDeviceRegistration(userInum, deviceRegistration);

		return true;
	}

	public void addOneStepDeviceRegistration(DeviceRegistration deviceRegistration) {
		ldapEntryManager.persist(deviceRegistration);
	}

	public void updateDeviceRegistration(String userInum, DeviceRegistration deviceRegistration) {
		prepareBranch(userInum);

		ldapEntryManager.merge(deviceRegistration);
	}

	public void disableUserDeviceRegistration(DeviceRegistration deviceRegistration) {
		deviceRegistration.setStatus(DeviceRegistrationStatus.COMPROMISED);

		ldapEntryManager.merge(deviceRegistration);
	}

	public void removeUserDeviceRegistration(DeviceRegistration deviceRegistration) {
		ldapEntryManager.remove(deviceRegistration);
	}

	public List<DeviceRegistration> getExpiredDeviceRegistrations(BatchOperation<DeviceRegistration> batchOperation, Date expirationDate, String[] returnAttributes, int sizeLimit, int chunkSize) {
		final String u2fBaseDn = getDnForOneStepU2fDevice(null);
		Filter expirationFilter = Filter.createLessOrEqualFilter("creationDate", ldapEntryManager.encodeTime(u2fBaseDn, expirationDate));

		List<DeviceRegistration> deviceRegistrations = ldapEntryManager.findEntries(u2fBaseDn, DeviceRegistration.class, expirationFilter, SearchScope.SUB, returnAttributes, batchOperation, 0, sizeLimit, chunkSize);

		return deviceRegistrations;
	}

    public int getCountDeviceRegistrations(String appId) {
        String baseDn = userService.getDnForUser(null);
        
        Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);
        Filter activeDeviceFilter = Filter.createEqualityFilter("oxStatus", DeviceRegistrationStatus.ACTIVE.getValue());
        Filter resultFilter = Filter.createANDFilter(appIdFilter, activeDeviceFilter);
        
        return ldapEntryManager.countEntries(baseDn, DeviceRegistration.class, resultFilter);
    }

	/**
	 * Build DN string for U2F user device
	 */
	public String getDnForU2fDevice(String userInum, String oxId) {
		String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
		if (StringHelper.isEmpty(oxId)) {
			return baseDnForU2fDevices;
		}
		return String.format("oxId=%s,%s", oxId, baseDnForU2fDevices);
	}

	public String getBaseDnForU2fUserDevices(String userInum) {
        if (StringHelper.isEmpty(userInum)) {
            return getDnForOneStepU2fDevice("");
        }
		final String userBaseDn = userService.getDnForUser(userInum); // "ou=fido,inum=1234,ou=people,o=gluu"
		return String.format("ou=fido,%s", userBaseDn);
	}

	public String getDnForOneStepU2fDevice(String deviceRegistrationId) {
		final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=registered_devices,ou=u2f,o=gluu
		if (StringHelper.isEmpty(deviceRegistrationId)) {
			return String.format("ou=registered_devices,%s", u2fBaseDn);
		}

		return String.format("oxid=%s,ou=registered_devices,%s", deviceRegistrationId, u2fBaseDn);
	}

    /*
     * Generate non unique hash code to split keyHandle among small cluster with 10-20 elements
     *
     * This hash code will be used to generate small LDAP indexes
     */
    public int getKeyHandleHashCode(byte[] keyHandle) {
		int hash = 0;
		for (int j = 0; j < keyHandle.length; j++) {
			hash += keyHandle[j]*j;
		}

		return hash;
    }

    public void merge(DeviceRegistration device) {
        ldapEntryManager.merge(device);
    }
}
