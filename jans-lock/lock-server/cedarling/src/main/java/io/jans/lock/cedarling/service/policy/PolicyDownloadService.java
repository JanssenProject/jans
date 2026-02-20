/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.cedarling.service.policy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.cedarling.service.event.PolicyDownloadEvent;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingConfiguration;
import io.jans.lock.model.config.cedarling.PolicySource;
import io.jans.service.EncryptionService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
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
import jakarta.ws.rs.core.MediaType;

/**
 * @author Yuriy Movchan Date: 12/30/2023
 */
@ApplicationScoped
public class PolicyDownloadService {

	private static final int DEFAULT_INTERVAL = 30;

	public static final MediaType APPLICATION_ZIP_TYPE = new MediaType("application", "zip");

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	private BaseHttpService httpService;

    @Inject
    private EncryptionService encryptionService;

	private ObjectMapper objectMapper;

	private Map<String, LoadedPolicySource> loadedPolicySourcesUris;

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing Policy Download Service ...");
		this.isActive = new AtomicBoolean(false);

		this.objectMapper = new ObjectMapper();	
		this.loadedPolicySourcesUris = new HashMap<>();
	}

	public void initTimer() {
		log.debug("Initializing Policy Download Service Timer");

		final int delay = 30;
		final int interval = DEFAULT_INTERVAL;

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new PolicyDownloadEvent(),
				Scheduled.Literal.INSTANCE));
	}
	
	public void initTime(@Observes @ApplicationInitialized(ApplicationScoped.class) ApplicationInitializedEvent event) {
		initTimer();
		reloadPolicies();
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
		CedarlingConfiguration cedarlingConfiguration = appConfiguration.getCedarlingConfiguration();
		if ((cedarlingConfiguration.getPolicySources() == null) || (cedarlingConfiguration.getPolicySources().size() == 0)) {
			log.debug("Policies sources is not specified");
			
			// Remove all loaded policies
			cleanAllPolicies();
			return;
		}

		List<String> downloadedPolicies = new ArrayList<>();
		for(PolicySource policySource : cedarlingConfiguration.getPolicySources()) {
			// Load policy source from specified source
			List<LoadedPolicySource> loadedPolicySources = loadPolicy(policySource);
			if (loadedPolicySources == null) {
				continue;
			}

			for(LoadedPolicySource loadedPolicySource : loadedPolicySources) {
				String policyStoreUri = loadedPolicySource.getPolicyStoreUri();

				// Collect list of policyStoreUris
				downloadedPolicies.add(policyStoreUri);

				// Update loadedPolicySourcesUris
				loadedPolicySourcesUris.put(policyStoreUri, loadedPolicySource);
			}
		}

		// Remove unloaded policy Uris 
		for (Iterator<Entry<String, LoadedPolicySource>> it = loadedPolicySourcesUris.entrySet().iterator(); it.hasNext();) {
			Entry<String, LoadedPolicySource> item = it.next();
			if (!downloadedPolicies.contains(item.getKey())) {
				it.remove();
			}
		}

		log.debug("End URIs policies reload. Count policies: {}", loadedPolicySourcesUris.size());
	}

	private List<LoadedPolicySource> loadPolicy(PolicySource policySource) {
		String policyStoreUri = policySource.getPolicyStoreUri();
		if (!policySource.isEnabled()) {
			log.debug("Found disabled policy URI: {}", policyStoreUri);
			return null;
		}

		log.debug("Starting policy store load from URI: {}", policyStoreUri);
		if (StringHelper.isEmpty(policyStoreUri)) {
			log.warn("Policy store URI is not specified");
			return null;
		}

		// Decrypt Authorization Token
		String decryptedAuthorizationToken = encryptionService.decrypt(policySource.getAuthorizationToken(), true);

		HttpGet request = new HttpGet(policyStoreUri);

		// Use Authorization Token if specified
		if (StringHelper.isNotEmpty(decryptedAuthorizationToken)) {
			request.setHeader("Authorization", "Bearer " + decryptedAuthorizationToken);
		}

		CloseableHttpClient httpClient = httpService.getHttpsClient();
		try {
			HttpResponse httpResponse = httpClient.execute(request);

			boolean result = httpService.isResponseStastusCodeOk(httpResponse);
			if (!result) {
		    	log.error("Get invalid response from URI {}", policyStoreUri);
				return null;
			}
			
			MediaType responseMediaType = parseMediaType(httpResponse);
			if (responseMediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) || responseMediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
				return loadJsonPolicyFromResponse(httpResponse, policyStoreUri);
			} else if (responseMediaType.isCompatible(APPLICATION_ZIP_TYPE)) {
				return loadZipPoliciesFromResponse(httpResponse, policyStoreUri);
			}

			log.error("Unsupported response media type from URI {}", policyStoreUri);
		} catch (IOException ex) {
			log.error("Failed to execute load policies list from URI {}", policyStoreUri, ex);
		}

		return null;
	}

	private List<LoadedPolicySource> loadJsonPolicyFromResponse(HttpResponse httpResponse, String policyStoreUri) throws IOException {
		String policyJson = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
		JsonNode policyStoreJson = parsePolicyJson(policyStoreUri, policyJson);
		boolean valid = policyStoreJson != null; 

		log.debug("End JSON policies load from: '{}'", policyStoreUri);

		if (valid) {
			return Arrays.asList(new LoadedPolicySource(policyStoreUri, policyStoreJson.toString()));
		}

		return null;
	}

	private List<LoadedPolicySource> loadZipPoliciesFromResponse(HttpResponse httpResponse, String policyStoreUri) throws IOException {
		List<LoadedPolicySource> result = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(httpResponse.getEntity().getContent())) {
			ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
            	String fileName = ze.getName();
            	
            	if (ze.isDirectory()) {
            		continue;
            	}

            	if (!(fileName.endsWith(".json") || fileName.endsWith(".cedar"))) {
            		continue;
            	}

            	byte[] fileBytes = IOUtils.toByteArray(zis);
            	String zipFilePolicy = new String(fileBytes, StandardCharsets.UTF_8);
	            zis.closeEntry();
	            
	            String filePath = policyStoreUri + ":" + fileName;
	            boolean valid = parsePolicyJson(filePath, zipFilePolicy) != null;

	            if (valid) {
	            	result.add(new LoadedPolicySource(filePath, zipFilePolicy));
	    		}
            }
		}
		
		log.debug("End Zip policies load from: '{}'", policyStoreUri);
		
		return result;
	}

	private JsonNode parsePolicyJson(String policyStoreUri, String policyJson) {
		try {
			if (policyJson.contains("\"cedar_version\"")) {
				JsonNode policyStoreJson = objectMapper.readTree(policyJson);
				
				if (!(policyStoreJson.hasNonNull("cedar_version") && policyStoreJson.hasNonNull("policy_stores"))) {
					log.error(String.format("Policy store is invalid: '{}'", policyStoreUri));
					return null;
				}

				return policyStoreJson;
			} if (policyJson.contains("\"content\"") && policyJson.contains("\"encoding\"") && policyJson.contains("\"html\"")) {
				JsonNode responseJson = objectMapper.readTree(policyJson);
				
				if (!(responseJson.hasNonNull("content") && responseJson.hasNonNull("encoding") && "base64".equals(responseJson.get("encoding").asText()))) {
					log.error(String.format("Response doesn't contains required 'content' and 'encoding' attributes : '{}'", responseJson));
					return null;
				}

				JsonNode policyStoreJson = objectMapper.readTree(httpService.decodeBase64(responseJson.get("content").asText()));
				
				if (!(policyStoreJson.hasNonNull("cedar_version") && policyStoreJson.hasNonNull("policy_stores"))) {
					log.error(String.format("Policy store is invalid: '{}'", policyStoreUri));
					return null;
				}

				return policyStoreJson;
			}
		} catch (Exception ex) {
			log.error(String.format("Failed to parse JSON response from URI: '{}'", policyStoreUri));
			log.trace(String.format("Failed to parse JSON file: '{}'", policyJson));
			return null;
		}
		
		log.error(String.format("Fetched policy doesn't contains \"cedar_version\": '{}'", policyJson));
		return null;
	}

	private MediaType parseMediaType(HttpResponse response) {
		Header contentTypeHeader = response.getFirstHeader("Content-Type");
		if (contentTypeHeader != null) {
			String contentTypeStr = contentTypeHeader.getValue();
			try {
				MediaType contentType = MediaType.valueOf(contentTypeStr);
				log.debug("Parsed content type '{}' to media type '{}'", contentTypeStr, contentType);
				
				return contentType;
			} catch (IllegalArgumentException ex) {
				log.error("Failed to parse content type: '{}'", contentTypeStr);
			}
		}

		return null;
	}

	private void cleanAllPolicies() {
		loadedPolicySourcesUris.clear();
	}

	public Map<String, LoadedPolicySource> getLoadedPolicies() {
		if (loadedPolicySourcesUris == null) {
			return new HashMap<String, PolicyDownloadService.LoadedPolicySource>();
		}

		return new HashMap<String, PolicyDownloadService.LoadedPolicySource>(loadedPolicySourcesUris);
	}

	public class LoadedPolicySource {
		private final String policyStoreUri;
		private final String policyJson;

		public LoadedPolicySource(String policyStoreUri, String policyJson) {
			this.policyStoreUri = policyStoreUri;
			this.policyJson = policyJson;
		}

		public String getPolicyJson() {
			return policyJson;
		}

		public String getPolicyStoreUri() {
			return policyStoreUri;
		}

		@Override
		public String toString() {
			return "LoadedPolicySource [policyJson=" + policyJson + ", policyStoreUri=" + policyStoreUri + "]";
		}
	}
}
