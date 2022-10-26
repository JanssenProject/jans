/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.ApplicationType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.service.net.NetworkService;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 07/30/2015
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named(MetricService.METRIC_SERVICE_COMPONENT_NAME)
public class MetricService extends io.jans.service.metric.MetricService {

    public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";

    private static final long serialVersionUID = 7875838160379126796L;

    @Inject
    private Instance<MetricService> instance;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private NetworkService networkService;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME)
    @ReportMetric
    private PersistenceEntryManager ldapEntryManager;

    public void initTimer() {
        initTimer(this.appConfiguration.getMetricReporterInterval(), this.appConfiguration.getMetricReporterKeepDataDays());
    }

    @Override
    public String baseDn() {
        return staticConfiguration.getBaseDn().getMetric();
    }

    public io.jans.service.metric.MetricService getMetricServiceInstance() {
        return instance.get();
    }

    @Override
    public boolean isMetricReporterEnabled() {
        return appConfiguration.isFeatureEnabled(FeatureFlagType.METRIC);
    }

    @Override
    public ApplicationType getApplicationType() {
        return ApplicationType.OX_AUTH;
    }

    @Override
    public PersistenceEntryManager getEntryManager() {
        return ldapEntryManager;
    }

    @Override
    public String getNodeIndetifier() {
        return networkService.getMacAdress();
    }

}