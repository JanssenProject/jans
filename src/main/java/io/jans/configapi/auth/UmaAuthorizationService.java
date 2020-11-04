/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.client.uma.exception.UmaException;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.ca.rs.protect.RsResource;
import io.jans.configapi.auth.service.UmaService;
import io.jans.configapi.auth.service.PatService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Named("umaAuthorizationService")
public class UmaAuthorizationService extends AuthorizationService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	Logger log;

	@Inject
	UmaResourceProtectionCache umaResourceProtectionCache;

	@Inject
	UmaService umaService;

	@Inject
	PatService patService;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	Instance<PersistenceEntryManager> persistenceManager;

	@Inject
	StaticConfiguration staticConf;

	public void validateAuthorization(String rpt, ResourceInfo resourceInfo, String methods, String path)
			throws Exception {
		log.debug(" UmaAuthorizationService::validateAuthorization() - rpt = " + rpt
				+ " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + " , methods = "
				+ methods + " , path = " + path + "\n");

		// todo FIXME Yuriy Z -> Puja : implementation is wrong overall. Here step by
		// step plan:
		// =================================
		// 1. first all endpoints of config API has to be protected by UMA. Protection
		// is made by registering UMA resource. - [Puja: DONE]
		// e.g. POST, PUT /client -> umaResource1; GET /client -> umaResource2
		// in this way we can grant access got GET /client (via umaResource2) but forbid
		// change it (don't give permission for umaResource1)
		// It corresponds to oxd protection command
		// https://gluu.org/docs/oxd/4.2/api/#uma-rs-protect-resources
		// It should not register umaResource again and again. Usually it should be one
		// UMA resource for one or many endpoints.
		// =================================
		// 2. on each call permission should be checked, which means
		// - get umaResource for exactly this endpoint
		// - introspect RPT which returns permissions
		// - Validate: check that permission corresponds to exactly this umaResource (no
		// match -> forbid)
		// - if resource match then check scopes
		// - It corresponds to oxd check access command
		// https://gluu.org/docs/oxd/4.2/api/#uma-rs-check-access
		// Does it make sense to use uma-rs lib ? It will simplify this task.
		// uma-rs in jans worlds is now part of jans-client-api. You can add dependency
		// on `jans-uma-rs` or `jans-uma-rs-resteasy` and re-use it.
		// https://github.com/JanssenProject/jans-client-api/blob/245692aa3911158c39729e5aa2513e44d254c48f/uma-rs-resteasy/src/main/java/io/jans/ca/rs/protect/resteasy/RptPreProcessInterceptor.java#L31

		// Get UmaResource
		UmaResource umaResource = getUmaResource(resourceInfo, methods, path);
		log.debug(" UmaAuthorizationService::validateAuthorization() - umaResource = " + umaResource);

		if (umaResource.getScopes() == null || umaResource.getScopes().isEmpty())
			return; // nothing to validate. Resource is not protected.

		validateRptToken(rpt, umaResource);
	}

	public void validateRptToken(String rpt, UmaResource umaResource) throws Exception {
		log.debug("UmaAuthorizationService::validateRptToken" + "() - Entry - rpt = " + rpt + " , umaResource = "
				+ umaResource + "\n\n\n");

		// Generate PAT token
		Token patToken = patService.getPatToken();
		log.debug("validateRptToken() - patToken = " + patToken + "\n\n\n");

		// Validate Token
		umaService.validateRptToken(patToken, rpt, umaResource.getId(), umaResource.getScopes());

		// Impt Verify later
		/*
		 * if (patToken == null || patToken.getIdToken() == null) { throw new
		 * UmaException("PAT cannot be null"); }
		 * 
		 * log.trace("Validating RPT, scopeIds: {}, authorization: {}", requestedScopes,
		 * authorization); System.out.
		 * println("Validating RPT, scopeIds: {}, authorization: {} , requestedScopes = "
		 * +requestedScopes+" , authorization = "+authorization); if
		 * (StringHelper.isNotEmpty(authorization) &&
		 * authorization.startsWith("Bearer ")) { String rptToken =
		 * authorization.substring(7);
		 * 
		 * RptIntrospectionResponse rptStatusResponse =
		 * umaService.introspectRpt(patToken, rptToken);
		 * 
		 * // TODO for puja: https://github.com/JanssenProject/jans-client-api/blob/
		 * 245692aa3911158c39729e5aa2513e44d254c48f/uma-rs-resteasy/src/main/java/io/
		 * jans/ca/rs/protect/resteasy/RptPreProcessInterceptor.java#L94 throw new
		 * UnsupportedOperationException("Not supported yet."); }
		 */

	}

	private UmaResource getUmaResource(ResourceInfo resourceInfo, String methods, String path) {
		log.debug(" UmaAuthorizationService::getUmaResource() - resourceInfo = " + resourceInfo
				+ " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + " , methods = "
				+ methods + " , path = " + path + "\n");
		log.debug(" UmaAuthorizationService::getUmaResource() - umaResourceProtectionCache.getAllUmaResources() = "
				+ umaResourceProtectionCache.getAllUmaResources());
		
		//???todo: Puja -> To be reviewed by Yuriy Z
		//Verify in cache
		Map<String, UmaResource> resources = umaResourceProtectionCache.getAllUmaResources();
		
		//Filter based on resource
		Set<String> keys = resources.keySet();
		List<String> filteredKeys = keys.stream()
				.filter(k -> k.contains(path))
				.collect(Collectors.toList());
		
		
		if(keys==null || keys.isEmpty()) {
			throw new WebApplicationException("No mataching resource found .",Response.status(Response.Status.UNAUTHORIZED).build());
		}
		
		for (String key : keys) {
			String[] result = key.split(":::");
			if(result !=null && result.length>0) {
				String httpMethods = result[0];
				String pathUrl = result[1];
				
				//todo: pending
			}
				
		}
		return umaResourceProtectionCache.getUmaResource(methods + " " + path);
	}

}