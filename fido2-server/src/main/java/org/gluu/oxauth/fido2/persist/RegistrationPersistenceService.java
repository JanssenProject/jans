/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.persist;

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

import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

import com.unboundid.ldap.sdk.Filter;

@ApplicationScoped
public class RegistrationPersistenceService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;
    
    @Inject
    private UserService userService;

    @Inject
    private LdapEntryManager ldapEntryManager;

    public Optional<Fido2RegistrationData> findByPublicKeyId(String publicKeyId) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter publicKeyIdFilter = Filter.createEqualityFilter("oxPublicKeyId", publicKeyId);
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, null, publicKeyIdFilter);
        
        if (fido2RegistrationnEntries.size() > 0) {
            return Optional.of(fido2RegistrationnEntries.get(0).getRegistrationData());
        }

        return Optional.empty();
    }

    public List<Fido2RegistrationData> findAllByUsername(String username) {
        String userInum = userService.getUserInum(username);
        if (userInum == null) {
            return Collections.emptyList();
        }

        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);

        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, null);
        
        if (fido2RegistrationnEntries.size() > 0) {
            return fido2RegistrationnEntries.parallelStream().map(f -> f.getRegistrationData()).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public List<Fido2RegistrationData> findAllByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2RegistrationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("oxCodeChallenge", challenge);
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = ldapEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, null, codeChallengFilter);
        
        if (fido2RegistrationnEntries.size() > 0) {
            return fido2RegistrationnEntries.parallelStream().map(f -> f.getRegistrationData()).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public void save(Fido2RegistrationData registrationData) {
        String userName = registrationData.getUsername();
        
        User user = userService.getUser(userName, "inum");
        if (user == null) {
            user = userService.addDefaultUser(userName);
        }
        String userInum = userService.getUserInum(user);

        prepareBranch(userInum);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String id = UUID.randomUUID().toString();
        
        String dn = getDnForRegistrationEntry(userInum, id);
        Fido2RegistrationEntry fido2RegistrationEntry = new Fido2RegistrationEntry(dn, id, now, null, userInum, registrationData.getPublicKeyId(), registrationData);

        ldapEntryManager.persist(fido2RegistrationEntry);
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

}
