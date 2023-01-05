/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.persist;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.shared.UserService;
import io.jans.as.common.model.common.User;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.ProcessBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

/**
 * Every registration is persisted under Person Entry
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
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
    private PersistenceEntryManager persistenceEntryManager;

    public void save(Fido2RegistrationData registrationData) {
        Fido2RegistrationEntry registrationEntry = buildFido2RegistrationEntry(registrationData);

        persistenceEntryManager.persist(registrationEntry);
    }

    public Fido2RegistrationEntry buildFido2RegistrationEntry(Fido2RegistrationData registrationData) {
		String userName = registrationData.getUsername();
        
        User user = userService.getUser(userName, "inum");
        if (user == null) {
            if (appConfiguration.getFido2Configuration().isUserAutoEnrollment()) {
                user = userService.addDefaultUser(userName);
            } else {
                throw new Fido2RuntimeException("Auto user enrollment was disabled. User not exists!");
            }
        }
        String userInum = userService.getUserInum(user);

        prepareBranch(userInum);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String id = UUID.randomUUID().toString();
        final String challenge = registrationData.getChallenge();

        String dn = getDnForRegistrationEntry(userInum, id);
        Fido2RegistrationEntry registrationEntry = new Fido2RegistrationEntry(dn, id, now, userInum, registrationData, challenge);
        registrationEntry.setRegistrationStatus(registrationData.getStatus());
        if (StringUtils.isNotEmpty(challenge)) {
        	registrationEntry.setChallangeHash(String.valueOf(getChallengeHashCode(challenge)));
        }
        
        registrationData.setCreatedDate(now);
        registrationData.setCreatedBy(userName);

        return registrationEntry;
	}

    public void update(Fido2RegistrationEntry registrationEntry) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();
        registrationData.setUpdatedDate(now);
        registrationData.setUpdatedBy(registrationData.getUsername());
        
        registrationEntry.setPublicKeyId(registrationData.getPublicKeyId());
        registrationEntry.setRegistrationStatus(registrationData.getStatus());

        persistenceEntryManager.merge(registrationEntry);
    }

    public void addBranch(final String baseDn) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("fido2_register");
        branch.setDn(baseDn);

        persistenceEntryManager.persist(branch);
    }

    public boolean containsBranch(final String baseDn) {
        return persistenceEntryManager.contains(baseDn, SimpleBranch.class);
    }

    public void prepareBranch(final String userInum) {
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (!persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	return;
        }

        // Create Fido2 base branch for registration entries if needed
        if (!containsBranch(baseDn)) {
            addBranch(baseDn);
        }
    }

    public Optional<Fido2RegistrationEntry> findByPublicKeyId(String publicKeyId) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter publicKeyIdFilter = Filter.createEqualityFilter("jansPublicKeyId", publicKeyId);
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, publicKeyIdFilter);
        
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
        if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	if (!containsBranch(baseDn)) {
                return Collections.emptyList();
        	}
        }

        Filter userFilter = Filter.createEqualityFilter("personInum", userInum);

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, userFilter);

        return fido2RegistrationnEntries;
    }

    public List<Fido2RegistrationEntry> findAllRegisteredByUsername(String username) {
        String userInum = userService.getUserInum(username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	if (!containsBranch(baseDn)) {
                return Collections.emptyList();
        	}
        }

        Filter userInumFilter = Filter.createEqualityFilter("personInum", userInum);
        Filter registeredFilter = Filter.createEqualityFilter("jansStatus", Fido2RegistrationStatus.registered.getValue());
        Filter filter = Filter.createANDFilter(userInumFilter, registeredFilter);

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, filter);

        return fido2RegistrationnEntries;
    }

    public List<Fido2RegistrationEntry> findByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("jansCodeChallenge", challenge);
        Filter codeChallengHashCodeFilter = Filter.createEqualityFilter("jansCodeChallengeHash", String.valueOf(getChallengeHashCode(challenge)));
        Filter filter = Filter.createANDFilter(codeChallengFilter, codeChallengHashCodeFilter);

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, filter);

        return fido2RegistrationnEntries;
    }

    public String getDnForRegistrationEntry(String userInum, String jsId) {
        // Build DN string for Fido2 registration entry
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (StringHelper.isEmpty(jsId)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", jsId, baseDn);
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


    public void cleanup(Date now, int batchSize) {
        // Cleaning expired entries
        BatchOperation<Fido2RegistrationEntry> cleanerRegistrationBatchService = new ProcessBatchOperation<Fido2RegistrationEntry>() {
            @Override
            public void performAction(List<Fido2RegistrationEntry> entries) {
                for (Fido2RegistrationEntry p : entries) {
                    log.debug("Removing Fido2 registration entry: {}, Creation date: {}", p.getChallange(), p.getCreationDate());
                    try {
                        persistenceEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        String baseDn = getDnForUser(null);
        persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, getExpiredRegistrationFilter(baseDn), SearchScope.SUB, new String[] {"jansCodeChallenge", "creationDate"}, cleanerRegistrationBatchService, 0, 0, batchSize);

        String branchDn = getDnForUser(null);
        if (persistenceEntryManager.hasBranchesSupport(branchDn)) {
	        // Cleaning empty branches
	        BatchOperation<SimpleBranch> cleanerBranchBatchService = new ProcessBatchOperation<SimpleBranch>() {
	            @Override
	            public void performAction(List<SimpleBranch> entries) {
	                for (SimpleBranch p : entries) {
	                    try {
	                        persistenceEntryManager.remove(p);
	                    } catch (Exception e) {
	                        log.error("Failed to remove entry", e);
	                    }
	                }
	            }
	        };
	        persistenceEntryManager.findEntries(branchDn, SimpleBranch.class, getEmptyRegistrationBranchFilter(), SearchScope.SUB, new String[] {"ou"}, cleanerBranchBatchService, 0, 0, batchSize);
        }
    }

    private Filter getExpiredRegistrationFilter(String baseDn) {
        int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        unfinishedRequestExpiration = unfinishedRequestExpiration == 0 ? 120 : unfinishedRequestExpiration;

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -unfinishedRequestExpiration);
        final Date unfinishedRequestExpirationDate = calendar.getTime();

        // Build unfinished request expiration filter
        Filter registrationStatusFilter = Filter.createNOTFilter(Filter.createEqualityFilter("jansStatus", Fido2RegistrationStatus.registered.getValue()));
        Filter compomisedStatusFilter = Filter.createNOTFilter(Filter.createEqualityFilter("jansStatus", Fido2RegistrationStatus.compromised.getValue()));

        Filter exirationDateFilter = Filter.createLessOrEqualFilter("creationDate",
                persistenceEntryManager.encodeTime(baseDn, unfinishedRequestExpirationDate));
        
        Filter unfinishedRequestFilter = Filter.createANDFilter(registrationStatusFilter, compomisedStatusFilter, exirationDateFilter);

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
