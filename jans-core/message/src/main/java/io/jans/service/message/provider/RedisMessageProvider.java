/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.service.cache.AbstractRedisProvider;
import io.jans.service.cache.RedisConfiguration;
import io.jans.service.cache.RedisProvider;
import io.jans.service.cache.RedisProviderFactory;
import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.model.config.RedisMessageConfiguration;
import io.jans.service.message.pubsub.PubSubInterface;
import io.jans.service.message.pubsub.PubSubRedisAdapter;
import io.jans.util.security.StringEncrypter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ShardedJedisPool;

/**
 * Redis message provider
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
@ApplicationScoped
public class RedisMessageProvider extends AbstractMessageProvider<AbstractRedisProvider> {

	@Inject
	private Logger log;

	@Inject
	private MessageConfiguration messageConfiguration;

	@Inject
	private StringEncrypter stringEncrypter;

	private ConcurrentHashMap<Integer, PubSubRedisAdapter> subscibedPubSubs;

	private AbstractRedisProvider redisProvider;
	private ExecutorService executorService;

	@PostConstruct
	public void init() {
	}

	@PreDestroy
	public void destroy() {
		log.debug("Destroying RedisProvider");

		shutdown();
		if (redisProvider != null) {
			redisProvider.destroy();
		}

		log.debug("Destroyed RedisMessageProvider");
	}

	@Override
	public void create(ExecutorService executorService) {
		this.executorService = executorService;
		this.subscibedPubSubs = new ConcurrentHashMap<>();

		try {
			RedisMessageConfiguration redisConfiguration = messageConfiguration.getRedisConfiguration();
			decryptPassword(redisConfiguration);
			log.debug("Starting RedisMessageProvider messages ... configuration {}", redisConfiguration);
			redisProvider = RedisProviderFactory.create(redisConfiguration);
			redisProvider.create();
			log.debug("RedisMessageProvider message started.");
		} catch (Exception ex) {
			log.error("Failed to start RedisMessageProvider messages", ex);
			throw new IllegalStateException("Error starting RedisMessageProvider messages", ex);
		}
	}

	public void configure(MessageConfiguration messageConfiguration, StringEncrypter stringEncrypter) {
		this.log = LoggerFactory.getLogger(RedisProvider.class);
		this.messageConfiguration = messageConfiguration;
		this.stringEncrypter = stringEncrypter;
	}

	private void decryptPassword(RedisMessageConfiguration redisConfiguration) {
		try {
			String encryptedPassword = redisConfiguration.getPassword();
			if (StringUtils.isNotBlank(encryptedPassword)) {
				redisConfiguration.setPassword(stringEncrypter.decrypt(encryptedPassword));
				log.trace("Decrypted redis password successfully.");
			}
		} catch (StringEncrypter.EncryptionException e) {
			log.error("Error during redis password decryption", e);
		}
	}

	public boolean isConnected() {
		return redisProvider.isConnected();
	}

	@Override
	public AbstractRedisProvider getDelegate() {
		return redisProvider;
	}

	@Override
	public MessageProviderType getProviderType() {
		return MessageProviderType.REDIS;
	}

	@Override
	public void subscribe(PubSubInterface pubSub, String... channels) {
		log.info("Starting new thread for subscribing to Redis channels {}", Arrays.asList(channels));
		Object objectPool = redisProvider.getDelegate();
		if (objectPool instanceof JedisPool) {
			executorService.execute(() -> {
				Jedis jedis = ((JedisPool) objectPool).getResource();
				PubSubRedisAdapter pubSubRedisAdapter = new PubSubRedisAdapter(pubSub);
				subscibedPubSubs.put(System.identityHashCode(pubSub), pubSubRedisAdapter);

				jedis.subscribe(pubSubRedisAdapter, channels);
			});
		} else if (objectPool instanceof JedisCluster) {
			executorService.execute(() -> {
				JedisCluster jedis = ((JedisCluster) objectPool);
				PubSubRedisAdapter pubSubRedisAdapter = new PubSubRedisAdapter(pubSub);
				subscibedPubSubs.put(System.identityHashCode(pubSub), pubSubRedisAdapter);

				jedis.subscribe(pubSubRedisAdapter, channels);
			});
		} else if (objectPool instanceof ShardedJedisPool) {
			// Not supported in current lib, also Sharded is deprecated in 5.x lib
			throw new UnsupportedOperationException("Sharded pool not provides PubSub in 3.9.x API");
		}
		log.info("Stopping thread after subscription end to Redis from channels {}", Arrays.asList(channels));
	}

	@Override
	public void unsubscribe(PubSubInterface pubSub) {
		log.info("Starting end subscription to Redis for {}", pubSub);

		int pubSubIdentifier = System.identityHashCode(pubSub);
		PubSubRedisAdapter pubSubRedisAdapter = subscibedPubSubs.get(pubSubIdentifier);
		if (pubSubRedisAdapter == null) {
			log.warn("PubSub {} in unsubscribe request is not registered", pubSub);
			return;
		}

		pubSubRedisAdapter.unsubscribe();
		subscibedPubSubs.remove(pubSubIdentifier);
		log.info("Sent request to end subscription to Redis for {}", pubSub);
	}

	@Override
	public boolean publish(String channel, String message) {
		Object objectPool = redisProvider.getDelegate();
		if (objectPool instanceof JedisPool) {
			CompletableFuture.runAsync(() -> {
				JedisPool pool = ((JedisPool) objectPool);
				Jedis jedis = pool.getResource();
				try {
					jedis.publish(channel, message);
				} finally {
					pool.returnResource(jedis);
				}
			});
		} else if (objectPool instanceof JedisCluster) {
			CompletableFuture.runAsync(() -> {
				Jedis jedis = ((JedisPool) objectPool).getResource();
				jedis.publish(channel, message);
			});
		} else if (objectPool instanceof ShardedJedisPool) {
			// Not supported in current lib, also Sharded is deprecatged in 5.x lib
			throw new UnsupportedOperationException("Sharded pool not provides PubSub in 3.9.x API");
		}

		return true;
	}

	@Override
	public void shutdown() {
		for (PubSubRedisAdapter pubSubRedisAdapter : subscibedPubSubs.values()) {
			try {
				pubSubRedisAdapter.unsubscribe();
			} catch (Throwable ex) {
				log.error("Failed to unsubscribe for {}", pubSubRedisAdapter.getPubSub());
			}
		}
		subscibedPubSubs.clear();
	}

}
