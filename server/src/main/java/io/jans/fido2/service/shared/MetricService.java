/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.fido2.service.shared;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.fido2.model.conf.AppConfiguration;
import org.gluu.model.ApplicationType;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.service.common.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.metric.inject.ReportMetric;
import org.gluu.service.net.NetworkService;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Named(MetricService.METRIC_SERVICE_COMPONENT_NAME)
public class MetricService extends org.gluu.service.metric.MetricService {
	
	public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";

	private static final long serialVersionUID = 7875838160379126796L;

	@Inject
    private Instance<MetricService> instance;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
    private StaticConfiguration staticConfiguration;

	@Inject
    private NetworkService networkService;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME)
    @ReportMetric
    private PersistenceEntryManager persistenceEntryManager;

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
        return ApplicationType.FIDO2;
    }

    @Override
    public PersistenceEntryManager getEntryManager() {
        return persistenceEntryManager;
    }

	@Override
	public String getNodeIndetifier() {
		return networkService.getMacAdress();
	}

}