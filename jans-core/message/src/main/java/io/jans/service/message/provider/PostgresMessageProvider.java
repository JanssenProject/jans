/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.sql.operation.impl.SqConnectionProviderPool;
import io.jans.service.cache.RedisProvider;
import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.model.config.PostgresMessageConfiguration;
import io.jans.service.message.pubsub.PubSubInterface;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Postgres message provider
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
@ApplicationScoped
public class PostgresMessageProvider extends AbstractMessageProvider<SqConnectionProviderPool> {

	@Inject
	private Logger log;

	@Inject
	private MessageConfiguration messageConfiguration;

	@Inject
	private StringEncrypter stringEncrypter;

	private ConcurrentHashMap<Integer, List<PostgresMessageListener>> subscibedPubSubs;

	private SqConnectionProviderPool сonnectionProviderPool;
	private ExecutorService executorService;

	@PostConstruct
	public void init() {
	}

	@PreDestroy
	public void destroy() {
		log.debug("Destroying PostgresProvider");

		shutdown();
		if (сonnectionProviderPool != null) {
			сonnectionProviderPool.destroy();
		}

		log.debug("Destroyed PostgresProvider");
	}

	@Override
	public void create(ExecutorService executorService) {
		this.executorService = executorService;
		this.subscibedPubSubs = new ConcurrentHashMap<>();

		try {
			PostgresMessageConfiguration postgresMessageConfiguration = messageConfiguration
					.getPostgresConfiguration();
			Properties connectionProperties = toPostgresProperties(postgresMessageConfiguration);
			log.debug("Starting PostgresMessageProvider messages ... configuration {}", postgresMessageConfiguration);

			сonnectionProviderPool = new SqConnectionProviderPool(connectionProperties);
			сonnectionProviderPool.create();
			if (!сonnectionProviderPool.isCreated()) {
				throw new IllegalStateException(
						String.format("Failed to create SQL connection pool for messaging! Result code: '%d'",
								сonnectionProviderPool.getCreationResultCode()));
			}
			log.debug("PostgresMessageProvider message was started.");
		} catch (Exception ex) {
			log.error("Failed to start PostgresProvider messages", ex);
			throw new IllegalStateException("Failed to create SQL connection pool for messaging!", ex);
		}
	}

	public void configure(MessageConfiguration messageConfiguration, StringEncrypter stringEncrypter) {
		this.log = LoggerFactory.getLogger(RedisProvider.class);
		this.messageConfiguration = messageConfiguration;
		this.stringEncrypter = stringEncrypter;
	}

	private Properties toPostgresProperties(PostgresMessageConfiguration postgresMessageConfiguration) {
		Properties connectionProperties = new Properties();
		setProperty(connectionProperties, "jdbc.driver.class-name", postgresMessageConfiguration.getDriverClassName());
		setProperty(connectionProperties, "db.schema.name", postgresMessageConfiguration.getDbSchemaName());
		setProperty(connectionProperties, "connection.uri", postgresMessageConfiguration.getConnectionUri());
		setProperty(connectionProperties, "auth.userName", postgresMessageConfiguration.getAuthUserName());
	
		String password = postgresMessageConfiguration.getAuthUserPassword();
		try {
			if (StringUtils.isNotBlank(password)) {
				password = stringEncrypter.decrypt(password);
				log.trace("Decrypted Postgres password successfully.");
			}
		} catch (StringEncrypter.EncryptionException e) {
			log.error("Error during Postgres password decryption", e);
		}
		setProperty(connectionProperties, "auth.userPassword", password);

		if (postgresMessageConfiguration.getConnectionPoolMaxTotal() != null) {
			setProperty(connectionProperties, "connection.pool.max-total", postgresMessageConfiguration.getConnectionPoolMaxTotal().toString());
		}

		if (postgresMessageConfiguration.getConnectionPoolMaxIdle() != null) {
			setProperty(connectionProperties, "connection.pool.max-idle", postgresMessageConfiguration.getConnectionPoolMaxIdle().toString());
		}

		if (postgresMessageConfiguration.getConnectionPoolMinIdle() != null) {
			setProperty(connectionProperties, "connection.pool.min-idle", postgresMessageConfiguration.getConnectionPoolMinIdle().toString());
		}

		return connectionProperties;
	}

	public void setProperty(Properties connectionProperties, String propertyName, String propertyValue) {
		if (StringHelper.isNotEmpty(propertyValue)) {
			connectionProperties.setProperty(propertyName, propertyValue);
		}
	}

	public boolean isConnected() {
		return сonnectionProviderPool.isConnected();
	}

	@Override
	public SqConnectionProviderPool getDelegate() {
		return сonnectionProviderPool;
	}

	@Override
	public MessageProviderType getProviderType() {
		return MessageProviderType.POSTGRES;
	}

