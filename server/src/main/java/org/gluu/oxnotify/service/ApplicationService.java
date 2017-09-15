/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.service;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxnotify.model.conf.Configuration;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@ApplicationScoped
@Named
public class ApplicationService {

	@Inject
	private Logger log;

	@Inject
	private Configuration configuration;

	@PostConstruct
	public void init() {

	}

}
