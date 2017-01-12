/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.config.StaticConf;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistrationStatus;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.service.CleanerTimer;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@Scope(ScopeType.STATELESS)
@Name("deviceRegistrationService")
@AutoCreate
public class DeviceRegistrationService {

	@In
	private LdapEntryManager ldapEntryManager;

	@In
	private UserService userService;

	@Logger
	private Log log;

	@In
	private StaticConf staticConfiguration;

	/**
	 * Get DeviceRegistrationService instance
	 *
	 * @return DeviceRegistrationService instance
	 */
	public static DeviceRegistrationService instance() {
		return (DeviceRegistrationService) Component.getInstance(DeviceRegistrationService.class);
	}

	public void addBranch(final String userInum) {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("fido");
		branch.setDn(getBaseDnForU2fUserDevices(userInum));

		ldapEntryManager.persist(branch);
	}

	public boolean containsBranch(final String userInum) {
		return ldapEntryManager.contains(SimpleBranch.class, getBaseDnForU2fUserDevices(userInum));
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

		return ldapEntryManager.find(DeviceRegistration.class, deviceDn, returnAttributes);
	}

	public List<DeviceRegistration> findUserDeviceRegistrations(String userInum, String appId, String ... returnAttributes) {
		prepareBranch(userInum);

		String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
		Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);

		return ldapEntryManager.findEntries(baseDnForU2fDevices, DeviceRegistration.class, returnAttributes, appIdFilter);
	}

	public List<DeviceRegistration> findDeviceRegistrationsByKeyHandle(String appId, String keyHandle, String ... returnAttributes) {
		if (org.xdi.util.StringHelper.isEmpty(appId) || StringHelper.isEmpty(keyHandle)) {
			return new ArrayList<DeviceRegistration>(0);
		}

		byte[] keyHandleDecoded = Base64Util.base64urldecode(keyHandle);

		String baseDn = userService.getDnForUser(null);

		Filter deviceObjectClassFilter = Filter.createEqualityFilter("objectClass", "oxDeviceRegistration");
		Filter deviceHashCodeFilter = Filter.createEqualityFilter("oxDeviceHashCode", String.valueOf(getKeyHandleHashCode(keyHandleDecoded)));
		Filter deviceKeyHandleFilter = Filter.createEqualityFilter("oxDeviceKeyHandle", keyHandle);
		Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);

		Filter filter = Filter.createANDFilter(deviceObjectClassFilter, deviceHashCodeFilter, appIdFilter, deviceKeyHandleFilter);

		return ldapEntryManager.findEntries(baseDn, DeviceRegistration.class, returnAttributes, filter);
	}

	public DeviceRegistration findOneStepUserDeviceRegistration(String deviceId, String... returnAttributes) {
		String deviceDn = getDnForOneStepU2fDevice(deviceId);

		return ldapEntryManager.find(DeviceRegistration.class, deviceDn);
	}

	public void addUserDeviceRegistration(String userInum, DeviceRegistration deviceRegistration) {
		prepareBranch(userInum);

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

	public List<DeviceRegistration> getExpiredDeviceRegistrations(BatchOperation<DeviceRegistration> batchOperation, Date expirationDate) {
		final String u2fBaseDn = getDnForOneStepU2fDevice(null);
		Filter expirationFilter = Filter.createLessOrEqualFilter("creationDate", ldapEntryManager.encodeGeneralizedTime(expirationDate));

		List<DeviceRegistration> deviceRegistrations = ldapEntryManager.findEntries(u2fBaseDn, DeviceRegistration.class, expirationFilter, SearchScope.SUB, null, batchOperation, 0, CleanerTimer.BATCH_SIZE, CleanerTimer.BATCH_SIZE);

		return deviceRegistrations;
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
		final String userBaseDn = userService.getDnForUser(userInum); // "ou=fido,inum=1234,ou=people,o=@!1111,o=gluu"
		return String.format("ou=fido,%s", userBaseDn);
	}

	public String getDnForOneStepU2fDevice(String deviceRegistrationId) {
		final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=registered_devices,ou=u2f,o=@!1111,o=gluu
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

}
