package io.jans.ca.server.persistence.service;

import io.jans.ca.common.ExpiredObject;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.persistence.providers.H2PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class PersistenceServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceServiceImpl.class);
    @Inject
    MainPersistenceService jansConfigurationService;
    private PersistenceService persistenceService;

    public void create() {
        persistenceService = createServiceInstance();
        persistenceService.create();
    }

    private PersistenceService getPersistenceService() {
        if (persistenceService == null) {
            create();
        }
        return persistenceService;
    }

    private PersistenceService createServiceInstance() {
        ApiAppConfiguration apiConf = this.jansConfigurationService.find();
        String storage = apiConf.getStorage();
        switch (storage) {
            case "jans_server_configuration":
                return jansConfigurationService;
            case "h2":
                return new SqlPersistenceServiceImpl(new H2PersistenceProvider(apiConf));
            case "redis":
                return new RedisPersistenceService(apiConf);
            default:
                LOG.error("Failed to recognize persistence provider. Unrecognized storage specified: {}, full api configuration: {}", storage, apiConf);
                return jansConfigurationService;
        }
    }

    public boolean create(Rp rp) {
        return getPersistenceService().create(rp);
    }

    public boolean createExpiredObject(ExpiredObject obj) {
        return getPersistenceService().createExpiredObject(obj);
    }

    public ExpiredObject getExpiredObject(String key) {
        return getPersistenceService().getExpiredObject(key);
    }

    public boolean isExpiredObjectPresent(String key) {
        return getPersistenceService().isExpiredObjectPresent(key);
    }

    public boolean update(Rp rp) {
        return getPersistenceService().update(rp);
    }

    public Rp getRp(String rpId) {
        return getPersistenceService().getRp(rpId);
    }

    public boolean removeAllRps() {
        return getPersistenceService().removeAllRps();
    }

    public Set<Rp> getRps() {
        return getPersistenceService().getRps();
    }

    public boolean deleteExpiredObjectsByKey(String key) {
        return getPersistenceService().deleteExpiredObjectsByKey(key);
    }

    public boolean deleteAllExpiredObjects() {
        return getPersistenceService().deleteAllExpiredObjects();
    }

    public void destroy() {
        getPersistenceService().destroy();
    }

    public boolean remove(String rpId) {
        return getPersistenceService().remove(rpId);
    }
}
