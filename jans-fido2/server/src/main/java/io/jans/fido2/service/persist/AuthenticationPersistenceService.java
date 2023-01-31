/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.persist;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.as.common.model.common.User;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.shared.UserService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.ProcessBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Every authentication is persisted under Person Entry
 * 
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class AuthenticationPersistenceService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private ChallengeGenerator challengeGenerator;

    @Inject
    private UserService userService;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public void save(Fido2AuthenticationData authenticationData) {
        Fido2AuthenticationEntry authenticationEntity = buildFido2AuthenticationEntry(authenticationData, false);

        save(authenticationEntity);
    }

    public void save(Fido2AuthenticationEntry authenticationEntity) {
        prepareBranch(authenticationEntity.getUserInum());

        persistenceEntryManager.persist(authenticationEntity);
    }

    public Fido2AuthenticationEntry buildFido2AuthenticationEntry(Fido2AuthenticationData authenticationData, boolean oneStep) {
		String userName = authenticationData.getUsername();
        
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
        final String challenge = authenticationData.getChallenge();

        String dn = oneStep ? getDnForAuthenticationEntry(null, id) : getDnForAuthenticationEntry(userInum, id);
        Fido2AuthenticationEntry authenticationEntity = new Fido2AuthenticationEntry(dn, authenticationData.getId(), now, userInum, authenticationData);
        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());
        if (StringUtils.isNotEmpty(challenge)) {
        	authenticationEntity.setChallengeHash(String.valueOf(challengeGenerator.getChallengeHashCode(challenge)));
        }
        authenticationEntity.setRpId(authenticationData.getApplicationId());

        authenticationData.setCreatedDate(now);
        authenticationData.setCreatedBy(userName);

        return authenticationEntity;
	}

    public void update(Fido2AuthenticationEntry authenticationEntity) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();
        authenticationData.setUpdatedDate(now);
        authenticationData.setUpdatedBy(authenticationData.getUsername());

        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());

        persistenceEntryManager.merge(authenticationEntity);
    }

    public void addBranch(final String baseDn) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("fido2_auth");
        branch.setDn(baseDn);

        persistenceEntryManager.persist(branch);
    }

    public boolean containsBranch(final String baseDn) {
        return persistenceEntryManager.contains(baseDn, SimpleBranch.class);
    }

    public void prepareBranch(final String userInum) {
        String baseDn = getBaseDnForFido2AuthenticationEntries(userInum);
        if (!persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	return;
        }

        // Create Fido2 base branch for authentication entries if needed
        if (!containsBranch(baseDn)) {
            addBranch(baseDn);
        }
    }

    public List<Fido2AuthenticationEntry> findByChallenge(String challenge, boolean oneStep) {
        String baseDn = oneStep ? getDnForAuthenticationEntry(null, null) : getBaseDnForFido2AuthenticationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("jansCodeChallenge", challenge);
        Filter codeChallengHashCodeFilter = Filter.createEqualityFilter("jansCodeChallengeHash", String.valueOf(challengeGenerator.getChallengeHashCode(challenge)));
        Filter filter = Filter.createANDFilter(codeChallengFilter, codeChallengHashCodeFilter);

        List<Fido2AuthenticationEntry> fido2AuthenticationEntries = persistenceEntryManager.findEntries(baseDn, Fido2AuthenticationEntry.class, filter);

        return fido2AuthenticationEntries;
    }

    public String getDnForAuthenticationEntry(String userInum, String jsId) {
    	String baseDn;
    	if (StringHelper.isEmpty(userInum)) {
    		baseDn = staticConfiguration.getBaseDn().getFido2Assertion();
    	} else {
	        // Build DN string for Fido2 registration entry
	        baseDn = getBaseDnForFido2AuthenticationEntries(userInum);
    	}
        // Build DN string for Fido2 authentication entry
        if (StringHelper.isEmpty(jsId)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", jsId, baseDn);
    }

    public String getBaseDnForFido2AuthenticationEntries(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_auth,inum=1234,ou=people,o=jans"
        if (StringHelper.isEmpty(userInum)) {
            return userBaseDn;
        }

        return String.format("ou=fido2_auth,%s", userBaseDn);
    }

    public String getDnForUser(String userInum) {
        String peopleDn = staticConfiguration.getBaseDn().getPeople();
        if (StringHelper.isEmpty(userInum)) {
            return peopleDn;
        }

        return String.format("inum=%s,%s", userInum, peopleDn);
    }

}
