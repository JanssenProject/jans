package org.xdi.oxauth.uma.authorization;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.jwt.Jwt;

/**
 * @author yuriyz on 06/02/2017.
 */
public class Claims {

    private Jwt claimsToken;
    private String claimsTokenAsString;
    private UmaPCT pct;
    private Map<String, Object> claims = new ConcurrentHashMap<String, Object>();

    public Claims(Jwt claimsToken, UmaPCT pct, String claimsTokenAsString) {
        this.claimsToken = claimsToken;
        this.pct = pct;
        this.claimsTokenAsString = claimsTokenAsString;
    }

    public String getClaimsTokenAsString() {
        return claimsTokenAsString;
    }

    public Set<String> keys() {
        return claims.keySet();
    }

    public Object get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }

        if (claims.containsKey(key)) {
            return claims.get(key);
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
        claims.put(key, value);
    }

    public void removeClaim(String key) {
        claims.remove(key);
    }
}
