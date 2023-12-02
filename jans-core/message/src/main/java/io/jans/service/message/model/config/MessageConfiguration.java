/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.model.config;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.service.cache.RedisConfiguration;
import jakarta.enterprise.inject.Vetoed;

/**
 * @author Yuriy Movchan Date: 30/11/2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class MessageConfiguration implements Serializable {

	private static final long serialVersionUID = 5047285980342633402L;

	private MessageProviderType messageProviderType = MessageProviderType.NULL;

	private NullMessageConfiguration nullConfiguration = new NullMessageConfiguration();

	private RedisConfiguration redisConfiguration;

	private PostgresMessageConfiguration postgresMessageConfiguration;

	public MessageProviderType getMessageProviderType() {
		return messageProviderType;
	}

	public void setMessageProviderType(MessageProviderType messageProviderType) {
		this.messageProviderType = messageProviderType;
	}

	public NullMessageConfiguration getNullConfiguration() {
		return nullConfiguration;
	}

	public void setNullConfiguration(NullMessageConfiguration nullConfiguration) {
		this.nullConfiguration = nullConfiguration;
	}

	public PostgresMessageConfiguration getPostgresMessageConfiguration() {
		return postgresMessageConfiguration;
	}

	public void setPostgresMessageConfiguration(PostgresMessageConfiguration postgresMessageConfiguration) {
		this.postgresMessageConfiguration = postgresMessageConfiguration;
	}

	public RedisConfiguration getRedisConfiguration() {
		return redisConfiguration;
	}

	public void setRedisConfiguration(RedisConfiguration redisConfiguration) {
		this.redisConfiguration = redisConfiguration;
	}

	@Override
	public String toString() {
		return "CacheConfiguration{" + "cacheProviderType=" + messageProviderType + ", nullConfiguration="
				+ nullConfiguration + ", redisConfiguration=" + redisConfiguration + ", postgresMessageConfiguration="
				+ postgresMessageConfiguration + '}';
	}
}
