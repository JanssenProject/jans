/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.client;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import io.jans.notify.model.NotifyMetadata;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
/**
 * Helper class which creates proxy
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class NotifyClientFactory {

	private final static NotifyClientFactory instance = new NotifyClientFactory();
	private ClientExecutor pooledClientExecutor;

	private NotifyClientFactory() {
        // Create polled client
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(200); // Increase max total connection to 200
		cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).setSslcontext(SSLContexts.createSystemDefault()).build();

		this.pooledClientExecutor = new ApacheHttpClient4Executor(httpClient);
	}

	public static NotifyClientFactory instance() {
		return instance;
	}

	public NotifyMetadataClientService createMetaDataConfigurationService(String issuer) {
		String metadataUri = issuer + "/.well-known/notify-configuration";
		return ProxyFactory.create(NotifyMetadataClientService.class, metadataUri, pooledClientExecutor);
	}

	public NotifyClientService createNotifyService(NotifyMetadata notifyMetadata) {
		String targetUri = notifyMetadata.getNotifyEndpoint();
		return ProxyFactory.create(NotifyClientService.class, targetUri, pooledClientExecutor);
	}

	public static String getAuthorization(String accessKeyId,  String secretAccessKey) {
		String credentials = accessKeyId + ":" + secretAccessKey;
		String authorization = "Basic " + Base64.encodeBase64String(credentials.getBytes());

		return authorization;
	}

}
