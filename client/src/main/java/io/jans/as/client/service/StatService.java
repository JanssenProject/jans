package io.jans.as.client.service;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface StatService {
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    JsonNode stat(@HeaderParam("Authorization") String authorization, @QueryParam("month") String month);

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    JsonNode stat(@HeaderParam("Authorization") String authorization, @FormParam("month") String month, @FormParam("client_id") String clientId, @FormParam("client_secret") String clientSecret);
}
