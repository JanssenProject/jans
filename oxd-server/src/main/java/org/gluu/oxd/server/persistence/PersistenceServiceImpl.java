package org.gluu.oxd.server.persistence;

import com.google.inject.Inject;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.ExpiredObject;
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
        String storage = configurationService.getConfiguration().getStorage();
        if ("h2".equalsIgnoreCase(storage)) {
            sqlProvider = new H2PersistenceProvider(configurationService);
            setTimerForDBCleanUpTask();
            return new SqlPersistenceServiceImpl(sqlProvider);
        } else if ("redis".equalsIgnoreCase(storage)) {
            return new RedisPersistenceService(configurationService.getConfiguration());
        } else if ("jdbc".equalsIgnoreCase(storage)) {
            sqlProvider = new JDBCPersistenceProvider(configurationService);
            setTimerForDBCleanUpTask();
            return new SqlPersistenceServiceImpl(sqlProvider);
        }
        throw new RuntimeException("Failed to create persistence provider. Unrecognized storage specified: " + storage + ", full configuration: " + configurationService.get());
    }

    public void setTimerForDBCleanUpTask() {
        ScheduledExecutorService scheduledExecutorService = CoreUtils.createExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                LOG.debug("Deleting expired_objects from database.");
                persistenceService.deleteAllExpiredObjects();
            }
        }, configurationService.get().getDbCleanupIntervalInHours(), configurationService.get().getDbCleanupIntervalInHours(), TimeUnit.HOURS);
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
