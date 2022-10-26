/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 * @author yuriyz on 05/31/2017.
 */
@ApplicationScoped
public class UmaPctService {

    public static final int DEFAULT_PCT_LIFETIME = 2592000;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    public UmaPCT updateClaims(UmaPCT pct, Jwt idToken, String clientId, List<UmaPermission> permissions) {
        try {
            String ticketPctCode = permissions.get(0).getAttributes().get("pct");
            UmaPCT ticketPct = StringUtils.isNotBlank(ticketPctCode) ? getByCode(ticketPctCode) : null;

            boolean hasPct = pct != null;

            if (!hasPct) {
                if (ticketPct != null) {
                    pct = ticketPct;
                } else {
                    pct = createPctAndPersist(clientId);
                }
            }

            // copy claims from pctTicket into normal pct
            JwtClaims pctClaims = pct.getClaims();
            if (ticketPct != null && hasPct) {
                JwtClaims ticketClaims = ticketPct.getClaims();
                for (String key : ticketClaims.keys()) {
                    pctClaims.setClaimObject(key, ticketClaims.getClaim(key), false);
                }
                pct = ticketPct;
            }

            if (idToken != null && idToken.getClaims() != null) {
                for (String key : idToken.getClaims().keys()) {
                    pctClaims.setClaimObject(key, idToken.getClaims().getClaim(key), false);
                }
            }

            pct.setClaims(pctClaims);
            log.trace("PCT code: {}, claims: {}", pct.getCode(), pct.getClaimValuesAsJson());

            pct.resetTtlFromExpirationDate();
            ldapEntryManager.merge(pct);

            return ldapEntryManager.find(UmaPCT.class, pct.getDn());
        } catch (Exception e) {
            log.error("Failed to update PCT claims. " + e.getMessage(), e);
        }

        return pct;
    }

    public UmaPCT getByCode(String pctCode) {
        try {
            final Filter filter = Filter.createEqualityFilter("tknCde", pctCode);
            final List<UmaPCT> entries = ldapEntryManager.findEntries(branchBaseDn(), UmaPCT.class, filter);
            if (entries != null && !entries.isEmpty()) {
                return entries.get(0);
            } else {
                log.error("Failed to find PCT by code: {}", pctCode);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public UmaPCT createPct(String clientId) {
        String code = generateCode();

        UmaPCT pct = new UmaPCT(pctLifetime());
        pct.setCode(code);
        pct.setDn(dn(pct.getCode()));
        pct.setClientId(clientId);
        return pct;
    }

    public UmaPCT createPctAndPersist(String clientId) {
        UmaPCT pct = createPct(clientId);
        persist(pct);
        return pct;
    }

    public int pctLifetime() {
        int lifeTime = appConfiguration.getUmaPctLifetime();
        if (lifeTime <= 0) {
            lifeTime = DEFAULT_PCT_LIFETIME;
        }
        return lifeTime;
    }

    public void persist(UmaPCT pct) {
        try {
            prepareBranch();

            pct.setDn(dn(pct.getCode()));
            ldapEntryManager.persist(pct);
        } catch (Exception e) {
            log.error("Failed to persist PCT, code: " + pct.getCode() + ". " + e.getMessage(), e);
        }
    }

    public void remove(UmaPCT umaPCT) {
        ldapEntryManager.remove(umaPCT);
    }

    public void remove(String pctCode) {
        remove(getByCode(pctCode));
    }

    public void remove(List<UmaPCT> pctList) {
        for (UmaPCT pct : pctList) {
            remove(pct);
        }
    }

    private void prepareBranch() {
        if (!ldapEntryManager.hasBranchesSupport(branchBaseDn())) {
            return;
        }

        if (!ldapEntryManager.contains(branchBaseDn(), SimpleBranch.class)) {
            addBranch();
        }
    }

    public void addBranch() {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("pct");
        branch.setDn(branchBaseDn());

        ldapEntryManager.persist(branch);
    }

    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    public String dn(String pctCode) {
        if (StringUtils.isBlank(pctCode)) {
            throw new IllegalArgumentException("PCT code is null or blank.");
        }
        return String.format("tknCde=%s,%s", pctCode, branchBaseDn());
    }

    public String branchBaseDn() {
        final String umaBaseDn = staticConfiguration.getBaseDn().getUmaBase(); // "ou=uma,o=jans"
        return String.format("ou=pct,%s", umaBaseDn);
    }

    public void merge(UmaPCT pct) {
        try {
            pct.resetTtlFromExpirationDate();
            ldapEntryManager.merge(pct);
        } catch (Exception e) {
            log.error("Failed to merge PCT, code: " + pct.getCode() + ". " + e.getMessage(), e);
        }
    }
}
