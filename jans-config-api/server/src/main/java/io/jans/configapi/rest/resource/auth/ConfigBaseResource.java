/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.configuration.ConfigurationFactory;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class ConfigBaseResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    protected static final String READ_ACCESS = "config-api-read";
    protected static final String WRITE_ACCESS = "config-api-write";
    protected static final String DEFAULT_LIST_SIZE = ApiConstants.DEFAULT_LIST_SIZE;
    // Pagination
    protected static final String DEFAULT_LIST_START_INDEX = ApiConstants.DEFAULT_LIST_START_INDEX;
    protected static final int DEFAULT_MAX_COUNT = ApiConstants.DEFAULT_MAX_COUNT;

    protected int getMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : DEFAULT_MAX_COUNT);
    }

}