	@Override
	public void subscribe(PubSubInterface pubSub, String... channels) {
		log.info("Starting new thread(s) for subscribing to Postgres channels {}", Arrays.asList(channels));

		List<PostgresMessageListener> listeners = new ArrayList<>();
		int countChannels = 0;
		for (String channel : channels) {
			Connection conn = сonnectionProviderPool.getConnection();
			try {
				PostgresMessageListener postgresMessageListener = new PostgresMessageListener(pubSub, conn);
				postgresMessageListener.subscribe(channel);
				listeners.add(postgresMessageListener);

				executorService.execute(postgresMessageListener);
				pubSub.onSubscribe(channel, ++countChannels);
			} catch (SQLException ex) {
				log.error(String.format("Failed to subscribe to Postgres channel {}", channel));
				if (conn != null) {
					try {
						conn.close();
					} catch (Exception ex2) {
						log.error(String.format(
								"Failed to release connection after subscribe attempt to Postgres channel {}",
								channel));
					}
				}
				throw new IllegalStateException(String.format("Failed to subscribe to Postgres channel {}", channel),
						ex);
			}
		}
		subscibedPubSubs.put(System.identityHashCode(pubSub), listeners);
	}

	@Override
	public void unsubscribe(PubSubInterface pubSub) {
		log.info("Starting end subscription to Postgres for {}", pubSub);

		int pubSubIdentifier = System.identityHashCode(pubSub);
		List<PostgresMessageListener> listeners = subscibedPubSubs.get(pubSubIdentifier);
		if (listeners == null) {
			log.warn("PubSub {} in unsubscribe request is not registered", pubSub);
			return;
		}

		unsubscribe(listeners);
		subscibedPubSubs.remove(pubSubIdentifier);
		log.info("Sent request to end subscription to Postgres for {}", pubSub);
	}

	private void unsubscribe(List<PostgresMessageListener> listeners) {
		for (Iterator<PostgresMessageListener> it = listeners.iterator(); it.hasNext();) {
			PostgresMessageListener listener = (PostgresMessageListener) it.next();
			try {
				listener.unsubscribe();
				it.remove();

				PubSubInterface pubSub = listener.getPubSub();
				pubSub.onUnsubscribe(listener.getChannel(), listeners.size());
			} catch (Throwable ex) {
				log.error("Failed to unsubscribe for {}", listener.getPubSub());
			}
		}
	}

	@Override
	public boolean publish(String channel, String message) {
		CompletableFuture.runAsync(() -> {
			try (Connection conn = сonnectionProviderPool.getConnection()) {
				try (Statement stmt = conn.createStatement()) {
					stmt.execute(
							String.format("NOTIFY %s, '%s'", channel, Base64.encodeBase64String(message.getBytes())));
				}
			} catch (SQLException ex) {
				log.error("Failed to publish message to channel {}", channel, ex);
			}
		});

		return true;
	}

	@Override
	public void shutdown() {
		for (List<PostgresMessageListener> listeners : subscibedPubSubs.values()) {
			unsubscribe(listeners);
		}
		subscibedPubSubs.clear();
	}

	class PostgresMessageListener implements Runnable {
		private PubSubInterface pubSub;
		private String channel;

		private Connection conn;
		private org.postgresql.PGConnection pgConn;

		private boolean active;

		PostgresMessageListener(PubSubInterface pubSub, Connection conn) throws SQLException {
			this.pubSub = pubSub;
			this.conn = conn;
			this.pgConn = conn.unwrap(org.postgresql.PGConnection.class);
			this.active = true;
		}

		public PubSubInterface getPubSub() {
			return pubSub;
		}

		public String getChannel() {
			return channel;
		}

		public void subscribe(String channel) throws SQLException {
			this.channel = channel;
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("LISTEN " + channel);
			}
		}

		public void unsubscribe() throws SQLException {
			active = false;
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("UNLISTEN " + channel);
			}
			conn.close();
		}

		public void run() {
			PostgresMessageConfiguration postgresMessageConfiguration = messageConfiguration
					.getPostgresConfiguration();
			int messageWaitMillis = postgresMessageConfiguration.getMessageWaitMillis();
			int messageSleepThreadTime = postgresMessageConfiguration.getMessageSleepThreadTime();

			try {
				while (active) {
					org.postgresql.PGNotification notifications[] = pgConn.getNotifications(messageWaitMillis);

					if (notifications != null) {
						for (int i = 0; i < notifications.length; i++) {
							pubSub.onMessage(notifications[i].getName(), new String(Base64.decodeBase64(notifications[i].getParameter()), StandardCharsets.UTF_8));
						}
					}

					Thread.sleep(messageSleepThreadTime);
				}
			} catch (SQLException ex) {
			} catch (InterruptedException ex) {
				log.error("Error during reading messages", ex);
			}
		}
	}

}
