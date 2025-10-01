/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.demo.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@Path("/demo")
@ApplicationScoped
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
