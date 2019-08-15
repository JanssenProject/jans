package org.gluu.demo.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/demo")
public class DemoEndpoint {

	@GET
	@Path("/status")
	public Response listClients() {
		return Response.ok("success").build();
	}

}
