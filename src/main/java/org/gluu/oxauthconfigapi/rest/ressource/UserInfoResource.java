/**
 * User Info configuration endpoint 
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
import org.gluu.oxauthconfigapi.rest.model.UserInfo;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.USER_INFO)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserInfoResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUserInfoConfiguration() throws IOException {
		UserInfo userInfo = new UserInfo();
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		userInfo.setUserInfoSigningAlgValuesSupported(appConfiguration.getUserInfoSigningAlgValuesSupported());
		userInfo.setUserInfoEncryptionAlgValuesSupported(appConfiguration.getUserInfoEncryptionAlgValuesSupported());
		userInfo.setUserInfoEncryptionEncValuesSupported(appConfiguration.getUserInfoEncryptionEncValuesSupported());

		return Response.ok(userInfo).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUserInfoConfiguration(@Valid UserInfo userInfo) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();

		appConfiguration.setUserInfoSigningAlgValuesSupported(userInfo.getUserInfoSigningAlgValuesSupported());
		appConfiguration.setUserInfoEncryptionAlgValuesSupported(userInfo.getUserInfoEncryptionAlgValuesSupported());
		appConfiguration.setUserInfoEncryptionEncValuesSupported(userInfo.getUserInfoEncryptionEncValuesSupported());

		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
