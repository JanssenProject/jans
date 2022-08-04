/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.util.Pair;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.util.INumGenerator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds permission tokens and permissions
 *
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class UmaPermissionService {

    private static final String ORGUNIT_OF_RESOURCE_PERMISSION = "uma_permission";
    private static final int DEFAULT_TICKET_LIFETIME = 3600;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private UmaScopeService scopeService;

    @Inject
    private AppConfiguration appConfiguration;

    public static String getDn(String clientDn, String ticket) {
        return String.format("jansTicket=%s,%s", ticket, getBranchDn(clientDn));
    }

    public static String getBranchDn(String clientDn) {
        return String.format("ou=%s,%s", ORGUNIT_OF_RESOURCE_PERMISSION, clientDn);
    }

    private List<UmaPermission> createPermissions(UmaPermissionList permissions, Pair<Date, Integer> expirationDate) {
        final String configurationCode = INumGenerator.generate(8) + "." + System.currentTimeMillis();

        final String ticket = generateNewTicket();
        List<UmaPermission> result = new ArrayList<>();
        for (io.jans.as.model.uma.UmaPermission permission : permissions) {
            UmaPermission p = new UmaPermission(permission.getResourceId(), scopeService.getScopeDNsByIdsAndAddToPersistenceIfNeeded(permission.getScopes()), ticket, configurationCode, expirationDate);
            if (permission.getParams() != null && !permission.getParams().isEmpty()) {
                p.getAttributes().putAll(permission.getParams());
            }
            result.add(p);
        }

        return result;
    }

    public String generateNewTicket() {
        return UUID.randomUUID().toString();
    }

    public String addPermission(UmaPermissionList permissionList, String clientDn) {
        try {
            List<UmaPermission> created = createPermissions(permissionList, ticketExpirationDate());
            for (UmaPermission permission : created) {
                addPermission(permission, clientDn);
            }
            return created.get(0).getTicket();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw e;
        }
    }

    public Pair<Date, Integer> ticketExpirationDate() {
        int lifeTime = appConfiguration.getUmaTicketLifetime();
        if (lifeTime <= 0) {
            lifeTime = DEFAULT_TICKET_LIFETIME;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, lifeTime);
        return new Pair<>(calendar.getTime(), lifeTime);
    }

    public void addPermission(UmaPermission permission, String clientDn) {
        try {
            addBranchIfNeeded(clientDn);
            permission.setDn(getDn(clientDn, permission.getTicket()));
            ldapEntryManager.persist(permission);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void merge(UmaPermission permission) {
        permission.resetTtlFromExpirationDate();
        ldapEntryManager.merge(permission);
    }

    public void mergeSilently(UmaPermission permission) {
        try {
            permission.resetTtlFromExpirationDate();
            ldapEntryManager.merge(permission);
        } catch (Exception e) {
            log.error("Failed to persist permission: " + permission, e);
        }
    }

    public List<UmaPermission> getPermissionsByTicket(String ticket) {
        try {
            final String baseDn = staticConfiguration.getBaseDn().getClients();
            final Filter filter = Filter.createEqualityFilter("jansTicket", ticket);
            return ldapEntryManager.findEntries(baseDn, UmaPermission.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public void deletePermission(String ticket) {
        try {
            final List<UmaPermission> permissions = getPermissionsByTicket(ticket);
            for (UmaPermission p : permissions) {
                ldapEntryManager.remove(p);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void addBranch(String clientDn) {
        final SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName(ORGUNIT_OF_RESOURCE_PERMISSION);
        branch.setDn(getBranchDn(clientDn));
        ldapEntryManager.persist(branch);
    }

    public void addBranchIfNeeded(String clientDn) {
        if (ldapEntryManager.hasBranchesSupport(clientDn) && !containsBranch(clientDn)) {
            addBranch(clientDn);
        }
    }

    public boolean containsBranch(String clientDn) {
        return ldapEntryManager.contains(getBranchDn(clientDn), SimpleBranch.class);
    }

    public String changeTicket(List<UmaPermission> permissions, Map<String, String> attributes) {
        String newTicket = generateNewTicket();

        for (UmaPermission permission : permissions) {
            ldapEntryManager.remove(permission);

            String dn = String.format("jansTicket=%s,%s", newTicket, StringUtils.substringAfter(permission.getDn(), ","));
            permission.setTicket(newTicket);
            permission.setDn(dn);
            permission.setAttributes(attributes);
            ldapEntryManager.persist(permission);
            log.trace("New ticket: {}, old permission: {}", newTicket, dn);
        }

        return newTicket;
    }
}
