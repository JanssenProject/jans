/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.net;


import java.util.HashMap;
import java.util.Map;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.ConnectionServiceConfiguration;
import io.jans.service.net.BaseHttpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with http/https requests
 *
 * @author Yuriy Movchan Date: 04/10/2023
 */
@ApplicationScoped
public class HttpService2 extends BaseHttpService {

	@Inject
	private AppConfiguration appConfiguration;
	
	public Map<String, Integer> getApplicationConnectionProperties() {
		ConnectionServiceConfiguration connectionServiceConfiguration = appConfiguration.getConnectionServiceConfiguration();
		if (connectionServiceConfiguration == null) {
			return null;
		}

		Map<String, Integer> conf = new HashMap<String, Integer>();
		conf.put(HTTPCLIENT_MAX_TOTAL, connectionServiceConfiguration.getMaxTotal());
		conf.put(HTTPCLIENT_MAX_PER_ROUTE, connectionServiceConfiguration.getMaxPerRoute());
		conf.put(HTTPCLIENT_VALIDATE_AFTER_INACTIVITY, connectionServiceConfiguration.getValidateAfterInactivity());

		return conf;
    }
}
