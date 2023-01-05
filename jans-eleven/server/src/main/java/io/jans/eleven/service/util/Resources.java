/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service.util;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which provides external resources for injection
 *
 * @author Yuriy Movchan Date: 14/03/2017
 */
public class Resources {

	@Produces
	public Logger getLogger(InjectionPoint ip) {
		Class<?> clazz = ip.getMember().getDeclaringClass();

		return LoggerFactory.getLogger(clazz);
	}

}
