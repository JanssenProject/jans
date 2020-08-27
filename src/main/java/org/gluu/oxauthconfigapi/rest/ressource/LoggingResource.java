package org.gluu.oxauthconfigapi.rest.ressource;

import com.couchbase.client.core.message.ResponseStatus;
import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.rest.model.Logging;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path(ApiConstants.BASE_API_URL + ApiConstants.LOGGING)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoggingResource {

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@Operation(summary = "Retrieve logging settings")
	@APIResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Logging.class, required = true)))
	public Response getLogging() {
		try {
			Logging logging = new Logging();
			AppConfiguration appConfiguration = jsonConfigurationService.getOxauthAppConfiguration();
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
		} catch (IOException e) {
			return Response.ok(ResponseStatus.INTERNAL_ERROR).build();
		}

	}

	@PUT
	@Operation(summary = "Update logging settings")
	public Response updateHero(@Valid Logging logging) {
		AppConfiguration appConfiguration;
		try {
			appConfiguration = jsonConfigurationService.getOxauthAppConfiguration();
			if (!StringUtils.isBlank(logging.getLoggingLevel())) {
				appConfiguration.setLoggingLevel(logging.getLoggingLevel());
			}
			if (!StringUtils.isBlank(logging.getLoggingLayout())) {
				appConfiguration.setLoggingLayout(logging.getLoggingLevel());
			}
			if (!StringUtils.isBlank(logging.getExternalLoggerConfiguration())) {
				appConfiguration.setExternalLoggerConfiguration(logging.getExternalLoggerConfiguration());
			}
			appConfiguration.setEnabledOAuthAuditLogging(logging.isEnabledOAuthAuditLogging());
			appConfiguration.setDisableJdkLogger(logging.isDisableJdkLogger());
			appConfiguration.setHttpLoggingEnabled(logging.isHttpLoggingEnabled());
			jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			return Response.ok(ResponseStatus.SUCCESS).build();
		} catch (IOException e) {
			return Response.ok(ResponseStatus.INTERNAL_ERROR).build();
		}

	}

}
