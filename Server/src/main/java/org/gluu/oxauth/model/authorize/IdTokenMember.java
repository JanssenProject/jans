/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.util.Util;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
 */
public class IdTokenMember {

    private List<Claim> claims;
    private Integer maxAge;

    public IdTokenMember(JSONObject jsonObject) throws JSONException {
        claims = new ArrayList<Claim>();

        for (Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ) {
            String claimName = iterator.next();
            ClaimValue claimValue = null;

            if (claimName != null && claimName.equals("max_age") && jsonObject.has("max_age")) {
                maxAge = jsonObject.getInt("max_age");
            } else if (jsonObject.isNull(claimName)) {
                claimValue = ClaimValue.createNull();
            } else {
                JSONObject claimValueJsonObject = jsonObject.getJSONObject(claimName);
                if (claimValueJsonObject.has("essential")) {
                    boolean essential = claimValueJsonObject.getBoolean("essential");
                    claimValue = ClaimValue.createEssential(essential);
                } else if (claimValueJsonObject.has("values")) {
                    JSONArray claimValueJsonArray = claimValueJsonObject.getJSONArray("values");
                    List<String> claimValueArr = Util.asList(claimValueJsonArray);
                    claimValue = ClaimValue.createValueList(claimValueArr);
                } else if (claimValueJsonObject.has("value")) {
                    String value = claimValueJsonObject.getString("value");
                    claimValue = ClaimValue.createSingleValue(value);
                }
            }

            Claim claim = new Claim(claimName, claimValue);
            claims.add(claim);
        }
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public Claim getClaim(String claimName) {
        if (StringUtils.isNotBlank(claimName)) {
            for (Claim claim : claims) {
                if (claimName.equals(claim.getName())) {
                    return claim;
                }
            }
        }

        return null;
    }
}