package org.gluu.oxauthconfigapi.rest.ressource;

import com.couchbase.client.core.message.ResponseStatus;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.SubjectConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Puja Sharma
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.SUBJECT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubjectConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	public Response getSubjectConfiguration() {
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			SubjectConfiguration subjectConfiguration = new SubjectConfiguration();
			subjectConfiguration.setSubjectTypesSupported(appConfiguration.getSubjectTypesSupported());
			subjectConfiguration.setShareSubjectIdBetweenClientsWithSameSectorId(
					appConfiguration.isShareSubjectIdBetweenClientsWithSameSectorId());
			return Response.ok(subjectConfiguration).build();
		} catch (Exception ex) {
			log.error("Failed to retrieve subject configuration properties.", ex);
			return getInternalServerError(ex);
		}
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSubjectConfiguration(@Valid SubjectConfiguration subjectConfiguration) {
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setSubjectTypesSupported(subjectConfiguration.getSubjectTypesSupported());
			appConfiguration.setShareSubjectIdBetweenClientsWithSameSectorId(
					subjectConfiguration.getShareSubjectIdBetweenClientsWithSameSectorId());
			// Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			return Response.ok(ResponseStatus.SUCCESS).build();
		} catch (Exception ex) {
			log.error("Failed to update subject configuration properties.", ex);
			return getInternalServerError(ex);
		}
	}
}
