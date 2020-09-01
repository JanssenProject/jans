/**
 * UMA configuration endpoint
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.UmaConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.UMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UmaConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUMAConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		UmaConfiguration umaConfiguration = new UmaConfiguration();
		umaConfiguration.setUmaConfigurationEndpoint(appConfiguration.getUmaConfigurationEndpoint());
		umaConfiguration.setUmaRptLifetime(appConfiguration.getUmaRptLifetime());
		umaConfiguration.setUmaTicketLifetime(appConfiguration.getUmaTicketLifetime());
		umaConfiguration.setUmaPctLifetime(appConfiguration.getUmaPctLifetime());
		umaConfiguration.setUmaResourceLifetime(appConfiguration.getUmaResourceLifetime());
		umaConfiguration.setUmaAddScopesAutomatically(appConfiguration.getUmaAddScopesAutomatically());
		umaConfiguration.setUmaValidateClaimToken(appConfiguration.getUmaValidateClaimToken());
		umaConfiguration.setUmaGrantAccessIfNoPolicies(appConfiguration.getUmaGrantAccessIfNoPolicies());
		umaConfiguration
				.setUmaRestrictResourceToAssociatedClient(appConfiguration.getUmaRestrictResourceToAssociatedClient());
		umaConfiguration.setUmaRptAsJwt(appConfiguration.getUmaRptAsJwt());
		return Response.ok(umaConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUMAConfiguration(@Valid UmaConfiguration umaConfiguration) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setUmaConfigurationEndpoint(umaConfiguration.getUmaConfigurationEndpoint());
		appConfiguration.setUmaRptLifetime(umaConfiguration.getUmaRptLifetime());
		appConfiguration.setUmaTicketLifetime(umaConfiguration.getUmaTicketLifetime());
		appConfiguration.setUmaPctLifetime(umaConfiguration.getUmaPctLifetime());
		appConfiguration.setUmaResourceLifetime(umaConfiguration.getUmaResourceLifetime());
		appConfiguration.setUmaAddScopesAutomatically(umaConfiguration.getUmaAddScopesAutomatically());
		appConfiguration.setUmaValidateClaimToken(umaConfiguration.getUmaValidateClaimToken());
		appConfiguration.setUmaGrantAccessIfNoPolicies(umaConfiguration.getUmaGrantAccessIfNoPolicies());
		appConfiguration
				.setUmaRestrictResourceToAssociatedClient(umaConfiguration.getUmaRestrictResourceToAssociatedClient());
		appConfiguration.setUmaRptAsJwt(umaConfiguration.getUmaRptAsJwt());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}
}
