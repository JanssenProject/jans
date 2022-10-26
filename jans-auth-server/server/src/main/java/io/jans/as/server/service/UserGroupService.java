/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.server.model.ldap.UserGroup;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Arrays;
import java.util.List;

/**
 * It's utility service which applications uses in custom authentication scripts
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan Date: 04/11/2014
 * @version 0.9, 27/07/2012
 */
@Stateless
@Named
public class UserGroupService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    public UserGroup loadGroup(String groupDN) {
        try {
            if (StringUtils.isNotBlank(groupDN)) {
                return ldapEntryManager.find(UserGroup.class, groupDN);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
        return null;
    }

    public boolean isUserInGroup(String groupDN, String userDN) {
        final UserGroup group = loadGroup(groupDN);
        if (group != null) {
            final String[] member = group.getMember();
            if (member != null) {
                return Arrays.asList(member).contains(userDN);
            }
        }
        return false;
    }

    public boolean isUserInGroupOrMember(String groupDn, String personDn) {
        Filter ownerFilter = Filter.createEqualityFilter("owner", personDn);
        Filter memberFilter = Filter.createEqualityFilter("member", personDn);
        Filter searchFilter = Filter.createORFilter(ownerFilter, memberFilter);

        try {
            return !ldapEntryManager.findEntries(groupDn, UserGroup.class, searchFilter, 1).isEmpty();
        } catch (EntryPersistenceException ex) {
            log.error("Failed to determine if person '{}' memeber or owner of group '{}'", personDn, groupDn, ex);
        }

        return false;
    }

    public boolean isInAnyGroup(String[] groupDNs, String userDN) {
        return groupDNs != null && isInAnyGroup(Arrays.asList(groupDNs), userDN);
    }

    public boolean isInAnyGroup(List<String> groupDNs, String userDN) {
        if (groupDNs != null && !groupDNs.isEmpty() && userDN != null && !userDN.isEmpty()) {
            for (String groupDN : groupDNs) {
                if (isUserInGroup(groupDN, userDN)) {
                    return true;
                }
            }
        }
        return false;
    }

}
