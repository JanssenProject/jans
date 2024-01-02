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
import org.slf4j.LoggerFactory;

import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.util.security.StringEncrypter;

/**
 * Message provider for non CDI applications
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
public class StandaloneMessageProviderFactory {

	private static final Logger LOG = LoggerFactory.getLogger(StandaloneMessageProviderFactory.class);

	private StringEncrypter stringEncrypter;
	private ExecutorService executorService;

	public StandaloneMessageProviderFactory(ExecutorService executorService, StringEncrypter stringEncrypter) {
		this.executorService = executorService;
		this.stringEncrypter = stringEncrypter;
	}

	public StandaloneMessageProviderFactory(StringEncrypter stringEncrypter) {
		this.executorService = Executors.newCachedThreadPool(new ThreadFactory() {
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable);
				thread.setName(MessageProviderFactory.MESSAGE_PROVIDER_THREAD_NAME);
				thread.setDaemon(true);
				return thread;
			}
		});
		this.stringEncrypter = stringEncrypter;
	}

	public MessageProvider<?> getMessageProvider(MessageConfiguration messageConfiguration) {
		MessageProviderType messageProviderType = messageConfiguration.getMessageProviderType();

		if (messageProviderType == null) {
			LOG.error("Failed to initialize messageProvider, messageProviderType is null. Fallback to NULL type.");
			messageProviderType = MessageProviderType.DISABLED;
		}

		// Create bean
		AbstractMessageProvider<?> messageProvider = null;
		switch (messageProviderType) {
		case DISABLED:
			if (stringEncrypter == null) {
				throw new RuntimeException("Factory is not initialized properly. stringEncrypter is not specified");
			}

			NullMessageProvider nullMessageProvider = new NullMessageProvider();
			nullMessageProvider.configure(messageConfiguration, stringEncrypter);
			nullMessageProvider.init();

			messageProvider = nullMessageProvider;
			break;
		case REDIS:
			if (stringEncrypter == null) {
				throw new RuntimeException("Factory is not initialized properly. stringEncrypter is not specified");
			}

			RedisMessageProvider redisMessageProvider = new RedisMessageProvider();
			redisMessageProvider.configure(messageConfiguration, stringEncrypter);
			redisMessageProvider.init();

			messageProvider = redisMessageProvider;
			break;
		case POSTGRES:
			if (stringEncrypter == null) {
				throw new RuntimeException("Factory is not initialized properly. stringEncrypter is not specified");
			}

			PostgresMessageProvider postgresMessageProvider = new PostgresMessageProvider();
			postgresMessageProvider.configure(messageConfiguration, stringEncrypter);
			postgresMessageProvider.init();

			messageProvider = postgresMessageProvider;
			break;
		}

		if (messageProvider == null) {
			throw new RuntimeException(
					"Failed to initialize messageProvider, messageProviderType is unsupported: " + messageProvider);
		}

		messageProvider.create(executorService);

		return messageProvider;
	}

	public int getActiveCount() {
		return ((ThreadPoolExecutor) executorService).getActiveCount();
	}

	public int getPoolSize() {
		return ((ThreadPoolExecutor) executorService).getPoolSize();
	}

}
