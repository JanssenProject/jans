/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.service.cdi.util.CdiUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class StatusIndexPoolService extends AbstractStatusIndexPoolService<StatusIndexPool> {

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public int getStatusListBitSize() {
        return appConfiguration.getStatusListBitSize();
    }

    @Override
    public Class<StatusIndexPool> getEntityClass() {
        return StatusIndexPool.class;
    }

    @Override
    public void markAllIndexesAsValid(List<Integer> enumerateAllIndexes) {
        // mark all indexes which we are re-using as VALID
        StatusListIndexService indexService = CdiUtil.bean(StatusListIndexService.class);
        indexService.updateStatusAtIndexes(enumerateAllIndexes, TokenStatus.VALID);
    }

    @Override
    public String logPrefix() {
        return "[TokenIndex] - ";
    }

    public String baseDn() {
        return staticConfiguration.getBaseDn().getStatusIndexPool();
    }
}