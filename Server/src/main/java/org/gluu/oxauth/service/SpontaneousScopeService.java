package org.gluu.oxauth.service;

import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Pattern;

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

    public void createSpontaneousScopeIfNeeded(String spontaneousScope) {
        Scope fromLdap = scopeService.getScopeById(spontaneousScope);
        if (fromLdap != null) { // scope already exists
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int lifetime = DEFAULT_SPONTANEOUS_SCOPE_LIFETIME_IN_SECONDS;
        if (appConfiguration.getSpontaneousScopeLifetime() > 0) {
            lifetime = appConfiguration.getSpontaneousScopeLifetime();
        }
        calendar.add(Calendar.SECOND, lifetime);

        Scope scope = new Scope();
        scope.setDefaultScope(false);
        scope.setDescription("Spontaneous scope: " + spontaneousScope);
        scope.setDisplayName(spontaneousScope);
        scope.setId(spontaneousScope);
        scope.setInum(UUID.randomUUID().toString());
        scope.setScopeType(ScopeType.SPONTANEOUS);
        scope.setDeletable(true);
        scope.setNewExpirationDate(calendar.getTime());
        scope.setDn("inum=" + scope.getInum() + "," + staticConfiguration.getBaseDn().getScopes());

        scopeService.persist(scope);
    }

    public boolean isAllowedBySpontaneousScopes(Client client, String scopeRequested) {
        if (!client.getAttributes().getAllowSpontaneousScopes()) {
            return false;
        }

        for (String spontaneousScope : client.getAttributes().getSpontaneousScopes()) {
            if (isAllowedBySpontaneousScope(spontaneousScope, scopeRequested)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAllowedBySpontaneousScope(String spontaneousScope, String scopeRequested) {
        try {
            boolean result = Pattern.matches(spontaneousScope, scopeRequested);
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
