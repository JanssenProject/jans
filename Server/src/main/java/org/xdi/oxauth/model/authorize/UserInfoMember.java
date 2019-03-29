/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.authorize;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.util.Util;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
 */
public class UserInfoMember {

    private List<Claim> claims;
    private List<String> preferredLocales;

    public UserInfoMember(JSONObject jsonObject) throws JSONException {
        claims = new ArrayList<Claim>();

        for (Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ) {
            String claimName = iterator.next();
            ClaimValue claimValue = null;

            if (jsonObject.isNull(claimName)) {
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
                }
            }

            Claim claim = new Claim(claimName, claimValue);
            claims.add(claim);
        }

        preferredLocales = new ArrayList<String>();
        if (jsonObject.has("preferred_locales")) {
            JSONArray preferredLocalesJsonArray = jsonObject.getJSONArray("preferred_locales");

            for (int i = 0; i < preferredLocalesJsonArray.length(); i++) {
                preferredLocales.add(preferredLocalesJsonArray.getString(i));
            }
        }
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public List<String> getPreferredLocales() {
        return preferredLocales;
    }
}