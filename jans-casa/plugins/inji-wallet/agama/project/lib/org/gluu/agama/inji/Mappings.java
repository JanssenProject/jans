package org.gluu.agama.inji;

import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Mappings {

    public static final UnaryOperator<Map<String, Object>> VC = 

        profile -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("ID", profile.get("mail"));
            map.put("uid", profile.get("userId"));
            map.put("mail", profile.get("mail"));
            map.put("displayName", profile.get("displayName"));
            map.put("givenName", profile.get("givenName"));
            // map.put("cn", profile.get("displayName"));
            // map.put("sn", profile.get("displayName"));

            return map;
        };    
    
    private Mappings() { }

}