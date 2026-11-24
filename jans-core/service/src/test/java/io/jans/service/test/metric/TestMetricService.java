/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.metric;

import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.MetricService;

/**
 * {@link MetricService} with fixed values and no CDI, for tests of metric aggregation.
 */
public class TestMetricService extends MetricService {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_BASE_DN = "ou=statistic,o=metric";

    private final transient PersistenceEntryManager entryManager;
    private final String baseDn;
    private final String applicationType;
    private final String nodeIdentifier;
    private volatile boolean reporterEnabled = true;

    public TestMetricService(PersistenceEntryManager entryManager, String applicationType, String nodeIdentifier) {
        this(entryManager, DEFAULT_BASE_DN, applicationType, nodeIdentifier);
    }

    public TestMetricService(PersistenceEntryManager entryManager, String baseDn, String applicationType, String nodeIdentifier) {
        this.entryManager = entryManager;
        this.baseDn = baseDn;
        this.applicationType = applicationType;
        this.nodeIdentifier = nodeIdentifier;
    }

    public void setReporterEnabled(boolean reporterEnabled) {
        this.reporterEnabled = reporterEnabled;
    }

    @Override
    public String baseDn() {
        return baseDn;
    }

    @Override
    public MetricService getMetricServiceInstance() {
        return this;
    }

    @Override
    public boolean isMetricReporterEnabled() {
        return reporterEnabled;
    }

    @Override
    public String getString() {
        return applicationType;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    @Override
    public PersistenceEntryManager getEntryManager() {
        return entryManager;
    }

}
