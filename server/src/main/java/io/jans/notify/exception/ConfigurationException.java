/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.exception;

/**
 * Configuration exception
 * 
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class ConfigurationException extends RuntimeException {

	private static final long serialVersionUID = -7590161991536595499L;

	public ConfigurationException() {
	}

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(Throwable cause) {
		super(cause);
	}

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
