/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.test.dev;

import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.model.config.PostgresMessageConfiguration;
import io.jans.service.message.provider.MessageProvider;
import io.jans.service.message.provider.StandaloneMessageProviderFactory;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;

/**
 * @author Yuriy Movchan Date: 30/11/2023
 */
public class StandalonePostresPubTokenMessageTest {

	public static void main(String[] args) throws EncryptionException, InterruptedException {
		StringEncrypter stringEncrypter = StringEncrypter.instance("aOm7B9mrWT66roqZCNcUr7ox");

		MessageConfiguration messageConfiguration = new MessageConfiguration();
		messageConfiguration.setMessageProviderType(MessageProviderType.POSTGRES);

		PostgresMessageConfiguration postgresMessageConfiguration = new PostgresMessageConfiguration();
		postgresMessageConfiguration.setDbSchemaName("public");
		postgresMessageConfiguration.setConnectionUri("jdbc:postgresql://localhost:5433/postgres");
		postgresMessageConfiguration.setAuthUserName("postgres");
		postgresMessageConfiguration.setAuthUserPassword("rgy1GUg+1kY="); // secret
		postgresMessageConfiguration.setMessageWaitMillis(100);
		postgresMessageConfiguration.setMessageSleepThreadTime(200);

		messageConfiguration.setPostgresConfiguration(postgresMessageConfiguration);

		StandaloneMessageProviderFactory messageProviderFactory = new StandaloneMessageProviderFactory(stringEncrypter);
		MessageProvider messageProvider = messageProviderFactory.getMessageProvider(messageConfiguration);

		System.out.printf("First test...\n");
		for (int i = 0; i < 1000; i++) {
			messageProvider.publish("id_token", "1111111");
			messageProvider.publish("code_token", "22222222222");
			messageProvider.publish("id_token", "333333333333333333333");
		}

		Thread.sleep(5 * 1000L);
		messageProvider.shutdown();
		System.out.printf("Active count %d, total: %d \n", messageProviderFactory.getActiveCount(),
				messageProviderFactory.getPoolSize());

		System.out.printf("End test...\n");
	}

}
