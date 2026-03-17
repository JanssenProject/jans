package io.jans.ca.plugin.adminui.utils.security;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
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


    // Cache for scope mappings
    private static Map<String, Map<String, List<String>>> scopeMappingCache;
    private static Map<String, List<String>> parentToFeaturesMap;

    public Map<String, Set<String>> processZipFile(ZipFile zipFile, JsonNode resourcesJson) throws Exception {
        long startTime = System.currentTimeMillis();

        // Load scope mappings once and cache them
        scopeMappingCache = buildScopeMappingCache(resourcesJson);

        // Load entity relationships
        parentToFeaturesMap = loadEntityRelationships(zipFile);

        log.trace("Total processing time for generating role-to-scope mapping from the policy-store: {} ms", System.currentTimeMillis() - startTime);
        return processPolicies(zipFile);
    }

    private Map<String, Map<String, List<String>>> buildScopeMappingCache(JsonNode resourcesJson) throws IOException {
        Map<String, Map<String, List<String>>> cache = new HashMap<>();

        for (JsonNode mapping : resourcesJson) {
            String resource = mapping.get("resource").asText().toLowerCase();
            String accessType = mapping.get("accessType").asText();
            List<String> scopes = new ArrayList<>();

            for (JsonNode scope : mapping.get("scopes")) {
                scopes.add(scope.asText());
            }

            cache.computeIfAbsent(resource, k -> new HashMap<>())
                    .put(accessType, Collections.unmodifiableList(scopes));
        }

        log.trace("Successfully loaded {} resource-to-scope mappings with {} access types", cache.size(), cache.values().stream().mapToInt(Map::size).sum());
        return cache;
    }

    private Map<String, List<String>> loadEntityRelationships(ZipFile zipFile) throws IOException {
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

    private void processEntityEntry(ZipFile zipFile, ZipEntry entry, Map<String, List<String>> parentToFeatures) {
        try (InputStream is = zipFile.getInputStream(entry)) {
            JsonNode entities = objectMapper.readTree(is);

            if (!entities.isArray()) return;

            for (JsonNode entity : entities) {
                JsonNode uid = entity.get("uid");
                if (uid == null) continue;

                if (!GLUU_FLEX_ADMINUI_RESOURCES_FEATURES.equals(uid.get("type").asText())) continue;

                String featureId = uid.get("id").asText();

                JsonNode parents = entity.get("parents");
                if (parents != null && parents.isArray() && parents.size() > 0) {
                    String parentId = parents.get(0).get("id").asText();
                    parentToFeatures.computeIfAbsent(parentId, k -> new CopyOnWriteArrayList<>()).add(featureId);
                }
            }
        } catch (IOException e) {
            log.trace("Error parsing entity: {} - {}", entry.getName(), e.getMessage());
        }
    }

    private Map<String, Set<String>> processPolicies(ZipFile zipFile) {
        // Use ConcurrentHashMap with CopyOnWriteArraySet for thread-safe aggregation
        Map<String, Set<String>> roleToScopes = new ConcurrentHashMap<>();

        List<ZipEntry> policyEntries = (List<ZipEntry>) zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith("policies/"))
                .filter(entry -> entry.getName().endsWith(".cedar") || entry.getName().endsWith(".cedarpl"))
                .toList();

        if (policyEntries.isEmpty()) {
            throw new IllegalStateException("No policy files found in policies folder");
        }

        log.trace("Processing {} policy files.", policyEntries.size());

        // Process policies in parallel for better performance
        policyEntries.parallelStream().forEach(entry ->
                processPolicyEntry(zipFile, entry, roleToScopes)
        );

        return roleToScopes;
    }

    private void processPolicyEntry(ZipFile zipFile, ZipEntry entry, Map<String, Set<String>> roleToScopes) {
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

            Set<String> scopes = resolveScopes(resource, actions);

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

    private String extractFirstMatch(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> extractAllMatches(Pattern pattern, String content) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            matches.add(matcher.group(1).toUpperCase());
        }
        return matches;
    }

    private Set<String> resolveScopes(String resource, List<String> actions) {
        Set<String> scopes = new HashSet<>();

        // Get all features to process (either single resource or multiple from parent)
        List<String> targetResources = parentToFeaturesMap.getOrDefault(resource, List.of(resource));

        for (String targetResource : targetResources) {
            Map<String, List<String>> resourceScopes = scopeMappingCache.get(targetResource.toLowerCase());

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