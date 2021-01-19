package io.jans.configapi.auth;

import com.google.common.base.Preconditions;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.ScopeType;
import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;

import io.jans.as.persistence.model.Scope;
import io.jans.configapi.service.ClientService;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.util.Jackson;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class ConfigApiResourceProtectionService {

    public static final String PROTECTION_CONFIGURATION_FILE_NAME = "config-api-rs-protect.json";

    @Inject
    Logger log;

    @Inject
    EncryptionService encryptionService;

    @Inject
    ConfigApiProtectionCache configApiProtectionCache;

    @Inject
    ScopeService scopeService;

    @Inject
    ClientService clientService;

    Collection<RsResource> rsResourceList;

    public ConfigApiResourceProtectionService() {

    }

    public Collection<RsResource> getResourceList() {
        return rsResourceList;
    }

    public void verifyResources(String apiProtectionType) throws Exception {
        log.debug(
                "\n ConfigApiResourceProtectionService::verifyResources() - apiProtectionType = " + apiProtectionType + "\n");
        // Load the resource json
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(PROTECTION_CONFIGURATION_FILE_NAME);

        RsResourceList resourceList = Jackson.createJsonMapper().readValue(inputStream, RsResourceList.class);
        this.rsResourceList = resourceList.getResources();
        log.trace("verifyResources() - rsResourceList{} ", rsResourceList);

        Preconditions.checkNotNull(rsResourceList, "Config Api Resource list cannot be null !!!");

        createScopeIfNeeded(apiProtectionType);
        log.trace("ConfigApiResourceProtectionService:::verifyResources() - configApiProtectionCache.getAllScopes() = "+configApiProtectionCache.getAllScopes()+"\n\n");
        log.trace("ConfigApiResourceProtectionService:::verifyResources() - configApiProtectionCache.getAllResources() = "+configApiProtectionCache.getAllResources()+"\n\n");

    }

    private void createScopeIfNeeded(String apiProtectionType) {
        log.trace("ConfigApiResourceProtectionService:::createScopeIfNeeded() - apiProtectionType = "+apiProtectionType+"\n ***********");
        List<String> rsScopes = null;
        List<Scope> scopeList = null;
        for (RsResource rsResource : rsResourceList) {
            for (Condition condition : rsResource.getConditions()) {
            	String resourceName = condition.getHttpMethods() + ":::" + rsResource.getPath();
            	scopeList = new ArrayList<Scope>();
            	rsScopes = condition.getScopes();
            	log.trace("ConfigApiResourceProtectionService:::createScopeIfNeeded() - resourceName = "+resourceName+" ,rsScopes = "+rsScopes+"\n\n");
            	
                for (String scopeName : rsScopes) {

                    // Check in cache
                	Scope scope = configApiProtectionCache.getScope(scopeName) ;
                    if (scope!= null) {
                        log.trace("Scope - '" + scopeName + "' exists in cache.");
                        scopeList.add(scope);
                        break;
                    }
                    // Check in DB
                    List<Scope> scopes = scopeService.searchScopes(scopeName, 2);
                    if (scopes != null && !scopes.isEmpty()) {
                        // Fetch existing scope to store in cache
                        scope = scopes.get(0);
                        if (scopes.size() > 1) {
                            log.error(scopes.size() + " Scope with same name.");
                            throw new WebApplicationException("Multiple Scope with same name - " + scopeName,
                                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                        }
                    }

                    ScopeType scopeType = ScopeType.OAUTH;
                    
                    if (scopes == null || scopes.isEmpty()) {
                        log.trace("Scope - '" + scopeName + "' does not exist, hence creating it.");
                        // Scope does not exists hence create Scope
                        scope = new Scope();
                        String inum = UUID.randomUUID().toString();
                        scope.setId(scopeName);
                        scope.setDisplayName(scopeName);
                        scope.setInum(inum);
                        scope.setDn(scopeService.getDnForScope(inum));
                        scope.setScopeType(scopeType);
                        scopeService.addScope(scope);
                    } else {
                        // Update resource
                        log.trace("Scope - '" + scopeName + "' already exists, hence updating it.");

                        scope.setId(scopeName);
                        scope.setDisplayName(scopeName);
                        scope.setDn(scopeService.getDnForScope(scope.getInum()));
                        scope.setScopeType(scopeType);
                        scopeService.updateScope(scope);
                    }
                    

                    // Add to scope cache
                    scopeList.add(scope);
                    configApiProtectionCache.putScope(scope);
                }//for scopes
                
                // Add to resource cache
                configApiProtectionCache.putResource(resourceName, scopeList);
                log.trace("ConfigApiResourceProtectionService:::createScopeIfNeeded() - resourceName = "+resourceName+" ,scopeList = "+scopeList);
            }//condition            
        }
    }

   
    private Date getCreationDate(RsResource rsResource) {
        final Calendar calendar = Calendar.getInstance();
        Date iat = calendar.getTime();

        if (rsResource.getIat() != null && rsResource.getIat() > 0) {
            iat = new Date(rsResource.getIat() * 1000L);
        }

        return iat;
    }

}
