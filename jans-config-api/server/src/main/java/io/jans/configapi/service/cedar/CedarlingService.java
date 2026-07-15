package io.jans.configapi.service.cedar;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.container.ResourceInfo;
import io.jans.core.cedarling.service.CedarlingAuthorizationService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;


@ApplicationScoped
@Named
public class CedarlingService {

    @Inject
    Logger logger;

    @Inject 
    CedarlingAuthorizationService cedarlingAuthorizationService;
    

    public boolean authorize(String token, String issuer, ResourceInfo resourceInfo, String method,
            String path)  {
        
        Map<String, String> tokens = new HashMap<>();
        tokens.put("ACCESS_TOKEN", token);
 
                
        Map<String, Object> context = new HashMap<>();
        java.util.List<String> actions = java.util.Arrays.stream(resourceInfo.getResourceMethod().getAnnotations())
                .map(a -> a.annotationType().getSimpleName())
                .collect(Collectors.toList());
        
        Map<String, Object> resource = new HashMap<>();
        resource.put("url",  path);
        
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("method", method);
        mapping.put("action", actions);
        mapping.put("issuer", issuer);
        
        resource.put("cedar_entity_mapping", mapping);

       

        return cedarlingAuthorizationService.authorize(tokens, method, resource, context);
    }
}
