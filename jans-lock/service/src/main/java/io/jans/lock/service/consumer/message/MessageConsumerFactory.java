/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.consumer.message;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.consumer.message.generic.NullMessageConsumer;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.qualifier.Implementation;
import io.jans.service.message.consumer.MessageConsumer;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Message consumer factory
 *
 * @author Yuriy Movchan Date: 12/18/2023
 */
@ApplicationScoped
public class MessageConsumerFactory {

	@Inject
	private Logger log;

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	@Implementation
	private Instance<MessageConsumer> messageConsumerProviderInstances;

	private boolean appStarted = false;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        this.appStarted  = true;
	}

	private MessageConsumer getMessageConsumer(String messageConsumerType) {
		for (MessageConsumer messageConsumer : messageConsumerProviderInstances) {
			String serviceMessageConsumerType = messageConsumer.getMessageConsumerType();
			if (StringHelper.equalsIgnoreCase(serviceMessageConsumerType, messageConsumerType)) {
				return messageConsumer;
			}
		}
		
		log.error("Failed to find message consumer with type '{}'. Using null message consumer", messageConsumerType);
		return messageConsumerProviderInstances.select(NullMessageConsumer.class).get();
	}

	@Asynchronous
    public void configurationUpdateEvent(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
		if (!appStarted) {
			return;
		}

		recreateMessageConsumer();
	}

	private void recreateMessageConsumer() {
        // Force to create new bean
		for (MessageConsumer messageConsumer : messageConsumerProviderInstances) {
			messageConsumerProviderInstances.destroy(messageConsumer);
			messageConsumer.destroy();
	    log.info("Destroyed messageConsumer instance '{}'", System.identityHashCode(messageConsumer));
		}
		produceMessageConsumer();
	}

	@Produces
	@ApplicationScoped
	public MessageConsumer produceMessageConsumer() {
		String messageConsumerType = appConfiguration.getPdpType();
		MessageConsumer messageConsumer = getMessageConsumer(messageConsumerType);
		
		return messageConsumer;
	}

}
