package io.jans.configapi.auth;

import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsProtector;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.ca.rs.protect.resteasy.PatProvider;
import io.jans.ca.rs.protect.resteasy.ResourceRegistrar;
import io.jans.ca.rs.protect.resteasy.ServiceProvider;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.auth.service.PatService;
import io.jans.configapi.auth.service.UmaService;
import io.jans.configapi.service.UmaResourceService;
import io.jans.configapi.util.Jackson;
import java.io.IOException;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
	UmaService umaService;

	@Inject
	PatService patService;

	@Inject
	UmaResourceProtectionCache umaResourceProtectionCache;

	@Inject
	ScopeService scopeService;

	@Inject
	UmaResourceService umaResourceService;

	Collection<RsResource> rsResourceList;

	Collection<Scope> scopes;

	Collection<UmaResource> umaResources;

	public UmaResourceProtectionService() {

	}

	// todo this method should be called during application startup and ONLY if
	// protection is set to UMA.
	// if not all resources exists then throw exception. All paths and methods here
	// are pre-define and must be based on
	// our REST classes (from io.jans.configapi.rest.resource).

	public Collection<RsResource> getResourceList() {
		return rsResourceList;
	}

	public void verifyUmaResources() throws Exception {

		// Load the uma resource json
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = loader.getResourceAsStream(PROTECTION_CONFIGURATION_FILE_NAME);
		log.debug(
				" \n\n UmaResourceProtectionService::verifyUmaResources() -modified **** inputStream = " + inputStream);
		this.rsResourceList = RsProtector.instance(inputStream).getResourceMap().values();

		// this.rsResourceList = Jackson.read(inputStream, RsResourceList.class);
		log.debug(" \n\n UmaResourceProtectionService::verifyUmaResources() - rsResourceList = "
				+ rsResourceList + "\n\n");

		// log.debug("Resource to verify - " + rsResourceList);
		// Throw error if resource details then
		Preconditions.checkNotNull(rsResourceList, "Config Api Resource list cannot be null !!!");

		createScopeIfNeeded();

		createResourceIfNeeded();

		// Verify Cache
		System.out
				.println("\n\n UmaResourceProtectionCache.getAllScopes = " + UmaResourceProtectionCache.getAllScopes());
		log.debug("\n\n UmaResourceProtectionCache.getAllUmaResources = "
				+ UmaResourceProtectionCache.getAllUmaResources());

	}

	private void createScopeIfNeeded() { // todo rename createScopeIfNeeded
		// todo - cache scopes in guava cache. Otherwise you check same scopes again and
		// again

		log.debug(" \n\n UmaResourceProtectionService::createScopeIfNeeded() - rsResourceList = "
				+ rsResourceList + "\n\n");
		List<String> rsScopes = null;
		for (RsResource rsResource : rsResourceList) {
			for (Condition condition : rsResource.getConditions()) {
				rsScopes = condition.getScopes();
				for (String scopeName : rsScopes) {
					log.debug(" \n\n UmaResourceProtectionService::createScopeIfNeeded() - scopeName = "
							+ scopeName + "\n\n");

					// Check in cache
					if (UmaResourceProtectionCache.getScope(scopeName) != null) {
						log.debug("Scope - '" + scopeName + "' exists in cache.");
						return;
					}

					// Check in DB
					List<Scope> scopes = scopeService.searchScopes(scopeName, 2);
					log.debug("Scopes - " + scopes);
					Scope scope = null;

					if (scopes != null && !scopes.isEmpty()) {
						// Fetch existing scope to store in cache
						scope = scopes.get(0);

						if (scopes.size() > 1) {
							log.error(scopes.size() + " UMA Scope with same name.");
							throw new WebApplicationException(scopes.size() + " UMA Scope with same name.",Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
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

						log.debug("Scope - '" + scope.getId() + "' created.");
						System.out
								.println(" \n\n UmaResourceProtectionService::createScopeIfNeeded() - scope created = "
										+ scope.getId() + "\n\n");
					}

					// Add to cache
					UmaResourceProtectionCache.putScope(scope);

				}
			}
			log.debug(
					" \n\n UmaResourceProtectionService::createScopeIfNeeded() - UmaResourceProtectionCache.getAllScopes() = "
							+ UmaResourceProtectionCache.getAllScopes() + "\n\n");
		}
	}

	public void createResourceIfNeeded() {
		log.debug(" \n\n UmaResourceProtectionService::createResourceIfNeeded() - rsResourceList = "
				+ rsResourceList + "\n\n");

		Map<String, UmaResource> allResources = UmaResourceProtectionCache.getAllUmaResources();

		// Pseudo Code
		// 1. Convert RsResource to UmaResource, while creating the UmaResource create a
		// List of scopes
		// 2. Now check if the scopes is present in case if not then find in DB if not
		// then create them
		// 3. Now check if the resources are present in case if not then find in DB if
		// not then create

		// todo if there are more then one -> throw exception. There should be
		for (RsResource rsResource : rsResourceList) {

			for (Condition condition : rsResource.getConditions()) {
				String umaResourceName = condition.getHttpMethods() + ":::" + rsResource.getPath(); //???todo: Puja -> To be reviewed by Yuriy Z

				// Check in cache
				if (UmaResourceProtectionCache.getUmaResource(umaResourceName) != null) {
					log.debug("UmaResource - '" + umaResourceName + "' exists in cache.");
					return;
				}

				// Check in DB
				List<UmaResource> umaResources = umaResourceService.findResources(umaResourceName, 2);
				log.debug("UmaResource - " + umaResources);
				UmaResource umaResource = null;

				if (umaResources != null && !umaResources.isEmpty()) { 
					// Fetch existing resources to store in cache
					umaResource = umaResources.get(0);
					if (umaResources.size() > 1) {
						log.error(umaResources.size() + " UMA Resource with same name.");
						throw new WebApplicationException(umaResources.size() + " UMA Resource with same name.",Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
					}
				}

				// Create Resource
				if (umaResources == null || umaResources.isEmpty()) {
					log.debug("UmaResource - '" + umaResources + "' does not exist, hence creating it.");
					umaResource = new UmaResource();
					String id = UUID.randomUUID().toString();
					umaResource.setId(id);
					umaResource.setDn(umaResourceService.getDnForResource(id));
					umaResource.setName(umaResourceName); // todo : name can be: <method> + <path>, e.g. POST /clients
					umaResource.setScopes(condition.getScopes()); // to POST /client we require `config-api-write` scope
					umaResourceService.addResource(umaResource);
					log.debug("UmaResource - '" + umaResource.getName() + "' created.");
					log.debug(
							" \n\n UmaResourceProtectionService::createResourceIfNeeded() - umaResource created = "
									+ umaResource + "\n\n");
				}

				// Add to cache
				UmaResourceProtectionCache.putUmaResource(umaResource);
			}
		}
		log.debug(
				" \n\n UmaResourceProtectionService::createResourceIfNeeded() - UmaResourceProtectionCache.getAllUmaResources() = "
						+ UmaResourceProtectionCache.getAllUmaResources() + "\n\n");

	}

	PatProvider patProvider = new PatProvider() {
		@Override
		public String getPatToken() {
			return getUmaToken();
		}

		@Override
		public void clearPat() {
			// do nothing
		}
	};

	private String getUmaToken() {
		String token = null;
		try {
			token = patService.getPatToken().getIdToken();
		} catch (Exception ex) {
			log.error("Toekn exception - " + ex);
		}
		return token;
	}
}
