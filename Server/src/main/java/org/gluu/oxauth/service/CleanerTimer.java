/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.fido2.persist.AuthenticationPersistenceService;
import org.gluu.oxauth.fido2.persist.RegistrationPersistenceService;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.service.fido.u2f.RequestService;
import org.gluu.oxauth.uma.service.UmaPctService;
import org.gluu.oxauth.uma.service.UmaResourceService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;
import org.gluu.service.cache.CacheConfiguration;
import org.gluu.service.cache.CacheProvider;
import org.gluu.service.cache.CacheProviderType;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.CleanerEvent;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;
import org.gluu.oxauth.model.config.StaticConfiguration;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class CleanerTimer {

	public final static int BATCH_SIZE = 25;
	private final static int DEFAULT_INTERVAL = 600; // 10 minutes

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private UmaPctService umaPctService;

	@Inject
	private UmaResourceService umaResourceService;

	@Inject
	private CacheProvider cacheProvider;

	@Inject
	@Named("u2fRequestService")
	private RequestService u2fRequestService;

	@Inject
	private AuthenticationPersistenceService authenticationPersistenceService;

	@Inject
	private RegistrationPersistenceService registrationPersistenceService;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private CacheConfiguration cacheConfiguration;

	@Inject
	private Event<TimerEvent> cleanerEvent;

	private AtomicBoolean isActive;

	public void initTimer() {
		log.debug("Initializing Cleaner Timer");
		this.isActive = new AtomicBoolean(false);

		int interval = appConfiguration.getCleanServiceInterval();
		if (interval < 0) {
			log.info("Cleaner Timer is disabled.");
			log.warn("Cleaner Timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
			return;
		}

		if (interval == 0) {
			interval = DEFAULT_INTERVAL;
		}

		cleanerEvent.fire(
				new TimerEvent(new TimerSchedule(interval, interval), new CleanerEvent(), Scheduled.Literal.INSTANCE));
	}

	@Asynchronous
	public void process(@Observes @Scheduled CleanerEvent cleanerEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			processImpl();
		} finally {
			this.isActive.set(false);
		}
	}

	public void processImpl() {
        int chunkSize = appConfiguration.getCleanServiceBatchChunkSize();
        if (chunkSize <= 0)
            chunkSize = BATCH_SIZE;
        try {
            Date now = new Date();

			for (String baseDn : createCleanServiceBaseDns()) {
				try {
					log.debug("Start clean up for baseDn: " + baseDn);
					final Stopwatch started = Stopwatch.createStarted();

					BatchOperation<DeletableEntity> batchOperation = new ProcessBatchOperation<DeletableEntity>() {
						@Override
						public void performAction(List<DeletableEntity> entries) {
							for (DeletableEntity entity : entries) {
								try {
									ldapEntryManager.removeRecursively(entity.getDn());
									log.trace("Removed {}", entity.getDn());
								} catch (Exception e) {
									log.error("Failed to remove entry, dn: " + entity.getDn(), e);
								}
							}
						}
					};

					Filter filter = Filter.createANDFilter(
					        Filter.createEqualityFilter("oxDeletable", "true"),
							Filter.createLessOrEqualFilter("oxAuthExpiration", ldapEntryManager.encodeTime(now))
                    );

					ldapEntryManager.findEntries(baseDn, DeletableEntity.class, filter, SearchScope.SUB,
							new String[] { "oxAuthExpiration", "oxDeletable" }, batchOperation, 0, chunkSize,
							chunkSize);

					log.debug("Finished clean up for baseDn: {}, takes: {}ms", baseDn, started.elapsed(TimeUnit.MILLISECONDS));
				} catch (Exception e) {
					log.error("Failed to process clean up for baseDn: {}", baseDn);
				}
			}

			processCache(now);

			this.registrationPersistenceService.cleanup(now, chunkSize);
			this.authenticationPersistenceService.cleanup(now, chunkSize);
		} catch (Exception e) {
			log.error("Failed to process clean up.", e);
		}
	}

	public Set<String> createCleanServiceBaseDns() {
        final String u2fBase = staticConfiguration.getBaseDn().getU2fBase();

        final Set<String> cleanServiceBaseDns = Sets.newHashSet(appConfiguration.getCleanServiceBaseDns());
        cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getClients());
        cleanServiceBaseDns.add(umaPctService.branchBaseDn());
        cleanServiceBaseDns.add(umaResourceService.getBaseDnForResource());
        cleanServiceBaseDns.add(String.format("ou=registration_requests,%s", u2fBase));
        cleanServiceBaseDns.add(String.format("ou=registered_devices,%s", u2fBase));
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getPeople());
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getMetric());
		if (cacheConfiguration.getNativePersistenceConfiguration() != null
				&& StringUtils.isNotBlank(cacheConfiguration.getNativePersistenceConfiguration().getBaseDn())) {
			cleanServiceBaseDns.add(cacheConfiguration.getNativePersistenceConfiguration().getBaseDn());
		}

        log.debug("Built-in base dns: " + cleanServiceBaseDns);

		return cleanServiceBaseDns;
	}

	private void processCache(Date now) {
		try {
            if (cacheConfiguration.getCacheProviderType() != CacheProviderType.NATIVE_PERSISTENCE) {
                cacheProvider.cleanup(now);
            }
		} catch (Exception e) {
			log.error("Failed to clean up cache.", e);
		}
	}
}