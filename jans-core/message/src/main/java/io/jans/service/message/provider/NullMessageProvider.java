/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.pubsub.PubSubInterface;
import io.jans.util.security.StringEncrypter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Null message provider
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
@ApplicationScoped
public class NullMessageProvider extends AbstractMessageProvider<NullPool> {

	@Inject
	private Logger log;

	@PostConstruct
	public void init() {
	}

	@Override
	public void create(ExecutorService executorService) {
	}

	public void create() {
		log.debug("Starting NullProvider messages ... configuration");
		log.debug("NullProvider message started.");
	}

	public void configure(MessageConfiguration messageConfiguration, StringEncrypter stringEncrypter) {
	}

	public void setMessageConfiguration(MessageConfiguration messageConfiguration) {
	}

	public boolean isConnected() {
		return true;
	}

	@PreDestroy
	public void destroy() {
	}

	@Override
	public NullPool getDelegate() {
		return null;
	}

	@Override
	public MessageProviderType getProviderType() {
		return MessageProviderType.DISABLED;
	}

	@Override
	public void subscribe(PubSubInterface pubSub, String... channels) {
	}

	@Override
	public void unsubscribe(PubSubInterface pubSub) {
	}

	@Override
	public boolean publish(String channel, String message) {
		return true;
	}

	@Override
	public void shutdown() {
	}

}
