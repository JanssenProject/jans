/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.client;

import jakarta.ws.rs.core.UriBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import io.jans.notify.model.NotifyMetadata;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
/**
 * Helper class which creates proxy
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class NotifyClientFactory {

	private final static NotifyClientFactory instance = new NotifyClientFactory();

	private ApacheHttpClient43Engine engine;

	private NotifyClientFactory() {
        // Create polled client
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(200); // Increase max total connection to 200
		cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).setSslcontext(SSLContexts.createSystemDefault()).build();

		this.engine = new ApacheHttpClient43Engine(httpClient);
	}

	public static NotifyClientFactory instance() {
		return instance;
	}

	public NotifyMetadataClientService createMetaDataConfigurationService(String issuer) {
		ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
		String metadataUri = issuer + "/.well-known/notify-configuration";
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataUri));

        return target.proxy(NotifyMetadataClientService.class);
    }

	public NotifyClientService createNotifyService(NotifyMetadata notifyMetadata) {
        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();

        String targetUri = notifyMetadata.getNotifyEndpoint();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(targetUri));

        return target.proxy(NotifyClientService.class);
	}

	public static String getAuthorization(String accessKeyId,  String secretAccessKey) {
		String credentials = accessKeyId + ":" + secretAccessKey;
		String authorization = "Basic " + Base64.encodeBase64String(credentials.getBytes());

		return authorization;
	}

}
