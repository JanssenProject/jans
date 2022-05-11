package io.jans.ca.server.rest;

import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.ApiConf;
import io.jans.ca.server.persistence.service.JansConfigurationService;
import io.jans.ca.server.utils.ErrorResponse;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health-check")
public class HealthCheckResource {

    @Inject
    Logger logger;
    @Inject
    JansConfigurationService jansConfigurationService;

    private static final String LOG_STATUS = "log_status";
    private static final String CONFIG_STATUS = "config_status";

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        logger.debug("Api Health Check - Entry");
        //validateIpAddressAllowed(httpRequest.getRemoteAddr());
        JSONObject oxdStatusJson = new JSONObject();
        oxdStatusJson.put("application", "oxd");
        oxdStatusJson.put("version", Utils.getOxdVersion());
        oxdStatusJson.put("status", "running");

        try {
            logger.error(ErrorResponse.DEFAULT_SAMPLE_ERROR.getDescription());
            oxdStatusJson.put(LOG_STATUS, "ok");
        } catch (Exception e) {
            oxdStatusJson.put(LOG_STATUS, "fail");
        }

        try {
            ApiConf dbConf = checkDatabaseConnection();
            if (dbConf != null) {
                oxdStatusJson.put(CONFIG_STATUS, "ok");
            } else {
                oxdStatusJson.put(CONFIG_STATUS, "config Not Found");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            oxdStatusJson.put(CONFIG_STATUS, "error Reading");
        }

        logger.debug("Api Health Check - jsonObject:{}", oxdStatusJson);

        return Response.ok(oxdStatusJson.toString(3)).build();
    }

    private ApiConf checkDatabaseConnection() {
        return jansConfigurationService.findConf();
    }
}
