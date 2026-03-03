/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;


import java.util.UUID;

import io.jans.as.model.config.StaticConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class ClusterNodeService extends io.jans.service.cluster.ClusterNodeService {

	public static final String CLUSTER_TYPE_JANS_AUTH = "jans-auth";

    public static final String LOCK_KEY = UUID.randomUUID().toString();

    @Inject
    private StaticConfiguration staticConfiguration;

	@Override
    public String getClusterNodeType() {
    	return CLUSTER_TYPE_JANS_AUTH;
    }

	@Override
	public String getLockKey() {
		return LOCK_KEY;
	}

	@Override
    public String getBaseClusterNodeDn() {
    	return staticConfiguration.getBaseDn().getNode();
    }

}
