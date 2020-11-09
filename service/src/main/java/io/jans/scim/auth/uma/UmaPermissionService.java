/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.auth.uma;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.exception.OxIntializationException;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.factory.ApacheHttpClient4EngineFactory;
import org.slf4j.Logger;

/**
 * Provide methods to work with permissions and RPT tokens
 * 
 * @author Yuriy Movchan Date: 12/06/2016
 */
@ApplicationScoped
@Named("umaPermissionService")
public class UmaPermissionService implements Serializable {

	private static final long serialVersionUID = -3347131971095468866L;

	@Inject
	private Logger log;

	@Inject
	private UmaMetadata umaMetadata;

	@Inject
	protected AppConfiguration appConfiguration;

	private io.jans.as.client.uma.UmaPermissionService permissionService;
	private UmaRptIntrospectionService rptStatusService;

	private final Pair<Boolean, Response> authenticationFailure = new Pair<Boolean, Response>(false, null);
	private final Pair<Boolean, Response> authenticationSuccess = new Pair<Boolean, Response>(true, null);

	private ClientHttpEngine clientHttpEngine;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) ApplicationInitializedEvent init) {
		try {
			if (this.umaMetadata != null) {
				if (appConfiguration.isRptConnectionPoolUseConnectionPooling()) {

					// For more information about PoolingHttpClientConnectionManager, please see:
					// http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/index.html?org/apache/http/impl/conn/PoolingHttpClientConnectionManager.html

					log.debug("##### Initializing custom ClientExecutor...");
					PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
					connectionManager.setMaxTotal(appConfiguration.getRptConnectionPoolMaxTotal());
					connectionManager.setDefaultMaxPerRoute(appConfiguration.getRptConnectionPoolDefaultMaxPerRoute());
					connectionManager.setValidateAfterInactivity(
							appConfiguration.getRptConnectionPoolValidateAfterInactivity() * 1000);
					CloseableHttpClient client = HttpClients.custom()
							.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
							.setKeepAliveStrategy(connectionKeepAliveStrategy).setConnectionManager(connectionManager)
							.build();
					
					this.clientHttpEngine = ApacheHttpClient4EngineFactory.create(client);
					log.info("##### Initializing custom ClientExecutor DONE");

					this.permissionService = UmaClientFactory.instance().createPermissionService(this.umaMetadata,
							clientHttpEngine);
					this.rptStatusService = UmaClientFactory.instance().createRptStatusService(this.umaMetadata,
							clientHttpEngine);

				} else {
					this.permissionService = UmaClientFactory.instance().createPermissionService(this.umaMetadata);
					this.rptStatusService = UmaClientFactory.instance().createRptStatusService(this.umaMetadata);
				}
			}
		} catch (Exception ex) {
			log.error("Failed to initialize UmaPermissionService", ex);
		}
	}

	@Produces
	@ApplicationScoped
	@Named("umaMetadataConfiguration")
	public UmaMetadata initUmaMetadataConfiguration() throws OxIntializationException {
		String umaConfigurationEndpoint = getUmaConfigurationEndpoint();
		if (StringHelper.isEmpty(umaConfigurationEndpoint)) {
			return null;
		}

		log.info("##### Getting UMA metadata ...");
		UmaMetadataService metaDataConfigurationService;
		if (this.clientHttpEngine == null) {
			metaDataConfigurationService = UmaClientFactory.instance().createMetadataService(umaConfigurationEndpoint);
		} else {
			metaDataConfigurationService = UmaClientFactory.instance().createMetadataService(umaConfigurationEndpoint,
					this.clientHttpEngine);
		}
		UmaMetadata metadataConfiguration = metaDataConfigurationService.getMetadata();

		log.info("##### Getting UMA metadata ... DONE");

		if (metadataConfiguration == null) {
			throw new OxIntializationException("UMA meta data configuration is invalid!");
		}

		return metadataConfiguration;
	}

	public String getUmaConfigurationEndpoint() {
		String umaIssuer = appConfiguration.getUmaIssuer();
		if (StringHelper.isEmpty(umaIssuer)) {
			log.trace("oxAuth UMA issuer isn't specified");
			return null;
		}

		String umaConfigurationEndpoint = umaIssuer;
		if (!umaConfigurationEndpoint.endsWith("uma2-configuration")) {
			umaConfigurationEndpoint += "/.well-known/uma2-configuration";
		}

		return umaConfigurationEndpoint;
	}

	public Pair<Boolean, Response> validateRptToken(Token patToken, String authorization, String umaResourceId,
			String scopeId) {
		return validateRptToken(patToken, authorization, umaResourceId, Arrays.asList(scopeId));
	}

	public Pair<Boolean, Response> validateRptToken(Token patToken, String authorization, String resourceId,
			List<String> scopeIds) {
		/*
		 * //caller of this method never pass null patToken if (patToken == null) {
		 * return authenticationFailure; }
		 */
		log.trace("Validating RPT, resourceId: {}, scopeIds: {}, authorization: {}", resourceId, scopeIds,
				authorization);

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

					if (returnScopeIds.containsAll(scopeIds)) {
						return authenticationSuccess;
					}

					log.error("Status response for RPT token: '{}' not contains right permissions", rptToken);
				}
			}
		}

		Response registerPermissionsResponse = prepareRegisterPermissionsResponse(patToken, resourceId, scopeIds);
		if (registerPermissionsResponse == null) {
			return authenticationFailure;
		}

		return new Pair<Boolean, Response>(true, registerPermissionsResponse);
	}

	private boolean isRptHasPermissions(RptIntrospectionResponse umaRptStatusResponse) {
		return !((umaRptStatusResponse.getPermissions() == null) || umaRptStatusResponse.getPermissions().isEmpty());
	}

	private RptIntrospectionResponse getStatusResponse(Token patToken, String rptToken) {
		String authorization = "Bearer " + patToken.getAccessToken();

		// Determine RPT token to status
		RptIntrospectionResponse rptStatusResponse = null;
		try {
			rptStatusResponse = this.rptStatusService.requestRptStatus(authorization, rptToken, "");
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

	public String registerResourcePermission(Token patToken, String resourceId, List<String> scopes) {
		//TODO: Added this if as a hack since init method is not called upon app startup in scim project   
		if (permissionService == null) {
			init(null);
		}
		//end
		
		UmaPermission permission = new UmaPermission();
		permission.setResourceId(resourceId);
		permission.setScopes(scopes);
		PermissionTicket ticket = permissionService.registerPermission("Bearer " + patToken.getAccessToken(),
				UmaPermissionList.instance(permission));
		if (ticket == null) {
			return null;
		}
		return ticket.getTicket();
	}

	private Response prepareRegisterPermissionsResponse(Token patToken, String resourceId, List<String> scopes) {
		String ticket = registerResourcePermission(patToken, resourceId, scopes);
		if (StringHelper.isEmpty(ticket)) {
			return null;
		}
		log.debug("Construct response: HTTP 401 (Unauthorized), ticket: '{}'", ticket);
		Response response = null;
		try {
			String authHeaderValue = String.format(
					"UMA realm=\"Authorization required\", host_id=%s, as_uri=%s, ticket=%s",
					getHost(appConfiguration.getApplicationUrl()), getUmaConfigurationEndpoint(), ticket);
			response = Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", authHeaderValue)
					.build();
		} catch (MalformedURLException ex) {
			log.error("Failed to determine host by URI", ex);
		}

		return response;
	}

	private String getHost(String uri) throws MalformedURLException {
		URL url = new URL(uri);

		return url.getHost();
	}

	private ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
		@Override
		public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {

			HeaderElementIterator headerElementIterator = new BasicHeaderElementIterator(
					httpResponse.headerIterator(HTTP.CONN_KEEP_ALIVE));

			while (headerElementIterator.hasNext()) {

				HeaderElement headerElement = headerElementIterator.nextElement();

				String name = headerElement.getName();
				String value = headerElement.getValue();

				if (value != null && name.equalsIgnoreCase("timeout")) {
					return Long.parseLong(value) * 1000;
				}
			}

			// Set own keep alive duration if server does not have it
			return appConfiguration.getRptConnectionPoolCustomKeepAliveTimeout() * 1000;
		}
	};

}