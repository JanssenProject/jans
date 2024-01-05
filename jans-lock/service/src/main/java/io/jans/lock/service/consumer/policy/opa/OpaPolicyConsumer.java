package io.jans.lock.service.consumer.policy.opa;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import com.unboundid.util.Base64;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.external.ExternalLockService;
import io.jans.lock.service.external.context.ExternalLockContext;
import io.jans.service.cdi.qualifier.Implementation;
import io.jans.service.net.BaseHttpService;
import io.jans.service.policy.consumer.PolicyConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OPA policy consumer
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@Implementation
@ApplicationScoped
public class OpaPolicyConsumer extends PolicyConsumer {

	private static final String LOCAL_POLICY_BASE_URI = "http://localhost:8181/v1/policies";

	public static String POLICY_CONSUMER_TYPE = "OPA";

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ExternalLockService externalLockService;

	@Inject
	private BaseHttpService httpService;

	@Inject
	private Logger log;
	
	private MessageDigest sha256Digest;
	
	private Map<String, List<String>> loadedPolicies;
	
	@PostConstruct
	public void init() {
		this.loadedPolicies = new ConcurrentHashMap<String, List<String>>();
		try {
			this.sha256Digest = MessageDigest.getInstance("SHA-256", "BC");
		} catch (NoSuchAlgorithmException ex) {
		} catch (NoSuchProviderException ex) {
			log.error("Failed to prepare SHA256 digister", ex);
		}
	}

	@Override
	public boolean putPolicies(String sourceUri, List<String> policies) {
		log.debug("PutPolicies from {}, count {}", sourceUri, policies.size());

		ExternalLockContext lockContext = new ExternalLockContext();
		externalLockService.beforePolicyPut(sourceUri, policies, lockContext);
		
		if (lockContext.isCancelPdpOperation()) {
			log.debug("PutPolicies was canceled by script");
			return true;
		}

		// Send rest request to OPA
		String baseId = Base64.urlEncode(sourceUri, false);

		if (!loadedPolicies.containsKey(baseId)) {
			loadedPolicies.put(baseId, new ArrayList<>(policies.size()));
		}
		
		List<String> policyIds = loadedPolicies.get(baseId);
		
		List<String> cleanPolicyIds= new ArrayList<>(policyIds);
		for (String policy : policies) {
			byte[] digest = sha256Digest.digest(policy.getBytes(StandardCharsets.UTF_8));
			String policyId = new BigInteger(1, digest).toString();
			
			if (policyIds.contains(policyId)) {
				cleanPolicyIds.remove(policyId);
				log.debug("Policy with digiest '{}' is already downloaded", policyId);
				continue;
			}

			String baseUrl = appConfiguration.getOpaConfiguration().getBaseUrl();
			HttpPut request = new HttpPut(String.format("%s/policies/%s", baseUrl, policyId));
			
			StringEntity stringEntity = new StringEntity(policy, ContentType.TEXT_PLAIN);
			request.setEntity(stringEntity);

			try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
				HttpResponse httpResponse = httpClient.execute(request);
				System.out.println(httpResponse);
				System.out.println(httpResponse.getStatusLine());
			} catch (IOException ex) {
		    	log.error("Failed to add policy to OPA", ex);
			}

			policyIds.add(policyId);
		}
		
		// Remove old policies after processing currentPoliciesDigests
		for (String policyId : cleanPolicyIds) {
			sendRemovePolicyRequest(policyId);
			policyIds.remove(policyId);
		}

		return true;
	}

	@Override
	public boolean removePolicies(String sourceUri) {
		log.debug("RemovePolicies from {}", sourceUri);

		ExternalLockContext lockContext = new ExternalLockContext();
		externalLockService.beforePolicyRemoval(sourceUri, lockContext);
		
		if (lockContext.isCancelPdpOperation()) {
			log.debug("RemovePolicies was canceled by script");
			return true;
		}
		
		// Sent rest request to OPA
		String baseId = Base64.urlEncode(sourceUri, false);
		List<String> policyIds = loadedPolicies.get(baseId);
		
		if (policyIds == null) {
	    	log.warn("There is no loadeed policies from sourceUri: '{}'", sourceUri);
	    	return false;
		}

		for (String policyId : policyIds) {
			sendRemovePolicyRequest(policyId);
		}

		return true;
	}

	public void sendRemovePolicyRequest(String policyId) {
		log.debug("Remove policy '{}'", policyId);
		
		String baseUrl = appConfiguration.getOpaConfiguration().getBaseUrl();
		HttpDelete request = new HttpDelete(String.format("%s/policies/%s", baseUrl, policyId));

		try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
			HttpResponse httpResponse = httpClient.execute(request);
			System.out.println(httpResponse);
			System.out.println(httpResponse.getStatusLine());
		} catch (IOException ex) {
			log.error("Failed to remove policy from OPA", ex);
		}
	}

	@Override
	public String getPolicyConsumerType() {
		return POLICY_CONSUMER_TYPE;
	}

}
