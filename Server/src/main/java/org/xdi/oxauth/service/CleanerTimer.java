/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.fido2.persist.AuthenticationPersistenceService;
import org.gluu.oxauth.fido2.persist.RegistrationPersistenceService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.model.ApplicationType;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.RequestMessageLdap;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.service.fido.u2f.RequestService;
import org.xdi.oxauth.uma.service.UmaPctService;
import org.xdi.oxauth.uma.service.UmaPermissionService;
import org.xdi.oxauth.uma.service.UmaResourceService;
import org.xdi.service.cache.CacheConfiguration;
import org.xdi.service.cache.CacheProvider;
import org.xdi.service.cdi.async.Asynchronous;
import org.xdi.service.cdi.event.CleanerEvent;
import org.xdi.service.cdi.event.Scheduled;
import org.xdi.service.timer.event.TimerEvent;
import org.xdi.service.timer.schedule.TimerSchedule;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
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
	private GrantService grantService;

	@Inject
	private UmaPctService umaPctService;

	@Inject
	private UmaPermissionService umaPermissionService;

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
	private MetricService metricService;

	@Inject
	private DeviceRegistrationService deviceRegistrationService;

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
			log.warn(
					"Cleaner Timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
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
		try {
			Date now = new Date();
			final int chunkSize = appConfiguration.getCleanServiceBatchChunkSize();

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

					Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("oxDeletable", "true"),
							Filter.createLessOrEqualFilter("oxAuthExpiration", ldapEntryManager.encodeTime(now)));

					ldapEntryManager.findEntries(baseDn, DeletableEntity.class, filter, SearchScope.SUB,
							new String[] { "oxAuthExpiration", "oxDeletable" }, batchOperation, 0, chunkSize,
							chunkSize);

					log.debug("Finished clean up for baseDn: {}, takes: {}ms", baseDn,
							started.elapsed(TimeUnit.MILLISECONDS));
				} catch (Exception e) {
					log.error("Failed to process clean up for baseDn: {}", baseDn);
				}
			}

			processCache(now);
			processAuthorizationGrantList();

			this.umaPermissionService.cleanup(now);
			this.umaPctService.cleanup(now);

			processU2fRequests();
			processU2fDeviceRegistrations();

			this.registrationPersistenceService.cleanup(now, BATCH_SIZE);
			this.authenticationPersistenceService.cleanup(now, BATCH_SIZE);

			processMetricEntries();
		} catch (Exception e) {
			log.error("Failed to process clean up.", e);
		}
	}

	public Set<String> createCleanServiceBaseDns() {
		final Set<String> cleanServiceBaseDns = Sets.newHashSet(appConfiguration.getCleanServiceBaseDns());
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getClients());
		cleanServiceBaseDns.add(umaPctService.branchBaseDn());
		cleanServiceBaseDns.add(umaResourceService.getBaseDnForResource());
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getU2fBase());
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getPeople());
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getMetric());
		if (cacheConfiguration.getNativePersistenceConfiguration() != null
				&& StringUtils.isNotBlank(cacheConfiguration.getNativePersistenceConfiguration().getBaseDn())) {
			cleanServiceBaseDns.add(cacheConfiguration.getNativePersistenceConfiguration().getBaseDn());
		}
		return cleanServiceBaseDns;
	}

	private void processCache(Date now) {
		try {
			cacheProvider.cleanup(now);
		} catch (Exception e) {
			log.error("Failed to clean up cache.", e);
		}
	}

	private void processAuthorizationGrantList() {
		log.debug("Start AuthorizationGrant clean up");
		grantService.cleanUp();
		log.debug("End AuthorizationGrant clean up");
	}

	private void processU2fRequests() {
		log.debug("Start U2F request clean up");

		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, -90);
		final Date expirationDate = calendar.getTime();

		BatchOperation<RequestMessageLdap> requestMessageLdapBatchService = new ProcessBatchOperation<RequestMessageLdap>() {
			@Override
			public void performAction(List<RequestMessageLdap> entries) {
				for (RequestMessageLdap requestMessageLdap : entries) {
					try {
						log.debug("Removing RequestMessageLdap: {}, Creation date: {}",
								requestMessageLdap.getRequestId(), requestMessageLdap.getCreationDate());
						u2fRequestService.removeRequestMessage(requestMessageLdap);
					} catch (Exception e) {
						log.error("Failed to remove entry", e);
					}
				}
			}
		};

		u2fRequestService.getExpiredRequestMessages(requestMessageLdapBatchService, expirationDate,
				new String[] { "oxRequestId", "creationDate" }, 0, BATCH_SIZE);
		log.debug("End U2F request clean up");
	}

	private void processU2fDeviceRegistrations() {
		log.debug("Start U2F request clean up");

		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, -90);
		final Date expirationDate = calendar.getTime();

		BatchOperation<DeviceRegistration> deviceRegistrationBatchService = new ProcessBatchOperation<DeviceRegistration>() {
			@Override
			public void performAction(List<DeviceRegistration> entries) {
				for (DeviceRegistration deviceRegistration : entries) {
					try {
						log.debug("Removing DeviceRegistration: {}, Creation date: {}", deviceRegistration.getId(),
								deviceRegistration.getCreationDate());
						deviceRegistrationService.removeUserDeviceRegistration(deviceRegistration);
					} catch (Exception e) {
						log.error("Failed to remove entry", e);
					}
				}
			}
		};
		deviceRegistrationService.getExpiredDeviceRegistrations(deviceRegistrationBatchService, expirationDate,
				new String[] { "oxId", "creationDate" }, 0, BATCH_SIZE);

		log.debug("End U2F request clean up");
	}

	private void processMetricEntries() {
		log.debug("Start metric entries clean up");

		int keepDataDays = appConfiguration.getMetricReporterKeepDataDays();

		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.DATE, -keepDataDays);
		Date expirationDate = calendar.getTime();

		metricService.removeExpiredMetricEntries(expirationDate, ApplicationType.OX_AUTH, metricService.applianceInum(),
				0, BATCH_SIZE);

		log.debug("End metric entries clean up");
	}

}