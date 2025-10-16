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
import jakarta.json.JsonObject;
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

import static java.nio.charset.StandardCharsets.UTF_8;

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
    private static String RESOURCE_PREFIX= "Gluu::Flex::AdminUI::Resources::";
    private static String ACTION_PREFIX= "Gluu::Flex::AdminUI::Action::";



    public GenericResponse getPolicyStore() throws ApplicationException {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            if(auiConfiguration.getUseRemotePolicyStore() && !Strings.isNullOrEmpty(auiConfiguration.getAuiPolicyStoreUrl())) {
                log.info("policy store request status code: {}", auiConfiguration.toString());
                Invocation.Builder request = ClientFactory.getClientBuilder(auiConfiguration.getAuiPolicyStoreUrl());
                request.header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON);
                Response response = request.get();

                log.info("policy store request status code: {}", response.getStatus());

                ObjectMapper mapper = new ObjectMapper();
                if (response.getStatus() == 200) {
                    String entity = response.readEntity(String.class);
                    JsonNode jsonNode = mapper.readValue(entity, JsonNode.class);
                    return CommonUtils.createGenericResponse(true, 200, "Policy store fetched.", jsonNode);
                }
                //getting error
                String jsonData = response.readEntity(String.class);
                log.error("{}: {}", ErrorResponse.RETRIEVE_POLICY_STORE_ERROR, jsonData);
                return CommonUtils.createGenericResponse(false, response.getStatus(), jsonData);
            } else {
                Path path = Paths.get("custom/config/admin-ui-policy-store.json");
                log.error("Absolute path: " + path.toAbsolutePath());
                // Create ObjectMapper instance
                ObjectMapper objectMapper = new ObjectMapper();
                // Read file content as bytes
                byte[] jsonBytes = Files.readAllBytes(path);
                // Parse bytes into JsonNode
                JsonNode policyStoreJsonNode = objectMapper.readTree(jsonBytes);
                // Print or use the JsonNode
                log.error(policyStoreJsonNode.toPrettyString());
                return CommonUtils.createGenericResponse(true, 200, "Policy store fetched successfully.", policyStoreJsonNode);
            }
        } catch (Exception e) {
            log.error(ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.RETRIEVE_POLICY_STORE_ERROR.getDescription());
        }
    }

    public GenericResponse syncRoleScopeMapping() throws ApplicationException {

        try {
            final Filter filter = Filter.createPresenceFilter(AppConstants.ADMIN_UI_RESOURCE);
            List<AdminUIResourceScopesMapping> adminUIResourceScopesMappings = entryManager.findEntries(AppConstants.ADMIN_UI_RESOURCE_SCOPES_MAPPING_DN, AdminUIResourceScopesMapping.class, filter);
            log.error("adminUIResourceScopesMappings: " +adminUIResourceScopesMappings.get(0).getResource());
            log.error("adminUIResourceScopesMappings: " +adminUIResourceScopesMappings.get(0).getScopes().get(0));
            log.error("adminUIResourceScopesMappings: " +adminUIResourceScopesMappings.get(1).getResource());
            log.error("adminUIResourceScopesMappings: " +adminUIResourceScopesMappings.get(1).getScopes().get(0));
            //get resource-scope mapping JsonNode
            JsonNode resourceScopesJson = CommonUtils.toJsonNode(adminUIResourceScopesMappings);
            log.error("resourceScopesJson: " +resourceScopesJson);
            //get policy-store JsonNode
            JsonNode policyStoreJson = getPolicyStore().getResponseObject();

            Map<String, Set<String>> principalsToScopesMap = mapPrincipalsToScopes(policyStoreJson, resourceScopesJson);
            log.error("principalsToScopesMap: " +principalsToScopesMap);
            // Convert map keys to list of AdminRole
            List<AdminRole> roles = principalsToScopesMap.keySet().stream()
                    .map(roleName -> {
                        AdminRole role = new AdminRole();
                        role.setRole(roleName);
                        role.setDescription("Auto-created role for " + roleName);
                        role.setDeletable(true);
                        return role;
                    })
                    .collect(Collectors.toList());
            adminUIService.resetRoles(roles);
            // Convert map to list of RolePermissionMapping
            List<RolePermissionMapping> rolePermissionMappings = principalsToScopesMap.entrySet().stream()
                    .map(entry -> {
                        RolePermissionMapping rpm = new RolePermissionMapping();
                        rpm.setRole(entry.getKey());
                        rpm.setPermissions(new ArrayList<>(entry.getValue()));
                        return rpm;
                    })
                    .collect(Collectors.toList());
            //removing duplicate permission mapped to roles
            List<RolePermissionMapping> updatedMappings = rolePermissionMappings.stream()
                    .map(entry -> {
                        RolePermissionMapping rpm = new RolePermissionMapping();
                        Set<String> uniqueElements = new HashSet<>(entry.getPermissions());
                        rpm.setRole(entry.getRole());
                        rpm.setPermissions(new ArrayList<>(uniqueElements));
                        return rpm;
                    })
                    .collect(Collectors.toList());

            adminUIService.resetPermissionsToRole(updatedMappings);
            return CommonUtils.createGenericResponse(true, 200, "Sync of role-to-scope mapping from policy-store completed successfully.");

        } catch(Exception e) {
            log.error(ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.SYNC_ROLE_SCOPES_MAPPING_ERROR.getDescription());
        }
    }

    /**
     * Main entry point for mapping principals to scopes.
     *
     * @param policyStoreJson Root JSON containing policy_stores array
     * @param resourcesJson JSON array from SQL with resource and scopes arrays
     * @return Map of principal (sanitized lowercase) to set of scopes
     */
    public static Map<String, Set<String>> mapPrincipalsToScopes(JsonNode policyStoreJson, JsonNode resourcesJson) {
        Map<String, Set<String>> resourceToCaps = buildResourceToScopes(resourcesJson);
        Set<String> allResourceKeys = resourceToCaps.keySet();
        Map<String, Set<String>> principalToScopes = new HashMap<>();

        ArrayNode policyStores = getPolicyStoresArray(policyStoreJson);

        for (JsonNode policyStore : policyStores) {
            ArrayNode policies = getArrayNode(policyStore, "policies");
            if (policies == null) continue;

            for (JsonNode policy : policies) {
                processPolicy(policy, policyStore, resourceToCaps, allResourceKeys, principalToScopes);
            }
        }

        return principalToScopes;
    }
    /**
     * Processes a single policy to extract principals and map them to scopes.
     */
    private static void processPolicy(JsonNode policy, JsonNode policyStore,
                                      Map<String, Set<String>> resourceToCaps,
                                      Set<String> allResourceKeys,
                                      Map<String, Set<String>> principalToScopes) {
        String cedarDsl = decodeBase64ToString(policy, "policy_content");
        if (cedarDsl == null) return;

        Set<String> principals = extractPrincipalsFromCedarDsl(cedarDsl);
        if (principals.isEmpty()) return;

        JsonNode schemaNode = decodeBase64ToJson(policyStore, "schema");
        Set<String> policyResources = extractResourceActionPairs(cedarDsl, schemaNode);
        Set<String> aggregatedScopes = aggregateScopes(policyResources, resourceToCaps, allResourceKeys);

        // Attach scopes to principals
        for (String principal : principals) {
            principalToScopes.computeIfAbsent(principal, k -> new HashSet<>()).addAll(aggregatedScopes);
        }
    }

    /**
     * Aggregates scopes from policy resources using direct matching and schema fallback.
     */
    private static Set<String> aggregateScopes(Set<String> policyResources,
                                               Map<String, Set<String>> resourceToCaps,
                                               Set<String> allResourceKeys) {
        Set<String> aggregatedScopes = new HashSet<>();

        for (String rawResource : policyResources) {
            if (rawResource == null || rawResource.isEmpty()) continue;

            String resourceKey = rawResource.toLowerCase(DEFAULT_LOCALE);
            findAndAddScopes(resourceKey, resourceToCaps, allResourceKeys, aggregatedScopes);
        }

        return aggregatedScopes;
    }

    /**
     * Finds scopes for a resource key and adds them to the aggregated set.
     */
    private static void findAndAddScopes(String resourceKey,
                                         Map<String, Set<String>> resourceToCaps,
                                         Set<String> allResourceKeys,
                                         Set<String> aggregatedScopes) {
        // Direct match
        if (resourceToCaps.containsKey(resourceKey)) {
            aggregatedScopes.addAll(resourceToCaps.get(resourceKey));
            return;
        }

        // Case-insensitive match
        allResourceKeys.stream()
                .filter(key -> key.equalsIgnoreCase(resourceKey))
                .findFirst()
                .ifPresent(matchedKey -> aggregatedScopes.addAll(resourceToCaps.get(matchedKey)));
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
     * Extracts resource-action pairs from Cedar DSL policy.
     */
    private static Set<String> extractResourceActionPairs(String policy, JsonNode schemaNode) {
        Set<String> resources = extractResourcesFromPolicy(policy);
        Set<String> actions = extractActionsFromPolicy(policy);

        return buildResourceActionPairs(resources, actions, schemaNode);
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
     * Builds resource-action pairs combining resources and actions.
     */
    private static Set<String> buildResourceActionPairs(Set<String> resources, Set<String> actions, JsonNode schemaNode) {
        Set<String> pairs = new HashSet<>();

        for (String resource : resources) {
            Map<String, Set<String>> entityTypeToMembers = schemaNode == null ?
                    Collections.emptyMap() : buildEntityTypeIndex(schemaNode, resource);

            Set<String> resourceSet = entityTypeToMembers.keySet();
            for (String entity : resourceSet) {
                for (String action : actions) {
                    String pair = (entity + "~" + action).toLowerCase(DEFAULT_LOCALE).replace("\"", "");
                    pairs.add(pair);
                }
            }
        }

        return pairs;
    }

    /**
     * Normalizes resource by removing namespace prefix.
     */
    private static String normalizeResource(String value) {
        return value.replace(RESOURCE_PREFIX, "").trim();
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

        String value = node.asText("");
        return value.isEmpty() ? null : value;
    }

    /**
     * Builds entity type index from schema JSON.
     */
    private static Map<String, Set<String>> buildEntityTypeIndex(JsonNode schemaJson, String resource) {
        Map<String, Set<String>> index = new HashMap<>();
        if (schemaJson == null || resource == null) return index;

        String resourceLower = resource.toLowerCase(DEFAULT_LOCALE);
        JsonNode entityTypesNode = findEntityTypesNode(schemaJson);

        if (entityTypesNode != null && !entityTypesNode.isMissingNode()) {
            if (entityTypesNode.isObject()) {
                processEntityTypesObject(entityTypesNode, resourceLower, index);
            } else if (entityTypesNode.isArray()) {
                processEntityTypesArray(entityTypesNode, resourceLower, index);
            }
        }

        return index;
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
