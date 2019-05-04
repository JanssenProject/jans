/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;


/**
 *
 * @author Shekhar L.
 * @version 09/16/2017
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebKeysSettings {
    @JsonProperty("server_ip")
    private String serverIP;

    @JsonProperty("update_at")
    private Date updateAt;

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public Date getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serverIP == null) ? 0 : serverIP.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WebKeysSettings other = (WebKeysSettings) obj;
        if (serverIP == null) {
            if (other.serverIP != null) {
                return false;
            }
        } else if (!serverIP.equals(other.serverIP)) {
            return false;
        }

        return true;
    }

}
