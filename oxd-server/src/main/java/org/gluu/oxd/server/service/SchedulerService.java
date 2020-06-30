package org.gluu.oxd.server.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.server.persistence.service.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerService {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerService.class);

    private ConfigurationService configurationService;
    private PersistenceService persistenceService;
    private KeyGeneratorService keyGeneratorService;

    @Inject
    public SchedulerService(ConfigurationService configurationService, PersistenceService persistenceService, KeyGeneratorService keyGeneratorService) {
        this.configurationService = configurationService;
        this.persistenceService = persistenceService;
        this.keyGeneratorService = keyGeneratorService;
    }

    public void scheduleTasks() {
        ScheduledExecutorService scheduledExecutorService = CoreUtils.createExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                dbCleanUpTask();
                jwksRegenerationTask();
            }
        }, configurationService.get().getDbCleanupIntervalInHours(), configurationService.get().getDbCleanupIntervalInHours(), TimeUnit.HOURS);
    }

    public void jwksRegenerationTask() {
        if (configurationService.get().getEnableJwksGeneration()) {

            LOG.trace("Delete jwks from object if not present in storage...");
            if (keyGeneratorService.getKeysFromStorage() == null) {
                LOG.trace("Jwks not present in storage. Resetting jwks in object to null...");
                keyGeneratorService.setKeys(null);
            }

            LOG.trace("Generating jwks if missing in storage or expired...");
            keyGeneratorService.getKeys();
        }
    }

    public void dbCleanUpTask() {
        LOG.trace("Deleting expired_objects from storage...");
        String storage = configurationService.get().getStorage();
        //this task is not required for redis and couchbase storage
        if (!Lists.newArrayList("redis", "couchbase").contains(storage)) {
            persistenceService.deleteAllExpiredObjects();
        }
    }

}
