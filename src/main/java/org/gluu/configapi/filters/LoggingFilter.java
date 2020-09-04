package org.gluu.configapi.filters;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import io.vertx.core.http.HttpServerRequest;

@Provider
public class LoggingFilter implements ContainerRequestFilter {

	@Context
	UriInfo info;

	@Context
	HttpServerRequest request;

	@Inject
	Logger logger;

	public void filter(ContainerRequestContext context) {
		logger.info("***********************************************************************");
		logger.info("****Request " + context.getMethod() + " " + info.getPath() + " from IP "
				+ request.remoteAddress().toString());

	}

}
