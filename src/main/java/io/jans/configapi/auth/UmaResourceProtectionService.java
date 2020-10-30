package io.jans.configapi.auth;

import io.jans.ca.rs.protect.RsProtector;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;
import io.jans.configapi.util.Jackson;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;


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
	
	Collection<RsResource> resourceList;
	
	public UmaResourceProtectionService()  {
	}
	
	

	// todo this method should be called during application startup and ONLY if protection is set to UMA.
    // if not all resources exists then throw exception. All paths and methods here are pre-define and must be based on
    // our REST classes (from io.jans.configapi.rest.resource).

	public Collection<RsResource> getResourceList() {
		return resourceList;
	}

	public void verifyUmaResources() throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(PROTECTION_CONFIGURATION_FILE_NAME);
        this.resourceList = RsProtector.instance(inputStream).getResourceMap().values();
		log.debug(" \n\n UmaResourceProtectionService::verifyUmaResources() - resourceList = "+resourceList+"\n\n");
		//log.debug("Resource to verify - " + resourceList);
		if (resourceList == null) {
			log.error("UMA Resource are blank !!!!!!");
			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
		}

		// Verify Resources
		//createResourceIfNeeded();
		

	}

	private void createScopeIfNeeded(List<String> resourceScopes) { // todo rename createScopeIfNeeded
	    // todo - cache scopes in guava cache. Otherwise you check same scopes again and again

		for (String scopeName : resourceScopes) {
			List<Scope> scopes = scopeService.searchScopes(scopeName, 1);
			log.trace("Scopes - '" + scopes);
			if (scopes == null || scopes.isEmpty()) {
				log.trace("Scope - '" + scopeName + "' does not exist, hence creating it.");
				// Create Scope
				Scope scope = new Scope();
				String inum = UUID.randomUUID().toString();
				scope.setId(scopeName);
				scope.setDisplayName(scopeName);
				scope.setInum(inum);
				scope.setDn(scopeService.getDnForScope(inum));
				scope.setScopeType(ScopeType.UMA);
				scopeService.addScope(scope);
				
				////Add to cache
				UmaResourceProtectionCache.putScope(scope);
			}
		}
	}

	public void createResourceIfNeeded() { 
		log.debug(" \n\n UmaResourceProtectionService::createResourceIfNeeded() - resourceList = "+resourceList+"\n\n");
		Map<String, UmaResource> allResources = UmaResourceProtectionCache.getAllUmaResources();
		
		/*
			
		//Collect list of resources that are not yet created
		List<UmaResource> missingUmaResource = resourceList
				.stream()
				.filter(resource -> UmaResourceProtectionCache.getUmaResource(resource.getPath())) // todo : append the httpMethod???
				.collect(Collectors.toList());
		//List<UmaResource> resources = umaResourceService.findResources(resourceInfo.getClass().getName(), 1000);

		// todo if there are more then one -> throw exception. There should be exactly one
		log.trace("UmaResource - '" + resources);
	*/
	
	}

	private UmaResource createUmaResource(ResourceInfo resourceInfo, String methods, String path) {
		List<String> resources = Arrays.asList(path);
		UmaResource umaResource = new UmaResource();

		String id = UUID.randomUUID().toString();
		umaResource.setId(id);
		umaResource.setDn(umaResourceService.getDnForResource(id));
		umaResource.setName(resourceInfo.getClass().getName()); // todo : name can be: <method> + <path>, e.g. POST /clients
		umaResource.setScopes(AuthUtil.getRequestedScopes(resourceInfo)); // to POST /client we require `config-api-write` scope
		umaResource.setResources(resources);
		umaResourceService.addResource(umaResource);
		log.trace("UmaResource - '" + umaResource.getName() + "' created.");
		
		//Add to cache
		UmaResourceProtectionCache.putUmaResource(umaResource);
		
		return umaResource;
	}

}
