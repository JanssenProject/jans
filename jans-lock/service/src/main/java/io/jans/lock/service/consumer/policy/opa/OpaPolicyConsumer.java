package io.jans.lock.service.consumer.policy.opa;

import org.json.JSONArray;
import org.slf4j.Logger;

import io.jans.service.policy.consumer.PolicyConsumer;
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
	private Logger log;

	@Override
	public boolean putPolicies(String sourceUri, JSONArray policies) {
		log.debug("putPolicies from {}, count {}", sourceUri, policies.length());
		return true;
	}

	@Override
	public boolean removePolicies(String sourceUri) {
		log.debug("removePolicies from {}", sourceUri);
		return true;
	}

	@Override
	public String getPolicyConsumerType() {
		return POLICY_CONSUMER_TYPE;
	}

}
