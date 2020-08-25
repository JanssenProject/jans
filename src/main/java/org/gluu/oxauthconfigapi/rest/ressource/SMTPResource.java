/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.gluu.model.SmtpConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.model.GluuConfiguration;
import org.gluu.oxtrust.service.ConfigurationService;
import org.gluu.oxtrust.service.EncryptionService;
import org.gluu.service.MailService;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.SMTP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SMTPResource extends BaseResource {

	@Inject
	private ConfigurationService configurationService;

	@Inject
	EncryptionService encryptionService;

	@Inject
	private MailService mailService;

	@GET
	@Operation(summary = "Retrieve smtp configuration", description = "Retrieve smtp configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SmtpConfiguration.class)), description = "success"),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSmtpServerConfiguration() {
		try {
			SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
			if (smtpConfiguration != null) {
				return Response.ok(smtpConfiguration).build();
			} else {
				return Response.ok(new SmtpConfiguration()).build();
			}
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@PUT
	@Operation(summary = "Update smtp configuration", description = "Update smtp configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SmtpConfiguration.class)), description = "success"
					+ ""),
			@APIResponse(responseCode = "404", description = "Not found"),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSmtpConfiguration(@Valid @NotNull SmtpConfiguration smtpConfiguration) {
		try {
			if (smtpConfiguration.getPassword() != null && !smtpConfiguration.getPassword().isEmpty()) {
				configurationService.encryptedSmtpPassword(smtpConfiguration);
			}
			GluuConfiguration configurationUpdate = configurationService.getConfiguration();
			configurationUpdate.setSmtpConfiguration(smtpConfiguration);
			configurationService.updateConfiguration(configurationUpdate);
			return Response.ok(configurationService.getConfiguration().getSmtpConfiguration()).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@GET
	@Path(ApiConstants.STATUS)
	@Operation(summary = "Test smtp configuration", description = "Test smtp configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SmtpConfiguration.class)), description = "success"),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response testSmtpConfiguration() {
		try {
			SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
			smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(smtpConfiguration.getPassword()));
			boolean result = mailService.sendMail(smtpConfiguration, smtpConfiguration.getFromEmailAddress(),
					smtpConfiguration.getFromName(), smtpConfiguration.getFromEmailAddress(), null,
					"SMTP Configuration verification", "Mail to test smtp configuration",
					"Mail to test smtp configuration");
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add("service", "SMTP SERVER CONFIGURATION TEST");
			builder.add("status", result ? "OKAY" : "FAILED");

			return Response.ok(result ? true : false).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

}
