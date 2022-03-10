package io.jans.configapi.security.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@ApplicationScoped
public interface StatClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response statGet(@HeaderParam("Authorization") String authorization, @QueryParam("month") String month,
            @QueryParam("format") String format);

}
