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

package io.jans.core.cedarling.service.policy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

import io.jans.core.cedarling.service.event.PolicyDownloadEvent;
import io.jans.core.cedarling.model.CedarlingConfiguration;
import io.jans.core.cedarling.model.PolicySource;
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

	/** Seconds between policy store reloads; each reload publishes a {@link PolicySnapshotEvent}. */
	public static final int RELOAD_INTERVAL_SECONDS = 30;

	public static final MediaType APPLICATION_ZIP_TYPE = new MediaType("application", "zip");

	public static final int MAX_ARCHIVE_BYTES = 16 * 1024 * 1024;

	public static final int MAX_SIGSTORE_BUNDLE_BYTES = 1024 * 1024;

	public static final String SIGSTORE_BUNDLE_SUFFIX = ".sigstore.json";

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private Event<PolicySnapshotEvent> policySnapshotEvent;

    @Inject
	private CedarlingConfiguration cedarlingConfiguration;

	@Inject
	private BaseHttpService httpService;

    @Inject
    private EncryptionService encryptionService;

	private ObjectMapper objectMapper;

	private final AtomicReference<Map<String, LoadedPolicySource>> loadedPolicySourcesUris = new AtomicReference<>(
			Collections.emptyMap());

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing Policy Download Service ...");
		this.isActive = new AtomicBoolean(false);

		this.objectMapper = new ObjectMapper();	
		this.loadedPolicySourcesUris.set(Collections.emptyMap());
	}

	public void initTimer() {
		log.debug("Initializing Policy Download Service Timer");

		final int delay = 30;
		final int interval = RELOAD_INTERVAL_SECONDS;

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
		} catch (Exception ex) {
			log.error("Exception happened while reloading policies", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private void reloadPolicies() {
		
		if ((cedarlingConfiguration.getPolicySources() == null) || (cedarlingConfiguration.getPolicySources().isEmpty())) {
			log.debug("Policies sources is not specified");
			
			// Remove all loaded policies
			cleanAllPolicies();
			return;
		}

		Map<String, LoadedPolicySource> reloaded = new LinkedHashMap<>();
		for(PolicySource policySource : cedarlingConfiguration.getPolicySources()) {
			// Load policy source from specified source
			List<LoadedPolicySource> loadedPolicySources = loadPolicy(policySource);
			if (loadedPolicySources == null || loadedPolicySources.isEmpty()) {
				continue;
			}

			for(LoadedPolicySource loadedPolicySource : loadedPolicySources) {
				String policyStoreUri = loadedPolicySource.getPolicyStoreUri();
				if (reloaded.put(policyStoreUri, loadedPolicySource) != null) {
					log.warn("Policy '{}' from '{}' replaces a policy loaded from another source", policyStoreUri, loadedPolicySource.getSourceUri());
				}
			}
		}

		publish(Collections.unmodifiableMap(reloaded));

		log.debug("End URIs policies reload. Count policies: {}", reloaded.size());
	}

	private List<LoadedPolicySource> loadPolicy(PolicySource policySource) {
		String policyStoreUri = policySource.getPolicyStoreUri();
		if (!policySource.isEnabled()) {
			log.debug("Found disabled policy URI: {}", policyStoreUri);
			return Collections.emptyList();
		}

		log.debug("Starting policy store load from URI: {}", policyStoreUri);
		if (StringHelper.isEmpty(policyStoreUri)) {
			log.warn("Policy store URI is not specified");
			return Collections.emptyList();
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
		    	return Collections.emptyList();
			}
			
			MediaType responseMediaType = parseMediaType(httpResponse);
            if (responseMediaType != null) {
                if (responseMediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)
                        || responseMediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                    return loadJsonPolicyFromResponse(httpResponse, policyStoreUri);
                } else if (responseMediaType.isCompatible(APPLICATION_ZIP_TYPE)
                        || responseMediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE)) {
                    return loadArchiveFromResponse(httpResponse, policyStoreUri, decryptedAuthorizationToken);
                }
            }

			log.error("Unsupported response media type from URI {}", policyStoreUri);
		} catch (IOException ex) {
			log.error("Failed to execute load policies list from URI {}", policyStoreUri, ex);
		}

		return Collections.emptyList();
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

	private List<LoadedPolicySource> loadArchiveFromResponse(HttpResponse httpResponse, String policyStoreUri, String authorizationToken) throws IOException {
		byte[] archive;
		try (InputStream is = httpResponse.getEntity().getContent()) {
			archive = is.readNBytes(MAX_ARCHIVE_BYTES + 1);
		}

		if (archive.length > MAX_ARCHIVE_BYTES) {
			log.error("Policy archive from URI {} exceeds {} bytes", policyStoreUri, MAX_ARCHIVE_BYTES);
			return Collections.emptyList();
		}

		JsonNode policyStore;
		try {
			policyStore = CjarArchive.readPolicyStoreMetadata(archive, getMaxEntries(), objectMapper);
		} catch (IllegalStateException ex) {
			log.error("Rejected policy archive from URI {}: {}", policyStoreUri, ex.getMessage());
			return Collections.emptyList();
		}

		if (policyStore == null) {
			return loadZipPolicies(new ByteArrayInputStream(archive), policyStoreUri);
		}

		String policyStoreId = CjarArchive.text(policyStore, "id");
		if (StringHelper.isEmpty(policyStoreId)) {
			log.error("Policy store archive from URI {} has no policy_store.id in metadata.json", policyStoreUri);
			return Collections.emptyList();
		}

		String bundleHash = CjarArchive.bundleHash(archive);
		String sigstoreBundle = loadSigstoreBundle(policyStoreUri, authorizationToken);
		log.debug("End policy store archive load from: '{}', id: '{}', bundle hash: '{}', sigstore bundle: {}", policyStoreUri, policyStoreId, bundleHash, sigstoreBundle != null);

		LoadedPolicySource loadedPolicySource = new LoadedPolicySource(policyStoreId, archive, bundleHash, policyStoreUri);
		loadedPolicySource.setPolicyStoreName(CjarArchive.text(policyStore, "name"));
		loadedPolicySource.setPolicyStoreVersion(CjarArchive.text(policyStore, "version"));
		loadedPolicySource.setSigstoreBundle(sigstoreBundle);

		return Arrays.asList(loadedPolicySource);
	}

	private String loadSigstoreBundle(String policyStoreUri, String authorizationToken) {
		String bundleUri = policyStoreUri + SIGSTORE_BUNDLE_SUFFIX;
		HttpGet request = new HttpGet(bundleUri);
		if (StringHelper.isNotEmpty(authorizationToken)) {
			request.setHeader("Authorization", "Bearer " + authorizationToken);
		}

		try {
			HttpResponse httpResponse = httpService.getHttpsClient().execute(request);
			if (!httpService.isResponseStastusCodeOk(httpResponse)) {
				log.debug("No Sigstore bundle at URI {}", bundleUri);
				EntityUtils.consumeQuietly(httpResponse.getEntity());
				return null;
			}

			if (httpResponse.getEntity() == null) {
				log.debug("Empty Sigstore bundle response from URI {}", bundleUri);
				return null;
			}

			byte[] bundle;
			try (InputStream is = httpResponse.getEntity().getContent()) {
				bundle = is.readNBytes(MAX_SIGSTORE_BUNDLE_BYTES + 1);
			}

			if (bundle.length > MAX_SIGSTORE_BUNDLE_BYTES) {
				log.error("Sigstore bundle from URI {} exceeds {} bytes", bundleUri, MAX_SIGSTORE_BUNDLE_BYTES);
				return null;
			}

			String bundleJson = new String(bundle, StandardCharsets.UTF_8);
			objectMapper.readTree(bundleJson);

			return bundleJson;
		} catch (IOException ex) {
			log.warn("Failed to load Sigstore bundle from URI {}", bundleUri, ex);
			return null;
		}
	}

	private List<LoadedPolicySource> loadZipPolicies(InputStream zipStream, String policyStoreUri) throws IOException {
		List<LoadedPolicySource> result = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(zipStream)) {
			ZipEntry ze;
			int entriesCount = 0;
          
            while ((ze = zis.getNextEntry()) != null) {
                entriesCount++;
            	String fileName = ze.getName();
            	
            	if (ze.isDirectory() || (!(fileName.endsWith(".json") || fileName.endsWith(".cedar"))) ){
            		continue;
            	}
            	
            	validateZip(entriesCount);

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
	
	private int getMaxEntries() {
	    return this.cedarlingConfiguration.getMaxEntries();
	}
	
	private void validateZip(int entriesCount) {
	 // Guard against Zip Bomb: Entry Count       
        if (getMaxEntries() > 0 && entriesCount > getMaxEntries()) {
            throw new IllegalStateException("Archive contains too many files.");
        }
	}

	private JsonNode parsePolicyJson(String policyStoreUri, String policyJson) {
		try {
			if (policyJson.contains("\"cedar_version\"")) {
				JsonNode policyStoreJson = objectMapper.readTree(policyJson);
				
				if (!(policyStoreJson.hasNonNull("cedar_version") && policyStoreJson.hasNonNull("policy_stores"))) {
					log.error("Policy store is invalid: {}", policyStoreUri);
					return null;
				}

				return policyStoreJson;
			} if (policyJson.contains("\"content\"") && policyJson.contains("\"encoding\"") && policyJson.contains("\"html\"")) {
				JsonNode responseJson = objectMapper.readTree(policyJson);
				
				if (!(responseJson.hasNonNull("content") && responseJson.hasNonNull("encoding") && "base64".equals(responseJson.get("encoding").asText()))) {
					log.error("Response doesn't contains required 'content' and 'encoding' attributes : '{}'", responseJson);
					return null;
				}

				JsonNode policyStoreJson = objectMapper.readTree(httpService.decodeBase64(responseJson.get("content").asText()));
				
				if (!(policyStoreJson.hasNonNull("cedar_version") && policyStoreJson.hasNonNull("policy_stores"))) {
					log.error("Policy store is invalid: '{}'", policyStoreUri);
					return null;
				}

				return policyStoreJson;
			}
		} catch (Exception ex) {
			log.error("Failed to parse JSON response from URI: {}", policyStoreUri);
			log.trace("Failed to parse JSON file: {}", policyJson);
			return null;
		}
		
		log.error("Fetched policy doesn't contains \"cedar_version\": {}", policyJson);
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
		publish(Collections.emptyMap());
	}

	/**
	 * Swaps in the new set atomically, so readers see either the previous or the new one, then
	 * tells observers. An observer failure is logged and never undoes the swap.
	 */
	private void publish(Map<String, LoadedPolicySource> snapshot) {
		Map<String, LoadedPolicySource> previous = loadedPolicySourcesUris.getAndSet(snapshot);
		try {
			policySnapshotEvent.fire(new PolicySnapshotEvent(previous, snapshot, System.currentTimeMillis()));
		} catch (RuntimeException ex) {
			log.error("Policy snapshot observer failed", ex);
		}
	}

	/**
	 * @return the current snapshot, unmodifiable; a reload publishes a new map instead of
	 *         changing this one
	 */
	public Map<String, LoadedPolicySource> getLoadedPolicies() {
		return loadedPolicySourcesUris.get();
	}

	public class LoadedPolicySource {
		private final String policyStoreUri;
		private final String policyJson;
		private final byte[] archive;
		private final String bundleHash;
		private final String sourceUri;
		private String policyStoreName;
		private String policyStoreVersion;
		private String sigstoreBundle;

		public LoadedPolicySource(String policyStoreUri, String policyJson) {
			this(policyStoreUri, policyJson, null, null, policyStoreUri);
		}

		public LoadedPolicySource(String policyStoreId, byte[] archive, String bundleHash, String sourceUri) {
			this(policyStoreId, null, archive, bundleHash, sourceUri);
		}

		private LoadedPolicySource(String policyStoreUri, String policyJson, byte[] archive, String bundleHash, String sourceUri) {
			this.policyStoreUri = policyStoreUri;
			this.policyJson = policyJson;
			this.archive = archive;
			this.bundleHash = bundleHash;
			this.sourceUri = sourceUri;
		}

		public String getPolicyJson() {
			return policyJson;
		}

		public String getPolicyStoreUri() {
			return policyStoreUri;
		}

		public boolean isArchive() {
			return archive != null;
		}

		public byte[] getArchive() {
			return archive;
		}

		public String getBundleHash() {
			return bundleHash;
		}

		public String getSourceUri() {
			return sourceUri;
		}

		public String getPolicyStoreName() {
			return policyStoreName;
		}

		public void setPolicyStoreName(String policyStoreName) {
			this.policyStoreName = policyStoreName;
		}

		public String getPolicyStoreVersion() {
			return policyStoreVersion;
		}

		public void setPolicyStoreVersion(String policyStoreVersion) {
			this.policyStoreVersion = policyStoreVersion;
		}

		public String getSigstoreBundle() {
			return sigstoreBundle;
		}

		public void setSigstoreBundle(String sigstoreBundle) {
			this.sigstoreBundle = sigstoreBundle;
		}

		@Override
		public String toString() {
			return "LoadedPolicySource [policyStoreUri=" + policyStoreUri + ", sourceUri=" + sourceUri
					+ ", bundleHash=" + bundleHash + ", policyJson=" + policyJson + "]";
		}
	}
}
