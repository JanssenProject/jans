package io.jans.scim.auth.oauth;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.scim.auth.IProtectionService;
import io.jans.scim.service.filter.ProtectedApi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@ApplicationScoped
public class DefaultOAuthProtectionService extends BaseOAuthProtectionService {

    @Inject
    private Logger log;
    
    public Response processIntrospectionResponse(IntrospectionResponse iresponse,
            ResourceInfo resourceInfo) {
        
        Response response = null;
        List<String> scopes = getRequestedScopes(resourceInfo);
        log.info("Call requires scopes: {}", scopes);	
        List<String> tokenScopes = Optional.ofNullable(iresponse).map(IntrospectionResponse::getScope)
                .orElse(null);

        if (tokenScopes == null || !iresponse.isActive() || !tokenScopes.containsAll(scopes)) {
            String msg = "Invalid token or insufficient scopes";
            log.error("{}. Token scopes: {}", msg, tokenScopes);
            //see section 3.12 RFC 7644
            response = IProtectionService.simpleResponse(Response.Status.FORBIDDEN, msg);
        }
        return response;

    }

    private List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        
        List<String> scopes = new ArrayList<>();
        scopes.addAll(getScopesFromAnnotation(resourceInfo.getResourceClass()));
        scopes.addAll(getScopesFromAnnotation(resourceInfo.getResourceMethod()));
        return scopes;
        
    }

    private List<String> getScopesFromAnnotation(AnnotatedElement elem) {		
        return optAnnnotation(elem, ProtectedApi.class).map(ProtectedApi::scopes)
            .map(Arrays::asList).orElse(Collections.emptyList());
    }	

    private static <T extends Annotation> Optional<T> optAnnnotation(AnnotatedElement elem,
            Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }
    
}
