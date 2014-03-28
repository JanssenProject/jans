package org.xdi.oxauth.token.ws.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for validate token REST web services
 *
 * @author Javier Rojas Blum Date: 10.27.2011
 */
@Path("/oxauth")
public interface ValidateTokenRestWebService {

	@GET
	@Path("/validate")
	@Produces({ MediaType.APPLICATION_JSON })
	Response validateAccessToken(@QueryParam("access_token") String accessToken,
			@Context SecurityContext sec);
}