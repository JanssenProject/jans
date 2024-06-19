/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.server.service.cdi.event.TokenPoolUpdateEvent;
import io.jans.model.cluster.ClusterNode;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class ClusterNodeManager {

	@Inject
	private Logger log;

	@Inject
	private ClusterNodeService clusterNodeService;

	@Inject
	private Event<TimerEvent> timerEvent;

	private AtomicBoolean isActive;
	
	private ClusterNode clusterNode;

	@PostConstruct
	public void init() {
		log.info("Initializing Cluster Manager ...");
		this.isActive = new AtomicBoolean(false);
		
		this.clusterNode = clusterNodeService.allocate();

		log.info("Assigned cluster node id '{}' for this instance", clusterNode.getId());
	}

	public void initTimer() {
		log.debug("Initializing Policy Download Service Timer");

		final int delayInSeconds = 30;
		final int intervalInSeconds = 30;

		timerEvent.fire(new TimerEvent(new TimerSchedule(delayInSeconds, intervalInSeconds), new TokenPoolUpdateEvent(),
				Scheduled.Literal.INSTANCE));
	}

	@Asynchronous
	public void reloadNodesTimerEvent(@Observes @Scheduled TokenPoolUpdateEvent tokenPoolUpdateEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			updateClusterNode();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading nodes", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private void updateClusterNode() {
		clusterNodeService.refresh(clusterNode);
	}


    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("Stopping cluster manager...");
        clusterNodeService.release(clusterNode);
    }
    
    public Integer getClusterNodeId() {
    	return clusterNode.getId();
    }

}