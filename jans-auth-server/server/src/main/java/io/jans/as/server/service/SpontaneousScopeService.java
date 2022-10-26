/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.CreatorType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Pair;
import io.jans.as.persistence.model.Scope;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.python.google.common.collect.Sets;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Stateless
@Named
public class SpontaneousScopeService {

    private static final int DEFAULT_SPONTANEOUS_SCOPE_LIFETIME_IN_SECONDS = 60 * 60 * 24; // 24h
    @Inject
    private Logger log;
    @Inject
    private StaticConfiguration staticConfiguration;
    @Inject
    private AppConfiguration appConfiguration;
    @Inject
    private ScopeService scopeService;

    public Scope createSpontaneousScopeIfNeeded(Set<String> regExps, String scopeId, String clientId) {
        Scope fromPersistence = scopeService.getScopeById(scopeId);
        if (fromPersistence != null) { // scope already exists
            return fromPersistence;
        }

        final Pair<Boolean, String> isAllowed = isAllowedBySpontaneousScopes(regExps, scopeId);
        if (isFalse(isAllowed.getFirst())) {
            log.error("Forbidden by client. Check client configuration.");
            return null;
        }

        Scope regexpScope = scopeService.getScopeById(isAllowed.getSecond());

        Scope scope = new Scope();
        scope.setDefaultScope(false);
        scope.setDescription("Spontaneous scope: " + scope);
        scope.setDisplayName(scopeId);
        scope.setId(scopeId);
        scope.setInum(UUID.randomUUID().toString());
        scope.setScopeType(ScopeType.SPONTANEOUS);
        scope.setDeletable(true);
        scope.setExpirationDate(new Date(getLifetime()));
        scope.setDn("inum=" + scope.getInum() + "," + staticConfiguration.getBaseDn().getScopes());
        scope.setCreatorId(clientId);
        scope.setCreatorType(CreatorType.CLIENT);
        scope.getAttributes().setSpontaneousClientScopes(Lists.newArrayList(isAllowed.getSecond()));
        scope.setUmaAuthorizationPolicies(regexpScope != null ? regexpScope.getUmaAuthorizationPolicies() : new ArrayList<>());

        scopeService.persist(scope);
        log.trace("Created spontaneous scope: {}, dn: {}", scope.getId(), scope.getDn());
        return scope;
    }

    public long getLifetime() {
        Calendar expiration = Calendar.getInstance();
        int lifetime = DEFAULT_SPONTANEOUS_SCOPE_LIFETIME_IN_SECONDS;
        if (appConfiguration.getSpontaneousScopeLifetime() > 0) {
            lifetime = appConfiguration.getSpontaneousScopeLifetime();
        }
        expiration.add(Calendar.SECOND, lifetime);
        return expiration.getTimeInMillis();
    }

    public boolean isAllowedBySpontaneousScopes(Client client, String scopeRequested) {
        if (isFalse(appConfiguration.getAllowSpontaneousScopes())) {
            return false;
        }

        if (isFalse(client.getAttributes().getAllowSpontaneousScopes())) {
            return false;
        }

        return isAllowedBySpontaneousScopes(Sets.newHashSet(client.getAttributes().getSpontaneousScopes()), scopeRequested).getFirst();
    }

    public boolean isAllowedBySpontaneousScopeRegExps(Set<String> regExps, String scopeRequested) {
        return isAllowedBySpontaneousScopes(regExps, scopeRequested).getFirst();
    }

    public Pair<Boolean, String> isAllowedBySpontaneousScopes(Set<String> regExps, String scopeRequested) {

        for (String spontaneousScope : regExps) {
            if (isAllowedBySpontaneousScope(spontaneousScope, scopeRequested)) {
                return new Pair<>(true, spontaneousScope);
            }
        }

        return new Pair<>(false, null);
    }

    public boolean isAllowedBySpontaneousScope(String spontaneousScope, String scopeRequested) {
        try {
            boolean result = spontaneousScope.equals(scopeRequested);

            if (!result) {
                result = Pattern.matches(spontaneousScope, scopeRequested);
            }

            if (result) {
                log.trace("Scope {} allowed by spontaneous scope: {}", scopeRequested, spontaneousScope);
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
