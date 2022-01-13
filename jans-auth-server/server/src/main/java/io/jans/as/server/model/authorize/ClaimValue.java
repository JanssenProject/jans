/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.authorize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
 */
public class ClaimValue {

    private ClaimValueType claimValueType;
    private List<String> values;
    private String value;
    private Boolean essential;

    private ClaimValue() {
    }

    public static ClaimValue createNull() {
        ClaimValue claimValue = new ClaimValue();
        claimValue.claimValueType = ClaimValueType.NULL;

        return claimValue;
    }

    public static ClaimValue createEssential(boolean essentialValue) {
        ClaimValue claimValue = new ClaimValue();
        claimValue.setEssential(essentialValue);

        // todo do we need code below ?
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

    public Boolean getEssential() {
        return essential;
    }

    public void setEssential(Boolean essential) {
        this.essential = essential;
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

    public String getValueAsString() {
        if (values != null && !values.isEmpty()) {
            return String.join(" ", values);
        }
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