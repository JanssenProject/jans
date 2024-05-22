package io.jans.lock.service.provider.metric;

import java.util.List;

import org.slf4j.Logger;

import io.jans.lock.service.consumer.policy.PolicyConsumer;
import io.jans.service.cdi.qualifier.Implementation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Null policy consumer
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@Implementation
@ApplicationScoped
public class NullMessageProvider extends PolicyConsumer {
	
	public static String POLICY_CONSUMER_TYPE = "DISABLED";

	@Inject
	private Logger log;

	@Inject
	@Implementation
	private Instance<PolicyConsumer> policyConsumerProviderInstance;

	@Override
	public boolean putPolicies(String sourceUri, List<String> policies) {
		log.debug("PutPolicies from {}, count {}", sourceUri, policies.size());
		return true;
	}

	@Override
	public boolean removePolicies(String sourceUri) {
		log.debug("RemovePolicies from {}", sourceUri);
		return true;
	}

	@Override
	public String getPolicyConsumerType() {
		return POLICY_CONSUMER_TYPE;
	}

	@Override
	public void destroy() {
		log.debug("Destory Policies");
	}

}
