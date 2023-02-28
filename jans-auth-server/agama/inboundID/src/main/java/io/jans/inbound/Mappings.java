package io.jans.inbound;

import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.Map;

/**
 * Fields of this class can be referenced in the config properties of flow ExternalSiteLogin
 * (see the flow docs).
 */
public final class Mappings {

    public static final UnaryOperator<Map<String, Object>>

        GOOGLE = profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put(Attrs.UID, "google-" + profile.get("sub"));
            map.put(Attrs.MAIL, profile.get("email"));
            map.put(Attrs.CN, profile.get("name"));
            map.put(Attrs.SN, profile.get("family_name"));
            map.put(Attrs.DISPLAY_NAME, profile.get("given_name"));
            map.put(Attrs.GIVEN_NAME, profile.get("given_name"));
            
            return map;
        };

    public static final UnaryOperator<Map<String, Object>>
    //See https://developers.facebook.com/docs/graph-api/reference/user

        FACEBOOK = profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put(Attrs.UID, "facebook-" + profile.get("id"));
            map.put(Attrs.MAIL, profile.get("email"));
            map.put(Attrs.CN, profile.get("name"));
            map.put(Attrs.SN, profile.get("last_name"));
            map.put(Attrs.DISPLAY_NAME, profile.get("first_name"));
            map.put(Attrs.GIVEN_NAME, profile.get("first_name"));
            
            return map;
        };

    public static final UnaryOperator<Map<String, Object>>

        APPLE = profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put(Attrs.UID, "apple-" + profile.get("sub"));
            map.put(Attrs.MAIL, profile.get("email"));
            
            return map;
        };

    public static final UnaryOperator<Map<String, Object>>
    //See https://docs.github.com/en/rest/users/users

        GITHUB = profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put(Attrs.UID, "github-" + profile.getOrDefault("login", profile.get("id")));
            map.put(Attrs.MAIL, profile.get("email"));
            map.put(Attrs.DISPLAY_NAME, profile.get("name"));
            map.put(Attrs.GIVEN_NAME, profile.get("name"));
            
            return map;
        };

    private Mappings() { }

}
