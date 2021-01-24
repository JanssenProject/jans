package io.jans.scim.service.filter;

import io.jans.scim.service.ConfigurationService;

import java.io.IOException;

import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.Priorities;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class EnabledServiceFilter implements ContainerRequestFilter {

	@Inject
	private Logger log;
	
	@Inject
	private ConfigurationService configurationService;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		log.trace("Incoming JAX-RS request");
        if (!configurationService.getConfiguration().isScimEnabled()) {        	
            requestContext.abortWith(Response.status(Response.Status.NOT_FOUND)
                .entity("SCIM is disabled").build());
		}
	}

}
