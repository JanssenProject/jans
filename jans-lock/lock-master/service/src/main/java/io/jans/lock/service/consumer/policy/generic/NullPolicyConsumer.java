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

package io.jans.lock.service.consumer.policy.generic;

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
public class NullPolicyConsumer extends PolicyConsumer {
	
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
		log.debug("Destroy Policies");
	}

}
