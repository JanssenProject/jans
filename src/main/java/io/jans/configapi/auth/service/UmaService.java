/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth.service;

import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.auth.*;
import io.jans.configapi.service.ConfigurationService;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.orm.util.StringHelper;
import io.jans.util.init.Initializable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.LinkedList;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class UmaService extends Initializable implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

	@Inject
	Logger log;

	@Inject
	ConfigurationService configurationService;

	private UmaMetadata umaMetadata;
	private UmaPermissionService umaPermissionService;
	private UmaRptIntrospectionService umaRptIntrospectionService;

	@Override
	protected void initInternal() {
		try {
			loadUmaConfigurationService();
		} catch (Exception ex) {
			throw new ConfigurationException("Failed to load oxAuth UMA configuration", ex);
		}
	}

	public UmaMetadata getUmaMetadata() throws Exception {
		init();
		return this.umaMetadata;
	}

	public void loadUmaConfigurationService() throws Exception {
		this.umaMetadata = getUmaMetadataConfiguration();
		this.umaPermissionService = AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
		this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
	}

	@Produces
	@ApplicationScoped
	@Named("umaMetadataConfiguration")
	public UmaMetadata getUmaMetadataConfiguration() throws OxIntializationException {

		log.info("##### Getting UMA Metadata Service ...");
		log.debug(
				"\n\n UmaService::initUmaMetadataConfiguration() - configurationService.find().getUmaConfigurationEndpoint() = "
						+ configurationService.find().getUmaConfigurationEndpoint());
		UmaMetadataService umaMetadataService = AuthClientFactory
				.getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);
		log.debug("\n\n UmaService::initUmaMetadataConfiguration() - umaMetadataService = " + umaMetadataService);

		log.info("##### Getting UMA Metadata ...");
		UmaMetadata umaMetadata = umaMetadataService.getMetadata();
		log.debug("\n\n UmaService::initUmaMetadataConfiguration() - umaMetadata = " + umaMetadata);
		log.info("##### Getting UMA metadata ... DONE");

		if (umaMetadata == null) {
			throw new OxIntializationException("UMA meta data configuration is invalid!");
		}

		return umaMetadata;
	}

	public void validateRptToken(Token patToken, String authorization, String resourceId, List<String> scopeIds) {

		log.trace("Validating RPT, resourceId: {}, scopeIds: {}, authorization: {}", resourceId, scopeIds,
				authorization);

		if (patToken == null) {
			log.info("Token is blank"); // todo yuriy-> puja: it's not enough to return unauthorize, in UMA ticket has to be registered
			Response registerPermissionsResponse = prepareRegisterPermissionsResponse(patToken, resourceId, scopeIds);
			throw new WebApplicationException("Token is blank.", registerPermissionsResponse);
		}

		if (StringHelper.isNotEmpty(authorization) && authorization.startsWith("Bearer ")) {
			String rptToken = authorization.substring(7);

			RptIntrospectionResponse rptStatusResponse = getStatusResponse(patToken, rptToken);
			log.trace("RPT status response: {} ", rptStatusResponse);
			if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
				log.warn("Status response for RPT token: '{}' is invalid, will do a retry", rptToken);
			} else {
				boolean rptHasPermissions = isRptHasPermissions(rptStatusResponse);

				if (rptHasPermissions) {
					// Collect all scopes
					List<String> returnScopeIds = new LinkedList<String>();
					for (UmaPermission umaPermission : rptStatusResponse.getPermissions()) {
						if (umaPermission.getScopes() != null) {
							returnScopeIds.addAll(umaPermission.getScopes());
						}
					}

						log.error("Status response for RPT token: '{}' not contains right permissions", rptToken);
					}
				}
			}

		Response registerPermissionsResponse = prepareRegisterPermissionsResponse(patToken, resourceId, scopeIds);
		throw new WebApplicationException("UMA authentication failed.", registerPermissionsResponse);

	}

	private boolean isRptHasPermissions(RptIntrospectionResponse umaRptStatusResponse) {
		return !((umaRptStatusResponse.getPermissions() == null) || umaRptStatusResponse.getPermissions().isEmpty());
	}

	private RptIntrospectionResponse getStatusResponse(Token patToken, String rptToken) {
		String authorization = "Bearer " + patToken.getAccessToken();

		// Determine RPT token to status
		RptIntrospectionResponse rptStatusResponse = null;
		try {
			rptStatusResponse = this.umaRptIntrospectionService.requestRptStatus(authorization, rptToken, "");
		} catch (Exception ex) {
			log.error("Failed to determine RPT status", ex);
			ex.printStackTrace();
		}

		// Validate RPT status response
		if ((rptStatusResponse == null) || !rptStatusResponse.getActive()) {
			return null;
		}

		return rptStatusResponse;
	}

	private Response prepareRegisterPermissionsResponse(Token patToken, String resourceId, List<String> scopes) {
		String ticket = registerResourcePermission(patToken, resourceId, scopes);	
		Response response = null;
		if (StringHelper.isEmpty(ticket)) {
			// return null;
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			return response;
		}
		log.debug("Construct response: HTTP 401 (Unauthorized), ticket: '{}'", ticket);
		
		try {
			String authHeaderValue = String.format(
					"UMA realm=\"Authorization required\", host_id=%s, as_uri=%s, ticket=%s",
					getHost(this.umaMetadata.getIssuer()), configurationService.find().getUmaConfigurationEndpoint(),
					ticket);
			response = Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", authHeaderValue)
					.build();
		} catch (MalformedURLException ex) {
			log.error("Failed to determine host by URI", ex);
		}

		return response;
	}

	public String registerResourcePermission(Token patToken, String resourceId, List<String> scopes) {
		UmaPermission permission = new UmaPermission();
		permission.setResourceId(resourceId);
		permission.setScopes(scopes);
		PermissionTicket ticket = this.umaPermissionService.registerPermission("Bearer " + patToken.getAccessToken(),
				UmaPermissionList.instance(permission));
		if (ticket == null) {
			return null;
		}
		return ticket.getTicket();
	}

	private String getHost(String uri) throws MalformedURLException {
		URL url = new URL(uri);
		return url.getHost();
	}

}
