/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.policy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.policy.event.PolicyDownloadEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.net.BaseHttpService;
import io.jans.service.policy.consumer.PolicyConsumer;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan Date: 12/30/2023
 */
@ApplicationScoped
public class PolicyDownloadService {

	private static final int DEFAULT_INTERVAL = 30;

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	private BaseHttpService httpService;
	
	@Inject
	private PolicyConsumer policyConsumer;

	private ObjectMapper objectMapper;

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing Policy Download Service ...");
		this.isActive = new AtomicBoolean(true);

		this.objectMapper = new ObjectMapper();		
	}

	public void initTimer() {
		log.debug("Initializing Policy Download Service Timer");

		final int delay = 30;
		final int interval = DEFAULT_INTERVAL;

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new PolicyDownloadEvent(),
				Scheduled.Literal.INSTANCE));
	}

	@Asynchronous
	public void reloadPoliciesTimerEvent(@Observes @Scheduled PolicyDownloadEvent policyDownloadEvent) {
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
		log.debug("Starting policies reload");
		List<String> policiesJsonUris = appConfiguration.getPoliciesJsonUris();
		if (policiesJsonUris == null) {
			return;
		}

		for (String policiesJsonUri : policiesJsonUris) {
			if (StringHelper.isEmpty(policiesJsonUri)) {
				continue;
			}

			List<String> downloadedPolicies = new ArrayList<>();

			HttpGet request = new HttpGet(policiesJsonUri);
			try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
				HttpResponse httpResponse = httpClient.execute(request);
				
				String policiesJson = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
				
				JsonNode policiesArray = objectMapper.readTree(policiesJson);
				if (!policiesArray.isArray()) {
					log.error(String.format("Policies URI should provides json array. Skipping it...", policiesJsonUri));
					continue;
				}
				
				for (JsonNode policyUri : policiesArray) {
					String downloadedPolicy = downloadPolicy(policyUri.asText());
					if (StringHelper.isNotEmpty(downloadedPolicy)) {
						downloadedPolicies.add(downloadedPolicy);
					}
				}
			} catch (IOException ex) {
		    	log.error("Failed to execute load policies list from URI {}", policiesJsonUri, ex);
			}
			
			policyConsumer.putPolicies(policiesJsonUri, downloadedPolicies);
		}
	}

	private String downloadPolicy(String policyUri) {
		HttpGet request = new HttpGet(policyUri);

		try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
			HttpResponse httpResponse = httpClient.execute(request);
			
			String policy = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
			
			return policy;
		} catch (IOException ex) {
	    	log.error("Failed to load policy from URI {}", policyUri, ex);
		}
		
		return null;
	}

}
