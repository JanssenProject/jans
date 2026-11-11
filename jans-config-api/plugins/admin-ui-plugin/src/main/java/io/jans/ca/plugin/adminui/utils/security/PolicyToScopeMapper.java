package io.jans.ca.plugin.adminui.utils.security;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.zip.*;

import com.fasterxml.jackson.databind.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

@Singleton
public class PolicyToScopeMapper {

    @Inject
    Logger log;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern ROLE_PATTERN = Pattern.compile(
            "getTag\\(\"jansAdminUIRole\"\\)\\.contains\\(\"([^\"]+)\"\\)"
    );
    private static final Pattern RESOURCE_PATTERN = Pattern.compile(
            "resource in GluuFlexAdminUIResources::(?:ParentResource|Features)::\"([^\"]+)\""
    );
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "GluuFlexAdminUI::Action::\"([^\"]+)\""
    );

    public static final String GLUU_FLEX_ADMINUI_RESOURCES_FEATURES = "GluuFlexAdminUIResources::Features";


    /**
     * Generate a mapping from role names to their aggregated scopes by processing a policy-store ZIP and a resources JSON.
     *
     * @param zipFile       the ZIP file containing policies (policies/*.cedar|*.cedarpl) and entity definitions (entities/*.json)
     * @param resourcesJson JSON structure that maps resources and access types to scope lists (used to build the scope mapping cache)
     * @return a map where each key is a role name and the value is the set of resolved scope strings for that role
     */
    public Map<String, Set<String>> processZipFile(ZipFile zipFile, JsonNode resourcesJson) {
        long startTime = System.currentTimeMillis();
        // Build mappings locally for this invocation
        Map<String, Map<String, List<String>>> scopeMapping = buildScopeMappingCache(resourcesJson);
        // Load entity relationships
        Map<String, List<String>> parentToFeatures = loadEntityRelationships(zipFile);

        log.trace("Total processing time for generating role-to-scope mapping from the policy-store: {} ms", System.currentTimeMillis() - startTime);
        return processPolicies(zipFile, scopeMapping, parentToFeatures);
    }

    /**
     * Builds a lookup cache mapping resource identifiers to access-type-to-scopes mappings.
     * The provided `resourcesJson` is expected to be an array of objects where each object contains
     * `resource` (string), `accessType` (string), and `scopes` (array of strings). Resource keys are
     * normalized to lower-case when stored.
     *
     * @param resourcesJson JSON array of resource→accessType→scopes mappings
     * @return a map where keys are lower-cased resource identifiers and values are maps from access type
     * to an unmodifiable list of scope strings
     */
    private Map<String, Map<String, List<String>>> buildScopeMappingCache(JsonNode resourcesJson) {
        Map<String, Map<String, List<String>>> cache = new HashMap<>();

        for (JsonNode mapping : resourcesJson) {
            JsonNode resourceNode = mapping.get("resource");
            JsonNode accessTypeNode = mapping.get("accessType");
            JsonNode scopesNode = mapping.get("scopes");

            if (resourceNode == null || accessTypeNode == null || scopesNode == null) {
                log.warn("Skipping malformed mapping entry: missing required field");
                continue;
            }

            String resource = resourceNode.asText().toLowerCase();
            String accessType = accessTypeNode.asText();
            List<String> scopes = new ArrayList<>();

            for (JsonNode scope : scopesNode) {
                scopes.add(scope.asText());
            }

            cache.computeIfAbsent(resource, k -> new HashMap<>())
                    .put(accessType, Collections.unmodifiableList(scopes));
        }

        log.trace("Successfully loaded {} resource-to-scope mappings with {} access types", cache.size(), cache.values().stream().mapToInt(Map::size).sum());
        return cache;
    }

    /**
     * Scans the ZIP for JSON entity files under the "entities/" directory and builds a map of parent IDs to their child feature IDs.
     * <p>
     * Each matching entity file is parsed and any discovered parent→feature relationships are added to the returned map; invalid or unreadable entity files are ignored (errors are logged).
     *
     * @param zipFile ZIP file containing entity JSON files under the "entities/" path
     * @return a map where keys are parent IDs and values are lists of feature IDs; empty if no relationships were found
     */
    private Map<String, List<String>> loadEntityRelationships(ZipFile zipFile) {
        Map<String, List<String>> parentToFeatures = new HashMap<>();

        zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith("entities/"))
                .filter(entry -> entry.getName().endsWith(".json"))
                .forEach(entry -> processEntityEntry(zipFile, entry, parentToFeatures));

        if (parentToFeatures.isEmpty()) {
            log.trace("No entity relationships found");
        } else {
            int totalFeatures = parentToFeatures.values().stream().mapToInt(List::size).sum();
            log.trace("Loaded {} parent relationships covering {} features", parentToFeatures.size(), totalFeatures);
        }

        return parentToFeatures;
    }

    /**
     * Parses a single entity JSON file from the ZIP and records feature → parent relationships into the provided map.
     *
     * <p>Reads the ZIP entry as JSON; if the root is an array, iterates its elements and for each element whose
     * `uid.type` equals {@code GLUU_FLEX_ADMINUI_RESOURCES_FEATURES} extracts `uid.id` as the feature ID and,
     * if a non-empty `parents` array exists, takes the first parent's `id` and adds the feature ID to the
     * list stored under that parent ID in {@code parentToFeatures}.</p>
     *
     * <p>Malformed entries or missing fields are skipped. IO parsing errors are logged and not rethrown.</p>
     *
     * @param zipFile          the ZIP archive containing the entry
     * @param entry            the ZIP entry pointing to an entity JSON file
     * @param parentToFeatures map that will be populated with parentId → list of featureIds; lists are created as
     *                         {@link java.util.concurrent.CopyOnWriteArrayList} when a parentId is first encountered
     */
    private void processEntityEntry(ZipFile zipFile, ZipEntry entry, Map<String, List<String>> parentToFeatures) {
        try (InputStream is = zipFile.getInputStream(entry)) {
            JsonNode entities = objectMapper.readTree(is);

            if (!entities.isArray()) return;

            for (JsonNode entity : entities) {
                JsonNode uid = entity.get("uid");
                if (uid == null) continue;

                JsonNode typeNode = uid.get("type");
                if (typeNode == null || !GLUU_FLEX_ADMINUI_RESOURCES_FEATURES.equals(typeNode.asText())) continue;

                JsonNode idNode = uid.get("id");
                if (idNode == null) continue;
                String featureId = idNode.asText();

                JsonNode parents = entity.get("parents");
                if (parents != null && parents.isArray() && !parents.isEmpty()) {
                    JsonNode firstParent = parents.get(0);
                    JsonNode parentIdNode = firstParent != null ? firstParent.get("id") : null;
                    if (parentIdNode == null) continue;
                    String parentId = parentIdNode.asText();
                    parentToFeatures.computeIfAbsent(parentId, k -> new CopyOnWriteArrayList<>()).add(featureId);
                }
            }
        } catch (IOException e) {
            log.error("Error parsing entity: {} - {}", entry.getName(), e.getMessage());
        }
    }

    /**
     * Builds a mapping from role name to the set of scopes aggregated from policy files found in the ZIP.
     *
     * @param zipFile          the ZIP archive to scan for policy files under the "policies/" directory
     * @param scopeMapping
     * @param parentToFeatures
     * @return a map whose keys are role names and whose values are the sets of scopes assigned to those roles
     * @throws IllegalStateException if no policy files (*.cedar or *.cedarpl) are present in the "policies/" folder
     */
    private Map<String, Set<String>> processPolicies(ZipFile zipFile, Map<String, Map<String, List<String>>> scopeMapping, Map<String, List<String>> parentToFeatures) {
        // Use ConcurrentHashMap with CopyOnWriteArraySet for thread-safe aggregation
        Map<String, Set<String>> roleToScopes = new ConcurrentHashMap<>();

        List<ZipEntry> policyEntries = (List<ZipEntry>) zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith("policies/"))
                .filter(entry -> entry.getName().endsWith(".cedar") || entry.getName().endsWith(".cedarpl"))
                .collect(Collectors.toList());

        if (policyEntries.isEmpty()) {
            throw new IllegalStateException("No policy files found in policies folder");
        }

        log.trace("Processing {} policy files.", policyEntries.size());

        // Process policies in parallel for better performance
        policyEntries.parallelStream().forEach(entry ->
                processPolicyEntry(zipFile, entry, roleToScopes, scopeMapping, parentToFeatures)
        );

        return roleToScopes;
    }

    /**
     * Parses a single policy file from the given ZIP entry, extracts the role, resource, and actions,
     * resolves those into scopes, and merges the resulting scopes into the provided role-to-scopes map.
     *
     * @param zipFile      the ZIP archive containing the policy entry
     * @param entry        the ZIP entry for the policy file to process
     * @param roleToScopes a concurrent map that accumulates scopes per role; scopes for the extracted role
     *                     will be added to the existing set (created if absent)
     */
    private void processPolicyEntry(ZipFile zipFile, ZipEntry entry, Map<String, Set<String>> roleToScopes, Map<String, Map<String, List<String>>> scopeMapping, Map<String, List<String>> parentToFeatures) {
        try (InputStream is = zipFile.getInputStream(entry)) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            String role = extractFirstMatch(ROLE_PATTERN, content);
            if (role == null) {
                log.warn("Warning: No role found in policy: {}", entry.getName());
                return;
            }

            String resource = extractFirstMatch(RESOURCE_PATTERN, content);
            if (resource == null) {
                log.warn("Warning: No resource found in policy: {}", entry.getName());
                return;
            }

            List<String> actions = extractAllMatches(ACTION_PATTERN, content);
            if (actions.isEmpty()) {
                log.warn("Warning: No actions found in policy: {}", entry.getName());
                return;
            }

            Set<String> scopes = resolveScopes(resource, actions, scopeMapping, parentToFeatures);

            if (!scopes.isEmpty()) {
                // This automatically aggregates scopes for the same role across multiple files
                roleToScopes.computeIfAbsent(role, k -> ConcurrentHashMap.newKeySet())
                        .addAll(scopes);
                log.trace("Added {} scopes for role {} from {}",
                        scopes.size(), role, entry.getName());
            }

        } catch (IOException e) {
            log.error("Error processing policy: {}", entry.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Extracts the first capturing-group match from the given content using the provided regex pattern.
     *
     * @param pattern the regular expression to apply; must contain at least one capturing group
     * @param content the text to search for a match
     * @return the text captured by the first capturing group, or `null` if the pattern does not match
     */
    private String extractFirstMatch(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Finds all matches of the provided regex pattern in the given content and returns the first
     * capturing group's values converted to upper case, preserving match order.
     *
     * @param pattern a compiled regex that contains at least one capturing group; the method
     *                extracts group 1 from each match
     * @param content the text to search for pattern matches
     * @return a list of group-1 match strings converted to upper case, in the order they were found
     */
    private List<String> extractAllMatches(Pattern pattern, String content) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            matches.add(matcher.group(1).toUpperCase());
        }
        return matches;
    }

    /**
     * Resolve and aggregate authorization scopes for a given resource and list of actions.
     * <p>
     * If the provided resource maps to multiple child features, scopes for each child are
     * included. For each target resource, this looks up configured scopes for each action
     * and returns the union of all matching scopes.
     *
     * @param resource         the resource identifier or parent resource whose scopes should be resolved
     * @param actions          list of action names to resolve scopes for (e.g., "READ", "WRITE")
     * @param scopeMapping
     * @param parentToFeatures
     * @return the set of matching scope strings for the given resource and actions (may be empty)
     */
    private Set<String> resolveScopes(String resource, List<String> actions, Map<String, Map<String, List<String>>> scopeMapping, Map<String, List<String>> parentToFeatures) {
        Set<String> scopes = new HashSet<>();

        // Get all features to process (either single resource or multiple from parent)
        List<String> targetResources = parentToFeatures.getOrDefault(resource, List.of(resource));

        for (String targetResource : targetResources) {
            Map<String, List<String>> resourceScopes = scopeMapping.get(targetResource.toLowerCase());

            if (resourceScopes != null) {
                for (String action : actions) {
                    List<String> actionScopes = resourceScopes.get(action);
                    if (actionScopes != null) {
                        scopes.addAll(actionScopes);
                    }
                }
            }
        }

        return scopes;
    }
}