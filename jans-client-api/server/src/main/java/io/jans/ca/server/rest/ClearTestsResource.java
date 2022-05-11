package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.EmptyParams;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class ClearTestsResource extends BaseResource {

    @GET
    @Path("/clear-tests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearTests() {
        logger.info("Api Resource: clear-tests");
        String result = process(CommandType.CLEAR_TESTS, null, EmptyParams.class, null, null);
        logger.info("Api Resource: clear-tests - result:{}", result);
        if (result.contains("FAIL")) {
            return Response.status(500, "Failed to clear test").build();
        } else {
            return Response.ok(result).build();
        }
    }
}
