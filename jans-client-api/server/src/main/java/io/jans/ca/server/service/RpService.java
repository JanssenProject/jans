package io.jans.ca.server.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import io.jans.ca.server.persistence.service.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 */

public class RpService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RpService.class);

    private static Cache<String, Rp> rpCache;

    private ValidationService validationService;

    private PersistenceService persistenceService;

    @Inject
    public RpService(ValidationService validationService, PersistenceService persistenceService, ConfigurationService configurationService) {

        rpCache = CacheBuilder.newBuilder()
                .expireAfterWrite(configurationService.get() != null ? configurationService.get().getRpCacheExpirationInMinutes() : 60, TimeUnit.MINUTES)
                .build();

        this.validationService = validationService;
        this.persistenceService = persistenceService;
    }

    public void removeAllRps() {
        rpCache.invalidateAll();
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

        Rp rp = rpCache.getIfPresent(rpId);
        if (rp == null) {
            rp = persistenceService.getRp(rpId);
            if (rp != null) {
                rpCache.put(rpId, rp);
            }
        }
        rp = validationService.validate(rp);
        return rp;
    }

    public Map<String, Rp> getRps() {
        return Maps.newHashMap(rpCache.asMap());
    }

    public void update(Rp rp) {
        put(rp);
        persistenceService.update(rp);
    }

    public void updateSilently(Rp rp) {
        try {
            update(rp);
        } catch (Exception e) {
            LOG.error("Failed to update site configuration: " + rp, e);
        }
    }

    public void create(Rp rp) {
        if (StringUtils.isBlank(rp.getRpId())) {
            rp.setRpId(UUID.randomUUID().toString());
        }

        if (rpCache.getIfPresent(rp.getRpId()) == null) {
            put(rp);
            persistenceService.create(rp);
        } else {
            LOG.error("RP already exists in database, rp_id: " + rp.getRpId());
        }
    }

    private Rp put(Rp rp) {
        rpCache.put(rp.getRpId(), rp);
        return rp;
    }

    public boolean remove(String rpId) {
        boolean ok = persistenceService.remove(rpId);
        if (ok) {
            rpCache.invalidate(rpId);
        }
        return ok;
    }

    public Rp getRpByClientId(String clientId) {
        for (Rp rp : rpCache.asMap().values()) {
            if (rp.getClientId().equalsIgnoreCase(clientId)) {
                LOG.trace("Found rp by client_id: " + clientId + ", rp: " + rp);
                return rp;
            }
        }
        return null;
    }
}
