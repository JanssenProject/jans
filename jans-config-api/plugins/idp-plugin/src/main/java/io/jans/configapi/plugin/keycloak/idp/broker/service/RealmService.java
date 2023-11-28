/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.model.Realm;
import io.jans.configapi.plugin.keycloak.idp.broker.mapper.RealmMapper;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;
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
    IdpConfigService idpConfigService;

    @Inject
    KeycloakService keycloakService;

    public String getRealmDn() {
        return idpConfigService.getRealmDn();
    }

    public Realm getRealmByDn(String dn) {
        if (StringHelper.isNotEmpty(dn)) {
            try {
                return persistenceEntryManager.find(Realm.class, dn);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
        return null;
    }

    public List<Realm> getAllRealmDetails() {
        return getAllRealms(0);
    }

    public List<Realm> getAllRealmDetails(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForRealm(null), Realm.class, null, sizeLimit);
    }

    public Realm getRealmByInum(String inum) {
        Realm result = null;
        try {
            result = persistenceEntryManager.find(Realm.class, getDnForRealm(inum));
        } catch (Exception ex) {
            log.error("Failed to load Realm entry", ex);
        }
        return result;
    }

    public Realm getRealmByName(String name) {
        logger.info("Get RealmResource for name:{})", name);
        if (StringUtils.isBlank(name)) {

            new InvalidAttributeException("Realm name is null");

        }

        Filter nameFilter = Filter.createEqualityFilter("NAME", name);
        log.error("Search Realm with displayNameFilter:{}", nameFilter);
        return persistenceEntryManager.findEntries(getDnForRealm(null), Realm.class, nameFilter);
    }

    public List<Realm> searchRealm(String pattern, int sizeLimit) {

        log.error("Search Realm with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter nameFilter = Filter.createSubstringFilter("NAME", null, targetArray, null);
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(nameFilter, displayNameFilter, descriptionFilter, inumFilter);

        log.error("Search Realm with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForRealm(null), Realm.class, searchFilter, sizeLimit);
    }

    public Realm createNewRealm(Realm realm) {
        logger.error("Create new realm - realm:{})", realm);
        if (realm == null) {
            throw new InvalidAttributeException("Realm object is null");
        }
        String inum = generateInumForRealm();
        realm.setInum(inum);
        realm.setDn(getDnForRealm(inum));

        // Create Realm in DB
        persistenceEntryManager.persist(realm);
        realm = getRealmByInum(realm.getInum());

        // Add in KC
        if (idpConfigService.isIdpEnabled()) {
            RealmRepresentation realmRepresentation = convertToRealmRepresentation(realm);
            realmRepresentation = keycloakService.createNewRealm(realmRepresentation);
            realm = this.convertToRealm(realmRepresentation);
        }
        logger.error("Create new realm - realm:{})", realm);
        return realm;
    }

    public RealmRepresentation updateRealm(Realm realm) {
        logger.error("Update a realm - realm:{})", realm);
        if (realm == null) {
            new InvalidAttributeException("Realm object is null");
        }

        // Update Realm in DB
        persistenceEntryManager.merge(realm);

        // Update in KC
        if (idpConfigService.isIdpEnabled()) {
            RealmRepresentation realmRepresentation = convertToRealmRepresentation(realm);
            keycloakService.updateRealm(realmRepresentation);
            realm = this.convertToRealm(realmRepresentation);
        }

        logger.error("Update a realm - realm:{})", realm);
        return realm;
    }

    public void deleteRealm(String realmName) {
        logger.info("Delete realm:{})", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }

        // Delete from KC
        keycloakService.deleteRealm(realmName);

        // Delete from Jans DB
        persistenceEntryManager.removeRecursively(realmName.getDn(), Realm.class);
        return;
    }

    public boolean containsRealm(String dn) {
        return persistenceEntryManager.contains(dn, Realm.class);
    }

    public String generateInumForRealm() {
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForRealm(newInum);
        } while (this.containsRealm(newDn));

        return newInum;
    }

    public String getDnForRealm(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=realm,o=jans,%s", orgDn);
        }
        return String.format("inum=%s,ou=realm,o=jans,%s", inum, orgDn);
    }

    private Realm convertToRealm(RealmRepresentation realmRepresentation) {
        log.error("realmRepresentation:{}", realmRepresentation);
        Realm realm = null;
        if (realmRepresentation == null) {
            return realm;
        }
        realm = RealmMapper.kcRealmRepresentationToRealm(realmRepresentation);
        log.error("converted - realm:{}", realm);

        return realm;
    }

    private RealmRepresentation convertToRealmRepresentation(Realm realm) {
        log.error("realm:{}", realm);
        RealmRepresentation realmRepresentation = null;
        if (realm == null) {
            return realmRepresentation;
        }
        realmRepresentation = RealmMapper.realmToKCRealmRepresentation(realm);
        log.error("converted realmRepresentation:{}", realmRepresentation);

        log.error(
                "convert Realm data realmRepresentation.getId():{}, realmRepresentation.getRealm():{}, realmRepresentation.getDisplayName():{},realmRepresentation.isEnabled():{}",
                realmRepresentation.getId(), realmRepresentation.getRealm(), realmRepresentation.getDisplayName(),
                realmRepresentation.isEnabled());

        return realmRepresentation;
    }

}
