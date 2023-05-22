/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.client;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import io.jans.notify.model.NotifyMetadata;
import jakarta.ws.rs.core.UriBuilder;

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
		this.engine = createEngine();
	}

	public static NotifyClientFactory instance() {
		return instance;
	}

	public NotifyMetadataClientService createMetaDataConfigurationService(String issuer) {
        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(issuer + "/.well-known/notify-configuration"));

        return target.proxy(NotifyMetadataClientService.class);
	}

	public NotifyClientService createNotifyService(NotifyMetadata notifyMetadata) {
        ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(notifyMetadata.getNotifyEndpoint()));

        return target.proxy(NotifyClientService.class);
	}

	public static String getAuthorization(String accessKeyId, String secretAccessKey) {
		String credentials = accessKeyId + ":" + secretAccessKey;
		String authorization = "Basic " + Base64.encodeBase64String(credentials.getBytes());

		return authorization;
	}

	private ApacheHttpClient43Engine createEngine() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
        		.setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        engine.setFollowRedirects(false);
        
        return engine;
    }

}
