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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.consumer.policy.PolicyConsumer;
import io.jans.lock.service.policy.event.PolicyDownloadEvent;
import io.jans.service.EncryptionService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.net.BaseHttpService;
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

    @Inject
    private EncryptionService encryptionService;

	private ObjectMapper objectMapper;

	private List<String> loadedPoliciesJsonUris;
	private List<String> loadedPoliciesZipUris;

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing Policy Download Service ...");
		this.isActive = new AtomicBoolean(false);

		this.objectMapper = new ObjectMapper();	
		this.loadedPoliciesJsonUris = new ArrayList<>();
		this.loadedPoliciesZipUris = new ArrayList<>();
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
		reloadUrisPolicies();
		reloadZipPolicies();
	}

	private void reloadUrisPolicies() {
		log.debug("Starting URIs policies reload");
		String policiesJsonUrisAuthorizationToken = encryptionService.decrypt(appConfiguration.getPoliciesJsonUrisAuthorizationToken(), true);
		List<String> policiesJsonUris = appConfiguration.getPoliciesJsonUris();
		if (policiesJsonUris == null) {
			return;
		}

		List<String> newPoliciesJsonUris = new ArrayList<>();
		for (String policiesJsonUri : policiesJsonUris) {
			if (StringHelper.isEmpty(policiesJsonUri)) {
				continue;
			}

			List<String> downloadedPolicies = new ArrayList<>();

			HttpGet request = new HttpGet(policiesJsonUri);
			if (StringHelper.isNotEmpty(policiesJsonUrisAuthorizationToken)) {
				request.setHeader("Authorization", "Bearer " + policiesJsonUrisAuthorizationToken);
			}
			try {
				CloseableHttpClient httpClient = httpService.getHttpsClient();
				HttpResponse httpResponse = httpClient.execute(request);

				boolean result = httpService.isResponseStastusCodeOk(httpResponse);
				if (result) {
					String policiesJson = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
					
					JsonNode policiesArray = objectMapper.readTree(policiesJson);
					if (!policiesArray.isArray()) {
						log.error(String.format("Policies URI should provides json array. Skipping it...", policiesJsonUri));
						continue;
					}
					
					for (JsonNode policyUri : policiesArray) {
						String downloadedPolicy = downloadPolicy(policyUri.asText(), policiesJsonUrisAuthorizationToken);
						if (StringHelper.isNotEmpty(downloadedPolicy)) {
							downloadedPolicies.add(downloadedPolicy);
						}
					}
				} else {
			    	log.error("Get invalid response from URI {}", policiesJsonUri);
				}
			} catch (IOException ex) {
		    	log.error("Failed to execute load policies list from URI {}", policiesJsonUri, ex);
			}
			
			policyConsumer.putPolicies(policiesJsonUri, downloadedPolicies);
			newPoliciesJsonUris.add(policiesJsonUri);
		}
		
		// Remove policies from unloaded policy Uris 
		loadedPoliciesJsonUris.removeAll(newPoliciesJsonUris);
		for (String policiesJsonUri : loadedPoliciesJsonUris) {
			policyConsumer.removePolicies(policiesJsonUri);
		}
		
		loadedPoliciesJsonUris = newPoliciesJsonUris;
		log.debug("End URIs policies reload");
	}

	private void reloadZipPolicies() {
		log.debug("Starting Zip policies reload");
		String policiesZipUrisAuthorizationToken = encryptionService.decrypt(appConfiguration.getPoliciesZipUrisAuthorizationToken(), true);
		List<String> policiesZipUris = appConfiguration.getPoliciesZipUris();
		if (policiesZipUris == null) {
			return;
		}

		List<String> newPoliciesZipUris = new ArrayList<>();
		for (String policiesZipUri : policiesZipUris) {
			if (StringHelper.isEmpty(policiesZipUri)) {
				continue;
			}

			List<String> zipPolicies = new ArrayList<>();

			HttpGet request = new HttpGet(policiesZipUri);
			if (StringHelper.isNotEmpty(policiesZipUrisAuthorizationToken)) {
				request.setHeader("Authorization", "Bearer " + policiesZipUrisAuthorizationToken);
			}
			try {
				CloseableHttpClient httpClient = httpService.getHttpsClient();
				HttpResponse httpResponse = httpClient.execute(request);

				boolean result = httpService.isResponseStastusCodeOk(httpResponse);
				if (result) {
					try (ZipInputStream zis = new ZipInputStream(httpResponse.getEntity().getContent())) {
						ZipEntry ze;
			            while ((ze = zis.getNextEntry()) != null) {
			            	String fileName = ze.getName();
			            	
			            	if (ze.isDirectory()) {
			            		continue;
			            	}
	
			            	if (!fileName.endsWith(".rego")) {
			            		continue;
			            	}
	
			            	byte[] fileBytes = IOUtils.toByteArray(zis);
			            	String zipFilePolicy = new String(fileBytes, StandardCharsets.UTF_8);
				            zis.closeEntry();
	
				            if (StringHelper.isNotEmpty(zipFilePolicy)) {
								zipPolicies.add(zipFilePolicy);
							}
			            }
					}
				} else {
			    	log.error("Get invalid response from URI {}", policiesZipUri);
				}
			} catch (IOException ex) {
		    	log.error("Failed to execute load policies list from Zip {}", policiesZipUri, ex);
			}
			
			policyConsumer.putPolicies(policiesZipUri, zipPolicies);
			newPoliciesZipUris.add(policiesZipUri);
		}
		
		// Remove policies from unloaded policy Uris 
		loadedPoliciesZipUris.removeAll(newPoliciesZipUris);
		for (String policiesRegoUri : loadedPoliciesZipUris) {
			policyConsumer.removePolicies(policiesRegoUri);
		}
		
		loadedPoliciesZipUris = newPoliciesZipUris;
		log.debug("End Zip policies reload");
	}

	private String downloadPolicy(String policyUri, String authorizationToken) {
		HttpGet request = new HttpGet(policyUri);
		if (StringHelper.isNotEmpty(authorizationToken)) {
			request.setHeader("Authorization", "Bearer " + authorizationToken);
		}

		try {
			CloseableHttpClient httpClient = httpService.getHttpsClient();
			HttpResponse httpResponse = httpClient.execute(request);
			boolean result = httpService.isResponseStastusCodeOk(httpResponse);
			if (!result) {
		    	log.error("Get invalid response from policy URI {}", policyUri);
				return null;
			}
			
			String policy = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
			
			return policy;
		} catch (IOException ex) {
	    	log.error("Failed to load policy from URI {}", policyUri, ex);
		}
		
		return null;
	}

}
