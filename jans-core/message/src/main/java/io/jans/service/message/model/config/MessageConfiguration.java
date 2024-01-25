/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.model.config;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.enterprise.inject.Vetoed;

/**
 * @author Yuriy Movchan Date: 30/11/2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class MessageConfiguration implements Serializable {

	private static final long serialVersionUID = 5047285980342633402L;

	private MessageProviderType messageProviderType = MessageProviderType.DISABLED;

	@Hidden
	private NullMessageConfiguration nullConfiguration = new NullMessageConfiguration();

	private RedisMessageConfiguration redisConfiguration;

	private PostgresMessageConfiguration postgresConfiguration;

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

	public PostgresMessageConfiguration getPostgresConfiguration() {
		return postgresConfiguration;
	}

	public void setPostgresConfiguration(PostgresMessageConfiguration postgresMessageConfiguration) {
		this.postgresConfiguration = postgresMessageConfiguration;
	}

	public RedisMessageConfiguration getRedisConfiguration() {
		return redisConfiguration;
	}

	public void setRedisConfiguration(RedisMessageConfiguration redisConfiguration) {
		this.redisConfiguration = redisConfiguration;
	}

	@Override
	public String toString() {
		return "MessageConfiguration [messageProviderType=" + messageProviderType + ", nullConfiguration="
				+ nullConfiguration + ", redisConfiguration=" + redisConfiguration + ", postgresConfiguration="
				+ postgresConfiguration + "]";
	}
}
