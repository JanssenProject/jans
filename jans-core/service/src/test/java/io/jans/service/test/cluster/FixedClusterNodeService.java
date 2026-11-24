/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.cluster;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.LoggerFactory;

import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cluster.ClusterNodeService;

/**
 * {@link ClusterNodeService} with fixed settings and an explicit entry manager, for tests without CDI.
 * Records the DN of every insert that {@code allocate()} attempts.
 */
public class FixedClusterNodeService extends ClusterNodeService {

    private final String baseDn;
    private final String type;
    private final String lockKey;
    private final List<String> persistedDns = new CopyOnWriteArrayList<>();

    public FixedClusterNodeService(PersistenceEntryManager entryManager, String baseDn, String type, String lockKey) {
        this.baseDn = baseDn;
        this.type = type;
        this.lockKey = lockKey;
        // The base class gets both from CDI and has no setters
        setBaseField("entryManager", entryManager);
        setBaseField("log", LoggerFactory.getLogger(FixedClusterNodeService.class));
    }

    private void setBaseField(String name, Object value) {
        try {
            Field field = ClusterNodeService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to set field '" + name + "' of ClusterNodeService", ex);
        }
    }

    @Override
    protected void persist(ClusterNode clusterNode) {
        persistedDns.add(clusterNode.getDn());
        super.persist(clusterNode);
    }

    /**
     * @return the DNs of all inserts attempted so far, in order
     */
    public List<String> getPersistedDns() {
        return new ArrayList<>(persistedDns);
    }

    @Override
    public String getLockKey() {
        return lockKey;
    }

    @Override
    public String getClusterNodeType() {
        return type;
    }

    @Override
    public String getBaseClusterNodeDn() {
        return baseDn;
    }

}
