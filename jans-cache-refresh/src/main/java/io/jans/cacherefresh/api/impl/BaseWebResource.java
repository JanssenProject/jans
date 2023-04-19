package io.jans.cacherefresh.api.impl;

import org.slf4j.Logger;

public class BaseWebResource {

	public BaseWebResource() {
	}

	public void log(Logger logger, Exception e) {
		logger.debug("++++++++++API-ERROR", e);
	}

	public void log(Logger logger, String message) {
		logger.info(message);
	}

}
