/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

import com.google.common.collect.Lists;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.model.util.Base64Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Javier Rojas Blum
 * @version January 3, 2018
 */
public abstract class JwtClaimSet {

    private final Map<String, Object> claims;

    protected JwtClaimSet() {
        claims = new LinkedHashMap<>();
    }

    protected JwtClaimSet(JSONObject jsonObject) {
        this();
        load(jsonObject);
    }

    protected JwtClaimSet(String base64JsonObject) throws InvalidJwtException {
        this();
        load(base64JsonObject);
    }

    public Set<String> keys() {
        return claims.keySet();
    }

    public Object getClaim(String key) {
        return claims.get(key);
    }

    public boolean hasClaim(String key) {
        return getClaim(key) != null;
    }

    public String getClaimAsString(String key) {
        Object claim = getClaim(key);

        if (claim != null) {
            return claim.toString();
        } else {
            return null;
        }
    }

    public JSONObject getClaimAsJSON(String key) {
        String claim = getClaimAsString(key);

        try {
            if (claim != null) {
                JSONObject json = null;
                json = new JSONObject(claim);
                return json;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String> getClaimAsStringList(String key) {
        List<String> list = new ArrayList<>();
        Object values = getClaim(key);

        try {
            if (values instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) values;
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.getString(i));
                }
            } else {
                String claim = getClaimAsString(key);
                if (claim != null) {
                    list.add(claim);
                }
            }
        } catch (JSONException e) {
            // ignore
        }

        return list;
    }

