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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@ApplicationScoped
public class MgtUtil {

    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    UserMgtConfigSource configSource;

    public static final String DATE_PATTERN_YYYY_MM_DD = "yyyy-MM-dd";


    public int getRecordMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }

    public Date parseStringToDateObj(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN_YYYY_MM_DD);

        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            logger.error("Error in parsing string to date. Allowed Date Format : {},  Date-String : {} ", DATE_PATTERN_YYYY_MM_DD, dateString);
        }
        return date;
    }
}