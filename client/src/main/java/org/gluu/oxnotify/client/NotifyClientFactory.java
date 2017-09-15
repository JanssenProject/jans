/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.client;

import javax.ws.rs.core.UriBuilder;

import org.gluu.oxnotify.model.NotifyMetadata;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

/**
 * Helper class which creates proxy
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class NotifyClientFactory {

	private final static NotifyClientFactory instance = new NotifyClientFactory();
	private ResteasyClient client = new ResteasyClientBuilder().build();

	private NotifyClientFactory() {
	}

	public static NotifyClientFactory instance() {
		return instance;
	}

	public NotifyMetadata createMetaDataConfigurationService(String metaDataUri) {
		ResteasyWebTarget target = client.target(UriBuilder.fromPath(metaDataUri));
		NotifyMetadataClientService proxy = target.proxy(NotifyMetadataClientService.class);

		return proxy.getMetadataConfiguration();
	}

	public NotifyClientService createNotifyService(NotifyMetadata notifyMetadata) {
		ResteasyWebTarget target = client.target(UriBuilder.fromPath(notifyMetadata.getNotifyEndpoint()));
		return target.proxy(NotifyClientService.class);
	}

}
