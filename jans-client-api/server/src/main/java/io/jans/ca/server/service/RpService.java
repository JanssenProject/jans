package io.jans.ca.server.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.op.OpClientFactoryImpl;
import io.jans.ca.server.persistence.service.PersistenceServiceImpl;
import io.jans.ca.server.persistence.service.JansConfigurationService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class RpService {

    @Inject
    Logger logger;
    private static Cache<String, Rp> rpCache;

    @Inject
    JansConfigurationService jansConfigurationService;

    @Inject
    ValidationService validationService;
    @Inject
    PersistenceServiceImpl persistenceService;
    @Inject
    OpClientFactoryImpl opClientFactory;
    @Inject
    HttpService httpService;

    private Cache<String, Rp> getRpCache() {
        if (rpCache != null) {
            return rpCache;
        } else {
            return CacheBuilder.newBuilder()
                    .expireAfterWrite(jansConfigurationService.findConf() != null ? jansConfigurationService.find().getRpCacheExpirationInMinutes() : 60, TimeUnit.MINUTES)
                    .build();
        }
    }

    public void removeAllRps() {
        getRpCache().invalidateAll();
        persistenceService.removeAllRps();
    }

    public void load() {
        Set<Rp> rps = persistenceService.getRps();
        if (rps == null)
            return;

        for (Rp rp : rps) {
            put(rp);
        }
    }

    public Rp getRp(String rpId) {
        Preconditions.checkNotNull(rpId);
        Preconditions.checkState(!Strings.isNullOrEmpty(rpId));

        Rp rp = getRpCache().getIfPresent(rpId);
        if (rp == null) {
            rp = persistenceService.getRp(rpId);
            if (rp != null) {
                getRpCache().put(rpId, rp);
            }
        }
        rp = validationService.validate(rp);
        return rp;
    }

    public Map<String, Rp> getRps() {
        return Maps.newHashMap(getRpCache().asMap());
    }

    public void update(Rp rp) {
        put(rp);
        persistenceService.update(rp);
    }

    public void updateSilently(Rp rp) {
        try {
            update(rp);
        } catch (Exception e) {
            logger.error("Failed to update site configuration: " + rp, e);
        }
    }

    public void create(Rp rp) {
        if (StringUtils.isBlank(rp.getRpId())) {
            rp.setRpId(UUID.randomUUID().toString());
        }

        if (getRpCache().getIfPresent(rp.getRpId()) == null) {
            put(rp);
            persistenceService.create(rp);
        } else {
            logger.error("RP already exists in database, rp_id: " + rp.getRpId());
        }
    }

    private Rp put(Rp rp) {
        getRpCache().put(rp.getRpId(), rp);
        return rp;
    }

    public boolean remove(String rpId) {
        boolean ok = persistenceService.remove(rpId);
        if (ok) {
            getRpCache().invalidate(rpId);
        }
        return ok;
    }

    public Rp getRpByClientId(String clientId) {
        for (Rp rp : getRpCache().asMap().values()) {
            if (rp.getClientId().equalsIgnoreCase(clientId)) {
                logger.trace("Found rp by client_id: " + clientId + ", rp: " + rp);
                return rp;
            }
        }
        return null;
    }

    public Rp defaultRp() {
        return jansConfigurationService.find().getDefaultSiteConfig();
    }

    public RegisterClient createRegisterClient(String registrationEndpoint, RegisterRequest registerRequest) {
        RegisterClient registerClient = opClientFactory.createRegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(httpService.getClientEngine());
        return registerClient;
    }

    public JansConfigurationService getConfigurationService() {
        return jansConfigurationService;
    }

    public PersistenceServiceImpl getPersistenceService() {
        return persistenceService;
    }
}
