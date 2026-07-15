package io.jans.configapi.service.cedar;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.container.ResourceInfo;
import io.jans.core.cedarling.service.CedarlingAuthorizationService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
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
        Map<String, Object> resource = new HashMap<>();
        resource.put("url",  path);
                
        Map<String, Object> context = new HashMap<>();
        context.put("cedar_entity_mapping",Map.of("method", method, "action", resourceInfo.getResourceMethod().getAnnotations() ,"issuer", issuer));
       

        return cedarlingAuthorizationService.authorize(tokens,method, resource, context);
    }
}
