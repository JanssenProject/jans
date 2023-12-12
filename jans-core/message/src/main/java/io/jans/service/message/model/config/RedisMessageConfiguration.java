/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.service.cache.RedisConfiguration;

/**
 * @author Yuriy Movchan Date: 07/12/2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisMessageConfiguration extends RedisConfiguration {
}
