package org.xdi.oxauth.client.model.authorize;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 03.07.2012
 */
public class IdTokenMember {

    private List<Claim> claims;
    private Integer maxAge;

    public IdTokenMember() {
        claims = new ArrayList<Claim>();
        maxAge = null;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();

        if (claims != null && !claims.isEmpty()) {
            JSONObject claimsObj = new JSONObject();

            for (Claim claim : claims) {
                JSONObject claimValue = claim.getClaimValue().toJSONObject();
                if (claimValue == null) {
                    claimsObj.put(claim.getName(), JSONObject.NULL);
                } else {
                    claimsObj.put(claim.getName(), claimValue);
                }
            }

            obj.put("claims", claimsObj);
        }
        if (maxAge != null) {
            obj.put("max_age", maxAge);
        }

        return obj;
    }
}