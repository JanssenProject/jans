/*
 * Copyright [2026] [Janssen Project]
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

package io.jans.lock.server.service;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import io.jans.lock.model.config.StaticConfiguration;
import io.jans.service.cluster.ClusterLockService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cluster claims (entries of {@code jansClusterLock}) of the Lock server. Lock has no cluster node
 * service, so the lock key is a random UUID per JVM.
 */
@ApplicationScoped
public class LockClusterLockService extends ClusterLockService {

	/** Same value as {@code baseDn.node} of the other Janssen servers. */
	public static final String DEFAULT_NODE_DN = "ou=node,o=jans";

	private static final String CONFIGURATION_RDN_PREFIX = "ou=configuration,";

	private static final String NODE_RDN_PREFIX = "ou=node,";

	private final String lockKey = UUID.randomUUID().toString();

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private MetricService metricService;

	@Override
	protected io.jans.orm.PersistenceEntryManager getEntryManager() {
		return metricService.getEntryManager();
	}

	/**
	 * The Lock static configuration has no {@code node} entry. The node branch sits under the same root
	 * as the configuration branch, so it is derived from {@code baseDn.configuration}.
	 */
	@Override
	public String getBaseDn() {
		String configurationDn = staticConfiguration.getBaseDn().getConfiguration();
		if (StringUtils.startsWith(configurationDn, CONFIGURATION_RDN_PREFIX)) {
			return NODE_RDN_PREFIX + configurationDn.substring(CONFIGURATION_RDN_PREFIX.length());
		}

		return DEFAULT_NODE_DN;
	}

	@Override
	public String getLockKey() {
		return lockKey;
	}

	@Override
	public String getNodeIdentifier() {
		return metricService.getNodeIdentifier();
	}
}
