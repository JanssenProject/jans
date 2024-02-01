/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.model.config;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * @author Yuriy Movchan Date: 30/11/2023
 */
@XmlEnum(String.class)
public enum MessageProviderType {

	DISABLED, REDIS, POSTGRES

}
