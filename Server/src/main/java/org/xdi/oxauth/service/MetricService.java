/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.gluu.model.ApplicationType;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.metric.inject.ReportMetric;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 07/30/2015
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named(MetricService.METRIC_SERVICE_COMPONENT_NAME)
public class MetricService extends org.gluu.service.metric.MetricService {
	
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

	public org.gluu.service.metric.MetricService getMetricServiceInstance() {
		return instance.get();
	}

    @Override
    public boolean isMetricReporterEnabled() {
        if (this.appConfiguration.getMetricReporterEnabled() == null) {
            return false;
        }

        return this.appConfiguration.getMetricReporterEnabled();
    }

    @Override
    public ApplicationType getApplicationType() {
        return ApplicationType.OX_AUTH;
    }

    @Override
    public PersistenceEntryManager getEntryManager() {
        return ldapEntryManager;
    }

}