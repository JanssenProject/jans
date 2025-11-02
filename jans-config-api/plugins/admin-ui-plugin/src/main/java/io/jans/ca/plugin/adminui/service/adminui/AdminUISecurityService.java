package io.jans.ca.plugin.adminui.service.adminui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    // Constants
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Locale DEFAULT_LOCALE = Locale.ROOT;

    // Regex patterns for parsing Cedar DSL
    private static final Pattern PRINCIPAL_PATTERN =
            Pattern.compile("Gluu::Flex::AdminUI::Role::\"([A-Za-z0-9_\\-\\.]+)");
    private static final Pattern RESOURCE_ASSIGNMENT_PATTERN =
            Pattern.compile("resource\\s*(?:==|in|is)\\s*([^;\\n]+)");
    private static final Pattern SINGLE_ACTION_PATTERN =
            Pattern.compile("action\\s*==\\s*([^;\\n]+)");
    private static final Pattern MULTI_ACTION_PATTERN =
            Pattern.compile("action\\s*in\\s*\\[([^\\]]+)\\]");

    private static final String RESOURCE_PREFIX= "Gluu::Flex::AdminUI::Resources::";
    private static final String ACTION_PREFIX= "Gluu::Flex::AdminUI::Action::";
    private static final String PARENT_RESOURCE_PREFIX = "ParentResource::";
    private static final String FEATURES_PREFIX = "Features::";
    private static final String FEATURES_PREFIX_FOR_DEFAULT_ENTITIES = RESOURCE_PREFIX + "Features";

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
            if(auiConfiguration.getUseRemotePolicyStore() && !Strings.isNullOrEmpty(auiConfiguration.getAuiPolicyStoreUrl())) {
                Invocation.Builder request = ClientFactory.getClientBuilder(auiConfiguration.getAuiPolicyStoreUrl());
                request.header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON);
                Response response = request.get();

                log.info("policy store request status code: {}", response.getStatus());

                ObjectMapper mapper = new ObjectMapper();
                if (response.getStatus() == 200) {
                    String entity = response.readEntity(String.class);
                    JsonNode policyStoreJsonNode = mapper.readValue(entity, JsonNode.class);
                    return CommonUtils.createGenericResponse(true, 200, "Policy store fetched successfully.", policyStoreJsonNode);
                }
                // Handle non-200 responses
                String jsonData = response.readEntity(String.class);
                log.error("{}: {}", ErrorResponse.RETRIEVE_POLICY_STORE_ERROR, jsonData);
                return CommonUtils.createGenericResponse(false, response.getStatus(), jsonData);
            } else {
                // Load policy store from default local file path
                String policyStorePath = Optional.ofNullable(auiConfiguration.getAuiDefaultPolicyStorePath())
                        .filter(path -> !Strings.isNullOrEmpty(path))
                        .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);

                Path path = Paths.get(policyStorePath);

                log.error("Absolute path of default : " + path.toAbsolutePath());
                // Create ObjectMapper instance
                ObjectMapper objectMapper = new ObjectMapper();
                // Read file content as bytes
                byte[] jsonBytes = Files.readAllBytes(path);
                // Parse bytes into JsonNode
                JsonNode policyStoreJsonNode = objectMapper.readTree(jsonBytes);
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
            if (!auiConfiguration.getUseRemotePolicyStore() ||
                    Strings.isNullOrEmpty(auiConfiguration.getAuiPolicyStoreUrl())) {

                return CommonUtils.createGenericResponse(
                        false, 500,
                        "Either remote policy-store URL is not configured or it is not enabled for use."
                );
            }

            // Build the client request for the remote policy store
            Invocation.Builder request = ClientFactory
                    .getClientBuilder(auiConfiguration.getAuiPolicyStoreUrl())
                    .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON);

            // Execute GET request
            try (Response response = request.get()) {
                //Response response = request.get();
                int status = response.getStatus();
                log.info("Policy store request status code: {}", status);

                ObjectMapper mapper = new ObjectMapper();

                if (status == 200) {
                    // Parse response entity into JsonNode
                    String responseEntity = response.readEntity(String.class);
                    JsonNode policyStoreJson = mapper.readTree(responseEntity);

                    // Resolve path for local policy store file
                    String policyStorePath = Optional.ofNullable(auiConfiguration.getAuiDefaultPolicyStorePath())
                            .filter(path -> !Strings.isNullOrEmpty(path))
                            .orElse(AppConstants.DEFAULT_POLICY_STORE_FILE_PATH);

                    // Overwrite local policy store file with remote JSON
                    Path path = Paths.get(policyStorePath);
                    mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), policyStoreJson);

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

            Map<String, Set<String>> principalsToScopesMap = mapPrincipalsToScopes(policyStoreJson, resourceScopesJson);

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

    /**
     * Main entry point for mapping principals to scopes.
     *
     * @param policyStoreJson Root JSON containing policy_stores array
     * @param resourcesJson JSON array from SQL with resource and scopes arrays
     * @return Map of principal (sanitized lowercase) to set of scopes
     */
    public static Map<String, Set<String>> mapPrincipalsToScopes(JsonNode policyStoreJson, JsonNode resourcesJson) {
        if (policyStoreJson == null || resourcesJson == null) {
            return Collections.emptyMap();
        }

        Map<String, Set<String>> resourceToCaps = buildResourceToScopes(resourcesJson);
        Set<String> allResourceKeys = Collections.unmodifiableSet(resourceToCaps.keySet());
        Map<String, Set<String>> principalToScopes = new HashMap<>();

        ArrayNode policyStores = getPolicyStoresArray(policyStoreJson);

        for (JsonNode policyStore : policyStores) {
            ArrayNode policies = getArrayNode(policyStore, "policies");
            if (policies == null || policies.isEmpty()) continue;

            for (JsonNode policy : policies) {
                processPolicy(policy, policyStore, resourceToCaps, allResourceKeys, principalToScopes);
            }
        }

        return Collections.unmodifiableMap(principalToScopes);
    }
    /**
     * Processes a single policy to extract principals and map them to scopes.
     */
    private static void processPolicy(JsonNode policy, JsonNode policyStore,
                                      Map<String, Set<String>> resourceToCaps,
                                      Set<String> allResourceKeys,
                                      Map<String, Set<String>> principalToScopes) {
        String cedarDsl = decodeBase64ToString(policy, "policy_content");
        if (cedarDsl == null || cedarDsl.trim().isEmpty()) return;

        Set<String> principals = extractPrincipalsFromCedarDsl(cedarDsl);
        if (principals.isEmpty()) return;

        Set<String> policyResources = extractResourceActionPairs(cedarDsl, policyStore);
        Set<String> aggregatedScopes = aggregateScopes(policyResources, resourceToCaps, allResourceKeys, policyStore);

        // Only attach scopes if we found valid ones
        if (!aggregatedScopes.isEmpty()) {
            for (String principal : principals) {
                principalToScopes.computeIfAbsent(principal, k -> new HashSet<>()).addAll(aggregatedScopes);
            }
        }
    }

    /**
     * Aggregates scopes from policy resources using direct matching, schema fallback, and default_entities fallback.
     */
    private static Set<String> aggregateScopes(Set<String> policyResources,
                                               Map<String, Set<String>> resourceToCaps,
                                               Set<String> allResourceKeys,
                                               JsonNode policyStore) {
        if (policyResources == null || policyResources.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> aggregatedScopes = new HashSet<>();

        for (String rawResource : policyResources) {
            if (rawResource == null || rawResource.trim().isEmpty()) continue;

            String resourceKey = rawResource.toLowerCase(DEFAULT_LOCALE).trim();

            // Try direct matching first
            if (!findAndAddScopes(resourceKey, resourceToCaps, allResourceKeys, aggregatedScopes)) {
                // If direct matching fails, try default_entities fallback
                findAndAddScopesFromDefaultEntities(resourceKey, resourceToCaps, allResourceKeys, aggregatedScopes, policyStore);
            }
        }

        return aggregatedScopes;
    }

    /**
     * Finds scopes for a resource key and adds them to the aggregated set.
     * Returns true if scopes were found, false otherwise.
     */
    private static boolean findAndAddScopes(String resourceKey,
                                            Map<String, Set<String>> resourceToCaps,
                                            Set<String> allResourceKeys,
                                            Set<String> aggregatedScopes) {
        if (resourceKey == null || resourceKey.trim().isEmpty()) {
            return false;
        }

        // Direct match
        Set<String> scopes = resourceToCaps.get(resourceKey);
        if (scopes != null && !scopes.isEmpty()) {
            aggregatedScopes.addAll(scopes);
            return true;
        }

        // Case-insensitive match using stream with early termination
        return allResourceKeys.stream()
                .filter(key -> key.equalsIgnoreCase(resourceKey))
                .findFirst()
                .map(matchedKey -> {
                    Set<String> matchedScopes = resourceToCaps.get(matchedKey);
                    if (matchedScopes != null) {
                        aggregatedScopes.addAll(matchedScopes);
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * Finds scopes by looking in default_entities for Features with matching parent resources.
     */
    private static void findAndAddScopesFromDefaultEntities(String resourceKey,
                                                            Map<String, Set<String>> resourceToCaps,
                                                            Set<String> allResourceKeys,
                                                            Set<String> aggregatedScopes,
                                                            JsonNode policyStore) {
        JsonNode defaultEntitiesNode = decodeBase64ToJson(policyStore, "default_entities");
        if (defaultEntitiesNode == null) return;

        Set<String> featureIds = findFeatureIdsByParentResource(defaultEntitiesNode, resourceKey);

        for (String featureId : featureIds) {
            // Try to find scopes for each feature ID
            findAndAddScopes(featureId, resourceToCaps, allResourceKeys, aggregatedScopes);
        }
    }

    /**
     * Finds all Feature entity IDs that have the specified resource in their parents field.
     */
    private static Set<String> findFeatureIdsByParentResource(JsonNode defaultEntitiesNode, String targetResource) {
        Set<String> featureIds = new HashSet<>();

        if (defaultEntitiesNode == null || !defaultEntitiesNode.isObject()) {
            return featureIds;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = defaultEntitiesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String entityId = entry.getKey();
            JsonNode entityData = decodeEntityData(entry.getValue());

            if (isFeatureWithMatchingParent(entityData, targetResource)) {
                featureIds.add(entityId.toLowerCase(DEFAULT_LOCALE));
            }
        }

        return featureIds;
    }

    /**
     * Decodes base64 encoded entity data to JSON.
     */
    private static JsonNode decodeEntityData(JsonNode encodedNode) {
        if (encodedNode == null || !encodedNode.isTextual()) {
            return null;
        }

        try {
            String base64String = encodedNode.asText();
            byte[] raw = Base64.getDecoder().decode(base64String);
            String jsonString = new String(raw, StandardCharsets.UTF_8);
            return MAPPER.readTree(jsonString);
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    /**
     * Checks if the entity is a Feature type and has the target resource in its parents field.
     */
    private static boolean isFeatureWithMatchingParent(JsonNode entityData, String targetResource) {
        if (entityData == null || !entityData.isObject()) {
            return false;
        }

        // Check if it's a Feature type
        JsonNode typeNode = entityData.path("uid").path("type");
        if (!typeNode.isTextual() || !FEATURES_PREFIX_FOR_DEFAULT_ENTITIES.equalsIgnoreCase(typeNode.asText())) {
            return false;
        }

        // Check parents field for matching resource
        JsonNode parentsNode = entityData.path("parents");
        if (parentsNode.isArray()) {
            for (JsonNode parentNode : parentsNode) {
                JsonNode id = parentNode.path("id");
                if (id.isTextual()) {
                    String parent = normalizeParentResource(id.asText());
                    String target = normalizeResource(targetResource);

                    // Check if parent matches the target resource
                    if (parent.equals(target)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Normalizes parent resource by removing namespace and type prefix.
     */
    private static String normalizeParentResource(String parent) {
        if (parent == null) return null;

        // Remove ParentResource:: prefix if present
        String normalized = parent.replace("ParentResource::", "");

        // Remove any other namespace prefixes
        if (normalized.contains("::")) {
            normalized = normalized.substring(normalized.lastIndexOf("::") + 2);
        }

        return normalized.toLowerCase(DEFAULT_LOCALE).trim();
    }
    // ==================== HELPER METHODS ====================

    /**
     * Gets policy stores array from JSON, handling both array and single object formats.
     */
    private static ArrayNode getPolicyStoresArray(JsonNode policyStoreJson) {
        ArrayNode policyStores = getArrayNode(policyStoreJson, "policy_stores");
        if (policyStores == null) {
            policyStores = MAPPER.createArrayNode().add(policyStoreJson);
        }
        return policyStores;
    }

    /**
     * Extracts array node from parent JSON, converting objects to arrays if needed.
     */
    private static ArrayNode getArrayNode(JsonNode parent, String field) {
        if (parent == null) return null;

        ArrayNode arrayNode = MAPPER.createArrayNode();
        JsonNode node = parent.path(field);

        if (node != null && node.isObject()) {
            // Convert object to array of values
            node.fields().forEachRemaining(entry -> arrayNode.add(entry.getValue()));
        } else if (node != null && node.isArray()) {
            return (ArrayNode) node;
        }

        return arrayNode;
    }

    /**
     * Extracts principals from Cedar DSL policy content.
     */
    private static Set<String> extractPrincipalsFromCedarDsl(String cedarDsl) {
        Set<String> principals = new HashSet<>();
        Matcher matcher = PRINCIPAL_PATTERN.matcher(cedarDsl);

        while (matcher.find()) {
            principals.add(matcher.group(1).toLowerCase(DEFAULT_LOCALE));
        }

        return principals;
    }

    /**
     * Extracts resource-action pairs from Cedar DSL policy using default_entities fallback.
     */
    private static Set<String> extractResourceActionPairs(String policy, JsonNode policyStore) {
        Set<String> resources = extractResourcesFromPolicy(policy);
        Set<String> actions = extractActionsFromPolicy(policy);

        return buildResourceActionPairs(resources, actions, policyStore);
    }

    /**
     * Extracts resources from policy text.
     */
    private static Set<String> extractResourcesFromPolicy(String policy) {
        Set<String> resources = new HashSet<>();
        Matcher matcher = RESOURCE_ASSIGNMENT_PATTERN.matcher(policy);

        while (matcher.find()) {
            String resourceValue = cleanValue(matcher.group(1));
            extractResourcesFromValue(resourceValue, resources);
        }

        return resources;
    }

    /**
     * Extracts resources from a resource value (single or array).
     */
    private static void extractResourcesFromValue(String resourceValue, Set<String> resources) {
        if (resourceValue.startsWith("[") && resourceValue.endsWith("]")) {
            // Array form: [Resource1, Resource2]
            String arrayContent = resourceValue.substring(1, resourceValue.length() - 1);
            Arrays.stream(arrayContent.split(","))
                    .map(AdminUISecurityService::cleanValue)
                    .map(AdminUISecurityService::normalizeResource)
                    .filter(cleaned -> !cleaned.isEmpty())
                    .forEach(resources::add);
        } else if (!resourceValue.isEmpty()) {
            // Single resource
            resources.add(normalizeResource(resourceValue));
        }
    }

    /**
     * Extracts actions from policy text.
     */
    private static Set<String> extractActionsFromPolicy(String policy) {
        Set<String> actions = new HashSet<>();

        // Single action
        Matcher singleMatcher = SINGLE_ACTION_PATTERN.matcher(policy);
        if (singleMatcher.find()) {
            actions.add(normalizeAction(cleanValue(singleMatcher.group(1))));
        }

        // Multiple actions
        Matcher multiMatcher = MULTI_ACTION_PATTERN.matcher(policy);
        if (multiMatcher.find()) {
            String actionsString = multiMatcher.group(1);
            Arrays.stream(actionsString.split(","))
                    .map(AdminUISecurityService::cleanValue)
                    .map(AdminUISecurityService::normalizeAction)
                    .filter(cleaned -> !cleaned.isEmpty())
                    .forEach(actions::add);
        }

        return actions;
    }

    /**
     * Builds resource-action pairs combining resources and actions with default_entities fallback.
     */
    private static Set<String> buildResourceActionPairs(Set<String> resources, Set<String> actions, JsonNode policyStore) {
        Set<String> pairs = new HashSet<>();

        for (String resource : resources) {
            // Use default_entities to find matching feature entities
            Set<String> matchingEntities = findMatchingFeatureEntities(policyStore, resource);

            for (String entity : matchingEntities) {
                for (String action : actions) {
                    String pair = (entity + "~" + action).toLowerCase(DEFAULT_LOCALE).replace("\"", "");
                    pairs.add(pair);
                }
            }
        }

        return pairs;
    }

    /**
     * Finds matching feature entities from default_entities based on parent resource.
     */
    private static Set<String> findMatchingFeatureEntities(JsonNode policyStore, String resource) {
        Set<String> matchingEntities = new HashSet<>();

        // First, add the original resource itself
        matchingEntities.add(resource);

        // Then look for Features in default_entities that have this resource as parent
        JsonNode defaultEntitiesNode = getDefaultEntities(policyStore);
        if (defaultEntitiesNode == null) return matchingEntities;

        Iterator<Map.Entry<String, JsonNode>> fields = defaultEntitiesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            //String entityId = entry.getKey();
            JsonNode entityData = decodeEntityData(entry.getValue());

            if (isFeatureWithMatchingParent(entityData, resource)) {
                //matchingEntities.add(entityId);
                JsonNode uidNode = entityData.path("uid");
                if (!uidNode.isMissingNode()) {
                    JsonNode idNode = uidNode.path("id");
                    if (!idNode.isMissingNode() && idNode.isTextual()) {
                        String normalizedId = idNode.asText();
                        matchingEntities.add(normalizedId.toLowerCase(DEFAULT_LOCALE).trim());
                    }
                }
            }
        }

        return matchingEntities;
    }

    /**
     * Gets default_entities as JSON object (not base64 decoded since the field itself is JSON)
     */
    private static JsonNode getDefaultEntities(JsonNode policyStore) {
        JsonNode defaultEntitiesNode = policyStore.path("default_entities");
        if (defaultEntitiesNode.isMissingNode() || defaultEntitiesNode.isNull() || !defaultEntitiesNode.isObject()) {
            return null;
        }
        return defaultEntitiesNode;
    }

    /**
     * Normalizes resource by removing namespace prefix.
     */
    private static String normalizeResource(String value) {
        return value.replace(RESOURCE_PREFIX + PARENT_RESOURCE_PREFIX, "")
                .replace(RESOURCE_PREFIX + FEATURES_PREFIX, "")
                .replace("\"", "")
                .toLowerCase(DEFAULT_LOCALE)
                .trim();
    }

    /**
     * Normalizes action by removing namespace prefix.
     */
    private static String normalizeAction(String value) {
        return value.replace(ACTION_PREFIX, "").trim();
    }

    /**
     * Cleans string value by removing quotes and trimming.
     */
    private static String cleanValue(String value) {
        if (value == null) return null;
        return value.trim()
                .replaceAll("^\"|\"$", "")
                .replaceAll("^'|'$", "")
                .trim();
    }

    /**
     * Builds resource to Scopes mapping from SQL-derived JSON.
     */
    private static Map<String, Set<String>> buildResourceToScopes(JsonNode resourcesJson) {
        Map<String, Set<String>> map = new HashMap<>();
        if (resourcesJson == null) return map;

        Iterable<JsonNode> items = extractResourcesArray(resourcesJson);

        for (JsonNode item : items) {
            String resource = firstNonEmptyText(item, "resource", "name");
            String accessType = firstNonEmptyText(item, "access_type", "accessType", "type");

            if (resource == null || accessType == null) continue;

            String key = (resource + "~" + accessType).toLowerCase(DEFAULT_LOCALE);
            Set<String> scopes = extractScopes(item);

            map.merge(key, scopes, (oldSet, newSet) -> {
                oldSet.addAll(newSet);
                return oldSet;
            });
        }

        return map;
    }

    /**
     * Extracts resources array from JSON structure.
     */
    private static Iterable<JsonNode> extractResourcesArray(JsonNode resourcesJson) {
        if (resourcesJson.isArray()) {
            return resourcesJson;
        } else {
            JsonNode arr = resourcesJson.path("resources");
            return arr.isArray() ? arr : Collections.emptyList();
        }
    }

    /**
     * Extracts scopes from JSON item.
     */
    private static Set<String> extractScopes(JsonNode item) {
        Set<String> scopes = new HashSet<>();
        JsonNode capsNode = item.has("scopes") ? item.get("scopes") : item.get("capability");

        if (capsNode != null && capsNode.isArray()) {
            for (JsonNode capability : capsNode) {
                if (capability.isTextual()) {
                    scopes.add(capability.asText());
                }
            }
        }

        return scopes;
    }

    /**
     * Decodes base64 field to string.
     */
    private static String decodeBase64ToString(JsonNode parent, String field) {
        String base64String = getFieldAsText(parent, field);
        if (base64String == null) return null;

        try {
            byte[] raw = Base64.getDecoder().decode(base64String);
            return new String(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Decodes base64 field to JSON.
     */
    private static JsonNode decodeBase64ToJson(JsonNode parent, String field) {
        String base64String = getFieldAsText(parent, field);
        if (base64String == null) return null;

        try {
            byte[] raw = Base64.getDecoder().decode(base64String);
            return MAPPER.readTree(new String(raw, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    /**
     * Gets field value as text, returns null if missing or empty.
     */
    private static String getFieldAsText(JsonNode parent, String field) {
        if (parent == null) return null;
        JsonNode node = parent.path(field);
        if (node.isMissingNode() || node.isNull()) return null;

        // Handle array and object nodes - convert to JSON string
        if (node.isArray() || node.isObject()) {
            try {
                return MAPPER.writeValueAsString(node);
            } catch (IOException e) {
                return null;
            }
        }

        String value = node.asText("");
        return value.isEmpty() ? null : value;
    }

    /**
     * Finds entityTypes node in schema JSON.
     */
    private static JsonNode findEntityTypesNode(JsonNode schemaJson) {
        // Try direct path first
        JsonNode resourcesNode = schemaJson.path("Gluu::Flex::AdminUI::Resources");
        JsonNode entityTypesNode = resourcesNode.has("entityTypes") ?
                resourcesNode.get("entityTypes") : null;

        // Fallback: search for any "entityTypes" field
        return entityTypesNode != null ? entityTypesNode : findNodeByFieldName(schemaJson, "entityTypes");
    }

    /**
     * Processes entity types from object structure.
     */
    private static void processEntityTypesObject(JsonNode entityTypesNode, String resourceLower,
                                                 Map<String, Set<String>> index) {
        entityTypesNode.fields().forEachRemaining(entry -> {
            String entityTypeName = entry.getKey();
            JsonNode entityTypeNode = entry.getValue();

            if (shouldIncludeEntityType(entityTypeName, entityTypeNode, resourceLower)) {
                Set<String> members = extractMemberOfTypes(entityTypeNode);
                index.put(entityTypeName.toLowerCase(DEFAULT_LOCALE), members);
            }
        });
    }

    /**
     * Processes entity types from array structure.
     */
    private static void processEntityTypesArray(JsonNode entityTypesNode, String resourceLower,
                                                Map<String, Set<String>> index) {
        for (JsonNode entityTypeNode : entityTypesNode) {
            String entityTypeName = firstNonEmptyText(entityTypeNode, "name", "entityType", "id");
            if (entityTypeName == null) continue;

            if (shouldIncludeEntityType(entityTypeName, entityTypeNode, resourceLower)) {
                Set<String> members = extractMemberOfTypes(entityTypeNode);
                index.put(entityTypeName.toLowerCase(DEFAULT_LOCALE), members);
            }
        }
    }

    /**
     * Determines if entity type should be included based on resource matching.
     */
    private static boolean shouldIncludeEntityType(String entityTypeName, JsonNode entityTypeNode, String resourceLower) {
        // Direct name match
        if (entityTypeName.equalsIgnoreCase(resourceLower)) {
            return true;
        }

        // MemberOf types match
        JsonNode memberOf = entityTypeNode.path("memberOfTypes");
        if (memberOf.isArray()) {
            for (JsonNode member : memberOf) {
                if (member.isTextual() && member.asText().equalsIgnoreCase(resourceLower)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Extracts memberOf types from entity type node.
     */
    private static Set<String> extractMemberOfTypes(JsonNode entityTypeNode) {
        Set<String> members = new HashSet<>();
        JsonNode memberOf = entityTypeNode.path("memberOfTypes");

        if (memberOf.isArray()) {
            for (JsonNode member : memberOf) {
                if (member.isTextual()) {
                    members.add(member.asText().toLowerCase(DEFAULT_LOCALE));
                }
            }
        }

        return members;
    }

    /**
     * Finds node by field name using DFS.
     */
    private static JsonNode findNodeByFieldName(JsonNode root, String fieldName) {
        if (root == null) return null;

        Deque<JsonNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            JsonNode node = stack.pop();
            if (node.has(fieldName)) return node.get(fieldName);
            if (node.isContainerNode()) {
                node.elements().forEachRemaining(stack::push);
            }
        }

        return null;
    }

    /**
     * Gets first non-empty text value from multiple possible field names.
     */
    private static String firstNonEmptyText(JsonNode node, String... fieldNames) {
        if (node == null) return null;

        for (String field : fieldNames) {
            if (node.has(field) && node.get(field).isTextual()) {
                String value = node.get(field).asText().trim();
                if (!value.isEmpty()) return value;
            }
        }

        return null;
    }
}
