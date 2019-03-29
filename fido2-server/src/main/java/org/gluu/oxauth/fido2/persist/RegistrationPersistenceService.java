/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.persist;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationStatus;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.service.UserService;

@ApplicationScoped
public class RegistrationPersistenceService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private UserService userService;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    public Optional<Fido2RegistrationEntry> findByPublicKeyId(String publicKeyId) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter publicKeyIdFilter = Filter.createEqualityFilter("oxPublicKeyId", publicKeyId);
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, publicKeyIdFilter);
        
        if (fido2RegistrationnEntries.size() > 0) {
            return Optional.of(fido2RegistrationnEntries.get(0));
        }

        return Optional.empty();
    }

    public List<Fido2RegistrationEntry> findAllByUsername(String username) {
        String userInum = userService.getUserInum(username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (containsBranch(baseDn)) {
            List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, null);
    
            return fido2RegistrationnEntries;
        }
        
        return Collections.emptyList();
    }

    public List<Fido2RegistrationEntry> findAllRegisteredByUsername(String username) {
        String userInum = userService.getUserInum(username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (containsBranch(baseDn)) {
            Filter registeredFilter = Filter.createEqualityFilter("oxStatus", Fido2RegistrationStatus.registered.getValue());
            List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, registeredFilter);
    
            return fido2RegistrationnEntries;
        }
        
        return Collections.emptyList();
    }

    public List<Fido2RegistrationEntry> findAllByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("oxCodeChallenge", challenge);
        Filter codeChallengHashCodeFilter = Filter.createEqualityFilter("oxCodeChallengeHash", String.valueOf(getChallengeHashCode(challenge)));
        Filter filter = Filter.createANDFilter(codeChallengFilter, codeChallengHashCodeFilter);

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, filter);

        return fido2RegistrationnEntries;
    }

    public void save(Fido2RegistrationData registrationData) {
        String userName = registrationData.getUsername();
        
        User user = userService.getUser(userName, "inum");
        if (user == null) {
            if (appConfiguration.getFido2Configuration().isUserAutoEnrollment()) {
                user = userService.addDefaultUser(userName);
            } else {
                throw new Fido2RPRuntimeException("Auto user enrollment was disabled. User not exists!");
            }
        }
        String userInum = userService.getUserInum(user);

        prepareBranch(userInum);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String id = UUID.randomUUID().toString();
        final String challenge = registrationData.getChallenge();

        String dn = getDnForRegistrationEntry(userInum, id);
        Fido2RegistrationEntry registrationEntry = new Fido2RegistrationEntry(dn, id, now, userInum, null, registrationData, challenge);
        registrationEntry.setRegistrationStatus(registrationData.getStatus());
        registrationEntry.setChallangeHash(String.valueOf(getChallengeHashCode(challenge)));
        
        registrationData.setCreatedDate(now);
        registrationData.setCreatedBy(userName);

        ldapEntryManager.persist(registrationEntry);
    }

    public void update(Fido2RegistrationEntry registrationEntry) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();
        registrationData.setUpdatedDate(now);
        registrationData.setUpdatedBy(registrationData.getUsername());
        
        registrationEntry.setPublicKeyId(registrationData.getPublicKeyId());
        registrationEntry.setRegistrationStatus(registrationData.getStatus());

        ldapEntryManager.merge(registrationEntry);
    }

    public void addBranch(final String baseDn) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("fido2_register");
        branch.setDn(baseDn);

        ldapEntryManager.persist(branch);
    }

    public boolean containsBranch(final String baseDn) {
        return ldapEntryManager.contains(SimpleBranch.class, baseDn);
    }

    public void prepareBranch(final String userInum) {
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        // Create Fido2 base branch for registration entries if needed
        if (!containsBranch(baseDn)) {
            addBranch(baseDn);
        }
    }

    public String getDnForRegistrationEntry(String userInum, String oxId) {
        // Build DN string for Fido2 registration entry
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (StringHelper.isEmpty(oxId)) {
            return baseDn;
        }
        return String.format("oxId=%s,%s", oxId, baseDn);
    }

    public String getBaseDnForFido2RegistrationEntries(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_register,inum=1234,ou=people,o=gluu"
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


    public void cleanup(Date now, int batchSize) {
        // Cleaning expired entries
        BatchOperation<Fido2RegistrationEntry> cleanerRegistrationBatchService = new ProcessBatchOperation<Fido2RegistrationEntry>() {
            @Override
            public void performAction(List<Fido2RegistrationEntry> entries) {
                for (Fido2RegistrationEntry p : entries) {
                    log.debug("Removing Fido2 registration entry: {}, Creation date: {}", p.getChallange(), p.getCreationDate());
                    try {
                        ldapEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        ldapEntryManager.findEntries(getDnForUser(null), Fido2RegistrationEntry.class, getExpiredRegistrationFilter(), SearchScope.SUB, new String[] {"oxCodeChallenge", "creationDate"}, cleanerRegistrationBatchService, 0, 0, batchSize);

        // Cleaning empty branches
        BatchOperation<SimpleBranch> cleanerBranchBatchService = new ProcessBatchOperation<SimpleBranch>() {
            @Override
            public void performAction(List<SimpleBranch> entries) {
                for (SimpleBranch p : entries) {
                    try {
                        ldapEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        ldapEntryManager.findEntries(getDnForUser(null), SimpleBranch.class, getEmptyRegistrationBranchFilter(), SearchScope.SUB, new String[] {"ou"}, cleanerBranchBatchService, 0, 0, batchSize);
    }

    private Filter getExpiredRegistrationFilter() {
        int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        unfinishedRequestExpiration = unfinishedRequestExpiration == 0 ? 120 : unfinishedRequestExpiration;

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -unfinishedRequestExpiration);
        final Date unfinishedRequestExpirationDate = calendar.getTime();

        // Build unfinished request expiration filter
        Filter registrationStatusFilter = Filter.createNOTFilter(Filter.createEqualityFilter("oxStatus", Fido2RegistrationStatus.registered.getValue()));

        Filter exirationDateFilter = Filter.createLessOrEqualFilter("creationDate",
                ldapEntryManager.encodeTime(unfinishedRequestExpirationDate));
        
        Filter unfinishedRequestFilter = Filter.createANDFilter(registrationStatusFilter, exirationDateFilter);

        return unfinishedRequestFilter;
    }

    private Filter getEmptyRegistrationBranchFilter() {
        return Filter.createANDFilter(Filter.createEqualityFilter("ou", "fido2_register"), Filter.createORFilter(
                Filter.createEqualityFilter("numsubordinates", "0"), Filter.createEqualityFilter("hasSubordinates", "FALSE")));
    }

    public int getChallengeHashCode(String challenge) {
        int hash = 0;
        byte[] challengeBytes = challenge.getBytes();
        for (int j = 0; j < challengeBytes.length; j++) {
            hash += challengeBytes[j]*j;
        }

        return hash;
    }

}
