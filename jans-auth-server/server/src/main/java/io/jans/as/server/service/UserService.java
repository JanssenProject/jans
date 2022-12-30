/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.util.AttributeConstants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.search.filter.Filter;
import io.jans.service.net.NetworkService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Provides operations with users.
 *
 * @author Javier Rojas Blum
 * @version @version August 20, 2019
 */
@ApplicationScoped
public class UserService extends io.jans.as.common.service.common.UserService {

    public static final String[] USER_OBJECT_CLASSES = new String[]{AttributeConstants.JANS_PERSON};

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private NetworkService networkService;

    @Override
    public List<String> getPersonCustomObjectClassList() {
        if (LdapEntryManagerFactory.PERSISTENCE_TYPE.equals(persistenceEntryManager.getPersistenceType(getPeopleBaseDn()))) {
            return appConfiguration.getPersonCustomObjectClassList();
        }

        return null;
    }

    @Override
    public String getPeopleBaseDn() {
        return staticConfiguration.getBaseDn().getPeople();
    }

    public long countFido2RegisteredDevices(String username) {
        String userInum = getUserInum(username);
        if (userInum == null) {
            return 0;
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
            if (!persistenceEntryManager.contains(baseDn, SimpleBranch.class)) {
                return 0;
            }
        }

        Filter userInumFilter = Filter.createEqualityFilter("personInum", userInum);
        Filter registeredFilter = Filter.createEqualityFilter("jansStatus", "registered");
        Filter filter = Filter.createANDFilter(userInumFilter, registeredFilter);

        return persistenceEntryManager.countEntries(baseDn, Fido2RegistrationEntry.class, filter);
    }

    public String getBaseDnForFido2RegistrationEntries(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_register,inum=1234,ou=people,o=jans"

        return String.format("ou=fido2_register,%s", userBaseDn);
    }

    public String getBaseDnForFidoDevices(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido,inum=1234,ou=people,o=jans"

        return String.format("ou=fido,%s", userBaseDn);
    }

}
