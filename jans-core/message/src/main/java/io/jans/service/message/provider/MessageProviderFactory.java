/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Message provider factory
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
@ApplicationScoped
public class MessageProviderFactory {

	public static final String MESSAGE_PROVIDER_THREAD_NAME = "MessageProviderThread";

	@Inject
	private Logger log;

	@Inject
	private MessageConfiguration messageConfiguration;

	@Inject
	@Any
	private Instance<MessageProvider> instance;

	private ExecutorService executorService;
	private MessageProvider messageProvider;

	@PostConstruct
	public void create() {
		executorService = Executors.newCachedThreadPool(new ThreadFactory() {
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable);
				thread.setName(MESSAGE_PROVIDER_THREAD_NAME);
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	@PreDestroy
	public void destroy() {
		shutdown();
	}

	@Produces
	@ApplicationScoped
	public MessageProvider getMessageProvider() {
		log.debug("Started to create message provider");

		messageProvider = getMessageProvider(messageConfiguration);

		return messageProvider;
	}

	public MessageProvider getMessageProvider(MessageConfiguration messageConfiguration) {
		MessageProviderType messageProviderType = messageConfiguration.getMessageProviderType();

		// Create proxied bean
		AbstractMessageProvider<?> messageProvider = null;
		switch (messageProviderType) {
		case DISABLED:
			messageProvider = instance.select(NullMessageProvider.class).get();
			break;
		case REDIS:
			messageProvider = instance.select(RedisMessageProvider.class).get();
			break;
		case POSTGRES:
			messageProvider = instance.select(PostgresMessageProvider.class).get();
			break;
		}

		if (messageProvider == null) {
			throw new RuntimeException(
					"Failed to initialize messageProvider, messageProvider is unsupported: " + messageProviderType);
		}

		// Call message provider from context class loader to load JDBC driver
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			messageProvider.create(executorService);
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}

		return messageProvider;
	}

	public int getActiveCount() {
		return ((ThreadPoolExecutor) executorService).getActiveCount();
	}

	public int getPoolSize() {
		return ((ThreadPoolExecutor) executorService).getPoolSize();
	}

	public void shutdown() {
		if (messageProvider != null) {
			log.info("Starting message provider shutdown...");
			messageProvider.shutdown();
			messageProvider = null;
		}

		if (executorService != null) {
			log.info("Starting message provider thread pool shutdown...");
			executorService.shutdownNow();
			executorService = null;
		}

		log.info("Successfully stopped message provider pool");
	}

}
