/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.persist;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationData;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationEntry;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationStatus;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

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
    private PersistenceEntryManager ldapEntryManager;

    public List<Fido2AuthenticationEntry> findByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2AuthenticationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("oxCodeChallenge", challenge);

        List<Fido2AuthenticationEntry> fido2AuthenticationEntries = ldapEntryManager.findEntries(baseDn, Fido2AuthenticationEntry.class, codeChallengFilter);

        return fido2AuthenticationEntries;
    }

    public void save(Fido2AuthenticationData authenticationData) {
        String userName = authenticationData.getUsername();
        
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

        String dn = getDnForAuthenticationEntry(userInum, id);
        Fido2AuthenticationEntry authenticationEntity = new Fido2AuthenticationEntry(dn, authenticationData.getId(), now, null, userInum, authenticationData);
        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());

        authenticationData.setCreatedDate(now);
        authenticationData.setCreatedBy(userName);


        ldapEntryManager.persist(authenticationEntity);
    }

    public void update(Fido2AuthenticationEntry authenticationEntity) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();
        authenticationData.setUpdatedDate(now);
        authenticationData.setUpdatedBy(authenticationData.getUsername());

        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());

        ldapEntryManager.merge(authenticationEntity);
        System.err.println("Updated: " + authenticationEntity.getDn());
    }

    public void addBranch(final String baseDn) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("fido2_auth");
        branch.setDn(baseDn);

        ldapEntryManager.persist(branch);
    }

    public boolean containsBranch(final String baseDn) {
        return ldapEntryManager.contains(SimpleBranch.class, baseDn);
    }

    public void prepareBranch(final String userInum) {
        String baseDn = getBaseDnForFido2AuthenticationEntries(userInum);
        // Create Fido2 base branch for authentication entries if needed
        if (!containsBranch(baseDn)) {
            addBranch(baseDn);
        }
    }

    public String getDnForAuthenticationEntry(String userInum, String oxId) {
        // Build DN string for Fido2 authentication entry
        String baseDn = getBaseDnForFido2AuthenticationEntries(userInum);
        if (StringHelper.isEmpty(oxId)) {
            return baseDn;
        }
        return String.format("oxId=%s,%s", oxId, baseDn);
    }

    public String getBaseDnForFido2AuthenticationEntries(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_auth,inum=1234,ou=people,o=gluu"
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
                        ldapEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        ldapEntryManager.findEntries(getDnForUser(null), Fido2AuthenticationEntry.class, getExpiredAuthenticationFilter(), SearchScope.SUB, new String[] {"oxCodeChallenge", "creationDate"}, cleanerAuthenticationBatchService, 0, 0, batchSize);

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
        ldapEntryManager.findEntries(getDnForUser(null), SimpleBranch.class, getEmptyAuthenticationBranchFilter(), SearchScope.SUB, new String[] {"ou"}, cleanerBranchBatchService, 0, 0, batchSize);
    }

    private Filter getExpiredAuthenticationFilter() {
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
        Filter authenticationStatusFilter1 = Filter.createNOTFilter(Filter.createEqualityFilter("oxStatus", Fido2AuthenticationStatus.authenticated.getValue()));

        Filter exirationDateFilter1 = Filter.createLessOrEqualFilter("creationDate",
                ldapEntryManager.encodeTime(unfinishedRequestExpirationDate));
        
        Filter unfinishedRequestFilter = Filter.createANDFilter(authenticationStatusFilter1, exirationDateFilter1);

        // Build authentication history expiration filter
        Filter authenticationStatusFilter2 = Filter.createEqualityFilter("oxStatus", Fido2AuthenticationStatus.authenticated.getValue());

        Filter exirationDateFilter2 = Filter.createLessOrEqualFilter("creationDate",
                ldapEntryManager.encodeTime(authenticationHistoryExpirationDate));
        
        Filter authenticationHistoryFilter = Filter.createANDFilter(authenticationStatusFilter2, exirationDateFilter2);

        return Filter.createORFilter(unfinishedRequestFilter, authenticationHistoryFilter);
    }

    private Filter getEmptyAuthenticationBranchFilter() {
        return Filter.createANDFilter(Filter.createEqualityFilter("ou", "fido2_auth"), Filter.createORFilter(
                Filter.createEqualityFilter("numsubordinates", "0"), Filter.createEqualityFilter("hasSubordinates", "FALSE")));
    }

}
