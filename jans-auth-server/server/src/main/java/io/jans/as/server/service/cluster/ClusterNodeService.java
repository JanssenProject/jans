/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class ClusterNodeService {

	@Inject
	private Logger log;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private PersistenceEntryManager entryManager;

	/**
	 * returns ClusterNode by Dn
	 *
	 * @return ClusterNode
	 */
	public ClusterNode getClusterNodeByDn(String dn) {
		return entryManager.find(ClusterNode.class, dn);
	}

	/**
	 * returns ClusterNode by Id
	 *
	 * @return ClusterNode
	 */
	public ClusterNode getClusterNodeById(Integer id) {
		return entryManager.find(ClusterNode.class, getDnForClusterNode(id));
	}

	/**
	 * returns a list of all ClusterNodes
	 *
	 * @return list of ClusterNodes
	 */
	public List<ClusterNode> getAllClusterNodes() {
		String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNodes();

		return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, Filter.createPresenceFilter("jansNum"));
	}

	public List<String> getClusterNodesDns(List<Integer> nodeIds) {
		List<String> clusterNodesDns = new ArrayList<>();

		for (Integer nodeId : nodeIds) {
			ClusterNode clusterNode = getClusterNodeById(nodeId);
			if (clusterNode != null) {
				clusterNodesDns.add(clusterNode.getDn());
			}
		}

		return clusterNodesDns;
	}

	public void persist(ClusterNode clusterNode) {
		entryManager.persist(clusterNode);
	}

	public void update(ClusterNode clusterNode) {
		entryManager.merge(clusterNode);
	}

	public String getDnForClusterNode(Integer id) {
		return String.format("jansNum=%d,%s", id, staticConfiguration.getBaseDn().getNodes());
	}

}