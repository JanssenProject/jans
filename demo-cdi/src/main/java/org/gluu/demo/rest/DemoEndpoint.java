package org.gluu.demo.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@Path("/demo")
public class DemoEndpoint {

	@Inject
	private Logger log;

	@GET
	@Path("/status")
	public Response listClients() {
		log.info("Get request to demo endpoint");
		return Response.ok("success").build();
	}

}
