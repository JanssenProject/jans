package org.gluu.oxd.server.persistence.service;

import com.google.inject.Inject;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.server.persistence.providers.H2PersistenceProvider;
import org.gluu.oxd.server.persistence.providers.JDBCPersistenceProvider;
import org.gluu.oxd.server.persistence.providers.SqlPersistenceProvider;
import org.gluu.oxd.server.service.ConfigurationService;
import org.gluu.oxd.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 */

public class PersistenceServiceImpl implements PersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceServiceImpl.class);

    private ConfigurationService configurationService;
    private SqlPersistenceProvider sqlProvider;
    private PersistenceService persistenceService;

    @Inject
    public PersistenceServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void create() {
        persistenceService = createServiceInstance();
        persistenceService.create();
    }

    private PersistenceService createServiceInstance() {

        String storage = this.configurationService.getConfiguration().getStorage();
        switch (storage) {
            case "h2":
                this.sqlProvider = new H2PersistenceProvider(this.configurationService);
                return new SqlPersistenceServiceImpl(this.sqlProvider, this.configurationService);
            case "redis":
                return new RedisPersistenceService(this.configurationService.getConfiguration());
            case "jdbc":
                this.sqlProvider = new JDBCPersistenceProvider(this.configurationService);
                return new SqlPersistenceServiceImpl(this.sqlProvider, this.configurationService);
            case "gluu_server_configuration":
                return new GluuPersistenceService(this.configurationService.getConfiguration());
            case "ldap":
                return new GluuPersistenceService(this.configurationService.getConfiguration(), storage);
            case "couchbase":
                return new GluuPersistenceService(this.configurationService.getConfiguration(), storage);
        }
        throw new RuntimeException("Failed to create persistence provider. Unrecognized storage specified: " + storage + ", full configuration: " + this.configurationService.get());
    }

    public boolean create(Rp rp) {
        return persistenceService.create(rp);
    }

    public boolean createExpiredObject(ExpiredObject obj) {
        return persistenceService.createExpiredObject(obj);
    }

    public ExpiredObject getExpiredObject(String key) {
        return persistenceService.getExpiredObject(key);
    }

    public boolean isExpiredObjectPresent(String key) {
        return persistenceService.isExpiredObjectPresent(key);
    }

    public boolean update(Rp rp) {
        return persistenceService.update(rp);
    }

    public Rp getRp(String oxdId) {
        return persistenceService.getRp(oxdId);
    }

    public boolean removeAllRps() {
        return persistenceService.removeAllRps();
    }

    public Set<Rp> getRps() {
        return persistenceService.getRps();
    }

    public boolean deleteExpiredObjectsByKey(String key) {
        return persistenceService.deleteExpiredObjectsByKey(key);
    }

    public boolean deleteAllExpiredObjects() {
        return persistenceService.deleteAllExpiredObjects();
    }

    public void destroy() {
        persistenceService.destroy();
    }

    @Override
    public boolean remove(String oxdId) {
        return persistenceService.remove(oxdId);
    }
}
