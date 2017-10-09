/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.client;

import java.util.Base64;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.gluu.oxnotify.model.NotifyMetadata;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * Helper class which creates proxy
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class NotifyClientFactory {

	private final static NotifyClientFactory instance = new NotifyClientFactory();
	private ResteasyClient client;
	private ResteasyClient pooledClient;

	private NotifyClientFactory() {
		// Create single connection client
		this.client = new ResteasyClientBuilder().build();

		// Create polled client
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(200); // Increase max total connection to 200
		cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
		ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
		 
		this.pooledClient = new ResteasyClientBuilder().httpEngine(engine).build();
	}

	public static NotifyClientFactory instance() {
		return instance;
	}

	public NotifyMetadataClientService createMetaDataConfigurationService(String issuer) {
		ResteasyWebTarget target = client.target(UriBuilder.fromPath(issuer + "/.well-known/notify-configuration"));
		return target.proxy(NotifyMetadataClientService.class);
	}

	public NotifyClientService createNotifyService(NotifyMetadata notifyMetadata) {
		ResteasyWebTarget target = pooledClient.target(UriBuilder.fromPath(notifyMetadata.getNotifyEndpoint()));
		return target.proxy(NotifyClientService.class);
	}

	public static String getAuthorization(String accessKeyId,  String secretAccessKey) {
		String credentials = accessKeyId + ":" + secretAccessKey;
		String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

		return authorization;
	}

}
