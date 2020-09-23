package org.gluu.configapi.rest.resource;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.model.SmtpConfiguration;
import org.gluu.oxauth.service.common.ConfigurationService;
import org.gluu.oxauth.service.common.EncryptionService;
import org.gluu.service.MailService;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.oxauth.persistence.model.configuration.GluuConfiguration;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.SMTP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigSmtpResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    MailService mailService;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getSmtpServerConfiguration() {
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        return Response.ok(Objects.requireNonNullElseGet(smtpConfiguration, SmtpConfiguration::new)).build();
    }

    @POST
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response setupSmtpConfiguration(@Valid SmtpConfiguration smtpConfiguration) throws EncryptionException {
        String password = smtpConfiguration.getPassword();
        if (password != null && !password.isEmpty()) {
            smtpConfiguration.setPassword(encryptionService.encrypt(password));
        }
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        return Response.status(Response.Status.CREATED)
                .entity(configurationService.getConfiguration().getSmtpConfiguration()).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateSmtpConfiguration(@Valid SmtpConfiguration smtpConfiguration) throws EncryptionException {
        String password = smtpConfiguration.getPassword();
        if (password != null && !password.isEmpty()) {
            smtpConfiguration.setPassword(encryptionService.encrypt(password));
        }
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        return Response.ok(configurationService.getConfiguration().getSmtpConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.STATUS)
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response testSmtpConfiguration() throws EncryptionException {
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
    }

    @DELETE
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response removeSmtpConfiguration() {
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        configurationUpdate.setSmtpConfiguration(new SmtpConfiguration());
        configurationService.updateConfiguration(configurationUpdate);
        return Response.noContent().build();
    }

}
