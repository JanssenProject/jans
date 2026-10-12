/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;

import static io.jans.as.model.util.Util.escapeLog;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.lock.service.LockConfigService;
import io.jans.configapi.plugin.lock.service.LockService;
import io.jans.configapi.plugin.lock.util.Constants;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.core.cedarling.model.CedarlingConfiguration;
import io.jans.core.cedarling.model.PolicySource;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.Conf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(Constants.POLICY_STORES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PolicyStoreResource extends BaseResource {

    private static final String CONFIG_NULL_ERR_MSG = "It seems Lock module is not setup, kindly check.";
    private static final String LOCK_POLICY_STORES_URL = "/jans-lock/api/v1/policy/stores";

    @Inject
    Logger logger;

    @Inject
    LockConfigService lockConfigService;

    @Inject
    LockService lockService;

    @Inject
    ConfigurationService configurationService;

    @Operation(summary = "Lists Lock policy store sources", description = "Lists policy store sources registered in Lock configuration and, when the token grants https://jans.io/oauth/lock/policy.readonly, the .cjar stores currently loaded by Lock with version and bundle hash", operationId = "get-lock-policy-stores", tags = {
            "Lock - Policy Stores" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_CONFIG_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.LOCK_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.LOCK_CONFIG_WRITE_ACCESS }, superScopes = { Constants.LOCK_ADMIN_ACCESS,
                    Constants.LOCK_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getPolicyStores(
            @Parameter(description = "Authorization header forwarded to Lock") @HeaderParam("Authorization") String authorization) {
        List<Map<String, Object>> sources = new ArrayList<>();
        for (PolicySource policySource : getPolicySources(getLockConfiguration())) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("policyStoreUri", policySource.getPolicyStoreUri());
            source.put("enabled", policySource.isEnabled());
            sources.add(source);
        }

        JsonNode loaded = null;
        try {
            loaded = lockService.getPolicyStores(getIssuer() + LOCK_POLICY_STORES_URL, authorization);
        } catch (Exception ex) {
            logger.warn("Failed to read loaded policy stores from Lock: {}", escapeLog(ex.getMessage()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sources", sources);
        result.put("loaded", loaded);
        return Response.ok(result).build();
    }

    @Operation(summary = "Registers a Lock policy store source", description = "Adds or updates a .cjar or JSON policy store source in Lock configuration. Lock loads it on the next policy refresh", operationId = "post-lock-policy-store", tags = {
            "Lock - Policy Stores" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "Policy store source", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PolicySource.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PolicySource.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.LOCK_ADMIN_ACCESS, Constants.LOCK_WRITE_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response registerPolicyStore(@NotNull PolicySource policySource) {
        String policyStoreUri = policySource.getPolicyStoreUri();
        if (!isHttpsUri(policyStoreUri)) {
            throwBadRequestException("policyStoreUri must be an absolute https URI");
        }
        if (StringUtils.isNotBlank(policySource.getAuthorizationToken())) {
            throwBadRequestException("authorizationToken is not accepted here, set it through lockConfig");
        }

        Conf conf = findLockConf();
        CedarlingConfiguration cedarlingConfiguration = getCedarlingConfiguration(conf.getDynamic());
        List<PolicySource> policySources = cedarlingConfiguration.getPolicySources() == null ? new ArrayList<>()
                : new ArrayList<>(cedarlingConfiguration.getPolicySources());

        PolicySource registered = policySources.stream()
                .filter(source -> policyStoreUri.equals(source.getPolicyStoreUri())).findFirst().orElse(null);
        if (registered == null) {
            registered = new PolicySource();
            registered.setPolicyStoreUri(policyStoreUri);
            policySources.add(registered);
        }
        registered.setEnabled(policySource.isEnabled());

        cedarlingConfiguration.setPolicySources(policySources);
        lockConfigService.mergeLockConfig(conf);
        logger.info("Registered Lock policy store source:{}, enabled:{}", escapeLog(policyStoreUri),
                policySource.isEnabled());

        PolicySource response = new PolicySource();
        response.setPolicyStoreUri(policyStoreUri);
        response.setEnabled(policySource.isEnabled());
        return Response.ok(response).build();
    }

    @Operation(summary = "Removes a Lock policy store source", description = "Removes a policy store source from Lock configuration", operationId = "delete-lock-policy-store", tags = {
            "Lock - Policy Stores" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.LOCK_ADMIN_ACCESS, Constants.LOCK_WRITE_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response deletePolicyStore(
            @Parameter(description = "Policy store source URI") @QueryParam("policyStoreUri") @NotNull String policyStoreUri) {
        Conf conf = findLockConf();
        CedarlingConfiguration cedarlingConfiguration = getCedarlingConfiguration(conf.getDynamic());
        List<PolicySource> policySources = cedarlingConfiguration.getPolicySources() == null ? new ArrayList<>()
                : new ArrayList<>(cedarlingConfiguration.getPolicySources());

        if (!policySources.removeIf(source -> policyStoreUri.equals(source.getPolicyStoreUri()))) {
            throwNotFoundException("Not Found", "Policy store source is not registered");
        }

        cedarlingConfiguration.setPolicySources(policySources);
        lockConfigService.mergeLockConfig(conf);
        logger.info("Removed Lock policy store source:{}", escapeLog(policyStoreUri));
        return Response.noContent().build();
    }

    static boolean isHttpsUri(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && StringUtils.isNotBlank(uri.getHost());
        } catch (Exception ex) {
            return false;
        }
    }

    private Conf findLockConf() {
        Conf conf = lockConfigService.findLockConf();
        if (conf == null || conf.getDynamic() == null) {
            throwInternalServerException(CONFIG_NULL_ERR_MSG);
        }
        return conf;
    }

    private AppConfiguration getLockConfiguration() {
        AppConfiguration lockConfiguration = lockConfigService.find();
        if (lockConfiguration == null) {
            throwInternalServerException(CONFIG_NULL_ERR_MSG);
        }
        return lockConfiguration;
    }

    private CedarlingConfiguration getCedarlingConfiguration(AppConfiguration lockConfiguration) {
        if (lockConfiguration.getCedarlingConfiguration() == null) {
            lockConfiguration.setCedarlingConfiguration(new CedarlingConfiguration());
        }
        return lockConfiguration.getCedarlingConfiguration();
    }

    private List<PolicySource> getPolicySources(AppConfiguration lockConfiguration) {
        CedarlingConfiguration cedarlingConfiguration = lockConfiguration.getCedarlingConfiguration();
        if (cedarlingConfiguration == null || cedarlingConfiguration.getPolicySources() == null) {
            return new ArrayList<>();
        }
        return cedarlingConfiguration.getPolicySources();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
