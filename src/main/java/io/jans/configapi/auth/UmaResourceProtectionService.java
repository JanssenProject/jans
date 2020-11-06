package io.jans.configapi.auth;

import com.google.common.base.Preconditions;

import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsProtector;
import io.jans.ca.rs.protect.RsResource;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@ApplicationScoped
public class UmaResourceProtectionService {

    public static final String PROTECTION_CONFIGURATION_FILE_NAME = "uma-rs-protect.json";

    @Inject
    Logger log;

    @Inject
    UmaResourceProtectionCache umaResourceProtectionCache;

    @Inject
    ScopeService scopeService;

    @Inject
    UmaResourceService umaResourceService;

    Collection<RsResource> rsResourceList;

    public UmaResourceProtectionService() {

    }

    public Collection<RsResource> getResourceList() {
        return rsResourceList;
    }

    public void verifyUmaResources() throws Exception {

        // Load the uma resource json
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(PROTECTION_CONFIGURATION_FILE_NAME);
        log.debug(" \n\n UmaResourceProtectionService::verifyUmaResources() -modified **** inputStream = " + inputStream);
        this.rsResourceList = RsProtector.instance(inputStream).getResourceMap().values();

        log.debug(" \n\n UmaResourceProtectionService::verifyUmaResources() - rsResourceList = " + rsResourceList+ "\n\n");

        Preconditions.checkNotNull(rsResourceList, "Config Api Resource list cannot be null !!!");

        createScopeIfNeeded();
        
        createResourceIfNeeded();

        log.debug("\n\n UmaResourceProtectionCache.getAllUmaResources = " + UmaResourceProtectionCache.getAllUmaResources());

    }

    private void createScopeIfNeeded() { // todo rename createScopeIfNeeded
        // todo - cache scopes in guava cache. Otherwise you check same scopes again and
        // again

        List<String> rsScopes = null;
        for (RsResource rsResource : rsResourceList) {
            for (Condition condition : rsResource.getConditions()) {
                rsScopes = condition.getScopes();
                for (String scopeName : rsScopes) {
                   
                    // Check in cache
                    if (UmaResourceProtectionCache.getScope(scopeName) != null) {
                        log.debug("Scope - '" + scopeName + "' exists in cache.");
                        return;
                    }

                    // Check in DB
                    List<Scope> scopes = scopeService.searchScopes(scopeName, 2);
                    Scope scope = null;

                    if (scopes != null && !scopes.isEmpty()) {
                        // Fetch existing scope to store in cache
                        scope = scopes.get(0);
                        if (scopes.size() > 1) {
                            log.error(scopes.size() + " UMA Scope with same name.");
                            throw new WebApplicationException("Multiple UMA Scope with same name - "+scopeName,Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                        }
                    }

                    if (scopes == null || scopes.isEmpty()) {
                        log.debug("Scope - '" + scopeName + "' does not exist, hence creating it.");
                        // Scope does not exists hence create Scope
                        scope = new Scope();
                        String inum = UUID.randomUUID().toString();
                        scope.setId(scopeName);
                        scope.setDisplayName(scopeName);
                        scope.setInum(inum);
                        scope.setDn(scopeService.getDnForScope(inum));
                        scope.setScopeType(ScopeType.UMA);
                        scopeService.addScope(scope);
                    }

                    // Add to cache
                    UmaResourceProtectionCache.putScope(scope);

                }
            }
        }
    }

    public void createResourceIfNeeded() {
        log.debug(" \n\n UmaResourceProtectionService::createResourceIfNeeded() - rsResourceList = " + rsResourceList
                + "\n\n");

        Map<String, UmaResource> allResources = UmaResourceProtectionCache.getAllUmaResources();

        for (RsResource rsResource : rsResourceList) {

            for (Condition condition : rsResource.getConditions()) {
                String umaResourceName = condition.getHttpMethods() + ":::" + rsResource.getPath(); //??todo: Puja -> To be reviewed by Yuriy Z
                                                      
                // Check in cache
                if (UmaResourceProtectionCache.getUmaResource(umaResourceName) != null) {
                    log.debug("UmaResource - '" + umaResourceName + "' exists in cache.");
                    return;
                }

                // Check in DB
                List<UmaResource> umaResources = umaResourceService.findResources(umaResourceName, 2);
                UmaResource umaResource = null;

                if (umaResources != null && !umaResources.isEmpty()) {
                    // Fetch existing resources to store in cache
                    umaResource = umaResources.get(0);
                    if (umaResources.size() > 1) {
                        log.error(umaResources.size() + " UMA Resource with same name.");
                        throw new WebApplicationException("Multiple UMA Resource with same name - "+umaResourceName,
                                Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                    }
                }

                // Create Resource
                if (umaResources == null || umaResources.isEmpty()) {
                    log.debug("UmaResource - '" + umaResources + "' does not exist, hence creating it.");
                    umaResource = new UmaResource();
                    String id = UUID.randomUUID().toString();
                    umaResource.setId(id);
                    umaResource.setDn(umaResourceService.getDnForResource(id));
                    umaResource.setName(umaResourceName);
                    umaResource.setScopes(getScopes(condition.getScopes()));
                                                                           
                    umaResourceService.addResource(umaResource);
                }

                // Add to cache
                UmaResourceProtectionCache.putUmaResource(umaResourceName, umaResource);
            }
        }
    }

    // ??todo: Puja -> To be reviewed by Yuriy Z
    // Reason for this method::: This is required because the uma-rs-protect.json contains scope name example -> https://jans.io/oauth/config/scopes.readonly
    // However when i verified existing UMAresource like oxTrust SCIM in LDAP it has scope inum + orgnization rather than name 
    // Example: displayName = oxTrust api Resource , oxAuthUmaScope =inum=1122-BBCC,ou=scopes,o=gluu
    // Is this fine? 
    private List<String> getScopes(List<String> resourceScopes) {
        List<String> scopes = new ArrayList();
        log.debug("getScopes() - resourceScopes= " + resourceScopes);
        for (String strScope : resourceScopes) {
            Scope scope = UmaResourceProtectionCache.getScope(strScope);
            if (scope != null) {
                scopes.add(scope.getInum()+","+scope.getDn());
            }
        }
        log.debug("getScopes() - scopes= " + scopes);
        return scopes;

    }
}
