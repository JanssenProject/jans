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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

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
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    MailService mailService;

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
    public Response getSmtpServerConfiguration() {
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        log.debug(SMTP_CONFIGURATION + ":{}", smtpConfiguration);
        return Response.ok(Objects.requireNonNullElseGet(smtpConfiguration, SmtpConfiguration::new)).build();
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
        String password = smtpConfiguration.getPassword();
        if (password != null && !password.isEmpty()) {
            smtpConfiguration.setPassword(encryptionService.encrypt(password));
        }

        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        log.debug("configurationUpdate:{}", configurationUpdate);
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        return Response.status(Response.Status.CREATED)
                .entity(configurationService.getConfiguration().getSmtpConfiguration()).build();
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
        String password = smtpConfiguration.getPassword();
        if (password != null && !password.isEmpty()) {
            smtpConfiguration.setPassword(encryptionService.encrypt(password));
        }
        log.debug(SMTP_CONFIGURATION + ":{}", smtpConfiguration);
        GluuConfiguration configurationUpdate = configurationService.getConfiguration();
        log.debug("configurationUpdate:{}", configurationUpdate);
        configurationUpdate.setSmtpConfiguration(smtpConfiguration);
        configurationService.updateConfiguration(configurationUpdate);
        return Response.ok(configurationService.getConfiguration().getSmtpConfiguration()).build();
    }

    @Operation(summary = "Test SMTP server configuration", description = "Test SMTP server configuration", operationId = "test-config-smtp", tags = {
            "Configuration – SMTP" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SMTP_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(name = "status", type = "boolean", description = "boolean value true if successful"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.SMTP_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SMTP_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response testSmtpConfiguration() throws EncryptionException {

        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        log.debug("Testing smtpConfiguration:{}", smtpConfiguration);
        smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(smtpConfiguration.getPassword()));
        boolean status = mailService.sendMail(smtpConfiguration, smtpConfiguration.getFromEmailAddress(),
                smtpConfiguration.getFromName(), smtpConfiguration.getFromEmailAddress(), null,
                "SMTP Configuration verification", "Mail to test smtp configuration",
                "Mail to test smtp configuration");
        log.debug("smtpConfiguration test status:{}", status);
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

}
