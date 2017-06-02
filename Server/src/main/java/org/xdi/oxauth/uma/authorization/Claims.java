package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.model.jwt.Jwt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yuriyz on 06/02/2017.
 */
public class Claims {

    private Jwt claimsToken;
    private UmaPCT pct;
    private Map<String, Object> claims = new ConcurrentHashMap<String, Object>();

    public Claims(Jwt claimsToken, UmaPCT pct) {
        this.claimsToken = claimsToken;
        this.pct = pct;
    }

    public Object get(String claimName) {
        if (claims.containsKey(claimName)) {
            return claims.get(claimName);
        } else if (claimsToken != null && claimsToken.getClaims() != null && claimsToken.getClaims().hasClaim(claimName)) {
            return claimsToken.getClaims().getClaim(claimName);
        } else if (pct != null && pct.getClaims() != null && pct.getClaims().hasClaim(claimName)) {
            return claimsToken.getClaims().getClaim(claimName);
        }
        return null;
    }
}
