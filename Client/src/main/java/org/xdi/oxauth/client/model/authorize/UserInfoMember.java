package org.xdi.oxauth.client.model.authorize;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 03.07.2012
 */
public class UserInfoMember {

    private List<Claim> claims;
    private List<String> preferredLocales;

    public UserInfoMember() {
        claims = new ArrayList<Claim>();
        preferredLocales = new ArrayList<String>();
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    public List<String> getPreferredLocales() {
        return preferredLocales;
    }

    public void setPreferredLocales(List<String> preferredLocales) {
        this.preferredLocales = preferredLocales;
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
        if (preferredLocales != null && !preferredLocales.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (String locale : preferredLocales) {
                arr.put(locale);
            }

            obj.put("preferred_locales", arr);
        }

        return obj;
    }
}