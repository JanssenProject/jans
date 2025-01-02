/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.ads.model.Deployment;
import io.jans.config.GluuConfiguration;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.AgamaDeploymentsService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.LdapConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.model.PagedResult;
import io.jans.service.custom.CustomScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * @author Puja Sharma
 */
@Path(ApiConstants.ACRS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AcrsResource extends ConfigBaseResource {

    private static final String AGAMA_PREFIX = "agama_";
    private static final String AGAMA_PREFIX_SEPERATOR = "_";

    @Inject
    Logger log;

    @Inject
    private ApiAppConfiguration appConfiguration;

    @Inject
    ConfigurationService configurationService;

    @Inject
    CustomScriptService customScriptService;

    @Inject
    AgamaDeploymentsService agamaDeploymentsService;

    @Inject
    LdapConfigurationService ldapConfigurationService;

    @Operation(summary = "Gets default authentication method.", description = "Gets default authentication method.", operationId = "get-acrs", tags = {
            "Default Authentication Method" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ACRS_READ_ACCESS, ApiAccessConstants.ACRS_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class), examples = @ExampleObject(name = "Response example", value = "example/acr/acr.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.ACRS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getDefaultAuthenticationMethod() {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        AuthenticationMethod authenticationMethod = new AuthenticationMethod();
        authenticationMethod.setDefaultAcr(gluuConfiguration.getAuthenticationMode());
        return Response.ok(authenticationMethod).build();
    }

    @Operation(summary = "Updates default authentication method.", description = "Updates default authentication method.", operationId = "put-acrs", tags = {
            "Default Authentication Method" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ACRS_WRITE_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class), examples = @ExampleObject(name = "Request json example", value = "example/acr/acr.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "Unauthorized"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_WRITE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateDefaultAuthenticationMethod(@NotNull AuthenticationMethod authenticationMethod) {
        log.info("ACRS details to  update - authenticationMethod:{}", authenticationMethod);

        if (authenticationMethod == null || StringUtils.isBlank(authenticationMethod.getDefaultAcr())) {
            throwBadRequestException("Default authentication method should not be null or empty !");
        }

        if (authenticationMethod != null && StringUtils.isNotBlank(authenticationMethod.getDefaultAcr())) {
            validateAuthenticationMethod(authenticationMethod.getDefaultAcr());

            final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();
            gluuConfiguration.setAuthenticationMode(authenticationMethod.getDefaultAcr());
            configurationService.merge(gluuConfiguration);
        }
        return Response.ok(authenticationMethod).build();
    }

    private void validateAuthenticationMethod(String authenticationMode) {
        log.debug("authenticationMethod:{}, appConfiguration.isAcrValidationEnabled():{}", authenticationMode,
                appConfiguration.isAcrValidationEnabled());

        // if authentication validation check is enabled then validate
        boolean isAcrValid = isAcrValid(authenticationMode);
        log.debug("isAcrValid:{}", isAcrValid);
        if (appConfiguration.isAcrValidationEnabled() && (!isAcrValid)) {
            throwBadRequestException("INVALID_ACR",
                    String.format("Authentication script {%s} is not valid/active", authenticationMode));

        }
    }

    private boolean isAcrValid(String authenticationMode) {
        boolean isValid = false;
        log.info(" Validate ACR being set - authenticationMethod:{}, appConfiguration.getAcrExclusionList():{}",
                authenticationMode, appConfiguration.getAcrExclusionList());

        if (appConfiguration.getAcrExclusionList() != null
                && appConfiguration.getAcrExclusionList().contains(authenticationMode)) {
            return true;
        }

        // Agama Flow
        if (StringUtils.isNotBlank(authenticationMode) && authenticationMode.startsWith(AGAMA_PREFIX)) {
            log.debug(" Agama authenticationMethod provided.");
            return isValidAgamaDeployment(authenticationMode);
        }

        List<GluuLdapConfiguration> ldapConfigurations = ldapConfigurationService.findLdapConfigurations();
        log.debug(" ldapConfigurations:{}", ldapConfigurations);
        if (ldapConfigurations != null && !ldapConfigurations.isEmpty()) {
            Optional<GluuLdapConfiguration> matchingLdapConfiguration = ldapConfigurations.stream()
                    .filter(d -> d.getConfigId().equals(authenticationMode)).findFirst();

            if (matchingLdapConfiguration.isPresent()) {
                GluuLdapConfiguration ldap = matchingLdapConfiguration.get();
                if (ldap != null) {
                    return true;
                }
            }

        }

        // if ACR being set is a script then it should be active
        CustomScript script = customScriptService.getScriptByDisplayName(authenticationMode);
        log.debug(" CustomScript:{}", script);
        if (script != null && script.isEnabled()) {
            log.debug(" script:{}, script.isEnabled():{}", script, script.isEnabled());
            return true;
        }
        log.debug(" isValid:{}", isValid);

        return isValid;
    }

    public boolean isValidAgamaDeployment(String authenticationMode) {
        boolean isValid = false;
        log.info(" Validate Agama ACR - authenticationMode:{},", authenticationMode);
        if (StringUtils.isBlank(authenticationMode)) {
            return isValid;
        }

        // Get deployed agama projects
        PagedResult<Deployment> deploymentPagedResult = agamaDeploymentsService.list(0, 0, getMaxCount());
        log.info(" Agama Deployments - deploymentPagedResult:{},", deploymentPagedResult);

        if (deploymentPagedResult != null && deploymentPagedResult.getEntries() != null
                && !deploymentPagedResult.getEntries().isEmpty()) {
            List<Deployment> agamaDeploymentList = deploymentPagedResult.getEntries();
            log.debug(" agamaDeploymentList:{},", agamaDeploymentList);

            Set<String> keys = getDirectLaunchFlows(agamaDeploymentList);
            log.info("Final DirectLaunchFlows - keys:{}, authenticationMode:{}, authenticationMode.indexOf(AGAMA_PREFIX_SEPERATOR):{} , authenticationMode.indexOf(AGAMA_PREFIX_SEPERATOR)+1:{}", keys, authenticationMode, authenticationMode.indexOf(AGAMA_PREFIX_SEPERATOR) , authenticationMode.indexOf(AGAMA_PREFIX_SEPERATOR)+1);
            String agamaAcr = authenticationMode;
            if (authenticationMode.indexOf(AGAMA_PREFIX_SEPERATOR) > 0) {
                agamaAcr = authenticationMode.substring(authenticationMode.indexOf(AGAMA_PREFIX_SEPERATOR)+1);
            }
            log.info(" agamaAcr:{},", agamaAcr);

            if (keys != null && !keys.isEmpty() && keys.contains(agamaAcr)) {
                log.debug(" keys.contains(agamaAcr):{},", keys.contains(agamaAcr));
                isValid = true;
            }
        }
        log.info(" isValidAgamaDeployment - isValid:{}", isValid);
        return isValid;
    }

    private Set<String> getDirectLaunchFlows(List<Deployment> agamaDeploymentList) {
        log.info(" agamaDeploymentList:{}", agamaDeploymentList);
        Set<String> keys = null;
        List<String> noDirectLaunchFlows = new ArrayList<>();
        if (agamaDeploymentList == null || agamaDeploymentList.isEmpty()) {
            return keys;
        }
        for (Deployment deployment : agamaDeploymentList) {
            log.info("Agama deployment:{},", deployment);
            if (deployment.getDetails() != null && deployment.getDetails().getFlowsError() != null) {
                keys = deployment.getDetails().getFlowsError().keySet();
                log.info(" Agama flow keys:{},", keys);

                if (deployment.getDetails().getProjectMetadata() != null) {
                    noDirectLaunchFlows.addAll(deployment.getDetails().getProjectMetadata().getNoDirectLaunchFlows());
                }
            }
        }
        log.info("All deployed agama keys:{}, noDirectLaunchFlows:{}", keys, noDirectLaunchFlows);
        if (keys != null && !keys.isEmpty() && noDirectLaunchFlows != null) {
            keys.removeAll(noDirectLaunchFlows);
        }
        log.info("Final agama main flow keys:{}", keys);
        return keys;
    }

}