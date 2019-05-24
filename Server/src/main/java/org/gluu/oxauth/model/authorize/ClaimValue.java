/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
 */
public class ClaimValue {

    private ClaimValueType claimValueType;
    private List<String> values;
    private String value;

    private ClaimValue() {
    }

    public static ClaimValue createNull() {
        ClaimValue claimValue = new ClaimValue();
        claimValue.claimValueType = ClaimValueType.NULL;

        return claimValue;
    }

    public static ClaimValue createEssential(boolean essentialValue) {
        ClaimValue claimValue = new ClaimValue();
        if (essentialValue) {
            claimValue.claimValueType = ClaimValueType.ESSENTIAL_TRUE;
        } else {
            claimValue.claimValueType = ClaimValueType.ESSENTIAL_FALSE;
        }
        return claimValue;
    }

    public static ClaimValue createValueList(List<String> values) {
        ClaimValue claimValue = new ClaimValue();
        claimValue.claimValueType = ClaimValueType.VALUE_LIST;

        claimValue.values = new ArrayList<String>(values);

        return claimValue;
    }

    public static ClaimValue createSingleValue(String value) {
        ClaimValue claimValue = new ClaimValue();
        claimValue.claimValueType = ClaimValueType.SINGLE_VALUE;

        claimValue.value = value;

        return claimValue;
    }

    public ClaimValueType getClaimValueType() {
        return claimValueType;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue() {
        return value;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = null;

        switch (claimValueType) {
            case NULL:
                break;
            case ESSENTIAL_TRUE:
                obj = new JSONObject();
                obj.put("essential", true);
                break;
            case ESSENTIAL_FALSE:
                obj = new JSONObject();
                obj.put("essential", false);
                break;
            case VALUE_LIST:
                JSONArray arr = new JSONArray();
                for (String value : values) {
                    arr.put(value);
                }
                obj = new JSONObject();
                obj.put("values", arr);
                break;
            case SINGLE_VALUE:
                obj = new JSONObject();
                obj.put("value", value);
                break;
        }

        return obj;
    }
}