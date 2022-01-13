/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.authorize;

import io.jans.as.model.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
 */
public class UserInfoMember {

    private final List<Claim> claims;
    private final List<String> preferredLocales;

    public UserInfoMember(JSONObject jsonObject) throws JSONException {
        claims = new ArrayList<>();

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