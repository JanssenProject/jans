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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.entry.Fido2AuthenticationData;
import io.jans.fido2.model.entry.Fido2AuthenticationEntry;
import io.jans.fido2.model.entry.Fido2AuthenticationStatus;
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
    private UserService userService;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    public void save(Fido2AuthenticationData authenticationData) {
        String userName = authenticationData.getUsername();
        
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

        String dn = getDnForAuthenticationEntry(userInum, id);
        Fido2AuthenticationEntry authenticationEntity = new Fido2AuthenticationEntry(dn, authenticationData.getId(), now, userInum, authenticationData);
        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());

        authenticationData.setCreatedDate(now);
        authenticationData.setCreatedBy(userName);


        persistenceEntryManager.persist(authenticationEntity);
    }

    public void update(Fido2AuthenticationEntry authenticationEntity) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();
        authenticationData.setUpdatedDate(now);
        authenticationData.setUpdatedBy(authenticationData.getUsername());

        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());

        persistenceEntryManager.merge(authenticationEntity);
        System.err.println("Updated: " + authenticationEntity.getDn());
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

    public List<Fido2AuthenticationEntry> findByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2AuthenticationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("jansCodeChallenge", challenge);

        List<Fido2AuthenticationEntry> fido2AuthenticationEntries = persistenceEntryManager.findEntries(baseDn, Fido2AuthenticationEntry.class, codeChallengFilter);

        return fido2AuthenticationEntries;
    }

    public String getDnForAuthenticationEntry(String userInum, String jsId) {
        // Build DN string for Fido2 authentication entry
        String baseDn = getBaseDnForFido2AuthenticationEntries(userInum);
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

    public void cleanup(Date now, int batchSize) {
        // Cleaning expired entries
        BatchOperation<Fido2AuthenticationEntry> cleanerAuthenticationBatchService = new ProcessBatchOperation<Fido2AuthenticationEntry>() {
            @Override
            public void performAction(List<Fido2AuthenticationEntry> entries) {
                for (Fido2AuthenticationEntry p : entries) {
                    log.debug("Removing Fido2 authentication entry: {}, Creation date: {}", p.getChallange(), p.getCreationDate());
                    try {
                        persistenceEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        
        String baseDn = getDnForUser(null);
        persistenceEntryManager.findEntries(baseDn, Fido2AuthenticationEntry.class, getExpiredAuthenticationFilter(baseDn), SearchScope.SUB, new String[] {"jansCodeChallenge", "creationDate"}, cleanerAuthenticationBatchService, 0, 0, batchSize);

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
	        persistenceEntryManager.findEntries(getDnForUser(null), SimpleBranch.class, getEmptyAuthenticationBranchFilter(), SearchScope.SUB, new String[] {"ou"}, cleanerBranchBatchService, 0, 0, batchSize);
        }
    }

    private Filter getExpiredAuthenticationFilter(String baseDn) {
        int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        unfinishedRequestExpiration = unfinishedRequestExpiration == 0 ? 120 : unfinishedRequestExpiration;

        int authenticationHistoryExpiration = appConfiguration.getFido2Configuration().getAuthenticationHistoryExpiration();
        authenticationHistoryExpiration = authenticationHistoryExpiration == 0 ? 15 * 24 * 3600 : authenticationHistoryExpiration;

        Calendar calendar1 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar1.add(Calendar.SECOND, -unfinishedRequestExpiration);
        final Date unfinishedRequestExpirationDate = calendar1.getTime();

        Calendar calendar2 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar2.add(Calendar.SECOND, -authenticationHistoryExpiration);
        final Date authenticationHistoryExpirationDate = calendar2.getTime();

        // Build unfinished request expiration filter
        Filter authenticationStatusFilter1 = Filter.createNOTFilter(Filter.createEqualityFilter("jansStatus", Fido2AuthenticationStatus.authenticated.getValue()));

        Filter exirationDateFilter1 = Filter.createLessOrEqualFilter("creationDate",
                persistenceEntryManager.encodeTime(baseDn, unfinishedRequestExpirationDate));
        
        Filter unfinishedRequestFilter = Filter.createANDFilter(authenticationStatusFilter1, exirationDateFilter1);

        // Build authentication history expiration filter
        Filter authenticationStatusFilter2 = Filter.createEqualityFilter("jansStatus", Fido2AuthenticationStatus.authenticated.getValue());

        Filter exirationDateFilter2 = Filter.createLessOrEqualFilter("creationDate",
                persistenceEntryManager.encodeTime(baseDn, authenticationHistoryExpirationDate));
        
        Filter authenticationHistoryFilter = Filter.createANDFilter(authenticationStatusFilter2, exirationDateFilter2);

        return Filter.createORFilter(unfinishedRequestFilter, authenticationHistoryFilter);
    }

    private Filter getEmptyAuthenticationBranchFilter() {
        return Filter.createANDFilter(Filter.createEqualityFilter("ou", "fido2_auth"), Filter.createORFilter(
                Filter.createEqualityFilter("numsubordinates", "0"), Filter.createEqualityFilter("hasSubordinates", "FALSE")));
    }

}
