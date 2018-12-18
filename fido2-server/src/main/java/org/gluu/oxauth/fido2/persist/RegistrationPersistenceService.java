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
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationEntry;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationEntry;
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
import com.unboundid.util.StaticUtils;

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

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, null);

        return fido2RegistrationnEntries;
    }

    public List<Fido2RegistrationEntry> findAllByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("oxCodeChallenge", challenge);
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, null, codeChallengFilter);

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
        
        registrationData.setCreatedDate(now);
        registrationData.setCreatedBy(userName);

        String dn = getDnForRegistrationEntry(userInum, id);
        Fido2RegistrationEntry registrationEntry = new Fido2RegistrationEntry(dn, id, now, null, userInum, registrationData.getPublicKeyId(), registrationData);
        registrationEntry.setRegistrationStatus(registrationData.getStatus());
        updateRegistrationAttributes(registrationEntry);

        ldapEntryManager.persist(registrationEntry);
    }

    public void update(Fido2RegistrationEntry registrationEntry) {
        updateRegistrationAttributes(registrationEntry);

        ldapEntryManager.merge(registrationEntry);
    }

    private void updateRegistrationAttributes(Fido2RegistrationEntry registrationEntry) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();
        registrationData.setUpdatedDate(now);
        registrationData.setUpdatedBy(registrationData.getUsername());

        registrationEntry.setRegistrationStatus(registrationData.getStatus());
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
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_register,inum=1234,ou=people,o=@!1111,o=gluu"
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
        int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        unfinishedRequestExpiration = unfinishedRequestExpiration == 0 ? 120 : unfinishedRequestExpiration;

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -unfinishedRequestExpiration);
        final Date unfinishedRequestExpirationDate = calendar.getTime();

        BatchOperation<Fido2RegistrationEntry> cleanerBatchService = new BatchOperation<Fido2RegistrationEntry>(ldapEntryManager) {
            @Override
            protected List<Fido2RegistrationEntry> getChunkOrNull(int chunkSize) {
                return ldapEntryManager.findEntries(getDnForUser(null), Fido2RegistrationEntry.class, getFilter(), SearchScope.SUB, null, this, 0, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<Fido2RegistrationEntry> entries) {
                for (Fido2RegistrationEntry p : entries) {
                    log.debug("Removing Fido2 registration entry: {}, Creation date: {}", p.getChallange(), p.getCreationDate());
                    try {
                        ldapEntryManager.remove(p);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }

            private Filter getFilter() {
                // Build unfinished request expiration filter
                Filter authenticationStatusFilter = Filter.createORFilter(Filter.createNOTFilter(Filter.createPresenceFilter("oxStatus")),
                        Filter.createNOTFilter(Filter.createEqualityFilter("oxStatus", "registerted")));

                Filter exirationDateFilter = Filter.createLessOrEqualFilter("creationDate",
                        StaticUtils.encodeGeneralizedTime(unfinishedRequestExpirationDate));
                
                Filter unfinishedRequestFilter = Filter.createANDFilter(authenticationStatusFilter, exirationDateFilter);

                return unfinishedRequestFilter;
            }
        };
        cleanerBatchService.iterateAllByChunks(batchSize);
    }


}
