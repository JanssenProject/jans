/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author yuriyz on 02/02/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemcachedConfiguration implements Serializable {

    private static final long serialVersionUID = 3380170170265842427L;

    private String servers; // server1:11211 server2:11211

    private int maxOperationQueueLength = 99999999;

    private int bufferSize = 32768;

    private int defaultPutExpiration = 60; // in seconds

    private MemcachedConnectionFactoryType connectionFactoryType = MemcachedConnectionFactoryType.DEFAULT;

    public int getDefaultPutExpiration() {
        return defaultPutExpiration;
    }

    public void setDefaultPutExpiration(int defaultPutExpiration) {
        this.defaultPutExpiration = defaultPutExpiration;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public int getMaxOperationQueueLength() {
        return maxOperationQueueLength;
    }

    public void setMaxOperationQueueLength(int maxOperationQueueLength) {
        this.maxOperationQueueLength = maxOperationQueueLength;
    }

    public MemcachedConnectionFactoryType getConnectionFactoryType() {
        return connectionFactoryType;
    }

    public void setConnectionFactoryType(MemcachedConnectionFactoryType connectionFactoryType) {
        this.connectionFactoryType = connectionFactoryType;
    }

    @Override
    public String toString() {
        return "MemcachedConfiguration{" + "servers='" + servers + '\'' + ", maxOperationQueueLength=" + maxOperationQueueLength + ", bufferSize="
                + bufferSize + ", defaultPutExpiration=" + defaultPutExpiration + ", connectionFactoryType=" + connectionFactoryType + '}';
    }
}
