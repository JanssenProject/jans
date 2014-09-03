package org.xdi.oxauth.model.jwt;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

/**
 * @author Javier Rojas Blum Date: 11.09.2012
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

    public Object getClaim(String key) {
        return claims.get(key);
    }

    public String getClaimAsString(String key) {
        Object claim = getClaim(key);

        if (claim != null) {
            return claim.toString();
        } else {
            return null;
        }
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

    public void setClaim(String key, List<String> values) {
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
            JSONObject jsonObject = toJsonObject();
            String jsonObjectString = jsonObject.toString();
            jsonObjectString = jsonObjectString.replace("\\/", "/");
            byte[] jsonObjectBytes = jsonObjectString.getBytes(Util.UTF8_STRING_ENCODING);
            return JwtUtil.base64urlencode(jsonObjectBytes);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
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
            String jsonObjectString = new String(JwtUtil.base64urldecode(base64JsonObject), Util.UTF8_STRING_ENCODING);
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