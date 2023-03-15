/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.persist;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.as.common.model.common.User;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.shared.UserService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Every registration is persisted under Person Entry
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class RegistrationPersistenceService extends io.jans.as.common.service.common.fido2.RegistrationPersistenceService {

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
        Fido2RegistrationEntry registrationEntry = buildFido2RegistrationEntry(registrationData, false);

        save(registrationEntry);
    }

    public Fido2RegistrationEntry buildFido2RegistrationEntry(Fido2RegistrationData registrationData, boolean oneStep) {
		String userName = registrationData.getUsername();

		String userInum = null;
    	if (!oneStep) {
	        User user = userService.getUser(userName, "inum");
	        if (user == null) {
	            if (appConfiguration.getFido2Configuration().isUserAutoEnrollment()) {
	                user = userService.addDefaultUser(userName);
	            } else {
	                throw new Fido2RuntimeException("Auto user enrollment was disabled. User not exists!");
	            }
	        }
	        userInum = userService.getUserInum(user);
    	}

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String id = UUID.randomUUID().toString();
        final String challenge = registrationData.getChallenge();

        String dn = oneStep ? getDnForRegistrationEntry(null, id) : getDnForRegistrationEntry(userInum, id);
        Fido2RegistrationEntry registrationEntry = new Fido2RegistrationEntry(dn, id, now, userInum, registrationData, challenge);
        registrationEntry.setRegistrationStatus(registrationData.getStatus());
        if (StringUtils.isNotEmpty(challenge)) {
        	registrationEntry.setChallengeHash(String.valueOf(getChallengeHashCode(challenge)));
        }
        registrationEntry.setRpId(registrationData.getApplicationId());

        registrationData.setCreatedDate(now);
        registrationData.setCreatedBy(userName);

        return registrationEntry;
	}

    public Optional<Fido2RegistrationEntry> findByPublicKeyId(String userName, String publicKeyId, String rpId) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);
    	if (StringHelper.isNotEmpty(userName)) {
            String userInum = userService.getUserInum(userName);
            if (userInum == null) {
                return Optional.empty();
            }
            baseDn = getBaseDnForFido2RegistrationEntries(userInum);
    	}

        Filter filter;
        Filter publicKeyIdFilter = Filter.createEqualityFilter("jansPublicKeyId", publicKeyId);
        Filter publicKeyIdHashFilter = Filter.createEqualityFilter("jansPublicKeyIdHash", getPublicKeyIdHash(publicKeyId));
        if (StringHelper.isNotEmpty(rpId)) {
        	Filter appIdFilter = Filter.createEqualityFilter("jansApp", rpId);
            filter = Filter.createANDFilter(publicKeyIdFilter, publicKeyIdHashFilter, appIdFilter);
        } else {
            filter = Filter.createANDFilter(publicKeyIdFilter, publicKeyIdHashFilter);
        }
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, filter);
        
        if (fido2RegistrationnEntries.size() > 0) {
            return Optional.of(fido2RegistrationnEntries.get(0));
        }

        return Optional.empty();
    }

    public Optional<Fido2RegistrationEntry> findByPublicKeyId(String publicKeyId, String rpId) {
    	return findByPublicKeyId(null, publicKeyId, rpId);
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
    
    public List<Fido2RegistrationEntry> findByChallenge(String challenge, boolean oneStep) {
        String baseDn = oneStep ? getDnForRegistrationEntry(null, null) : getBaseDnForFido2RegistrationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("jansCodeChallenge", challenge);
        Filter codeChallengHashCodeFilter = Filter.createEqualityFilter("jansCodeChallengeHash", String.valueOf(getChallengeHashCode(challenge)));
        Filter filter = Filter.createANDFilter(codeChallengFilter, codeChallengHashCodeFilter);

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, filter);

        return fido2RegistrationnEntries;
    }

    public String getBasedPeopleDn() {
    	return staticConfiguration.getBaseDn().getPeople();
    }

    public int getChallengeHashCode(String challenge) {
        int hash = 0;
        byte[] challengeBytes = challenge.getBytes(StandardCharsets.UTF_8);
        for (int j = 0; j < challengeBytes.length; j++) {
            hash += challengeBytes[j]*j;
        }

        return hash;
    }

    /*
     * Generate non unique hash code to split keyHandle among small cluster with 10-20 elements
     *
     * This hash code will be used to generate small LDAP indexes
     */
    public int getPublicKeyIdHash(String publicKeyId) {
        byte[] publicKeyIdBytes = publicKeyId.getBytes(StandardCharsets.UTF_8);
		int hash = 0;
		for (int j = 0; j < publicKeyIdBytes.length; j++) {
			hash += publicKeyIdBytes[j]*j;
		}

		return hash;
    }
    
    @Override
    public String getDnForRegistrationEntry(String userInum, String jsId) {
    	String baseDn;
    	if (StringHelper.isEmpty(userInum)) {
    		baseDn = staticConfiguration.getBaseDn().getFido2Attestation();
    	} else {
	        // Build DN string for Fido2 registration entry
	        baseDn = getBaseDnForFido2RegistrationEntries(userInum);
    	}
        if (StringHelper.isEmpty(jsId)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", jsId, baseDn);
    }

    public String getUserInum(String userName)
    {
    	return userService.getUserInum(userName);
    }
}
