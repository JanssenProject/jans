package org.gluu.oxauth.model.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Milton BO
 */
public class CIBACacheAuthReqIds implements Serializable {

    public static final String CACHE_KEY = "ciba_authentication_req_ids";

    private Map<String, Long> authReqIds = new HashMap<>();

    public CIBACacheAuthReqIds() {
    }

    public Map<String, Long> getAuthReqIds() {
        return authReqIds;
    }

    public void setAuthReqIds(Map<String, Long> authReqIds) {
        this.authReqIds = authReqIds;
    }

    @Override
    public String toString() {
        return "CIBACacheAuthReqIds{" +
                "authReqIds=" + authReqIds +
                '}';
    }

}
