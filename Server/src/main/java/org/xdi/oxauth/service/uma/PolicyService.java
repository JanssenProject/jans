/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.uma.persistence.UmaPolicy;
import org.xdi.oxauth.util.ServerUtil;

import com.unboundid.ldap.sdk.Filter;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */
@Scope(ScopeType.STATELESS)
@Name("umaPolicyService")
@AutoCreate
public class PolicyService {

    @Logger
    private Log log;
    @In
    private LdapEntryManager ldapEntryManager;

    public List<UmaPolicy> loadAllPolicies() {
        try {
            return ldapEntryManager.findEntries(baseDn(), UmaPolicy.class, Filter.createPresenceFilter("inum"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<UmaPolicy> loadPoliciesByScopeDns(List<String> p_umaScopeDns) {
        try {
            if (p_umaScopeDns == null || p_umaScopeDns.isEmpty()) {
                throw new IllegalArgumentException("Scopes can't be empty");
            }

            return ldapEntryManager.findEntries(baseDn(), UmaPolicy.class, Filter.create(buildScopesFilter(p_umaScopeDns)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public void persist(UmaPolicy p_policy) {
        try {
            if (StringUtils.isBlank(p_policy.getDn())) {
                p_policy.setDn(String.format("inum=%s,", p_policy.getInum()) + baseDn());
            }

            ldapEntryManager.persist(p_policy);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static String buildScopesFilter(List<String> p_umaScopes) {
        final StringBuilder sb = new StringBuilder();
        sb.append("(|");
        for (String scope : p_umaScopes) {
            sb.append(String.format("(oxAuthUmaScope=%s)", scope));
        }
        sb.append(")");
        return sb.toString();
    }

    private static String baseDn() {
        return ConfigurationFactory.getBaseDn().getUmaPolicy();
    }

    public static PolicyService instance() {
        return ServerUtil.instance(PolicyService.class);
    }
}
