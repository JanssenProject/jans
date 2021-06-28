/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.rest.model.Logging;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@Path(ApiConstants.LOGGING)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoggingResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.LOGGING_READ_ACCESS })
    public Response getLogging() {
        return Response.ok(this.getLoggingConfiguration()).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.LOGGING_WRITE_ACCESS })
    public Response updateLogConf(@Valid Logging logging) {
        log.debug("LOGGING configuration to be updated -logging = " + logging);
        Conf conf = configurationService.findConf();

        if (!StringUtils.isBlank(logging.getLoggingLevel())) {
            conf.getDynamic().setLoggingLevel(logging.getLoggingLevel());
        }
        if (!StringUtils.isBlank(logging.getLoggingLayout())) {
            conf.getDynamic().setLoggingLayout(logging.getLoggingLayout());
        }

        conf.getDynamic().setHttpLoggingEnabled(logging.isHttpLoggingEnabled());
        conf.getDynamic().setDisableJdkLogger(logging.isDisableJdkLogger());
        conf.getDynamic().setEnabledOAuthAuditLogging(logging.isEnabledOAuthAuditLogging());

        if (!StringUtils.isBlank(logging.getExternalLoggerConfiguration())) {
            conf.getDynamic().setExternalLoggerConfiguration(logging.getExternalLoggerConfiguration());
        }
        conf.getDynamic().setHttpLoggingExludePaths(logging.getHttpLoggingExludePaths());

        configurationService.merge(conf);

        logging = this.getLoggingConfiguration();
        return Response.ok(logging).build();
    }

    private Logging getLoggingConfiguration() {
        Logging logging = new Logging();
        AppConfiguration appConfiguration = configurationService.find();

        logging.setLoggingLevel(appConfiguration.getLoggingLevel());
        logging.setLoggingLayout(appConfiguration.getLoggingLayout());
        logging.setHttpLoggingEnabled(appConfiguration.getHttpLoggingEnabled());
        logging.setDisableJdkLogger(appConfiguration.getDisableJdkLogger());
        if (appConfiguration.getEnabledOAuthAuditLogging() == null) {
            logging.setEnabledOAuthAuditLogging(false);
        } else {
            logging.setEnabledOAuthAuditLogging(appConfiguration.getEnabledOAuthAuditLogging());
        }
        logging.setExternalLoggerConfiguration(appConfiguration.getExternalLoggerConfiguration());
        logging.setHttpLoggingExludePaths(appConfiguration.getHttpLoggingExludePaths());
        return logging;
    }

}
