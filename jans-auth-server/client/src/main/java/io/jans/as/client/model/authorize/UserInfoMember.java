/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.model.authorize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            for (Claim claim : claims) {
                JSONObject claimValue = claim.getClaimValue().toJSONObject();
                if (claimValue == null) {
                    obj.put(claim.getName(), JSONObject.NULL);
                } else {
                    obj.put(claim.getName(), claimValue);
                }
            }
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