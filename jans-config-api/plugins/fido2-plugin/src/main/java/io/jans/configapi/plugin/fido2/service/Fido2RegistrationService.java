/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import io.jans.as.common.service.OrganizationService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.model.fido2.Fido2DeviceData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class Fido2RegistrationService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    OrganizationService organizationService;

    @Inject
    private UserFido2Service userFido2Srv;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public List<Fido2RegistrationEntry> findAllByUsername(String username) {
        String userInum = userFido2Srv.getUserInum(username);
        log.error("\n\n userInum:{} based on username:{}", userInum, username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        log.error("\n\n baseDn:{} for userInum:{}, username:{}", baseDn, userInum, username);
        if (persistenceEntryManager.hasBranchesSupport(baseDn) && !containsBranch(baseDn)) {
            return Collections.emptyList();
        }

        Filter userFilter = Filter.createEqualityFilter("personInum", userInum);

        return persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, userFilter);

    }

    public List<Fido2RegistrationEntry> findAllRegisteredByUsername(String username) {
        String userInum = userFido2Srv.getUserInum(username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (persistenceEntryManager.hasBranchesSupport(baseDn) && !containsBranch(baseDn)) {
            return Collections.emptyList();

        }

        Filter registeredFilter = Filter.createEqualityFilter("jansStatus",
                Fido2RegistrationStatus.registered.getValue());

        return persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, registeredFilter);
    }

    public String getBaseDnForFido2RegistrationEntries(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_register,inum=1234,ou=people,o=jans"
        if (StringHelper.isEmpty(userInum)) {
            return userBaseDn;
        }

        return String.format("ou=fido2_register,%s", userBaseDn);
    }

    public String getDnForUser(String userInum) {
        String peopleDn = staticConfiguration.getBaseDn().getPeople();
        if (StringHelper.isEmpty(userInum)) {
            return peopleDn;
        }

        return String.format("inum=%s,%s", userInum, peopleDn);
    }

    public boolean containsBranch(final String baseDn) {
        return persistenceEntryManager.contains(baseDn, SimpleBranch.class);
    }

    public String getFido2DnForUSer(String userName) {
        String userInum = userFido2Srv.getUserInum(userName);
        log.error("\n\n userInum:{} based on userName:{}", userInum, userName);
        if (userInum == null) {
            throw new InvalidAttributeException("No user found with userName:{" + userName + "}!!!");
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        log.error("\n\n baseDn:{} for userInum:{}, userName:{}", baseDn, userInum, userName);
        return baseDn;
    }

    public void removeFido2DeviceData(String userName, String deviceUid) {
        log.error("\n\n Remove Fido2 device for userName:{} and deviceUid:{}", userName, deviceUid);
        if (StringUtils.isBlank(userName)) {
            throw new InvalidAttributeException("User name is null!");
        }

        if (StringUtils.isBlank(deviceUid)) {
            throw new InvalidAttributeException("Device uid is null!");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Fido2DeviceData for userName:{");
        sb.append(userName);
        sb.append("} and device uid:{");
        sb.append(deviceUid);
        sb.append("}");

        String userInum = userFido2Srv.getUserInum(userName);
        log.error("\n\n userInum:{} for userName:{}", userInum, userName);
        if (StringUtils.isBlank(userInum)) {
            throw new InvalidAttributeException("No user found with username:{" + userName + "}!");
        }

        Fido2DeviceData fido2DeviceData = this.getFido2DeviceById(userInum, deviceUid);
        if (fido2DeviceData == null) {
            throw new WebApplicationException("No " + sb + " found!");
        }

        String dn = getDnForFido2Device(userInum, deviceUid);
        log.error("\n\n DN for Fido2Device to be deleted is:{}", dn);

        persistenceEntryManager.removeRecursively(dn, Fido2DeviceData.class);
        fido2DeviceData = this.getFido2DeviceById(userInum, deviceUid);
        if (fido2DeviceData != null) {
            throw new WebApplicationException(sb + " could not be deleted!");
        }
        log.error("\n\n Successfully deleted {}", sb);
    }

    public void removeFido2DeviceData(String deviceUid) {
        log.error("\n\n Remove Fido2 device with uid:{}", deviceUid);

        if (StringUtils.isBlank(deviceUid)) {
            throw new InvalidAttributeException("Device uid is null!");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Fido2DeviceData ");
        sb.append(" with uid:{");
        sb.append(deviceUid);
        sb.append("}");

        Fido2DeviceData fido2DeviceData = this.getFido2DeviceById(null, deviceUid);
        if (fido2DeviceData == null) {
            throw new WebApplicationException("No " + sb + " found!");
        }

        String dn = getDnForFido2Device(null, deviceUid);
        log.error("\n\n DN for Fido2Device to be deleted is:{}", dn);

        persistenceEntryManager.removeRecursively(dn, Fido2DeviceData.class);
        fido2DeviceData = this.getFido2DeviceById(null, deviceUid);

        if (fido2DeviceData != null) {
            throw new WebApplicationException(sb + " could not be deleted!");
        }
        log.error("\n\n Successfully deleted {}", sb);
    }

    public Fido2DeviceData getFido2DeviceById(String userId, String uid) {
        log.debug("Get Fido2DeviceData for userId:{} - uid:{}", userId, uid);

        if (StringUtils.isBlank(uid)) {
            throw new InvalidAttributeException("Device uid is null!");
        }

        Fido2DeviceData fido2DeviceData = null;
        try {
            String dn = getDnForFido2Device(userId, uid);
            log.debug("Get Fido2DeviceData identified by dn:{}", dn);

            if (StringHelper.isNotEmpty(userId)) {
                fido2DeviceData = persistenceEntryManager.find(Fido2DeviceData.class, dn);
            } else {
                Filter filter = Filter.createEqualityFilter("jansId", uid);
                fido2DeviceData = persistenceEntryManager.findEntries(dn, Fido2DeviceData.class, filter).get(0);
            }
            log.error("\n\n Fido2DeviceData identified by dn:{} is:{}", dn, fido2DeviceData);
        } catch (Exception e) {
            log.error("Failed to find Fido2DeviceData with id: " + fido2DeviceData, e);
        }
        return fido2DeviceData;

    }

    public String getDnForFido2Device(String id, String userId) {
        String orgDn = organizationService.getDnForOrganization();
        if (!StringHelper.isEmpty(userId) && StringHelper.isEmpty(id)) {
            return String.format("ou=fido2_register,inum=%s,ou=people,%s", userId, orgDn);
        }
        if (!StringHelper.isEmpty(id) && !StringHelper.isEmpty(userId)) {
            return String.format("id=%s,ou=fido2_register,inum=%s,ou=people,%s", id, userId, orgDn);
        }
        return String.format("ou=people,%s", orgDn);
    }

}
