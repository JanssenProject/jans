/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.message;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.service.message.consumer.MessageConsumer;
import io.jans.service.message.provider.MessageProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan Date: 15/12/2023
 */
@ApplicationScoped
public class TokenSubService {

    @Inject
    protected Logger log;

    @Inject
	private AppConfiguration appConfiguration;

    @Inject
	private MessageProvider messageProvider;

    @Inject
	private MessageConsumer messageConsumer;

	public void subscribe() {
		log.info("Use message provider type: {}", messageProvider.getProviderType());
		
		messageProvider.subscribe(messageConsumer, appConfiguration.getTokenChannels().toArray(new String[0]));

		log.info("Subscribed to channels: {}", appConfiguration.getTokenChannels());
	}
}
