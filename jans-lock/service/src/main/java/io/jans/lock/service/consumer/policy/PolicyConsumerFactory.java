/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.consumer.policy;

import org.slf4j.Logger;

import io.jans.service.policy.consumer.PolicyConsumer;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Message consumer factory
 *
 * @author Yuriy Movchan Date: 12/20/2023
 */
@ApplicationScoped
public class PolicyConsumerFactory {

	@Inject
	private Logger log;

	@Inject
	@Any
	private Instance<PolicyConsumer> policyConsumerProviderInstances;

	public PolicyConsumer getMessageConsumer(String policyConsumerType) {
		for (PolicyConsumer policyConsumerProvider : policyConsumerProviderInstances) {
			String serviceMessageConsumerType = policyConsumerProvider.getPolicyConsumerType();
			if (StringHelper.equalsIgnoreCase(serviceMessageConsumerType, policyConsumerType)) {
				return policyConsumerProvider;
			}
		}
		
		log.error("Failed to find policy consumer with type '{}'. Using null policy consumer", policyConsumerType);
		return policyConsumerProviderInstances.select(NullPolicyConsumer.class).get();
	}

}