    public Date getClaimAsDate(String key) {
        Object claim = getClaim(key);

        if (claim != null) {
            if (claim instanceof Date) {
                return (Date) claim;
            } else if (claim instanceof Integer) {
                final long c = (Integer) claim;
                final long date = c * 1000;
                return new Date(date);
            } else if (claim instanceof Long) {
                return new Date((Long) claim * 1000);
            } else if (claim instanceof Double) {
                final double c = (Double) claim;
                final BigDecimal bigDecimal = BigDecimal.valueOf(c);

                long claimLong = bigDecimal.longValue();
                claimLong = claimLong * 1000;

                return new Date(claimLong);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Integer getClaimAsInteger(String key) {
        Object claim = getClaim(key);

        if (claim != null) {
            if (claim instanceof Integer) {
                return (Integer) claim;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Long getClaimAsLong(String key) {
        Object claim = getClaim(key);

        if (claim != null) {
            if (claim instanceof Long) {
                return (Long) claim;
            } else if (claim instanceof Integer) {
                return Long.valueOf((Integer) claim);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Character getClaimAsCharacter(String key) {
        Object claim = getClaim(key);

        if (claim != null) {
            if (claim instanceof Character) {
                return (Character) claim;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void setClaimObject(String key, Object value, boolean overrideValue) {
        if (value == null) {
            setNullClaim(key);
        } else if (value instanceof String) {
            setClaimString(key, value, overrideValue);
        } else if (value instanceof Date) {
            setClaim(key, (Date) value);
        } else if (value instanceof Boolean) {
            setClaim(key, (Boolean) value);
        } else if (value instanceof Integer) {
            setClaim(key, (Integer) value);
        } else if (value instanceof Long) {
            setClaim(key, (Long) value);
        } else if (value instanceof Character) {
            setClaim(key, (Character) value);
        } else if (value instanceof List) {
            setClaim(key, (List) value);
        } else if (value instanceof JwtSubClaimObject) {
            setClaim(key, (JwtSubClaimObject) value);
        } else if (value instanceof JSONObject) {
            setClaim(key, (JSONObject) value);
        } else if (value instanceof JSONArray) {
            setClaim(key, (JSONArray) value);
        } else {
            throw new UnsupportedOperationException("Claim value is not supported, key: " + key + ", value :" + value);
        }
    }

    private void setClaimString(String key, Object value, boolean overrideValue) {
        if (overrideValue) {
            setClaim(key, (String) value);
            return;
        }

        Object currentValue = getClaim(key);
        String valueAsString = (String) value;

        if (currentValue instanceof String) {
            if (!currentValue.equals(value)) {
                setClaim(key, Lists.newArrayList(currentValue.toString(), valueAsString));
            } else {
                setClaim(key, (String) value);
            }
        } else if (currentValue instanceof List) {
            List<String> currentValueAsList = (List) currentValue;
            if (!currentValueAsList.contains(valueAsString)) {
                currentValueAsList.add(valueAsString);
            }
        }
    }

    public void setNullClaim(String key) {
        claims.put(key, null);
    }

    public void setClaim(String key, String value) {
        claims.put(key, value);
    }

    public void setClaim(String key, Date value) {
        claims.put(key, value);
    }

    public void setClaim(String key, Boolean value) {
        claims.put(key, value);
    }

    public void setClaim(String key, Integer value) {
        claims.put(key, value);
    }

    public void setClaim(String key, Long value) {
        claims.put(key, value);
    }

    public void setClaim(String key, Character value) {
        claims.put(key, value);
    }

    public void setClaim(String key, List<?> values) {
        claims.put(key, values);
    }

    public void setClaim(String key, JwtSubClaimObject subClaimObject) {
        claims.put(key, subClaimObject);
    }

    public void setClaim(String key, JSONObject values) {
        claims.put(key, values);
    }

    public void setClaim(String key, JSONArray values) {
        claims.put(key, values);
    }

    public void setClaimFromJsonObject(String key, Object attribute) {
        if (attribute == null) {
            return;
        }

        if (attribute instanceof JSONArray) {
            claims.put(key, JsonApplier.getStringList((JSONArray) attribute));
        } else {
            String value = (String) attribute;
            claims.put(key, value);
        }
    }

    public void removeClaim(String key) {
        claims.remove(key);
    }

    public JSONObject toJsonObject() throws InvalidJwtException {
        JSONObject jsonObject = new JSONObject();

        try {
            for (Map.Entry<String, Object> claim : claims.entrySet()) {
                if (claim.getValue() instanceof Date) {
                    Date date = (Date) claim.getValue();
                    jsonObject.put(claim.getKey(), date.getTime() / 1000);
                } else if (claim.getValue() instanceof JwtSubClaimObject) {
                    JwtSubClaimObject subClaimObject = (JwtSubClaimObject) claim.getValue();
                    jsonObject.put(subClaimObject.getName(), subClaimObject.toJsonObject());
                } else if (claim.getValue() instanceof List) {
                    List<?> claimObjectList = (List<?>) claim.getValue();
                    JSONArray claimsJSONArray = new JSONArray();
                    for (Object claimObj : claimObjectList) {
                        claimsJSONArray.put(claimObj);
                    }
                    jsonObject.put(claim.getKey(), claimsJSONArray);
                } else {
                    jsonObject.put(claim.getKey(), claim.getValue());
                }
            }
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }

        return jsonObject;
    }

    public String toBase64JsonObject() throws InvalidJwtException {
        String jsonObjectString = toJsonString();
        byte[] jsonObjectBytes = jsonObjectString.getBytes(StandardCharsets.UTF_8);
        return Base64Util.base64urlencode(jsonObjectBytes);
    }

    public String toJsonString() throws InvalidJwtException {
        JSONObject jsonObject = toJsonObject();
        String jsonObjectString = jsonObject.toString();
        jsonObjectString = jsonObjectString.replace("\\/", "/");

        return jsonObjectString;
    }

    public Map<String, List<String>> toMap() throws InvalidJwtException {
        Map<String, List<String>> map = new HashMap<>();

        try {
            for (Map.Entry<String, Object> claim : claims.entrySet()) {
                String key = claim.getKey();
                Object value = claim.getValue();

                List<String> values = new ArrayList<>();
                if (value instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) value;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        values.add(jsonArray.getString(i));
                    }
                } else if (value != null) {
                    values.add(value.toString());
                }

                map.put(key, values);
            }
        } catch (JSONException e) {
            throw new InvalidJwtException(e);
        }

        return map;
    }

    public void load(JSONObject jsonObject) {
        claims.clear();

        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            Object value = jsonObject.opt(key);

            claims.put(key, value);
        }
    }

    public void load(String base64JsonObject) throws InvalidJwtException {
        try {
            String jsonObjectString = new String(Base64Util.base64urldecode(base64JsonObject), StandardCharsets.UTF_8);
            load(new JSONObject(jsonObjectString));
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }
    }
}