/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.ldap.TokenLdap;
import io.jans.as.server.service.fido.u2f.RequestService;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.DeletableEntity;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cache.CacheProvider;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.CleanerEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
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

	public final static int BATCH_SIZE = 1000;
	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager entryManager;

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
	private AppConfiguration appConfiguration;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private Event<TimerEvent> cleanerEvent;

	private long lastFinishedTime;

	private AtomicBoolean isActive;

	public void initTimer() {
		log.debug("Initializing Cleaner Timer");
		this.isActive = new AtomicBoolean(false);

		// Schedule to start cleaner every 30 seconds
		cleanerEvent.fire(
				new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new CleanerEvent(), Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
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

	private boolean isStartProcess() {
		int interval = appConfiguration.getCleanServiceInterval();
		if (interval < 0) {
			log.info("Cleaner Timer is disabled.");
			log.warn("Cleaner Timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
			return false;
		}

		long cleaningInterval = interval * 1000;

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

		return timeDiffrence >= cleaningInterval;
	}

	public void processImpl() {
        try {
			if (!isStartProcess()) {
				log.trace("Starting conditions aren't reached");
				return;
			}

			int chunkSize = appConfiguration.getCleanServiceBatchChunkSize();
            if (chunkSize <= 0)
                chunkSize = BATCH_SIZE;

            	Date now = new Date();

            	final Set<String> processedBaseDns = new HashSet<>();
            	for (CleanDescriptor cleanDescriptor : createCleanServiceBaseDns()) {
				String baseDn = cleanDescriptor.getBaseDn();
				try {
                    if (entryManager.hasExpirationSupport(baseDn)) {
                        continue;
                    }

                    log.debug("Start clean up for baseDn: " + baseDn);
					final Stopwatch started = Stopwatch.createStarted();

					Class<?> entryClass = cleanDescriptor.getClass();
					if (entryManager.hasBranchesSupport(baseDn)) {
						// In LDAP we can do clean from baseDN
						entryClass = null;
					}

					String processedBaseDn = baseDn.toLowerCase() + "_" + (entryClass == null ? "" : entryClass.getSimpleName());
					if (processedBaseDns.contains(processedBaseDn)) {
						log.warn("baseDn: {}, already processed. Please fix cleaner configuration! Skipping second run...", baseDn);
						continue;
					}

					processedBaseDns.add(processedBaseDn);
					int removed = cleanup(baseDn, entryClass, now, chunkSize);

					log.debug("Finished clean up for baseDn: {}, takes: {}ms, removed items: {}", baseDn, started.elapsed(TimeUnit.MILLISECONDS), removed);
				} catch (Exception e) {
					log.error("Failed to process clean up for baseDn: " + baseDn, e);
				}
			}

			processCache(now);

			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Exception e) {
			log.error("Failed to process clean up.", e);
		}
	}

	public List<CleanDescriptor> createCleanServiceBaseDns() {
        final String u2fBase = staticConfiguration.getBaseDn().getU2fBase();

        List<CleanDescriptor> cleanServiceBaseDns = new ArrayList<CleanDescriptor>();
        for (String cleanServiceBaseDn : appConfiguration.getCleanServiceBaseDns()) {
        	cleanServiceBaseDns.add(new CleanDescriptor(cleanServiceBaseDn, null));
        }

        cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getClients(), null));
        cleanServiceBaseDns.add(new CleanDescriptor(umaPctService.branchBaseDn(), null));
        cleanServiceBaseDns.add(new CleanDescriptor(umaResourceService.getBaseDnForResource(), null));
        cleanServiceBaseDns.add(new CleanDescriptor(String.format("ou=registration_requests,%s", u2fBase), null));
        cleanServiceBaseDns.add(new CleanDescriptor(String.format("ou=registered_devices,%s", u2fBase), null));
		cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getPeople(), null));
		cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getMetric(), null));
		cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getTokens(), TokenLdap.class));
		cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getAuthorizations(), null));
		cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getScopes(), null));
		cleanServiceBaseDns.add(new CleanDescriptor(staticConfiguration.getBaseDn().getSessions(), null));

        log.debug("Built-in base dns: " + cleanServiceBaseDns);

		return cleanServiceBaseDns;
	}

	public int cleanup(final String baseDn, final Class<?> entryClass, final Date now, final int batchSize) {
        try {
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("del", true),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn, now)));

            Class<?> cleanEntryClass = entryClass;
            if (cleanEntryClass == null) {
            	cleanEntryClass = DeletableEntity.class;
            }

            int removedCount = entryManager.remove(baseDn, cleanEntryClass, filter, batchSize);
            
            return removedCount;
        } catch (Exception e) {
            log.error("Failed to perform clean up.", e);
        }
        
        return 0;
    }

	private void processCache(Date now) {
		try {
            cacheProvider.cleanup(now);
		} catch (Exception e) {
			log.error("Failed to clean up cache.", e);
		}
	}
	
	private class CleanDescriptor {

		String baseDn;
		Class<?> entryClass;

		public CleanDescriptor(String baseDn, Class<?> entryClass) {
			super();
			this.baseDn = baseDn;
			this.entryClass = entryClass;
		}

		public String getBaseDn() {
			return baseDn;
		}

		public void setBaseDn(String baseDn) {
			this.baseDn = baseDn;
		}

		public Class<?> getEntryClass() {
			return entryClass;
		}

		public void setEntryClass(Class<?> entryClass) {
			this.entryClass = entryClass;
		}

	}

}