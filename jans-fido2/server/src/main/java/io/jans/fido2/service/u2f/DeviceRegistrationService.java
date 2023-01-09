/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.u2f;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.model.u2f.DeviceRegistrationStatus;
import io.jans.as.model.util.Base64Util;
import io.jans.fido2.model.u2f.DeviceRegistration;
import io.jans.fido2.service.shared.UserService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@ApplicationScoped
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
        String baseDn = getBaseDnForU2fUserDevices(userInum);
        if (!ldapEntryManager.hasBranchesSupport(baseDn)) {
            return;
        }

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

    public List<DeviceRegistration> findUserDeviceRegistrations(String userInum, String appId, String... returnAttributes) {
        prepareBranch(userInum);

        String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
        Filter userInumFilter = Filter.createEqualityFilter("personInum", userInum);
        Filter appIdFilter = Filter.createEqualityFilter("jansApp", appId);

        Filter filter = Filter.createANDFilter(userInumFilter, appIdFilter);

        return ldapEntryManager.findEntries(baseDnForU2fDevices, DeviceRegistration.class, filter, returnAttributes);
    }

    public List<DeviceRegistration> findDeviceRegistrationsByKeyHandle(String appId, String keyHandle, String... returnAttributes) {
        if (io.jans.util.StringHelper.isEmpty(appId) || StringHelper.isEmpty(keyHandle)) {
            return new ArrayList<DeviceRegistration>(0);
        }

        byte[] keyHandleDecoded = Base64Util.base64urldecode(keyHandle);

        String baseDn = userService.getDnForUser(null);

        Filter deviceObjectClassFilter = Filter.createEqualityFilter("objectClass", "jansDeviceRegistration");
        Filter deviceHashCodeFilter = Filter.createEqualityFilter("jansDeviceHashCode", getKeyHandleHashCode(keyHandleDecoded));
        Filter deviceKeyHandleFilter = Filter.createEqualityFilter("jansDeviceKeyHandle", keyHandle);
        Filter appIdFilter = Filter.createEqualityFilter("jansApp", appId);

        Filter filter = Filter.createANDFilter(deviceObjectClassFilter, deviceHashCodeFilter, appIdFilter, deviceKeyHandleFilter);
        log.debug("Filter --> "+filter);
        return ldapEntryManager.findEntries(baseDn, DeviceRegistration.class, filter, returnAttributes);
    }

    public DeviceRegistration findOneStepUserDeviceRegistration(String deviceId, String... returnAttributes) {
        String deviceDn = getDnForOneStepU2fDevice(deviceId);

        return ldapEntryManager.find(DeviceRegistration.class, deviceDn);
    }

    public void addUserDeviceRegistration(String userInum, DeviceRegistration deviceRegistration) {
        prepareBranch(userInum);

        // Final registration entry should be without expiration
        deviceRegistration.clearExpiration();

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

        // Final registration entry should be without expiration
        deviceRegistration.clearExpiration();

        //fix: personInum should be populated
        deviceRegistration.setUserInum(userInum);
        addUserDeviceRegistration(userInum, deviceRegistration);

        return true;
    }

    public void addOneStepDeviceRegistration(DeviceRegistration deviceRegistration) {
        // Set expiration for one step flow
        deviceRegistration.setExpiration();

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

        return ldapEntryManager.findEntries(u2fBaseDn, DeviceRegistration.class, expirationFilter, SearchScope.SUB, returnAttributes, batchOperation, 0, sizeLimit, chunkSize);
    }

    public int getCountDeviceRegistrations(String appId) {
        String baseDn = userService.getDnForUser(null);

        Filter appIdFilter = Filter.createEqualityFilter("jansApp", appId);
        Filter activeDeviceFilter = Filter.createEqualityFilter("jansStatus", DeviceRegistrationStatus.ACTIVE.getValue());
        Filter resultFilter = Filter.createANDFilter(appIdFilter, activeDeviceFilter);

        return ldapEntryManager.countEntries(baseDn, DeviceRegistration.class, resultFilter);
    }

    /**
     * Build DN string for U2F user device
     */
    public String getDnForU2fDevice(String userInum, String jsId) {
        String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
        if (StringHelper.isEmpty(jsId)) {
            return baseDnForU2fDevices;
        }
        return String.format("jansId=%s,%s", jsId, baseDnForU2fDevices);
    }

    public String getBaseDnForU2fUserDevices(String userInum) {
        if (StringHelper.isEmpty(userInum)) {
            return getDnForOneStepU2fDevice("");
        }
        final String userBaseDn = userService.getDnForUser(userInum); // "ou=fido,inum=1234,ou=people,o=jans"
        return String.format("ou=fido,%s", userBaseDn);
    }

    public String getDnForOneStepU2fDevice(String deviceRegistrationId) {
        final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=registered_devices,ou=u2f,o=jans
        if (StringHelper.isEmpty(deviceRegistrationId)) {
            return String.format("ou=registered_devices,%s", u2fBaseDn);
        }

        return String.format("jansId=%s,ou=registered_devices,%s", deviceRegistrationId, u2fBaseDn);
    }

    /*
     * Generate non unique hash code to split keyHandle among small cluster with 10-20 elements
     *
     * This hash code will be used to generate small LDAP indexes
     */
    public int getKeyHandleHashCode(byte[] keyHandle) {
        int hash = 0;
        for (int j = 0; j < keyHandle.length; j++) {
            hash += keyHandle[j] * j;
        }

        return hash;
    }

    public void merge(DeviceRegistration device) {
        ldapEntryManager.merge(device);
    }
}
