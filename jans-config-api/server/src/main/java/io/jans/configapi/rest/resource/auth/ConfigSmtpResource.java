/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SmtpConfiguration;
import io.jans.model.SmtpTest;
import io.jans.service.MailService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Path(ApiConstants.CONFIG + ApiConstants.SMTP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigSmtpResource extends ConfigBaseResource {

    private static final String SMTP_CONFIGURATION = "smtpConfiguration";

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private EncryptionService encryptionService;

    @Inject
    private MailService mailService;

    @Operation(summary = "Returns SMTP server configuration", description = "Returns SMTP server configuration", operationId = "get-config-smtp", tags = {
            "Configuration – SMTP" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SMTP_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/smtp/smtp-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SMTP_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getSmtpServerConfiguration() throws EncryptionException {
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        log.info(SMTP_CONFIGURATION + ":{} from DB", smtpConfiguration);
        decryptPassword(smtpConfiguration);
        log.info(SMTP_CONFIGURATION + ":{} fetched", smtpConfiguration);
        return Response.ok(smtpConfiguration).build();
    }

    @Operation(summary = "Adds SMTP server configuration", description = "Adds SMTP server configuration", operationId = "post-config-smtp", tags = {
            "Configuration – SMTP" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SMTP_WRITE_ACCESS }))
    @RequestBody(description = "SmtpConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/smtp/smtp.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/smtp/smtp-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response setupSmtpConfiguration(@Valid SmtpConfiguration smtpConfiguration) throws EncryptionException {
        log.debug(SMTP_CONFIGURATION + ":{}", smtpConfiguration);
        encryptPassword(smtpConfiguration);
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        log.debug("configurationUpdate:{}", configurationUpdate);
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        decryptPassword(smtpConfiguration);
        log.debug("After creeation " + SMTP_CONFIGURATION + ":{}", smtpConfiguration);
        return Response.status(Response.Status.CREATED).entity(smtpConfiguration).build();
    }

    @Operation(summary = "Updates SMTP server configuration", description = "Updates SMTP server configuration", operationId = "put-config-smtp", tags = {
            "Configuration – SMTP" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SMTP_WRITE_ACCESS }))
    @RequestBody(description = "SmtpConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/smtp/smtp.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/smtp/smtp-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateSmtpConfiguration(@Valid SmtpConfiguration smtpConfiguration) throws EncryptionException {
        log.debug(SMTP_CONFIGURATION + ":{}", smtpConfiguration);
        encryptPassword(smtpConfiguration);
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        log.debug("configurationUpdate:{}", configurationUpdate);
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        decryptPassword(smtpConfiguration);
        log.debug("After update " + SMTP_CONFIGURATION + ":{}", smtpConfiguration);
        return Response.ok(smtpConfiguration).build();
    }

    @Operation(summary = "Signing Test SMTP server configuration", description = "Signing Test SMTP server configuration", operationId = "test-config-smtp", tags = {
    "Configuration – SMTP" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.SMTP_WRITE_ACCESS }))
    @RequestBody(description = "SmtpTest object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpTest.class), examples = @ExampleObject(name = "Request json example", value = "example/auth/smtp/smtp_test.json")))    
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(name = "status", type = "boolean", description = "boolean value true if successful"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.SMTP_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response testSmtpConfiguration(@Valid SmtpTest smtpTest) throws EncryptionException {
        log.debug("smtpTest:{}", smtpTest);
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        smtpConfiguration.setSmtpAuthenticationAccountPasswordDecrypted(encryptionService.decrypt(smtpConfiguration.getSmtpAuthenticationAccountPassword()));
        smtpConfiguration.setKeyStorePasswordDecrypted(encryptionService.decrypt(smtpConfiguration.getKeyStorePassword()));
        boolean status = false;
        if (smtpTest.getSign()) {
            log.debug("smtpTest: trying to send signed email");
            status = mailService.sendMailSigned(smtpConfiguration.getFromEmailAddress(),
                    smtpConfiguration.getFromName(), smtpConfiguration.getFromEmailAddress(), null,
                    smtpTest.getSubject(), smtpTest.getMessage(),
                    smtpTest.getMessage());
        }
        else {
            log.debug("smtpTest: trying to send non-signed email");
            status = mailService.sendMail(smtpConfiguration.getFromEmailAddress(),
                    smtpConfiguration.getFromName(), smtpConfiguration.getFromEmailAddress(), null,
                    smtpTest.getSubject(), smtpTest.getMessage(),
                    smtpTest.getMessage());
        }
        log.info("smtpConfiguration test status:{}", status);
        return Response.ok(status).build();
    }

    @Operation(summary = "Deletes SMTP server configuration", description = "Deletes SMTP server configuration", operationId = "delete-config-smtp", tags = {
            "Configuration – SMTP" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SMTP_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_DELETE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response removeSmtpConfiguration() {
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        configurationUpdate.setSmtpConfiguration(new SmtpConfiguration());
        configurationService.updateConfiguration(configurationUpdate);
        return Response.noContent().build();
    }

    private SmtpConfiguration encryptPassword(SmtpConfiguration smtpConfiguration) throws EncryptionException {
        if (smtpConfiguration == null) {
            return smtpConfiguration;
        }
        String password = smtpConfiguration.getSmtpAuthenticationAccountPassword();
        if (password != null && !password.isEmpty()) {
            try {
                encryptionService.decrypt(password);
            } catch (Exception ex) {
                log.error("Exception while decryption of smtpConfiguration password hence will encrypt it!!!");
                smtpConfiguration.setSmtpAuthenticationAccountPassword(encryptionService.encrypt(password));
            }
        }
        password = smtpConfiguration.getKeyStorePassword();
        if (password != null && !password.isEmpty()) {
            try {
                encryptionService.decrypt(password);
            } catch (Exception ex) {
                log.error("Exception while decryption of smtpConfiguration password hence will encrypt it!!!");
                smtpConfiguration.setKeyStorePassword(encryptionService.encrypt(password));
            }
        }
        return smtpConfiguration;
    }

    private SmtpConfiguration decryptPassword(SmtpConfiguration smtpConfiguration) throws EncryptionException {
        if (smtpConfiguration != null) {
            String password = smtpConfiguration.getSmtpAuthenticationAccountPassword();
            if (password != null && !password.isEmpty()) {
                smtpConfiguration.setSmtpAuthenticationAccountPasswordDecrypted(encryptionService.decrypt(password));
            }
            password = smtpConfiguration.getKeyStorePassword();
            if (password != null && !password.isEmpty()) {
                smtpConfiguration.setKeyStorePasswordDecrypted(encryptionService.decrypt(password));
            }
        } else {
            smtpConfiguration = new SmtpConfiguration();
        }
        return smtpConfiguration;
    }

}
