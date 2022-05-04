/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.resource;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which provides external resources for injection
 *
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
public class Resources {

	@Produces
	public Logger getLogger(InjectionPoint ip) {
		Class<?> clazz = ip.getMember().getDeclaringClass();

		return LoggerFactory.getLogger(clazz);
	}

}
