/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidAttributeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;

@ApplicationScoped
public class RealmService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    KeycloakService keycloakService;

    public RealmRepresentation getRealmByName(String realm) {
        logger.info("Get RealmResource for realm:{})", realm);
        if (StringUtils.isBlank(realm)) {

            new InvalidAttributeException("Realm name is null");

        }
        return keycloakService.getRealmByName(realm);
    }

    public List<RealmRepresentation> getAllRealmDetails() {
        return keycloakService.getAllRealms();
    }

    public RealmRepresentation createNewRealm(RealmRepresentation realmRepresentation) {
        logger.info("Create new realm - realmRepresentation:{})", realmRepresentation);
        if (realmRepresentation == null) {
            new InvalidAttributeException("RealmRepresentation is null");
        }
        return keycloakService.createNewRealm(realmRepresentation);
    }

    public RealmRepresentation updateRealm(RealmRepresentation realmRepresentation) {
        logger.info("Update a realm - realmRepresentation:{})", realmRepresentation);
        if (realmRepresentation == null) {
            new InvalidAttributeException("RealmRepresentation is null");
        }
        return keycloakService.updateRealm(realmRepresentation);
    }

    public void deleteRealm(String realmName) {
        logger.info("Delete realm:{})", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }
        keycloakService.deleteRealm(realmName);

        return;
    }
}
