package io.jans.casa.acctlinking;

import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.Map;

/**
 * Fields of this class can be referenced in the config properties of flow 
 * io.jans.casa.acctlinking.Launcher
 */
public final class Mappings {

    public static final UnaryOperator<Map<String, Object>> GOOGLE = 

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("ID", profile.get("sub"));
            map.put("mail", profile.get("email"));
            map.put("cn", profile.get("name"));
            map.put("sn", profile.get("family_name"));
            map.put("displayName", profile.get("given_name"));
            map.put("givenName", profile.get("given_name"));
            
            return map;
        };

    //See https://developers.facebook.com/docs/graph-api/reference/user
    public static final UnaryOperator<Map<String, Object>> FACEBOOK = 

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("ID", profile.get("id"));
            map.put("mail", profile.get("email"));
            map.put("cn", profile.get("name"));
            map.put("sn", profile.get("last_name"));
            map.put("displayName", profile.get("first_name"));
            map.put("givenName", profile.get("first_name"));
            
            return map;
        };

    public static final UnaryOperator<Map<String, Object>> APPLE =

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("ID", profile.get("sub"));
            map.put("mail", profile.get("email"));
            
            return map;
        };

    //See https://docs.github.com/en/rest/users/users
    public static final UnaryOperator<Map<String, Object>> GITHUB = 

        profile -> {
            Map<String, Object> map = new HashMap<>();

            map.put("ID", profile.getOrDefault("login", profile.get("id")));
            map.put("mail", profile.get("email"));
            map.put("displayName", profile.get("name"));
            map.put("givenName", profile.get("name"));

            return map;
        };
    
    private Mappings() { }

}
