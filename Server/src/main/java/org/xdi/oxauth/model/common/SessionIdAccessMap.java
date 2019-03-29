/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.Sets;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/06/2014
 */

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
