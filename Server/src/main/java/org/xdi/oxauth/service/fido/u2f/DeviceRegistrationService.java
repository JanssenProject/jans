/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.List;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistrationStatus;
import org.xdi.oxauth.service.UserService;

import com.unboundid.ldap.sdk.Filter;

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

	public void addBranch(final String userInum) {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("u2f_devices");
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

		return ldapEntryManager.find(DeviceRegistration.class, deviceDn);
	}

	public List<DeviceRegistration> findUserDeviceRegistrations(String userInum, String appId, String... returnAttributes) {
		prepareBranch(userInum);

		String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
		Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);

		return ldapEntryManager.findEntries(baseDnForU2fDevices, DeviceRegistration.class, returnAttributes, appIdFilter);
	}

	public void addUserDeviceRegistration(String userInum, DeviceRegistration deviceRegistration) {
		prepareBranch(userInum);

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
		final String userBaseDn = userService.getDnForUser(userInum); // "ou=u2f_devices,inum=1234,ou=people,o=@!1111,o=gluu"
		return String.format("ou=u2f_devices,%s", userBaseDn);
	}

	/**
	 * Get DeviceRegistrationService instance
	 *
	 * @return DeviceRegistrationService instance
	 */
	public static DeviceRegistrationService instance() {
		return (DeviceRegistrationService) Component.getInstance(DeviceRegistrationService.class);
	}

}
