package org.xdi.oxd.rp.client;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class HrefDetails implements Serializable {

    private String code;
    private String accessToken;
    private String idToken;
    private Map<String, List<String>> claims = Maps.newHashMap();

    public HrefDetails() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
