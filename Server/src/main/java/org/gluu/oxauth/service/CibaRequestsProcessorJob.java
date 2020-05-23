/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.gluu.oxauth.ciba.CIBAPingCallbackProxy;
import org.gluu.oxauth.ciba.CIBAPushErrorProxy;
import org.gluu.oxauth.model.ciba.PushErrorResponseType;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.CibaRequestsProcessorEvent;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Milton BO
 * @version May 20, 2020
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class CibaRequestsProcessorJob {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private Event<TimerEvent> processorEvent;

	@Inject
	private CIBAPushErrorProxy cibaPushErrorProxy;

	@Inject
	private CIBAPingCallbackProxy cibaPingCallbackProxy;

	@Inject
	private AuthorizationGrantList authorizationGrantList;

	@Inject
	private GrantService grantService;

	private long lastFinishedTime;

	private AtomicBoolean isActive;

	/**
	 * Method invoked from the appInitializer to start processing every some time.
	 */
	public void initTimer() {
		log.debug("Initializing CIBA requests processor");
		this.isActive = new AtomicBoolean(false);
		int intervalSec = appConfiguration.getBackchannelRequestsProcessorJobIntervalSec();

		// Schedule to start processor every N seconds
		processorEvent.fire(new TimerEvent(new TimerSchedule(intervalSec, intervalSec),
				new CibaRequestsProcessorEvent(), Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
	}

	@Asynchronous
	public void process(@Observes @Scheduled CibaRequestsProcessorEvent cibaRequestsProcessorEvent) {
		if (this.isActive.get()) {
			return;
		}
		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			if (jobIsFree()) {
				processImpl();
				this.lastFinishedTime = System.currentTimeMillis();
			} else {
				log.trace("Starting conditions aren't reached for CIBA requestes processor");
			}
		} finally {
			this.isActive.set(false);
		}
	}

	/**
	 * Defines whether the job is still in process or it is free according to the time interval defined.
	 * @return True in case it is free to start a new process.
	 */
	private boolean jobIsFree() {
		int interval = appConfiguration.getBackchannelRequestsProcessorJobIntervalSec();
		if (interval < 0) {
			log.info("CIBA Requests processor timer is disabled.");
			log.warn("CIBA Requests processor timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal CIBA Requests processor up timer run.");
			return false;
		}

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;
		return timeDiffrence >= interval * 1000;
	}

	/**
	 * Main process that process CIBA requests in cache.
	 */
	public void processImpl() {
        try {
			CIBACacheAuthReqIds cibaCacheAuthReqIds = grantService.getCacheCibaAuthReqIds();
			for (Map.Entry<String, Long> entry : cibaCacheAuthReqIds.getAuthReqIds().entrySet()) {
				Date now = new Date();
				if (entry.getValue() < now.getTime()) {
					CIBAGrant cibaGrant = authorizationGrantList.getCIBAGrant(entry.getKey());
					if (cibaGrant != null) {
						processExpiredRequest(cibaGrant);
					}
					authorizationGrantList.removeCibaGrantFromProcessorCache(entry.getKey());
				}
			}
		} catch (Exception e) {
			log.error("Failed to process CIBA request from cache.", e);
		}
	}

	/**
	 * Method responsible to process expired CIBA grants, set them as expired in cache
	 * and send callbacks to the client
	 * @param cibaGrant Object containing data related to the CIBA grant.
	 */
	private void processExpiredRequest(CIBAGrant cibaGrant) {
		if (cibaGrant.getUserAuthorization() != CIBAGrantUserAuthorization.AUTHORIZATION_PENDING
				&& cibaGrant.getUserAuthorization() != CIBAGrantUserAuthorization.AUTHORIZATION_EXPIRED) {
			return;
		}
		log.info("Authentication request id {} has expired", cibaGrant.getCIBAAuthenticationRequestId());

		cibaGrant.setUserAuthorization(CIBAGrantUserAuthorization.AUTHORIZATION_EXPIRED);
		cibaGrant.setTokensDelivered(false);
		cibaGrant.save();

		if (cibaGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {
			cibaPushErrorProxy.pushError(cibaGrant.getCIBAAuthenticationRequestId().getCode(),
					cibaGrant.getClient().getBackchannelClientNotificationEndpoint(),
					cibaGrant.getClientNotificationToken(),
					PushErrorResponseType.EXPIRED_TOKEN,
					"Request has expired and there was no answer from the end user.");
		} else if (cibaGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PING) {
			cibaPingCallbackProxy.pingCallback(
					cibaGrant.getCIBAAuthenticationRequestId().getCode(),
					cibaGrant.getClient().getBackchannelClientNotificationEndpoint(),
					cibaGrant.getClientNotificationToken()
			);
		}
	}

}