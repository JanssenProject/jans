/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 07/30/2015
 */
@Scope(ScopeType.APPLICATION)
@Name(MetricService.METRIC_SERVICE_COMPONENT_NAME)
@Startup
public class MetricService extends org.xdi.service.metric.MetricService {
	
	public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";

	private static final long serialVersionUID = 7875838160379126796L;

	@Logger
    private Log log;

	@In
    private ApplianceService applianceService;

	@In
	private ConfigurationFactory configurationFactory;

    @Create
    public void create() {
    	init(configurationFactory.getConfiguration().getMetricReporterInterval());
    }

	@Override
	public String baseDn() {
		return ConfigurationFactory.instance().getBaseDn().getMetric();
	}

	@Override
	public String applianceInum() {
		return applianceService.getApplianceInum();
	}

	@Override
	public String getComponentName() {
		return METRIC_SERVICE_COMPONENT_NAME;
	}

    public static MetricService instance() {
        return (MetricService) Component.getInstance(MetricService.class);
    }

}