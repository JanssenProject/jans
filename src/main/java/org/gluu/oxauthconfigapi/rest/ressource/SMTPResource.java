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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.SMTP)
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

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response setupSmtpConfiguration(@Valid @NotNull SmtpConfiguration smtpConfiguration) {
		try {
			String password = smtpConfiguration.getPassword();
			if (password != null && !password.isEmpty()) {
				smtpConfiguration.setPassword(encryptionService.encrypt(password));
				password = null;
			}
			GluuConfiguration configurationUpdate = configurationService.getConfiguration();
			configurationUpdate.setSmtpConfiguration(smtpConfiguration);
			configurationService.updateConfiguration(configurationUpdate);
			return Response.status(Response.Status.CREATED)
					.entity(configurationService.getConfiguration().getSmtpConfiguration()).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSmtpConfiguration(@Valid @NotNull SmtpConfiguration smtpConfiguration) {
		try {
			String password = smtpConfiguration.getPassword();
			if (password != null && !password.isEmpty()) {
				smtpConfiguration.setPassword(encryptionService.encrypt(password));
				password = null;
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
			return Response.ok(builder.build()).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@DELETE
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response removeSmtpConfiguration() {
		try {
			GluuConfiguration configurationUpdate = configurationService.getConfiguration();
			configurationUpdate.setSmtpConfiguration(new SmtpConfiguration());
			configurationService.updateConfiguration(configurationUpdate);
			return Response.noContent().build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

}
