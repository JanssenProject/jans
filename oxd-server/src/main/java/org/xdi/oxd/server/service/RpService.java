package org.xdi.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.server.persistence.PersistenceService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 */

public class RpService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RpService.class);

    private final Map<String, Rp> rpMap = Maps.newConcurrentMap();

    private ValidationService validationService;

    private PersistenceService persistenceService;

    private ConfigurationService configurationService;

    @Inject
    public RpService(ValidationService validationService, PersistenceService persistenceService, ConfigurationService configurationService) {
        this.validationService = validationService;
        this.persistenceService = persistenceService;
        this.configurationService = configurationService;
    }

    public void removeAllRps() {
        persistenceService.removeAllRps();
    }

    public void load() {
        for (Rp rp : persistenceService.getRps()) {
            put(rp);
        }
    }

    public Rp getRp(String oxdId) {
        Preconditions.checkNotNull(oxdId);
        Preconditions.checkState(!Strings.isNullOrEmpty(oxdId));

        Rp rp = rpMap.get(oxdId);
        if (rp == null) {
            rp = persistenceService.getRp(oxdId);
            if (rp != null) {
                rpMap.put(oxdId, rp);
            }
        }
        rp = validationService.validate(rp);
        checkClientExpiredWithException(rp);
        return rp;
    }

    private void checkClientExpiredWithException(Rp rp) {
        if (checkClientExpired(rp)) {
            throw new ErrorResponseException(ErrorResponseCode.EXPIRED_CLIENT);
        }
    }

    private boolean checkClientExpired(Rp rp) {
        if (CoreUtils.isExpired(rp.getClientSecretExpiresAt())) {
            try {
                Boolean removeExpiredClients = configurationService.get().getRemoveExpiredClients();
                if (removeExpiredClients != null && removeExpiredClients) {
                    LOG.debug("Removing client because it's expired ... rp: " + rp);
                    rpMap.remove(rp.getOxdId());
                    persistenceService.remove(rp.getOxdId());
                    LOG.debug("Removed client because it's expired, rp: " + rp);
                }
            } catch (Exception e) {
                LOG.error("Failed to remove expired client, rp: " + rp, e);
            }
            return true;
        }
        return false;
    }

    public Map<String, Rp> getRps() {
        return Maps.newHashMap(rpMap);
    }

    public void update(Rp rp) throws IOException {
        put(rp);
        persistenceService.update(rp);
    }

    public void updateSilently(Rp rp) {
        try {
            update(rp);
        } catch (IOException e) {
            LOG.error("Failed to update site configuration: " + rp, e);
        }
    }

    public void create(Rp rp) throws IOException {
        if (StringUtils.isBlank(rp.getOxdId())) {
            rp.setOxdId(UUID.randomUUID().toString());
        }

        if (rpMap.get(rp.getOxdId()) == null) {
            put(rp);
            persistenceService.create(rp);
        } else {
            LOG.error("RP already exists in database, oxd_id: " + rp.getOxdId());
        }
    }

    private Rp put(Rp rp) {
        return rpMap.put(rp.getOxdId(), rp);
    }

    public boolean remove(String oxdId) {
        boolean ok = persistenceService.remove(oxdId);
        if (ok) {
            rpMap.remove(oxdId);
        }
        return ok;
    }

    public Rp getRpByClientId(String clientId) {
        for (Rp rp : rpMap.values()) {
            if (rp.getClientId().equalsIgnoreCase(clientId)) {
                LOG.trace("Found rp by client_id: " + clientId + ", rp: " + rp);
                return rp;
            }
        }
        return null;
    }
}
