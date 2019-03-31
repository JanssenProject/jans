package org.gluu.oxd.server.persistence;

import com.google.inject.Inject;
import org.gluu.oxd.server.service.ConfigurationService;
import org.gluu.oxd.server.service.Rp;

import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */

public class PersistenceServiceImpl implements PersistenceService {

    private ConfigurationService configurationService;
    private SqlPersistenceProvider sqlProvider;
    private PersistenceService persistenceService;

    @Inject
    public PersistenceServiceImpl(SqlPersistenceProvider sqlProvider, ConfigurationService configurationService) {
        this.sqlProvider = sqlProvider;
        this.configurationService = configurationService;
    }

    public void create() {
        persistenceService = createServiceInstance();
        persistenceService.create();
    }

    private PersistenceService createServiceInstance() {
        String storage = configurationService.getConfiguration().getStorage();
        if ("h2".equalsIgnoreCase(storage)) {
            return new SqlPersistenceServiceImpl(sqlProvider);
        } else if ("redis".equalsIgnoreCase(storage)) {
            return new RedisPersistenceService(configurationService.getConfiguration());
        }
        throw new RuntimeException("Failed to create persistence provider. Unrecognized storage specified: " + storage + ", full configuration: " + configurationService.get());
    }

    public boolean create(Rp rp) {
        return persistenceService.create(rp);
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

    public void destroy() {
        persistenceService.destroy();
    }

    @Override
    public boolean remove(String oxdId) {
        return persistenceService.remove(oxdId);
    }
}
