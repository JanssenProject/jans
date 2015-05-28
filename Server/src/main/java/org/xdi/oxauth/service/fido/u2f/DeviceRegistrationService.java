/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;

/**
 * Provides operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
//@Scope(ScopeType.STATELESS)
@Scope(ScopeType.APPLICATION)
@Name("deviceRegistrationService")
@AutoCreate
public class DeviceRegistrationService {

	@In
	private LdapEntryManager ldapEntryManager;

	@Logger
	private Log log;

	private HashMap<String, List<DeviceRegistration>> devices = new HashMap<String, List<DeviceRegistration>>();

	public void addBranch(final String userName) {
		SimpleBranch branch = new SimpleBranch();
		branch.setOrganizationalUnitName("device_registration");
		branch.setDn(getDnForResourceSet(null));

		ldapEntryManager.persist(branch);
	}

	public List<DeviceRegistration> findUserDeviceRegistrations(String appId, String userName) {
		List<DeviceRegistration> userDevices = devices.get(userName);
		
		if (userDevices == null) {
			return new ArrayList<DeviceRegistration>(0);
		}
		
		return userDevices;
	}

	public void addUserDeviceRegistration(String userName, String appId, DeviceRegistration deviceRegistration) {
		List<DeviceRegistration> userDevices = devices.get(userName);
		if (userDevices == null) {
			userDevices = new ArrayList<DeviceRegistration>();
			devices.put(userName, userDevices);
		}

		userDevices.add(deviceRegistration);
	}

	public void updateDeviceRegistration(String userName, DeviceRegistration usedDeviceRegistration) {
		List<DeviceRegistration> userDevices = devices.get(userName);
		if (userDevices != null) {
			for (Iterator<DeviceRegistration> it = userDevices.iterator(); it.hasNext();) {
				DeviceRegistration userDevice = (DeviceRegistration) it.next();
				if (userDevice.getKeyHandle() == usedDeviceRegistration.getKeyHandle()) {
					it.remove();
					break;
				}
			}

			userDevices.add(usedDeviceRegistration);
		}
	}

	/**
	 * Build DN string for resource set description
	 */
	public String getDnForResourceSet(String oxId) {
		if (StringHelper.isEmpty(oxId)) {
			return getBaseDnForResourceSet();
		}
		return String.format("oxId=%s,%s", oxId, getBaseDnForResourceSet());
	}

	public String getBaseDnForResourceSet() {
		final String umaBaseDn = ConfigurationFactory.getBaseDn().getUmaBase(); // "ou=uma,o=@!1111,o=gluu"
		return String.format("ou=resource_sets,%s", umaBaseDn);
	}

	/**
	 * Get ResourceSetService instance
	 *
	 * @return ResourceSetService instance
	 */
	public static DeviceRegistrationService instance() {
		return (DeviceRegistrationService) Component.getInstance(DeviceRegistrationService.class);
	}

}
