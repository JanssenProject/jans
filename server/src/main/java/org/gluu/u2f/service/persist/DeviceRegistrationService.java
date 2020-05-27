package org.gluu.u2f.service.persist;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.gluu.fido.model.entry.DeviceRegistration;
import org.gluu.oxauth.model.config.StaticConfiguration;

/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

import org.gluu.oxauth.model.fido.u2f.DeviceRegistrationStatus;
import org.gluu.oxauth.service.common.UserService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;

/**
 * Provides search operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/27/2020
 */
@ApplicationScoped
public class DeviceRegistrationService {

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private UserService userService;

	@Inject
	private StaticConfiguration staticConfiguration;

	public boolean containsBranch(final String baseDn) {
		return persistenceEntryManager.contains(baseDn, SimpleBranch.class);
	}

	public List<DeviceRegistration> findAllRegisteredByUsername(String username, String appId, String ... returnAttributes) {
        String userInum = userService.getUserInum(username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForU2fUserDevices(userInum);

		if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	if (!containsBranch(baseDn)) {
                return Collections.emptyList();
        	}
        }
		
        Filter resultFilter = null;
		if (StringUtils.isEmpty(appId)) {
			resultFilter = Filter.createEqualityFilter("oxStatus", DeviceRegistrationStatus.ACTIVE.getValue());
		} else {
	        Filter appIdFilter = Filter.createEqualityFilter("oxApplication", appId);
	        Filter activeDeviceFilter = Filter.createEqualityFilter("oxStatus", DeviceRegistrationStatus.ACTIVE.getValue());
	        resultFilter = Filter.createANDFilter(appIdFilter, activeDeviceFilter);
		}

		return persistenceEntryManager.findEntries(baseDn, DeviceRegistration.class, resultFilter, returnAttributes);
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
		final String peopleDn = staticConfiguration.getBaseDn().getPeople();
		return String.format("ou=fido,inum=%s,%s", userInum, peopleDn);
	}

}
