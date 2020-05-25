package org.gluu.oxauthconfigapi.rest.ressource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.gluu.oxauthconfigapi.rest.model.ApiHealth;
import org.gluu.oxtrust.service.ClientService;

@Path("/health")
@Produces(APPLICATION_JSON)
public class HealthCheck {

	@Inject
	ClientService clientService;

	@Context
	UriInfo uriInfo;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Return json object containing the state of the api.")
	@APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ApiHealth.class, required = true)))
	public Response health() {
		return Response.ok(new ApiHealth(true, "running-"+clientService.getAllClients().size())).build();
	}

}
