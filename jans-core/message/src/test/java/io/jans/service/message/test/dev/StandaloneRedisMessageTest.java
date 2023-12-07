/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.test.dev;

import io.jans.service.message.MessageProvider;
import io.jans.service.message.StandaloneMessageProviderFactory;
import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.model.config.RedisMessageConfiguration;
import io.jans.service.message.pubsub.PubSubInterface;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;

/**
 * @author Yuriy Movchan Date: 30/11/2023
 */
public class StandaloneRedisMessageTest {

	public static void main(String[] args) throws EncryptionException, InterruptedException {
		StringEncrypter stringEncrypter = StringEncrypter.instance("aOm7B9mrWT66roqZCNcUr7ox");

		MessageConfiguration messageConfiguration = new MessageConfiguration();
		messageConfiguration.setMessageProviderType(MessageProviderType.REDIS);

		RedisMessageConfiguration redisMessageConfiguration = new RedisMessageConfiguration();
		redisMessageConfiguration.setServers("192.168.1.151:6379");
		redisMessageConfiguration.setPassword("rgy1GUg+1kY="); // secret

		messageConfiguration.setRedisConfiguration(redisMessageConfiguration);

		StandaloneMessageProviderFactory messageProviderFactory = new StandaloneMessageProviderFactory(stringEncrypter);
		MessageProvider messageProvider = messageProviderFactory.getMessageProvider(messageConfiguration);

		PubSubInterface pubSubAdapter = new PubSubInterface() {

			@Override
			public void onUnsubscribe(String channel, int subscribedChannels) {
				System.out.println(String.format("onUnsubscribe %s : %d", channel, subscribedChannels));
			}

			@Override
			public void onSubscribe(String channel, int subscribedChannels) {
				System.out.println(String.format("onSubscribe %s : %d", channel, subscribedChannels));
			}

			@Override
			public void onMessage(String channel, String message) {
				System.out.println(String.format("onMessage %s : %s", channel, message));
			}
		};

		System.out.printf("First test...\n");
		messageProvider.subscribe(pubSubAdapter, "test1", "test2", "test3");
		for (int i = 0; i < 1000; i++) {
			messageProvider.publish("test1", "1111111");
			messageProvider.publish("test2", "1111112");
			messageProvider.publish("test3", "1111113");
		}

		Thread.sleep(5 * 1000L);
		messageProvider.unsubscribe(pubSubAdapter);
		messageProvider.shutdown();
		System.out.printf("Active count %d, total: %d \n", messageProviderFactory.getActiveCount(),
				messageProviderFactory.getPoolSize());

		Thread.sleep(1 * 1000L);
		System.out.printf("Active count %d, total: %d \n", messageProviderFactory.getActiveCount(),
				messageProviderFactory.getPoolSize());

		Thread.sleep(5 * 1000L);
		System.out.printf("Second test...\n");

		messageProvider.subscribe(pubSubAdapter, "test1");
		for (int i = 0; i < 1000; i++) {
			messageProvider.publish("test1", "1111111");
			messageProvider.publish("test2", "1111112");
			messageProvider.publish("test3", "1111113");
		}

		Thread.sleep(5 * 1000L);
		messageProvider.unsubscribe(pubSubAdapter);
		messageProvider.shutdown();
		System.out.printf("Active count %d, total: %d \n", messageProviderFactory.getActiveCount(),
				messageProviderFactory.getPoolSize());

		Thread.sleep(1 * 1000L);
		System.out.printf("Active count %d, total: %d \n", messageProviderFactory.getActiveCount(),
				messageProviderFactory.getPoolSize());

		System.out.printf("End test...\n");
	}

}
