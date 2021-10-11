package io.jans.configapi.security.api;

import com.google.common.base.Preconditions;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.ScopeType;
import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.configapi.util.Jackson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
        log.debug(
                "ApiProtectionService::verifyResources() - apiProtectionType:{}, clientId:{}, configurationFactory:{} ",
                apiProtectionType, clientId, configurationFactory);

        // Load the resource json
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(PROTECTION_CONFIGURATION_FILE_NAME);

        RsResourceList resourceList = Jackson.createJsonMapper().readValue(inputStream, RsResourceList.class);
        this.rsResourceList = resourceList.getResources();
        log.debug("verifyResources() - rsResourceList{} ", rsResourceList);

        Preconditions.checkNotNull(rsResourceList, "Config Api Resource list cannot be null !!!");

        createScopeIfNeeded(apiProtectionType);
        log.trace("ApiProtectionService:::verifyResources() - allScopes:{}, allResources:{} ",
                ApiProtectionCache.getAllScopes(), ApiProtectionCache.getAllResources());

        updateScopeForClientIfNeeded(clientId);

    }

    private void createScopeIfNeeded(String apiProtectionType) {
        log.debug("ApiProtectionService:::createScopeIfNeeded() - apiProtectionType:{}", apiProtectionType);

        List<String> rsScopes = null;
        List<Scope> scopeList = new ArrayList<>();
        for (RsResource rsResource : rsResourceList) {
            for (Condition condition : rsResource.getConditions()) {
                String resourceName = condition.getHttpMethods() + ":::" + rsResource.getPath();
                rsScopes = condition.getScopes();
                log.trace("ApiProtectionService:::createScopeIfNeeded() - resourceName:{}, rsScopes:{} ", resourceName,
                        rsScopes);

                // If no scopes for the path then skip validation
                if (rsScopes == null || rsScopes.isEmpty()) {
                    break;
                }

                for (String scopeName : rsScopes) {
                    log.debug("ApiProtectionService:::createScopeIfNeeded() - scopeName:{} ", scopeName);

                    // Check in cache
                    Scope scope = ApiProtectionCache.getScope(scopeName);
                    log.debug(
                            "ApiProtectionService:::createScopeIfNeeded() - ApiProtectionCache.getScope(scopeName):{}",
                            ApiProtectionCache.getScope(scopeName));

                    if (scope != null) {
                        log.debug("Scope - '{}' exists in cache.", scopeName);
                        scopeList.add(scope);
                        break;
                    }

                    // validate scope
                    scopeList = validateScope(scopeName);

                } // for scopes

                // Add to resource cache
                ApiProtectionCache.putResource(resourceName, scopeList);
                log.debug("ApiProtectionService:::createScopeIfNeeded() - resourceName:{}, scopeList:{}", resourceName,
                        scopeList);

            } // condition
        }
    }

    private List<Scope> validateScope(String scopeName) {
        List<Scope> scopeList = new ArrayList<>();
        Scope scope = null;
        // Check in DB
        log.debug("Verify Scope in DB - {} ", scopeName);
        List<Scope> scopes = scopeService.searchScopesById(scopeName);
        log.debug("Scopes from DB - {}'", scopes);

        if (scopes != null && !scopes.isEmpty()) {
            // Fetch existing scope to store in cache
            scope = scopes.get(0);
            log.debug("Scope from DB is - {}", scope.getId());
            scopeList.add(scope);
            if (scopes.size() > 1) {
                log.error("{} Scope with same name - {} ", scopes.size(), scopeName);
                throw new WebApplicationException("Multiple Scope with same name - " + scopeName,
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            }
        }

        ScopeType scopeType = ScopeType.OAUTH;
        log.trace("Scope details - scopes:{}, scopeName:{}, exclusiveAuthScopes:{}, isConfigApiScope(scopeName):{} '",
                scopes, scopeName, configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes(),
                isConfigApiScope(scopeName));

        // Create/Update scope only if they are config-api-resource scopes
        if (isConfigApiScope(scopeName)) {

            if (scopes == null || scopes.isEmpty()) {
                log.debug("Scope - '{}' does not exist, hence creating it.", scopeName);
                // Scope does not exists hence create Scope
                scope = new Scope();
                String inum = UUID.randomUUID().toString();
                scope.setId(scopeName);
                scope.setDisplayName(scopeName);
                scope.setInum(inum);
                scope.setDn(scopeService.getDnForScope(inum));
                scope.setScopeType(scopeType);
                scopeService.addScope(scope);
            }
            if (scope != null) {
                // Update resource
                log.debug("Scope - '{}' already exists, hence updating it.", scopeName);
                scope.setId(scopeName);
                scope.setScopeType(scopeType);
                scopeService.updateScope(scope);
            }
        }

        // Add to scope cache anyways
        scopeList.add(scope);
        ApiProtectionCache.putScope(scope);
        return scopeList;
    }

    private boolean isConfigApiScope(String scopeName) {
        return (configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes() == null
                || !configurationFactory.getApiAppConfiguration().getExclusiveAuthScopes().contains(scopeName));
    }

    private void updateScopeForClientIfNeeded(String clientId) {
        log.debug(" Internal clientId:{} ", clientId);

        if (StringUtils.isBlank(clientId)) {
            return;
        }

        try {
            Client client = this.clientService.getClientByInum(clientId);
            log.debug("updateScopeForClientIfNeeded() - Verify client:{} ", client);

            if (client != null) {
                // Assign scope
                // Prepare scope array
                List<String> scopes = getScopeWithDn(getAllScopes());
                log.trace("updateScopeForClientIfNeeded() - All scopes:{}", scopes);

                if (client.getScopes() != null) {
                    List<String> existingScopes = Arrays.asList(client.getScopes());
                    log.trace("updateScopeForClientIfNeeded() - Clients existing scopes:{} ", existingScopes);
                    if (scopes == null) {
                        scopes = new ArrayList<>();
                    }
                    scopes.addAll(existingScopes);
                }

                // Distinct scopes
                List<String> distinctScopes = (scopes == null ? Collections.emptyList()
                        : scopes.stream().distinct().collect(Collectors.toList()));
                log.debug("updateScopeForClientIfNeeded() - Distinct scopes to add:{} ", distinctScopes);

                String[] scopeArray = this.getAllScopesArray(distinctScopes);
                log.debug("All Scope to assign to client:{}", Arrays.asList(scopeArray));

                client.setScopes(scopeArray);
                this.clientService.updateClient(client);
            }
            client = this.clientService.getClientByInum(clientId);
            log.debug(" Verify scopes post assignment, clientId:{}, scopes:{}", clientId,
                    Arrays.asList(client.getScopes()));
        } catch (Exception ex) {
            log.error("Error while searching internal client", ex);
        }

    }

    private List<String> getAllScopes() {
        List<String> scopes = new ArrayList<>();

        // Verify in cache
        Map<String, Scope> scopeMap = ApiProtectionCache.getAllScopes();
        Set<String> keys = scopeMap.keySet();

        for (String id : keys) {
            Scope scope = ApiProtectionCache.getScope(id);
            scopes.add(scope.getInum());
        }
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
