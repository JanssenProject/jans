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
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = PluginConf.class)), examples = @ExampleObject(name = "Response example", value = "example/plugins/plugin-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.PLUGIN_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getPlugins() {
        return Response.ok(getPluginNames()).build();
    }

    @Operation(summary = "Get plugin by name", description = "Get plugin by name", operationId = "get-plugin-by-name", tags = {
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

        List<PluginConf> plugins = getPluginNames();
        Boolean deployed = false;
        logger.debug("All plugins:{} ", plugins);
        if (StringUtils.isNotBlank(pluginName) && !plugins.isEmpty()) {
            Optional<PluginConf> pluginNameOptional = plugins.stream()
                    .filter(plugin -> pluginName.equalsIgnoreCase(plugin.getName())).findAny();

            logger.debug("pluginNameOptional:{} ", pluginNameOptional);
            if (pluginNameOptional.isPresent()) {
                deployed = true;
            }
        }
        logger.debug("deployed:{} ", deployed);
        return Response.ok(deployed).build();
    }

    private List<PluginConf> getPluginNames() {

        List<PluginConf> plugins = this.authUtil.getPluginConf();
        logger.debug("Config plugins:{} ", plugins);
        List<PluginConf> pluginInfo = new ArrayList<>();
        for (PluginConf pluginConf : plugins) {
            logger.debug("pluginConf:{} ", pluginConf);
            if (StringUtils.isNotBlank(pluginConf.getClassName())) {
                try {
                    logger.debug("pluginConf.getClassName():{} ", pluginConf.getClassName());
                    Class.forName(pluginConf.getClassName());
                    PluginConf conf = new PluginConf();
                    conf.setName(pluginConf.getName());
                    conf.setDescription(pluginConf.getDescription());
                    pluginInfo.add(conf);
                } catch (ClassNotFoundException ex) {
                    logger.error("'{}' plugin not deployed", pluginConf.getName());
                }
            }

        }
        logger.debug("pluginInfo:{} ", pluginInfo);
        return pluginInfo;
    }

}
