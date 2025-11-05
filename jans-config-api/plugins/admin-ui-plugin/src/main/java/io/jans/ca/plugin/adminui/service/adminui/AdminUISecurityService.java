package io.jans.ca.plugin.adminui.service.adminui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.RolePermissionMapping;
import io.jans.ca.plugin.adminui.model.adminui.AdminUIResourceScopesMapping;
import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.ca.plugin.adminui.utils.security.PolicyStoreMapperHelper;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Singleton
public class AdminUISecurityService {

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    AdminUIService adminUIService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Retrieves the policy store configuration for the Admin UI.
     * <p>
     * This method checks if a remote policy store URL is configured and enabled.
     * If so, it fetches the policy store from the remote URL using a GET request.
     * Otherwise, it loads the local default policy store JSON file from the configured file path.
     * </p>
     *
     * <p>
     * The method returns a {@link GenericResponse} containing the policy store as a {@link JsonNode}
     * if successful, or an error response if the retrieval fails.
     * </p>
     *
     * @return {@link GenericResponse} containing the policy store data or an error message.
     * @throws ApplicationException if any unexpected error occurs while fetching or parsing the policy store.
     */

    public GenericResponse getPolicyStore() throws ApplicationException {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            // If the remote Policy Store URL is configured and enabled
            if(auiConfiguration.getUseCedarlingRemotePolicyStore() && !Strings.isNullOrEmpty(auiConfiguration.getAuiCedarlingPolicyStoreUrl())) {
                Invocation.Builder request = ClientFactory.getClientBuilder(auiConfiguration.getAuiCedarlingPolicyStoreUrl());
                request.header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON);
                Response response = request.get();

                log.info("policy store request status code: {}", response.getStatus());

                if (response.getStatus() == 200) {
                    String entity = response.readEntity(String.class);
                    JsonNode policyStoreJsonNode = MAPPER.readValue(entity, JsonNode.class);
                    return CommonUtils.createGenericResponse(true, 200, "Policy store fetched successfully.", policyStoreJsonNode);
                }
                // Handle non-200 responses
                String jsonData = response.readEntity(String.class);
                log.error("{}: {}", ErrorResponse.RETRIEVE_POLICY_STORE_ERROR, jsonData);
                return CommonUtils.createGenericResponse(false, response.getStatus(), jsonData);
            } else {
                // Load policy store from default local file path
                String policyStorePath = Optional.ofNullable(auiConfiguration.getAuiCedarlingDefaultPolicyStorePath())
                        .filter(path -> !Strings.isNullOrEmpty(path))
                        .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);

                Path path = Paths.get(policyStorePath);

