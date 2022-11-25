package io.jans.configapi.security.api;

import com.google.common.base.Preconditions;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.ScopeType;
import io.jans.configapi.core.protect.Condition;
import io.jans.configapi.core.protect.RsResource;
import io.jans.configapi.core.protect.RsResourceList;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.core.util.ProtectionScopeType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ApiProtectionService {

    public static final String PROTECTION_CONFIGURATION_FILE_NAME = "config-api-rs-protect.json";

    @Inject
    Logger log;

    @Inject
    ScopeService scopeService;

    @Inject
    ClientService clientService;

    @Inject
    ConfigurationFactory configurationFactory;

    Collection<RsResource> rsResourceList;

    public Collection<RsResource> getResourceList() {
        return rsResourceList;
    }

    public void verifyResources(String apiProtectionType, String clientId) throws IOException {
        log.error(
                "ApiProtectionService::verifyResources() - apiProtectionType:{}, clientId:{}, configurationFactory:{} ",
                apiProtectionType, clientId, configurationFactory);

        // Load the resource json
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(PROTECTION_CONFIGURATION_FILE_NAME);

        RsResourceList resourceList = Jackson.createJsonMapper().readValue(inputStream, RsResourceList.class);
        this.rsResourceList = resourceList.getResources();
        log.error("verifyResources() - rsResourceList{} ", rsResourceList);

        Preconditions.checkNotNull(rsResourceList, "Config Api Resource list cannot be null !!!");

        createScopeIfNeeded(apiProtectionType);
        log.error(
                "ApiProtectionService:::verifyResources() - getAllTypesOfScopes:{}, allResources:{}, getAllResourcesMap:{} ",
                ApiProtectionCache.getAllTypesOfScopes(), ApiProtectionCache.getAllResources(),
                ApiProtectionCache.getAllResourcesMap());

        updateScopeForClientIfNeeded(clientId);

    }

    private void createScopeIfNeeded(String apiProtectionType) {
        log.error("ApiProtectionService:::createScopeIfNeeded() - apiProtectionType:{}", apiProtectionType);

        List<Scope> scopeList = new ArrayList<>();
        for (RsResource rsResource : rsResourceList) {
            for (Condition condition : rsResource.getConditions()) {
                String resourceName = condition.getHttpMethods() + ":::" + rsResource.getPath();

                log.error(
                        "ApiProtectionService:::createScopeIfNeeded() - resourceName:{}, condition.getScopes():{}, condition.getGroupScopes():{}, condition.getSuperScopes():{}",
                        resourceName, condition.getScopes(), condition.getGroupScopes(), condition.getSuperScopes());

                // Process Scopes
                // If no scopes for the path then skip validation
                List<io.jans.configapi.core.protect.Scope> rsScopes = condition.getScopes();
                if (rsScopes != null && !rsScopes.isEmpty()) {
                    processScope(resourceName, ProtectionScopeType.SCOPE, rsScopes);
                }

                // If no group scopes for the path then skip validation
                List<io.jans.configapi.core.protect.Scope> groupScopes = condition.getGroupScopes();
                if (groupScopes != null && !groupScopes.isEmpty()) {
                    processScope(resourceName, ProtectionScopeType.GROUP, groupScopes);
                }

                // If no super scopes for the path then skip validation
                List<io.jans.configapi.core.protect.Scope> superScopes = condition.getSuperScopes();
                if (superScopes != null && !superScopes.isEmpty()) {
                    processScope(resourceName, ProtectionScopeType.SUPER, superScopes);
                }

                log.error("ApiProtectionService:::createScopeIfNeeded() - resourceName:{}, scopeList:{}", resourceName,
                        scopeList);

            } // condition
        }
    }

    private void processScope(String resourceName, ProtectionScopeType protectionScopeType,
            List<io.jans.configapi.core.protect.Scope> scopeList) {
        log.error("ApiProtectionService:::processScope() - resourceName:{}, protectionScopeType:{}, scopeList:{}",
                resourceName, protectionScopeType, scopeList);

        // return if no scopes
        if (scopeList == null || scopeList.isEmpty()) {
            return;
        }

        for (io.jans.configapi.core.protect.Scope rsScope : scopeList) {
            String inum = rsScope.getInum();
            String scopeName = rsScope.getName();
            log.error("ApiProtectionService:::processScope() - resourceName:{}, inum:{}, scopeName:{}", resourceName,
                    inum, scopeName);

            // return if no scope details
            if (StringUtils.isBlank(inum) || StringUtils.isBlank(scopeName)) {
                return;
            }

            List<Scope> scopes = validateScope(resourceName, protectionScopeType, rsScope);
            ApiProtectionCache.putResource(resourceName, scopes);
            ApiProtectionCache.putResourceScopeByType(resourceName, protectionScopeType, scopes);
        }
    }

    private List<Scope> validateScope(String resourceName, ProtectionScopeType protectionScopeType,
            io.jans.configapi.core.protect.Scope rsScope) {
        log.error("Verify Scope in DB - protectionScopeType:{}, rsScope:{} ", protectionScopeType, rsScope);
        List<Scope> scopeList = new ArrayList<>();

        // Check in DB
        Scope scope = scopeService.getScope(rsScope.getInum());
        log.error("Scopes from DB - {}'", scope);

        if (scope != null) {
            // Fetch existing scope to store in cache
            log.error("Scope from DB  scope.getInum():{}, scope.getId():{}", scope.getInum(), scope.getId());
            scopeList.add(scope);
        }

        ScopeType scopeType = ScopeType.OAUTH;
        log.error(
                "Scope details - scope:{}, rsScope.getName():{}, exclusiveAuthScopes:{}, isConfigApiScope(scopeName):{} '",
                scope, rsScope.getName(), configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes(),
                isConfigApiScope(rsScope.getName()));

        // Create/Update scope only if they are config-api-resource scopes
        if (isConfigApiScope(rsScope.getName())) {

            // ensure scope does not exists
            scope = scopeService.getScope(rsScope.getInum());
            log.error("Re-verify ConfigApiScope rsScope.getName():{} with rsScope.getInum():{} in DB - scope:{} ",
                    rsScope.getName(), rsScope.getInum(), scope);
            if (scope == null) {
                log.error("Scope - '{}' does not exist, hence creating it.", scope);
                // Scope does not exists hence create Scope
                scope = new Scope();
                String inum = rsScope.getInum();
                scope.setId(rsScope.getName());
                scope.setDisplayName(rsScope.getName());
                scope.setInum(inum);
                scope.setDn(scopeService.getDnForScope(inum));
                scope.setScopeType(scopeType);
                scopeService.addScope(scope);
            }

            // Update resource
            log.error("Scope - '{}' already exists, hence updating it.", rsScope.getName());
            scope.setId(rsScope.getName());
            scope.setScopeType(scopeType);
            scopeService.updateScope(scope);

        }

        // Add to scope if not null
        if (scope != null) {
            scopeList.add(scope);
            ApiProtectionCache.addScope(resourceName, protectionScopeType, scope);
        }
        return scopeList;
    }

    private boolean isConfigApiScope(String scopeName) {
        return (configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes() == null
                || !configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes().contains(scopeName));
    }

    private void updateScopeForClientIfNeeded(String clientId) {
        log.error(" Internal clientId:{} ", clientId);

        if (StringUtils.isBlank(clientId)) {
            return;
        }

        try {
            Client client = this.clientService.getClientByInum(clientId);
            log.error("updateScopeForClientIfNeeded() - Verify client:{} ", client);

            if (client != null) {
                // Assign scope
                // Prepare scope array
                List<String> scopes = getScopeWithDn(getAllScopes());
                log.error("updateScopeForClientIfNeeded() - All scopes:{}", scopes);

                if (client.getScopes() != null) {
                    List<String> existingScopes = Arrays.asList(client.getScopes());
                    log.error("updateScopeForClientIfNeeded() - Clients existing scopes:{} ", existingScopes);
                    if (scopes == null) {
                        scopes = new ArrayList<>();
                    }
                    scopes.addAll(existingScopes);
                }

                // Distinct scopes
                List<String> distinctScopes = (scopes == null ? Collections.emptyList()
                        : scopes.stream().distinct().collect(Collectors.toList()));
                log.error("updateScopeForClientIfNeeded() - Distinct scopes to add:{} ", distinctScopes);

                String[] scopeArray = this.getAllScopesArray(distinctScopes);
                log.error("All Scope to assign to client:{}", Arrays.asList(scopeArray));

                client.setScopes(scopeArray);
                this.clientService.updateClient(client);
            }
            client = this.clientService.getClientByInum(clientId);
            log.error(" Verify scopes post assignment, clientId:{}, scopes:{}", clientId,
                    Arrays.asList(client.getScopes()));
        } catch (Exception ex) {
            log.error("Error while searching internal client", ex);
        }

    }

    private List<String> getAllScopes() {
        List<String> scopes = new ArrayList<>();

        // Verify in cache
        Map<String, Scope> scopeMap = ApiProtectionCache.getAllTypesOfScopes();
        Set<String> keys = scopeMap.keySet();
        log.error(" All Scopes scopeMap:{}, keys:{}", scopeMap, keys);

        for (String id : keys) {
            Scope scope = scopeMap.get(id);
            log.error(" All Scopes scopeMap:{}, keys:{}", scopeMap, keys);
            scopes.add(scope.getInum());
        }
        log.error(" All Scopes being returned scopes:{}", scopes);
        return scopes;
    }

    private String[] getAllScopesArray(List<String> scopes) {
        String[] scopeArray = null;

        if (scopes != null && !scopes.isEmpty()) {
            scopeArray = new String[scopes.size()];
            for (int i = 0; i < scopes.size(); i++) {
                scopeArray[i] = scopes.get(i);
            }
        }
        return scopeArray;
    }

    private List<String> getScopeWithDn(List<String> scopes) {
        List<String> scopeList = null;
        if (scopes != null && !scopes.isEmpty()) {
            scopeList = new ArrayList<>();
            for (String id : scopes) {
                scopeList.add(this.scopeService.getDnForScope(id));
            }
        }
        return scopeList;
    }

}
