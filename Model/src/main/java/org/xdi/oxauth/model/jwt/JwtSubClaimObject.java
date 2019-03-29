package org.xdi.oxauth.model.jwt;

import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version Jun 10, 2015
 */
public class JwtSubClaimObject extends JwtClaimSet {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static JwtSubClaimObject fromMap(Map<String, String> map) {
        JwtSubClaimObject result = new JwtSubClaimObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.setClaim(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static JwtSubClaimObject fromBooleanMap(Map<String, Boolean> map) {
        JwtSubClaimObject result = new JwtSubClaimObject();
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            result.setClaim(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
