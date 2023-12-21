/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.policy;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.exception.ConfigurationException;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.Conf;
import io.jans.lock.model.config.Configuration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.ApplicationConfigurationFactory;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.BaseConfigurationReload;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.net.BaseHttpService;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Policy loader service
 *
 * @author Yuriy Movchan Date: 12/21/2023
 */
@ApplicationScoped
public class PolicyDownloader {

	@Inject
	private Logger log;

	@Inject
	private BaseHttpService httpService;

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing PolicyDownloader ...");
		this.isActive = new AtomicBoolean(true);
		try {
			reloadPolicies();
		} finally {
			this.isActive.set(false);
		}
	}

	@Asynchronous
	public void reloadConfigurationTimerEvent(@Observes @Scheduled ConfigurationEvent configurationEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reloadPolicies();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading policies", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private void reloadPolicies() {
		// Download and cache checksum into local cache

	}

}
