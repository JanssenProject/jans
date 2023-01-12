/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.PluginConf;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AuthUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.PLUGIN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PluginResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Operation(summary = "Gets list of Plugins", description = "Gets list of Plugins", operationId = "get-plugins", tags = {
            "Plugins" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.PLUGIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = PluginConf.class)), examples = @ExampleObject(name = "Response example", value = "example/plugins/plugins-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.PLUGIN_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getPlugins() {
        return Response.ok(getPluginNames()).build();
    }

    @Operation(summary = "Gets list of Plugins", description = "Gets list of Plugins", operationId = "get-plugins", tags = {
            "Plugins" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.PLUGIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.PLUGIN_NAME_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.PLUGIN_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response isPluginDeployed(
            @Parameter(description = "Plugin name") @NotNull @PathParam(ApiConstants.PLUGIN_NAME) String pluginName) {

        List<String> plugins = getPluginNames();
        Boolean deployed = false;
        logger.debug("plugins:{} ", plugins);
        if (StringUtils.isNotBlank(pluginName) && !plugins.isEmpty()) {
            Optional<String> pluginNameOptional = plugins.stream().findAny()
                    .filter(name -> pluginName.equalsIgnoreCase(name));
            logger.debug("pluginNameOptional:{} ", pluginNameOptional);
            if (pluginNameOptional.isPresent()) {
                deployed = true;
            }
        }
        logger.debug("deployed:{} ", deployed);
        return Response.ok(deployed).build();
    }

    private List<String> getPluginNames() {

        List<PluginConf> plugins = this.authUtil.getPluginConf();

        logger.debug("plugins:{} ", plugins);

        List<String> pluginNames = new ArrayList<>();
        for (PluginConf pluginConf : plugins) {
            logger.debug("pluginConf:{} ", pluginConf);
            if (StringUtils.isNotBlank(pluginConf.getClassName())) {
                try {
                    Class.forName(pluginConf.getClassName());
                    pluginNames.add(pluginConf.getName());
                } catch (ClassNotFoundException ex) {
                    logger.error("'{}` plugin not deployed", pluginConf.getName());
                }
            }

        }
        logger.debug("pluginNames:{} ", pluginNames);
        return pluginNames;
    }

}
