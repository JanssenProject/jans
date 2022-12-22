/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.core.interceptor.RequestInterceptor;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.configuration.ConfigurationFactory;

import jakarta.inject.Inject;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@RequestAuditInterceptor
@RequestInterceptor
public class ConfigBaseResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    protected int getMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, ApiConstants.DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }

}