                log.debug("Absolute path of policy-store file: {}", path.toAbsolutePath());
                // Read file content as bytes
                byte[] jsonBytes = Files.readAllBytes(path);
                // Parse bytes into JsonNode
                JsonNode policyStoreJsonNode = MAPPER.readTree(jsonBytes);
                return CommonUtils.createGenericResponse(true, 200, "Policy store fetched successfully.", policyStoreJsonNode);
            }
        } catch (Exception e) {
            log.error(ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription());
        }
    }

    /**
     * Fetches the remote policy store and overwrites the local default policy-store file if
     * remote policy-store is enabled and configured in AUI configuration.
     *
     * @return GenericResponse indicating success or failure along with details.
     * @throws ApplicationException if there is any error during the operation.
     */
    public GenericResponse setRemotePolicyStoreAsDefault() throws ApplicationException {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            // Validate if remote policy store usage is enabled and URL is configured
            if (!CommonUtils.toPrimitiveOrDefaultFalse(auiConfiguration.getUseCedarlingRemotePolicyStore()) ||
                    Strings.isNullOrEmpty(auiConfiguration.getAuiCedarlingPolicyStoreUrl())) {

                return CommonUtils.createGenericResponse(
                        false, 500,
                        "Either remote policy-store URL is not configured or it is not enabled for use."
                );
            }

            // Build the client request for the remote policy store
            Invocation.Builder request = ClientFactory
                    .getClientBuilder(auiConfiguration.getAuiCedarlingPolicyStoreUrl())
                    .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON);

            // Execute GET request
            try (Response response = request.get()) {
                int status = response.getStatus();
                log.info("Policy store request status code: {}", status);

                if (status == 200) {
                    // Parse response entity into JsonNode
                    String responseEntity = response.readEntity(String.class);
                    JsonNode policyStoreJson = MAPPER.readTree(responseEntity);

                    // Resolve path for local policy store file
                    String policyStorePath = Optional.ofNullable(auiConfiguration.getAuiCedarlingDefaultPolicyStorePath())
                            .filter(path -> !Strings.isNullOrEmpty(path))
                            .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);

                    // Overwrite local policy store file with remote JSON
                    Path path = Paths.get(policyStorePath);
                    MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), policyStoreJson);

                    log.info("Default policy-store overwritten successfully from remote source: {}", policyStorePath);

                    return CommonUtils.createGenericResponse(true, 200,
                            "Policy store fetched and overwritten successfully.");
                }

                // Handle non-200 HTTP responses
                String errorResponse = response.readEntity(String.class);
                log.error("{}: {}", ErrorResponse.REWRITING_DEFAULT_POLICY_STORE_ERROR, errorResponse);

                return CommonUtils.createGenericResponse(false, status, errorResponse);
            }

        } catch (Exception e) {
            log.error(ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription(), e);
            throw new ApplicationException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription()
            );
        }
    }

    /**
     * Synchronizes role-to-scope mappings in the Admin UI configuration based on the latest policy-store definitions.
     * <p>
     * This method performs the following operations:
     * <ul>
     *     <li>Fetches all resource-to-scope mappings from persistence (via {@link AdminUIResourceScopesMapping}).</li>
     *     <li>Retrieves the current policy-store JSON, either from a remote source or local file, using {@link #getPolicyStore()}.</li>
     *     <li>Uses {@code mapPrincipalsToScopes()} to generate a mapping of principals (roles) to corresponding scopes.</li>
     *     <li>Creates or updates {@link AdminRole} entries for each principal found in the policy-store.</li>
     *     <li>Generates {@link RolePermissionMapping} objects that map each role to its associated scopes (permissions).</li>
     *     <li>Removes any duplicate permissions and updates the Admin UI configuration with the new mappings.</li>
     * </ul>
     * </p>
     *
     * <p>
     * This synchronization ensures that access control roles and their permissions within the Admin UI
     * remain aligned with the definitions specified in the external policy-store.
     * </p>
     *
     * @return {@link GenericResponse} indicating success or failure of the synchronization process.
     *         On success, it includes a message stating that the sync completed successfully.
     * @throws ApplicationException if any error occurs while fetching, parsing, or updating the role-to-scope mappings.
     */
    public GenericResponse syncRoleScopeMapping() throws ApplicationException {
        try {
            // Retrieve all Admin UI resource-scope mappings from persistence
            final Filter filter = Filter.createPresenceFilter(AppConstants.ADMIN_UI_RESOURCE);
            List<AdminUIResourceScopesMapping> adminUIResourceScopesMappings =
                    entryManager.findEntries(AppConstants.ADMIN_UI_RESOURCE_SCOPES_MAPPING_DN,
                            AdminUIResourceScopesMapping.class, filter);

            // Convert resource-scope mappings to JSON safely
            JsonNode resourceScopesJson = CommonUtils.toJsonNode(adminUIResourceScopesMappings);
            JsonNode policyStoreJson = getPolicyStore().getResponseObject();

            // Validate inputs
            if (resourceScopesJson == null || policyStoreJson == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(),
                        "Invalid input data for synchronization");
            }

            Map<String, Set<String>> principalsToScopesMap = PolicyStoreMapperHelper.mapPrincipalsToScopes(policyStoreJson, resourceScopesJson);

            // Validate mapping results
            if (principalsToScopesMap.isEmpty()) {
                log.warn("No principal-to-scope mappings found during synchronization");
            }

            List<AdminRole> roles = createAdminRoles(principalsToScopesMap.keySet());
            List<RolePermissionMapping> rolePermissionMappings = createRolePermissionMappings(principalsToScopesMap);

            // Remove duplicate permissions efficiently
            List<RolePermissionMapping> updatedMappings = removeDuplicatePermissions(rolePermissionMappings);

            // Update services
            adminUIService.resetRoles(roles);
            adminUIService.resetPermissionsToRole(updatedMappings);

            return CommonUtils.createGenericResponse(true, 200,
                    "Sync of role-to-scope mappings from the policy store completed successfully.");

        } catch (ApplicationException e) {
            throw e; // Re-throw ApplicationException as is
        } catch (Exception e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription());
        }
    }

    /**
     * Extracted method for creating AdminRole objects
     */
    private List<AdminRole> createAdminRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> {
                    AdminRole role = new AdminRole();
                    role.setRole(roleName);
                    role.setDescription("Role created after parsing policy-store for " + roleName);
                    role.setDeletable(true);
                    return role;
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracted method for creating RolePermissionMapping objects
     */
    private List<RolePermissionMapping> createRolePermissionMappings(Map<String, Set<String>> principalsToScopesMap) {
        return principalsToScopesMap.entrySet().stream()
                .map(entry -> {
                    RolePermissionMapping rpm = new RolePermissionMapping();
                    rpm.setRole(entry.getKey());
                    rpm.setPermissions(new ArrayList<>(entry.getValue()));
                    return rpm;
                })
                .collect(Collectors.toList());
    }

    /**
     * Efficiently remove duplicate permissions using streams
     */
    private List<RolePermissionMapping> removeDuplicatePermissions(List<RolePermissionMapping> rolePermissionMappings) {
        return rolePermissionMappings.stream()
                .map(entry -> {
                    RolePermissionMapping rpm = new RolePermissionMapping();
                    rpm.setRole(entry.getRole());
                    // Use LinkedHashSet to maintain order while removing duplicates
                    rpm.setPermissions(new ArrayList<>(new LinkedHashSet<>(entry.getPermissions())));
                    return rpm;
                })
                .collect(Collectors.toList());
    }
}
