package io.jans.ca.server.rest;

import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.ApiConf;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

@Path("/health-check")
public class HealthCheckResource {

    @Inject
    Logger logger;
    @Inject
    MainPersistenceService jansConfigurationService;

    private static final String LOG_STATUS = "log_status";
    private static final String CONFIG_STATUS = "config_status";

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        logger.debug("Api Health Check - Entry");
        //validateIpAddressAllowed(httpRequest.getRemoteAddr());
        JSONObject clientApiStatusJson = new JSONObject();
        clientApiStatusJson.put("application", "jans-client-api");
        clientApiStatusJson.put("version", Utils.getJansClientApiVersion());
        clientApiStatusJson.put("status", "running");

        try {
            logger.error("Sample Error Test Log.");
            clientApiStatusJson.put(LOG_STATUS, "ok");
        } catch (Exception e) {
            clientApiStatusJson.put(LOG_STATUS, "fail");
        }

        try {
            ApiConf dbConf = checkDatabaseConnection();
            if (dbConf != null) {
                clientApiStatusJson.put(CONFIG_STATUS, "ok");
            } else {
                clientApiStatusJson.put(CONFIG_STATUS, "config Not Found");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            clientApiStatusJson.put(CONFIG_STATUS, "error Reading");
        }

        logger.debug("Api Health Check - jsonObject:{}", clientApiStatusJson);

        return Response.ok(clientApiStatusJson.toString(3)).build();
    }

    private ApiConf checkDatabaseConnection() {
        return jansConfigurationService.findConf();
    }
}
