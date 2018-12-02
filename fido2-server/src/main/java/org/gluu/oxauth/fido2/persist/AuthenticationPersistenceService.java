/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.persist;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationData;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationEntry;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.ldap.model.SimpleBranch;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

import com.unboundid.ldap.sdk.Filter;

@ApplicationScoped
public class AuthenticationPersistenceService {

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private UserService userService;

    @Inject
    private LdapEntryManager ldapEntryManager;

    public Optional<Fido2AuthenticationData> findByChallenge(String challenge) {
        String baseDn = getBaseDnForFido2AuthenticationEntries(null);

        Filter codeChallengFilter = Filter.createEqualityFilter("oxCodeChallenge", challenge);

        List<Fido2AuthenticationEntry> fido2AuthenticationEntries = ldapEntryManager.findEntries(baseDn, Fido2AuthenticationEntry.class, null, codeChallengFilter);
        
        if (fido2AuthenticationEntries.size() > 0) {
            return Optional.of(fido2AuthenticationEntries.get(0).getAuthenticationData());
        }

        return Optional.empty();
    }

    public void save(Fido2AuthenticationData authenticationData) {
        String userName = authenticationData.getUsername();
        
        User user = userService.getUser(userName, "inum");
        if (user == null) {
            user = userService.addDefaultUser(userName);
        }
        String userInum = userService.getUserInum(user);

        prepareBranch(userInum);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String id = UUID.randomUUID().toString();
        
        String dn = getDnForAuthenticationEntry(userInum, id);
        Fido2AuthenticationEntry fido2AuthenticationEntry = new Fido2AuthenticationEntry(dn, authenticationData.getId(), now, null, userInum, authenticationData);

        ldapEntryManager.persist(fido2AuthenticationEntry);
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

}
