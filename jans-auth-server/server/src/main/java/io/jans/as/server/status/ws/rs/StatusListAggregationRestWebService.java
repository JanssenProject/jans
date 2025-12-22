package io.jans.as.server.status.ws.rs;

import io.jans.as.server.service.DiscoveryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Path("/")
public class StatusListAggregationRestWebService {

    @Inject
    private Logger log;

    @Inject
    private DiscoveryService discoveryService;

    @GET
    @Path("/status_list_aggregation")
    @Produces({"application/json"})
    public Response requestStatusList() {
        try {
            JSONArray array = new JSONArray();
            array.put(discoveryService.getStatusListEndpoint());

            JSONObject json = new JSONObject();
            json.put("status_lists", array);

            return Response.status(Response.Status.OK)
                    .entity(json.toString())
                    .type("application/json")
                    .build();
        } catch (WebApplicationException e) {
            log.debug(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
