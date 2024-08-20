/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    ConfigurationFactory configurationFactory;

    @Inject
    private UserFido2Service userFido2Srv;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public int getRecordMaxCount() {
        log.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }

    public PagedResult<Fido2RegistrationEntry> searchFido2Registration(SearchRequest searchRequest) {
        log.debug("Search Fido2Registration with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };

                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter("jansRegistrationData", null, targetArray,
                        null);
                Filter statusFilter = Filter.createSubstringFilter("jansStatus", null, targetArray, null);
                Filter notificationConfFilter = Filter.createSubstringFilter("jansDeviceNotificationConf", null,
                        targetArray, null);
                Filter deviceDataFilter = Filter.createSubstringFilter("jansDeviceData", null, targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, statusFilter,
                        notificationConfFilter, deviceDataFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        log.trace("Fido2Registration pattern searchFilter:{}", searchFilter);

        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("Fido2Registration dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        log.debug("Fido2Registration searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnFido2RegistrationEntry(null), Fido2RegistrationEntry.class,
                searchFilter, null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

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

    public void removeFido2RegistrationEntry(String userName, String deviceUid) {
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

        String dn = getDnFido2RegistrationEntry(userInum, deviceUid);
        log.error("\n\n DN for Fido2Device to be deleted is:{}", dn);

        persistenceEntryManager.removeRecursively(dn, Fido2DeviceData.class);
        fido2DeviceData = this.getFido2DeviceById(userInum, deviceUid);
        if (fido2DeviceData != null) {
            throw new WebApplicationException(sb + " could not be deleted!");
        }
        log.error("\n\n Successfully deleted {}", sb);
    }

    public void removeFido2RegistrationEntry(String uuid) {
        log.error("\n\n Remove Fido2RegistrationEntry request for device with uuid:{}", uuid);

        if (StringUtils.isBlank(uuid)) {
            throw new InvalidAttributeException("Device uuid is null!");
        }

        Fido2RegistrationEntry fido2RegistrationEntry = this.getFido2RegistrationEntryByDeviceId(uuid);
        log.debug("Fido2RegistrationEntry identified by uuid:{} is:{}", uuid, fido2RegistrationEntry);
        if (fido2RegistrationEntry == null) {
            throw new WebApplicationException("No device found with uuid:{" + uuid + "}");
        }

        String dn = this.getDnFido2RegistrationEntry(fido2RegistrationEntry.getId());
        log.error("\n\n Remove Fido2RegistrationEntry with dn:{}", dn);

        // delete entry
        persistenceEntryManager.removeRecursively(dn, Fido2RegistrationEntry.class);

        // verify post delete
        fido2RegistrationEntry = this.getFido2RegistrationEntryByDeviceId(uuid);
        if (fido2RegistrationEntry != null) {
            throw new WebApplicationException(
                    "Fido2RegistrationEntry device with uuid:{" + uuid + "} could not be deleted!");
        }
        log.error("\n\n Successfully deleted Fido2RegistrationEntry device with uuid:{}", uuid);
    }

    public Fido2DeviceData getFido2DeviceById(String userId, String uid) {
        log.debug("Get Fido2DeviceData for userId:{} - uid:{}", userId, uid);

        if (StringUtils.isBlank(uid)) {
            throw new InvalidAttributeException("Device uid is null!");
        }

        Fido2DeviceData fido2DeviceData = null;
        try {
            String dn = getDnFido2RegistrationEntry(userId, uid);
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

    public Fido2RegistrationEntry getFido2RegistrationEntryByDeviceId(String uuid) {
        log.debug("Get Fido2RegistrationEntry with device uuid:{}", uuid);

        if (StringUtils.isBlank(uuid)) {
            throw new InvalidAttributeException("Device uuid is null!");
        }

        Fido2RegistrationEntry fido2RegistrationEntry = null;
        try {
            //Filter uuidFilter = Filter.createEqualityFilter("uuid", uuid);
            Filter filter = Filter.createEqualityFilter("jansDeviceData", uuid);
            log.info("Find device filter filter:{}", filter);

            List<Fido2RegistrationEntry> fido2List = persistenceEntryManager
                    .findEntries(getDnFido2RegistrationEntry(null), Fido2RegistrationEntry.class, filter);
            log.info("Fetched Fido2RegistrationEntry by uuid:{} are fido2List:{}", uuid, fido2List);

            if (fido2List != null && !fido2List.isEmpty()) {
                fido2RegistrationEntry = fido2List.get(0);
            }

            log.info("Fido2RegistrationEntry by uuid:{} are fido2RegistrationEntry:{}", uuid, fido2RegistrationEntry);

        } catch (Exception e) {
            log.error("Error while finding Fido2RegistrationEntry with device uuid:{} is {}" + uuid, e);
        }
        return fido2RegistrationEntry;
    }

    public String getDnFido2RegistrationEntry(String id, String userId) {
        String orgDn = organizationService.getDnForOrganization();
        if (!StringHelper.isEmpty(userId) && StringHelper.isEmpty(id)) {
            return String.format("ou=fido2_register,inum=%s,ou=people,%s", userId, orgDn);
        }
        if (!StringHelper.isEmpty(id) && !StringHelper.isEmpty(userId)) {
            return String.format("id=%s,ou=fido2_register,inum=%s,ou=people,%s", id, userId, orgDn);
        }
        return String.format("ou=people,%s", orgDn);
    }

    public String getDnFido2RegistrationEntry(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=fido2_register,%s", orgDn);
        }
        return String.format("inum=%s,ou=fido2_register,%s", inum, orgDn);
    }

}
