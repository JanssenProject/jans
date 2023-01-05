/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import com.google.common.collect.Sets;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@XmlRootElement
public class SessionIdAccessMap implements Serializable {

    @XmlElement(name = "map")
    private Map<String, Boolean> permissionGranted;

    public SessionIdAccessMap() {
    }

    public SessionIdAccessMap(Map<String, Boolean> permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    public Map<String, Boolean> getPermissionGranted() {
        return permissionGranted;
    }

    public void setPermissionGranted(Map<String, Boolean> permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    @XmlTransient
    public Set<String> clientIds() {
        return Sets.newHashSet(permissionGranted.keySet());
    }

    public Set<String> getClientIds(boolean granted) {
        Set<String> clientIds = Sets.newHashSet();
        for (Map.Entry<String, Boolean> entry : permissionGranted.entrySet()) {
            if (entry.getValue().equals(granted) ) {
                clientIds.add(entry.getKey());
            }
        }
        return clientIds;
    }

    public Boolean get(String clientId) {
        ensureInitialized();
        final Boolean result = permissionGranted.get(clientId);
        return result != null ? result : false;
    }

    private void ensureInitialized() {
        if (permissionGranted == null) {
            permissionGranted = new HashMap<String, Boolean>();
        }
    }

    public void put(String clientId, Boolean granted) {
        ensureInitialized();
        permissionGranted.put(clientId, granted);
    }

    public void putIfAbsent(String clientId) {
        ensureInitialized();
        if (permissionGranted.get(clientId) == null) {
            permissionGranted.put(clientId, false);
        }
    }
}