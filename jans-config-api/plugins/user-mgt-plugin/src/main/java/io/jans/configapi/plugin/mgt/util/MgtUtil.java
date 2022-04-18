/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.mgt.util;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.mgt.model.config.UserMgtConfigSource;
import io.jans.configapi.util.ApiConstants;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class MgtUtil {    
    
    @Inject
    Logger logger;    

    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject 
    UserMgtConfigSource configSource;

    public String getProperty(String propertyName) {
        logger.error("configSource.getValue(propertyName:{} ",configSource.getValue(propertyName));
        return configSource.getValue(propertyName);
    }

    public String getMaxCount() {
        logger.error("default.max.count:{} ",getProperty("default.max.count"));
        return getProperty("default.max.count");
    }
    
    public String getDefaultListSize() {
        logger.error("default.max.count:{} ",getProperty("default.list.size"));
        return (StringHelper.isNotEmpty(getProperty("default.list.size")) ? getProperty("default.list.size") : ApiConstants.DEFAULT_LIST_SIZE);
    }
    
    public String getDefaultListStartIndex() {
        logger.error("default.max.count:{} ",getProperty("default.list.start.index"));
        return (StringHelper.isNotEmpty(getProperty("default.list.start.index")) ? getProperty("default.list.start.index") : ApiConstants.DEFAULT_LIST_SIZE);
    }
    
    public int getRecordMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }
}