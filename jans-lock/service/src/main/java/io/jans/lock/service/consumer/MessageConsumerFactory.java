/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.consumer;

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
	private Instance<MessageConsumer> consumerProviderInstances;

	public MessageConsumer getMessageConsumer(String messageConsumerType) {
		for (MessageConsumer consumerProvider : consumerProviderInstances) {
			String serviceMessageConsumerType = consumerProvider.getMessageConsumerType();
			if (StringHelper.equalsIgnoreCase(serviceMessageConsumerType, messageConsumerType)) {
				return consumerProvider;
			}
		}
		
		return consumerProviderInstances.select(NullMessageConsumer.class).get();
	}

}
