/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import io.jans.as.model.jwt.Jwt;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yuriyz on 06/02/2017.
 */
public class Claims {

    private final Jwt claimsToken;
    private final String claimsTokenAsString;
    private final UmaPCT pct;
    private final Map<String, Object> claimMap = new ConcurrentHashMap<>();

    public Claims(Jwt claimsToken, UmaPCT pct, String claimsTokenAsString) {
        this.claimsToken = claimsToken;
        this.pct = pct;
        this.claimsTokenAsString = claimsTokenAsString;
    }

    public String getClaimsTokenAsString() {
        return claimsTokenAsString;
    }

    public Set<String> keys() {
        return claimMap.keySet();
    }

    public Object get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }

        if (claimMap.containsKey(key)) {
            return claimMap.get(key);
        } else if (claimsToken != null && claimsToken.getClaims() != null && claimsToken.getClaims().hasClaim(key)) {
            return claimsToken.getClaims().getClaim(key);
        } else if (pct != null && pct.getClaims() != null && pct.getClaims().hasClaim(key)) {
            return pct.getClaims().getClaim(key);
        }
        return null;
    }

    public Object getClaimTokenClaim(String key) {
        if (claimsToken != null && claimsToken.getClaims() != null && claimsToken.getClaims().hasClaim(key)) {
            return claimsToken.getClaims().getClaim(key);
        }
        return null;
    }

    public Object getPctClaim(String key) {
        if (pct != null && pct.getClaims() != null && pct.getClaims().hasClaim(key)) {
            return pct.getClaims().getClaim(key);
        }
        return null;
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    public void put(String key, Object value) {
        claimMap.put(key, value);
    }

    public void removeClaim(String key) {
        claimMap.remove(key);
    }
}
