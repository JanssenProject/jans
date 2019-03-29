/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwt;

import com.google.common.collect.Lists;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author Javier Rojas Blum
 * @version January 3, 2018
 */
public abstract class JwtClaimSet {

    private Map<String, Object> claims;

    public JwtClaimSet() {
        claims = new LinkedHashMap<String, Object>();
    }

    public JwtClaimSet(JSONObject jsonObject) {
        this();
        load(jsonObject);
    }

    public JwtClaimSet(String base64JsonObject) throws InvalidJwtException {
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
        List<String> list = new ArrayList<String>();
        Object claims = getClaim(key);

        try {
            if (claims != null && claims instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) claims;
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
                final BigDecimal bigDecimal = new BigDecimal(c);

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
            if (overrideValue) {
                setClaim(key, (String) value);
            } else {
                Object currentValue = getClaim(key);
                if (currentValue != null) {
                    setClaim(key, Lists.newArrayList(currentValue.toString(), (String) value));
                } else {
                    setClaim(key, (String) value);
                }
            }
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
        } else {
            throw new UnsupportedOperationException("Claim value is not supported, key: " + key + ", value :" + value);
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

    public void setClaim(String key, List values) {
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
                    List claimObjectList = (List) claim.getValue();
                    JSONArray claimsJSONArray = new JSONArray();
                    for (Object claimObj : claimObjectList) {
                        claimsJSONArray.put(claimObj);
                    }
                    jsonObject.put(claim.getKey(), claimsJSONArray);
                } else {
                    jsonObject.put(claim.getKey(), claim.getValue());
                }
            }
        } catch (JSONException e) {
            throw new InvalidJwtException(e);
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }

        return jsonObject;
    }

    public String toBase64JsonObject() throws InvalidJwtException {
        try {
            String jsonObjectString = toJsonString();
            byte[] jsonObjectBytes = jsonObjectString.getBytes(Util.UTF8_STRING_ENCODING);
            return Base64Util.base64urlencode(jsonObjectBytes);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String toJsonString() throws InvalidJwtException {
        JSONObject jsonObject = toJsonObject();
        String jsonObjectString = jsonObject.toString();
        jsonObjectString = jsonObjectString.replace("\\/", "/");

        return jsonObjectString;
    }

    public Map<String, List<String>> toMap() throws InvalidJwtException {
        Map<String, List<String>> map = new HashMap<String, java.util.List<String>>();

        try {
            for (Map.Entry<String, Object> claim : claims.entrySet()) {
                String key = claim.getKey();
                Object value = claim.getValue();

                List<String> values = new ArrayList<String>();
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
            String jsonObjectString = new String(Base64Util.base64urldecode(base64JsonObject), Util.UTF8_STRING_ENCODING);
            load(new JSONObject(jsonObjectString));
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJwtException(e);
        } catch (JSONException e) {
            throw new InvalidJwtException(e);
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }
    }
}