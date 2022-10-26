/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.client;

import io.jans.notify.model.NotifyMetadata;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import jakarta.ws.rs.core.UriBuilder;
import java.util.Base64;

/**
 * Helper class which creates proxy
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class NotifyClientFactory {

	private static final NotifyClientFactory instance = new NotifyClientFactory();
	private ResteasyClient client;
	private ResteasyClient pooledClient;

	private NotifyClientFactory() {
		// Create single connection client
		this.client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).build();

		// Create polled client
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(200); // Increase max total connection to 200
		cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
		ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
		 
		this.pooledClient = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
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
