package io.jans.configapi.auth;

import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.service.UmaResourceService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

@ApplicationScoped
public class ApiResourceService {

	@Inject
	Logger logger;

	@Inject
	ScopeService scopeService;

	@Inject
	UmaResourceService umaResourceService;

	public boolean resourceExists(ResourceInfo resourceInfo,String methods, String path) {
		logger.debug("Resource to verify - " + resourceInfo);
		if (resourceInfo == null) {
			logger.error("Resource is blank !!!!!!");
			throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
		}

		List<String> resourceScopes = AuthUtil.getRequestedScopes(resourceInfo);
		logger.debug("resourceScopes - " + resourceScopes);

		// Verify Scopes
		verifyScope(resourceScopes);

		// Verify Resources
		verifyResource(resourceInfo, methods,  path);
		
		return true;
	}

	private void verifyScope(List<String> resourceScopes) {

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
				Scope result = scopeService.getScopeByInum(inum);
				logger.trace("Scope - '" + scopeName + "' created.");
			}
		}

	}

	public void verifyResource(ResourceInfo resourceInfo, String methods, String path) {
		List<UmaResource> resources = umaResourceService.findResources(resourceInfo.getClass().getName(), 1000);
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
		umaResource.setName(resourceInfo.getClass().getName());
		umaResource.setScopes(AuthUtil.getRequestedScopes(resourceInfo));
		umaResource.setResources(resources);
		umaResourceService.addResource(umaResource);
		logger.trace("UmaResource - '" + umaResource.getName() + "' created.");
		return umaResource;
	}

}
