/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SmtpConfiguration;
import io.jans.service.MailService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Path(ApiConstants.CONFIG + ApiConstants.SMTP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigSmtpResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    MailService mailService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_READ_ACCESS })
    public Response getSmtpServerConfiguration() {
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        log.debug("getSmtpServerConfiguration() - smtpConfiguration = " + smtpConfiguration + "\n\n");
        return Response.ok(Objects.requireNonNullElseGet(smtpConfiguration, SmtpConfiguration::new)).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_WRITE_ACCESS })
    public Response setupSmtpConfiguration(@Valid SmtpConfiguration smtpConfiguration) throws EncryptionException {
        log.debug("setupSmtpConfiguration() - 1 - smtpConfiguration = " + smtpConfiguration + "\n\n");
        String password = smtpConfiguration.getPassword();
        if (password != null && !password.isEmpty()) {
            smtpConfiguration.setPassword(encryptionService.encrypt(password));
        }
        log.debug("setupSmtpConfiguration() - 2 - smtpConfiguration = " + smtpConfiguration + "\n\n");
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        log.debug("setupSmtpConfiguration() - 1 - configurationUpdate = " + configurationUpdate + "\n\n");
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        return Response.status(Response.Status.CREATED)
                .entity(configurationService.getConfiguration().getSmtpConfiguration()).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_WRITE_ACCESS })
    public Response updateSmtpConfiguration(@Valid SmtpConfiguration smtpConfiguration) throws EncryptionException {
        log.debug("updateSmtpConfiguration() - 1 - smtpConfiguration = " + smtpConfiguration + "\n\n");
        String password = smtpConfiguration.getPassword();
        if (password != null && !password.isEmpty()) {
            smtpConfiguration.setPassword(encryptionService.encrypt(password));
        }
        log.debug("updateSmtpConfiguration() - 2 - smtpConfiguration = " + smtpConfiguration + "\n\n");
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        log.debug("updateSmtpConfiguration() - 1 - configurationUpdate = " + configurationUpdate + "\n\n");
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        return Response.ok(configurationService.getConfiguration().getSmtpConfiguration()).build();
    }

    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_READ_ACCESS })
    public Response testSmtpConfiguration() throws EncryptionException {
        log.debug("\n ConfigSmtpResource::testSmtpConfiguration() - 1 - \n\n");
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        log.debug("\n ConfigSmtpResource::testSmtpConfiguration() - 1 - smtpConfiguration = " + smtpConfiguration
                + "\n\n");
        smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(smtpConfiguration.getPassword()));
        boolean status = mailService.sendMail(smtpConfiguration, smtpConfiguration.getFromEmailAddress(),
                smtpConfiguration.getFromName(), smtpConfiguration.getFromEmailAddress(), null,
                "SMTP Configuration verification", "Mail to test smtp configuration",
                "Mail to test smtp configuration");
        log.debug("\n ConfigSmtpResource::testSmtpConfiguration() - 2 - status = " + status + "\n\n");
        return Response.ok(status).build();
    }

    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_DELETE_ACCESS })
    public Response removeSmtpConfiguration() {
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        configurationUpdate.setSmtpConfiguration(new SmtpConfiguration());
        configurationService.updateConfiguration(configurationUpdate);
        return Response.noContent().build();
    }

}
