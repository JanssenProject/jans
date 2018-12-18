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
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.StaticUtils;

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

        List<Fido2AuthenticationEntry> fido2AuthenticationEntries = ldapEntryManager.findEntries(baseDn, Fido2AuthenticationEntry.class, null, codeChallengFilter);

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

        authenticationData.setCreatedDate(now);
        authenticationData.setCreatedBy(userName);

        String dn = getDnForAuthenticationEntry(userInum, id);
        Fido2AuthenticationEntry authenticationEntity = new Fido2AuthenticationEntry(dn, authenticationData.getId(), now, null, userInum, authenticationData);
        updateAuthenticationAttributes(authenticationEntity);

        ldapEntryManager.persist(authenticationEntity);
    }

    public void update(Fido2AuthenticationEntry authenticationEntity) {
        updateAuthenticationAttributes(authenticationEntity);

        ldapEntryManager.merge(authenticationEntity);
    }

    private void updateAuthenticationAttributes(Fido2AuthenticationEntry authenticationEntity) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();
        authenticationData.setUpdatedDate(now);
        authenticationData.setUpdatedBy(authenticationData.getUsername());

        authenticationEntity.setAuthenticationStatus(authenticationData.getStatus());
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
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_auth,inum=1234,ou=people,o=@!1111,o=gluu"
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

        BatchOperation<Fido2AuthenticationEntry> cleanerBatchService = new BatchOperation<Fido2AuthenticationEntry>(ldapEntryManager) {
            @Override
            protected List<Fido2AuthenticationEntry> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(getDnForUser(null), Fido2AuthenticationEntry.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<Fido2AuthenticationEntry> entries) {
                for (Fido2AuthenticationEntry p : entries) {
                    try {
                        log.debug("Removing Fido2 authentication entry: {}, Creation date: {}", p.getChallange(), p.getCreationDate());
                        ldapEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }

            private Filter getFilter() {
                // Build unfinished request expiration filter
                Filter authenticationStatusFilter1 = Filter.createORFilter(Filter.createNOTFilter(Filter.createPresenceFilter("oxStatus")),
                        Filter.createNOTFilter(Filter.createEqualityFilter("oxStatus", "registerted")));

                Filter exirationDateFilter1 = Filter.createLessOrEqualFilter("creationDate",
                        StaticUtils.encodeGeneralizedTime(unfinishedRequestExpirationDate));
                
                Filter unfinishedRequestFilter = Filter.createANDFilter(authenticationStatusFilter1, exirationDateFilter1);

                // Build authentication history expiration filter
                Filter authenticationStatusFilter2 = Filter.createORFilter(Filter.createNOTFilter(Filter.createPresenceFilter("oxStatus")),
                        Filter.createEqualityFilter("oxStatus", "registerted"));

                Filter exirationDateFilter2 = Filter.createLessOrEqualFilter("creationDate",
                        StaticUtils.encodeGeneralizedTime(authenticationHistoryExpirationDate));
                
                Filter authenticationHistoryFilter = Filter.createANDFilter(authenticationStatusFilter2, exirationDateFilter2);

                return Filter.createORFilter(unfinishedRequestFilter, authenticationHistoryFilter);
            }
        };
        cleanerBatchService.iterateAllByChunks(batchSize);
    }

}
