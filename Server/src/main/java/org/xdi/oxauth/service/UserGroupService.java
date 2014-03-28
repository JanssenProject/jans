package org.xdi.oxauth.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.ldap.UserGroup;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2012
 */
@Scope(ScopeType.APPLICATION)
@Name("userGroupService")
@AutoCreate
public class UserGroupService {

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    public UserGroup loadGroup(String p_groupDN) {
        try {
            if (StringUtils.isNotBlank(p_groupDN)) {
                return ldapEntryManager.find(UserGroup.class, p_groupDN);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
        return null;
    }

    public boolean isUserInGroup(String p_groupDN, String p_userDN) {
        final UserGroup group = loadGroup(p_groupDN);
        if (group != null) {
            final String[] member = group.getMember();
            if (member != null) {
                return Arrays.asList(member).contains(p_userDN);
            }
        }
        return false;
    }

    public boolean isInAnyGroup(String[] p_groupDNs, String p_userDN) {
        return p_groupDNs != null && isInAnyGroup(Arrays.asList(p_groupDNs), p_userDN);
    }

    public boolean isInAnyGroup(List<String> p_groupDNs, String p_userDN) {
        if (p_groupDNs != null && !p_groupDNs.isEmpty() && p_userDN != null && !p_userDN.isEmpty()) {
            for (String groupDN : p_groupDNs) {
                if (isUserInGroup(groupDN, p_userDN)) {
                    return true;
                }
            }
        }
        return false;
    }
}
