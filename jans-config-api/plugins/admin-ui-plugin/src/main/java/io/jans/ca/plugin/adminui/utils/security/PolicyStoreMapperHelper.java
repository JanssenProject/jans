package io.jans.ca.plugin.adminui.utils.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolicyStoreMapperHelper {

    private static final Locale DEFAULT_LOCALE = Locale.ROOT;

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        String normalized = parent.replace(PARENT_RESOURCE_PREFIX, "");

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
                    .map(PolicyStoreMapperHelper::cleanValue)
                    .map(PolicyStoreMapperHelper::normalizeResource)
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
                    .map(PolicyStoreMapperHelper::cleanValue)
                    .map(PolicyStoreMapperHelper::normalizeAction)
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
            JsonNode entityData = decodeEntityData(entry.getValue());

            if (isFeatureWithMatchingParent(entityData, resource)) {
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
