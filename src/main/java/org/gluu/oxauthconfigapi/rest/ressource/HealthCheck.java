package org.gluu.oxauthconfigapi.rest.ressource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.gluu.oxauthconfigapi.rest.model.ApiHealth;
import org.gluu.oxtrust.service.ClientService;

import com.couchbase.client.core.message.ResponseStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/health")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class HealthCheck {

	@Inject
	ClientService clientService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Return json object containing the state of the api.")
	@APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ApiHealth.class, required = true)))
	public Response health() {
		try {
			return Response.ok(new ObjectMapper().writeValueAsString(new ApiHealth(true, "running"))).build();
		} catch (JsonProcessingException e) {
			return Response.ok(ResponseStatus.INTERNAL_ERROR).build();
		}
	}

}
