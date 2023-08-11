package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.session.DeviceSession;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Named
@ApplicationScoped
public class DeviceSessionService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public String buildDn(String id) {
        return String.format("jansId=%s,%s", id, staticConfiguration.getBaseDn().getSessions());
    }

    public DeviceSession getDeviceSessionByDn(String dn) {
        try {
            return persistenceEntryManager.find(DeviceSession.class, dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    public DeviceSession getDeviceSession(String id) {
        if (StringUtils.isNotBlank(id)) {
            DeviceSession result = getDeviceSessionByDn(buildDn(id));
            log.debug("Found {} entries for deviceSession id = {}", result != null ? 1 : 0, id);

            return result;
        }
        return null;
    }

    public void persist(DeviceSession entity) {
        persistenceEntryManager.persist(entity);
    }

    public void merge(DeviceSession entity) {
        persistenceEntryManager.merge(entity);
    }
}
