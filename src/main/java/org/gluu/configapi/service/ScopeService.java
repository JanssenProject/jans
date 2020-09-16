package org.gluu.configapi.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * Responsible for OpenID Connect, OAuth2 and UMA scopes. (Type is defined by ScopeType.)
 *
 * @author Yuriy Zabrovarnyy
 */
public class ScopeService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public String baseDn() {
        return staticConfiguration.getBaseDn().getScopes();
    }

    public String createDn(String inum) {
        return String.format("inum=%s,%s", inum, baseDn());
    }

    public void persist(Scope scope) {
        if (StringUtils.isBlank(scope.getDn())) {
            scope.setDn(createDn(scope.getInum()));
        }

        persistenceEntryManager.persist(scope);
    }
}
