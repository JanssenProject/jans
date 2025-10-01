package io.jans.casa.acctlinking;

import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;

import java.util.*;

public class ProvidersCacher {
    
    public static void store(String key, int seconds, Map<String, Object> providers) {
        
        CacheService cs = CdiUtil.bean(CacheService.class);
        
        if (cs.get(key) == null) {
            List<Map<String, String>> prs = new ArrayList<>();
            
            for (String id : providers.keySet()) {
                
                Map<String, Object> map = (Map<String, Object>) providers.get(id);
                Map<String, String> mom = new HashMap<>();

                String enabled = propertyFrom(map, "enabled");
                if (enabled == null || Boolean.parseBoolean(enabled)) {
                    mom.put("id", id);
                    mom.put("displayName", propertyFrom(map, "displayName"));
                    mom.put("logoImg", propertyFrom(map, "logoImg"));
                    prs.add(mom);
                }
            }
            
            //one min expiration
            cs.put(seconds, key, prs);
        }
        
    }
    
    private static String propertyFrom(Map<String, Object> map, String name) {
        return Optional.ofNullable(map.get(name)).map(Object::toString).orElse(null);
    }
    
}
