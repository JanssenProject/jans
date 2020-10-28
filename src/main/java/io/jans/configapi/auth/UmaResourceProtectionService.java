package io.jans.configapi.auth;

import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UmaResourceProtectionService {

	@Inject
	Logger logger;

	@Inject
	ScopeService scopeService;

	@Inject
	UmaResourceService umaResourceService;

	// todo this method should be called during application startup and ONLY if protection is set to UMA.
    // if not all resources exists then throw exception. All paths and methods here are pre-define and must be based on
    // our REST classes (from io.jans.configapi.rest.resource).

	public boolean resourceExists(ResourceInfo resourceInfo,String methods, String path) {
		logger.debug("Resource to verify - " + resourceInfo);
		if (resourceInfo == null) {
			logger.error("Resource is blank !!!!!!");
			throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
		}

		List<String> resourceScopes = AuthUtil.getRequestedScopes(resourceInfo);
		logger.debug("resourceScopes - " + resourceScopes);

		createScopeIfNeeded(resourceScopes);

		// Verify Resources
		createResourceIfNeeded(resourceInfo, methods,  path);
		
		return true;
	}

	private void createScopeIfNeeded(List<String> resourceScopes) { // todo rename createScopeIfNeeded
	    // todo - cache scopes in guava cache. Otherwise you check same scopes again and again

		for (String scopeName : resourceScopes) {
			List<Scope> scopes = scopeService.searchScopes(scopeName, 1);
			logger.trace("Scopes - '" + scopes);
			if (scopes == null || scopes.isEmpty()) {
				logger.trace("Scope - '" + scopeName + "' does not exist, hence creating it.");
				// Create Scope
				Scope scope = new Scope();
				String inum = UUID.randomUUID().toString();
				scope.setId(scopeName);
				scope.setInum(inum);
				scope.setDn(scopeService.getDnForScope(inum));
				scope.setScopeType(ScopeType.UMA);
				scopeService.addScope(scope);
			}
		}
	}

	public void createResourceIfNeeded(ResourceInfo resourceInfo, String methods, String path) { // todo rename createResourceIfNeeded
		List<UmaResource> resources = umaResourceService.findResources(resourceInfo.getClass().getName(), 1000);

		// todo if there are more then one -> throw exception. There should be exactly one
		logger.trace("UmaResource - '" + resources);

		if (resources == null || resources.isEmpty()) {
			logger.trace("UmaResource - '" + resources + "' does not exist, hence creating it.");
			createUmaResource(resourceInfo, methods, path);
		}

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
		logger.trace("UmaResource - '" + umaResource.getName() + "' created.");
		return umaResource;
	}

}
