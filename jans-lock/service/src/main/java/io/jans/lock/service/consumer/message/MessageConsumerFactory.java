/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.consumer.message;

import org.slf4j.Logger;

import io.jans.service.message.consumer.MessageConsumer;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
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
	@Any
	private Instance<MessageConsumer> messageConsumerProviderInstances;

	public MessageConsumer getMessageConsumer(String messageConsumerType) {
		for (MessageConsumer messageConsumerProvider : messageConsumerProviderInstances) {
			String serviceMessageConsumerType = messageConsumerProvider.getMessageConsumerType();
			if (StringHelper.equalsIgnoreCase(serviceMessageConsumerType, messageConsumerType)) {
				return messageConsumerProvider;
			}
		}
		
		return messageConsumerProviderInstances.select(NullMessageConsumer.class).get();
	}

}
