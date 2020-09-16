package org.gluu.configapi.rest.resource;

import com.couchbase.client.core.message.ResponseStatus;
import org.apache.commons.lang.StringUtils;
import org.gluu.configapi.rest.model.Logging;
import org.gluu.configapi.service.ConfigurationService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.configuration.AppConfiguration;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.BASE_API_URL + ApiConstants.LOGGING)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoggingResource {

    @Inject
    ConfigurationService configurationService;

    @GET
    public Response getLogging() {
        Logging logging = new Logging();
        AppConfiguration appConfiguration = configurationService.find();
        logging.setDisableJdkLogger(appConfiguration.getDisableJdkLogger());
        logging.setLoggingLevel(appConfiguration.getLoggingLevel());
        logging.setLoggingLayout(appConfiguration.getLoggingLayout());
        if (appConfiguration.getEnabledOAuthAuditLogging() == null) {
            logging.setEnabledOAuthAuditLogging(false);
        } else {
            logging.setEnabledOAuthAuditLogging(appConfiguration.getEnabledOAuthAuditLogging());
        }
        logging.setHttpLoggingExludePaths(appConfiguration.getHttpLoggingExludePaths());
        logging.setHttpLoggingEnabled(appConfiguration.getHttpLoggingEnabled());
        logging.setExternalLoggerConfiguration(appConfiguration.getExternalLoggerConfiguration());
        return Response.ok(logging).build();
    }

    @PUT
    public Response updateHero(@Valid Logging logging) {
        Conf conf = configurationService.findForAppConfigurationOnly();

        if (!StringUtils.isBlank(logging.getLoggingLevel())) {
            conf.getDynamic().setLoggingLevel(logging.getLoggingLevel());
        }
        if (!StringUtils.isBlank(logging.getLoggingLayout())) {
            conf.getDynamic().setLoggingLayout(logging.getLoggingLevel());
        }
        if (!StringUtils.isBlank(logging.getExternalLoggerConfiguration())) {
            conf.getDynamic().setExternalLoggerConfiguration(logging.getExternalLoggerConfiguration());
        }
        conf.getDynamic().setEnabledOAuthAuditLogging(logging.isEnabledOAuthAuditLogging());
        conf.getDynamic().setDisableJdkLogger(logging.isDisableJdkLogger());
        conf.getDynamic().setHttpLoggingEnabled(logging.isHttpLoggingEnabled());

        configurationService.merge(conf);
        return Response.ok(ResponseStatus.SUCCESS).build();
    }

}
