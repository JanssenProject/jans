/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class Fido2RegistrationService {

    private static final String JANS_STATUS = "jansStatus";
    private static final String PERSON_INUM = "personInum";

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

    public Fido2RegistrationEntry getFido2RegistrationEntryById(String id) {
        Fido2RegistrationEntry fido2RegistrationEntry = null;
        try {
            fido2RegistrationEntry = persistenceEntryManager.find(Fido2RegistrationEntry.class,
                    getDnFido2RegistrationEntry(id));
        } catch (Exception ex) {
            log.error("Failed to get fido2RegistrationEntry identified by id:{" + id + "}", ex);
        }
        return fido2RegistrationEntry;
    }

    public PagedResult<Fido2RegistrationEntry> searchFido2Registration(SearchRequest searchRequest) {
        log.info("**** Search Fido2Registration with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                log.info(" **** Search Fido2Registration with assertionValue:{}", assertionValue);

                String[] targetArray = new String[] { assertionValue };

                Filter displayNameFilter = Filter.createSubstringFilter("displayName", null, targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter("jansRegistrationData", null, targetArray,
                        null);
                Filter statusFilter = Filter.createSubstringFilter(JANS_STATUS, null, targetArray, null);
                Filter notificationConfFilter = Filter.createSubstringFilter("jansDeviceNotificationConf", null,
                        targetArray, null);
                Filter deviceDataFilter = Filter.createSubstringFilter("jansDeviceData", null, targetArray, null);
                Filter personInumFilter = Filter.createSubstringFilter(PERSON_INUM, null, targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter("jansId", null, targetArray, null);

                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, statusFilter,
                        notificationConfFilter, deviceDataFilter, personInumFilter, inumFilter));

            }
            searchFilter = Filter.createORFilter(filters);
        }

        log.debug("Fido2Registration pattern searchFilter:{}", searchFilter);

        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldFilterData() != null && !searchRequest.getFieldFilterData().isEmpty()) {
            fieldValueFilters = DataUtil.createFilter(searchRequest.getFieldFilterData(),
                    getDnFido2RegistrationEntry(null), persistenceEntryManager);
        }

        searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                Filter.createANDFilter(fieldValueFilters));

        log.info(" Final - Fido2Registration searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnFido2RegistrationEntry(null), Fido2RegistrationEntry.class,
                searchFilter, null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public List<Fido2RegistrationEntry> findAllRegisteredByUsername(String username) {
        if (log.isInfoEnabled()) {
            log.info("Find Fido2 Registered by username:{}", escapeLog(username));
        }

        String userInum = userFido2Srv.getUserInum(username);
        log.info("Find Fido2 Registered by userInum:{}", userInum);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        log.info("Find Fido2 Registered by baseDn:{}", baseDn);
        if (persistenceEntryManager.hasBranchesSupport(baseDn) && !containsBranch(baseDn)) {
            return Collections.emptyList();
        }

        Filter searchFilter = Filter.createANDFilter(Filter.createEqualityFilter(PERSON_INUM, userInum),
                Filter.createEqualityFilter(JANS_STATUS, Fido2RegistrationStatus.registered.getValue()));

        log.info("Fido2 Registered by searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnFido2RegistrationEntry(baseDn), Fido2RegistrationEntry.class,
                searchFilter);
    }

    public PagedResult<Fido2RegistrationEntry> getFido2RegisteredByUsername(String username) {
        if (log.isInfoEnabled()) {
            log.info("Fetch Fido2 Registered by username:{}", escapeLog(username));
        }
        PagedResult<Fido2RegistrationEntry> fido2RegistrationEntry = null;
        String userInum = userFido2Srv.getUserInum(username);
        log.info("Find Fido2 Registered by userInum:{}", userInum);
        if (userInum == null) {
            return fido2RegistrationEntry;
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        log.info("Find Fido2 Registered by baseDn:{}", baseDn);
        if (persistenceEntryManager.hasBranchesSupport(baseDn) && !containsBranch(baseDn)) {
            return fido2RegistrationEntry;
        }

        Filter searchFilter = Filter.createANDFilter(Filter.createEqualityFilter(PERSON_INUM, userInum),
                Filter.createEqualityFilter(JANS_STATUS, Fido2RegistrationStatus.registered.getValue()));

        log.info("Fido2 Registered by searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnFido2RegistrationEntry(baseDn),
                Fido2RegistrationEntry.class, searchFilter, null, Constants.JANSID, SortOrder.ASCENDING,
                Integer.parseInt(ApiConstants.DEFAULT_LIST_START_INDEX),
                Integer.parseInt(ApiConstants.DEFAULT_LIST_SIZE), getRecordMaxCount());

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

    public void removeFido2RegistrationEntry(String id) {
        if (log.isInfoEnabled()) {
            log.info("Remove Fido2RegistrationEntry request for device with id:{}", escapeLog(id));
        }

        if (StringUtils.isBlank(id)) {
            throw new InvalidAttributeException("Fido2RegistrationEntry id is null!");
        }

        Fido2RegistrationEntry fido2RegistrationEntry = this.getFido2RegistrationEntryById(id);
        log.debug("Fido2RegistrationEntry identified by id:{} is:{}", id, fido2RegistrationEntry);
        if (fido2RegistrationEntry == null) {
            throw new InvalidAttributeException("No Fido2RegistrationEntry found with id:{" + id + "}");
        }

        // delete entry
        persistenceEntryManager.removeRecursively(fido2RegistrationEntry.getBaseDn(), Fido2RegistrationEntry.class);

        // verify post delete
        fido2RegistrationEntry = this.getFido2RegistrationEntryByDeviceId(id);
        if (fido2RegistrationEntry != null) {
            throw new WebApplicationException(
                    "Fido2RegistrationEntry device with id:{" + id + "} could not be deleted!");
        }
        log.info("Successfully deleted Fido2RegistrationEntry device with id:{}", id);
    }

    public Fido2RegistrationEntry getFido2RegistrationEntryByDeviceId(String uuid) {
        log.info("Get Fido2RegistrationEntry with device uuid:{}", uuid);

        if (StringUtils.isBlank(uuid)) {
            throw new InvalidAttributeException("Device uuid is null!");
        }

        Fido2RegistrationEntry fido2RegistrationEntry = null;
        try {
            String[] targetArray = new String[] { uuid };
            Filter filter = Filter.createSubstringFilter("jansDeviceData", null, targetArray, null);
            log.debug("Find device filter:{}", filter);

            List<Fido2RegistrationEntry> fido2List = persistenceEntryManager
                    .findEntries(getDnFido2RegistrationEntry(null), Fido2RegistrationEntry.class, filter);
            log.debug("Fetched Fido2RegistrationEntry by uuid:{} are fido2List:{}", uuid, fido2List);

            if (fido2List != null && !fido2List.isEmpty()) {
                fido2RegistrationEntry = fido2List.get(0);
            }

            log.info("Fido2RegistrationEntry by uuid:{} are fido2RegistrationEntry:{}", uuid, fido2RegistrationEntry);

        } catch (Exception e) {
            log.error("Error while finding Fido2RegistrationEntry with device uuid:{} is {}" + uuid, e);
        }
        return fido2RegistrationEntry;
    }

    public String getDnFido2RegistrationEntry(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=fido2_register,%s", orgDn);
        }
        return String.format("jansId=%s,ou=fido2_register,%s", inum, orgDn);
    }

}
