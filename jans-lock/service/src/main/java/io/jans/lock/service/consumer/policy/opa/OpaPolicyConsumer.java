package io.jans.lock.service.consumer.policy.opa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import com.unboundid.util.Base64;

import io.jans.lock.service.external.ExternalLockService;
import io.jans.lock.service.external.context.ExternalLockContext;
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
@ApplicationScoped
public class OpaPolicyConsumer extends PolicyConsumer {

	public static String POLICY_CONSUMER_TYPE = "OPA";

	@Inject
	private ExternalLockService externalLockService;

	@Inject
	private BaseHttpService httpService;

	@Inject
	private Logger log;
	
	private Map<String, List<String>> loadedPolicies;
	
	@PostConstruct
	public void init() {
		this.loadedPolicies = new ConcurrentHashMap<String, List<String>>();
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
		
		int idx = 0;
		for (String policy : policies) {
			String policyId = String.format("%s_%d", baseId, idx);
			HttpPut request = new HttpPut(String.format("http://localhost:8181/v1/policies/%s", policyId));
			
			StringEntity stringEntity = new StringEntity(policy, "text/plain");
			request.setEntity(stringEntity);

			try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
				HttpResponse httpResponse = httpClient.execute(request);
				System.out.println(httpResponse);
				System.out.println(httpResponse.getStatusLine());
			} catch (IOException ex) {
		    	log.error("Failed to execute put data request", ex);
		    	return false;
			}
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
			HttpDelete request = new HttpDelete(String.format("http://localhost:8181/v1/policies/%s", policyId));

			try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
				HttpResponse httpResponse = httpClient.execute(request);
				System.out.println(httpResponse);
				System.out.println(httpResponse.getStatusLine());
			} catch (IOException ex) {
		    	log.error("Failed to execute put data request", ex);
		    	return false;
			}
		}

		return true;
	}

	@Override
	public String getPolicyConsumerType() {
		return POLICY_CONSUMER_TYPE;
	}

}
