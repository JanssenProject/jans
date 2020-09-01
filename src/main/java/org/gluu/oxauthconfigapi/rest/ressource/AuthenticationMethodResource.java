package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.AuthenticationMethod;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.ConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.ACRS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationMethodResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	private ConfigurationService configurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getDefaultAuthenticationMethod() {
		AuthenticationMethod authenticationMethod = new AuthenticationMethod();
		authenticationMethod.setDefaultAcr(this.configurationService.getConfiguration().getAuthenticationMode());
		authenticationMethod.setOxtrustAcr(this.configurationService.getConfiguration().getOxTrustAuthenticationMode());
		return Response.ok(authenticationMethod).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateDefaultAuthenticationMethod(@Valid AuthenticationMethod authenticationMethod) {
		this.configurationService.getConfiguration().setAuthenticationMode(authenticationMethod.getDefaultAcr());
		this.configurationService.getConfiguration().setOxTrustAuthenticationMode(authenticationMethod.getOxtrustAcr());
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}