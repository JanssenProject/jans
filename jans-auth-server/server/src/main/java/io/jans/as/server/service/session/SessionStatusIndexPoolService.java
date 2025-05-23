package io.jans.as.server.service.session;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cluster.AbstractStatusIndexPoolService;
import io.jans.model.token.SessionStatusIndexPool;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.service.cdi.util.CdiUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class SessionStatusIndexPoolService extends AbstractStatusIndexPoolService<SessionStatusIndexPool> {

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public int getStatusListBitSize() {
        return appConfiguration.getSessionStatusListBitSize();
    }

    @Override
    public Class<SessionStatusIndexPool> getEntityClass() {
        return SessionStatusIndexPool.class;
    }

    public String baseDn() {
        return staticConfiguration.getBaseDn().getStatusIndexPool();
    }

    @Override
    public void markAllIndexesAsValid(List<Integer> enumerateAllIndexes) {
        // mark all indexes which we are re-using as VALID
        SessionStatusListIndexService indexService = CdiUtil.bean(SessionStatusListIndexService.class);
        indexService.updateStatusAtIndexes(enumerateAllIndexes, TokenStatus.VALID);
    }

    @Override
    public String logPrefix() {
        return "[SessionIndex] - ";
    }
}
