/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.link.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.link.util.Constants;
import io.jans.configapi.plugin.link.service.JansLinkService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.link.model.config.AppConfiguration;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import org.slf4j.Logger;

@Path(Constants.JANSLINK_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JansLinkConfigResource extends BaseResource {

    private static final String JANSLINK_CONFIGURATION = "jansLinkConfiguration";

    @Inject
    Logger logger;

    @Inject
    JansLinkService jansLinkService;

    @Inject
    private EncryptionService encryptionService;

    @Operation(summary = "Gets Jans Link App configuration.", description = "Gets Jans Link App configuration.", operationId = "get-jans-link-properties", tags = {
            "Jans Link - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANSLINK_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANSLINK_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.JANSLINK_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getJansLinkConfiguration() {
        AppConfiguration appConfiguration = this.jansLinkService.find();
        logger.debug("Jans Link details appConfiguration():{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @Operation(summary = "Updates Jans Link configuration properties.", description = "Updates Jans Link configuration properties.", operationId = "put-jans-link-properties", tags = {
            "Jans Link - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANSLINK_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "JansLinkConfiguration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JansLinkConfiguration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { Constants.JANSLINK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateJansLinkConfiguration(@NotNull AppConfiguration appConfiguration) throws EncryptionException {
        logger.debug("Jans Link details to be updated - appConfiguration:{} ", appConfiguration);
        checkResourceNotNull(appConfiguration, JANSLINK_CONFIGURATION);
        passwordEncryption(appConfiguration);
        this.jansLinkService.merge(appConfiguration);
        appConfiguration = this.jansLinkService.find();
        return Response.ok(appConfiguration).build();
    }

    private AppConfiguration passwordEncryption(AppConfiguration appConfiguration) throws EncryptionException {
        logger.debug("Password  Encryption - appConfiguration:{} ", appConfiguration);

        if (appConfiguration == null) {
            return appConfiguration;
        }

        // inumConfig
        GluuLdapConfiguration config = appConfiguration.getInumConfig();
        passwordEncryption(config);
        appConfiguration.setInumConfig(config);

        // targetConfig
        config = appConfiguration.getTargetConfig();
        passwordEncryption(config);
        appConfiguration.setTargetConfig(config);

        // sourceConfig
        List<GluuLdapConfiguration> sourceConfigList = appConfiguration.getSourceConfigs();
        if (sourceConfigList != null && !sourceConfigList.isEmpty()) {
            for (GluuLdapConfiguration ldapConfig : sourceConfigList) {
                passwordEncryption(ldapConfig);
                appConfiguration.setSourceConfigs(sourceConfigList);
            }
        }
        return appConfiguration;
    }

    private GluuLdapConfiguration passwordEncryption(GluuLdapConfiguration ldapConfiguration)
            throws EncryptionException {
        logger.debug("Password  Encryption - ldapConfiguration:{} ", ldapConfiguration);

        if (ldapConfiguration == null) {
            return ldapConfiguration;
        }

        String password = ldapConfiguration.getBindPassword();
        if (password != null && !password.isEmpty()) {
            try {
                encryptionService.decrypt(password);
            } catch (Exception ex) {
                logger.error("Exception while decryption of ldapConfiguration password hence will encrypt it!!!");
                ldapConfiguration.setBindPassword(encryptionService.encrypt(password));
            }
        }

        return ldapConfiguration;
    }

}