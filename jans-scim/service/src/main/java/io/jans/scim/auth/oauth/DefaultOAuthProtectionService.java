package io.jans.scim.auth.oauth;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.scim.auth.IProtectionService;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@ApplicationScoped
public class DefaultOAuthProtectionService extends BaseOAuthProtectionService {

    @Inject
    private Logger log;
    
    @Override
    public Response processIntrospectionResponse(IntrospectionResponse iresponse, List<String> scopes) {
        
        Response response = null;
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

}
