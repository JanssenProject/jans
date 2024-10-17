package io.jans.casa.acctlinking;

import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fields of this class can be referenced in the config properties of flow 
 * io.jans.casa.authn.acctlinking
 */
public final class Mappings {

    public static final UnaryOperator<Map<String, Object>> GOOGLE = 

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            String sub = profile.get("sub");
            String mail = profile.get("email");

            map.put("ID", Optional.ofNullable(mail).orElse(sub));
            map.put("mail", mail);
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
            
            String id = profile.get("id");
            String mail = profile.get("email");

            map.put("ID", Optional.ofNullable(mail).orElse(id));
            map.put("mail", mail);
            map.put("cn", profile.get("name"));
            map.put("sn", profile.get("last_name"));
            map.put("displayName", profile.get("first_name"));
            map.put("givenName", profile.get("first_name"));
            
            return map;
        };

    public static final UnaryOperator<Map<String, Object>> APPLE =

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            String sub = profile.get("sub");
            String mail = profile.get("email");

            map.put("ID", Optional.ofNullable(mail).orElse(sub));
            map.put("mail", mail);
            
            return map;
        };

    //See https://docs.github.com/en/rest/users/users
    public static final UnaryOperator<Map<String, Object>> GITHUB = 

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            String handle = profile.get("login");

            map.put("ID", Optional.ofNullable(handle).orElse(profile.get("id")));
            map.put("mail", profile.get("email"));
            map.put("displayName", profile.get("name"));
            map.put("givenName", profile.get("name"));

            return map;
        };
    
    private Mappings() { }

}
