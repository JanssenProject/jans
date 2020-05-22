package org.gluu.oxauth.model.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Milton BO
 */
public class CIBACacheAuthReqIds implements Serializable {

    public static final String CACHE_KEY = "ciba_authentication_req_ids";

    private Set<String> authReqIds = new HashSet<>();

    public CIBACacheAuthReqIds() {
    }

    public Set<String> getAuthReqIds() {
        return authReqIds;
    }

    public void setAuthReqIds(Set<String> authReqIds) {
        this.authReqIds = authReqIds;
    }

    @Override
    public String toString() {
        return "CIBACacheAuthReqIds{" +
                "authReqIds=" + authReqIds +
                '}';
    }

}
