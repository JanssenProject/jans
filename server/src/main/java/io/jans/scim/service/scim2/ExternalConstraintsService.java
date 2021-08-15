package io.jans.scim.service.scim2;

import java.util.Optional;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import io.jans.orm.model.base.Entry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.scim.service.external.ExternalScimService;
import io.jans.scim.service.external.OperationContext;
import io.jans.scim.service.external.TokenDetails;
import io.jans.scim.ws.rs.scim2.BaseScimWebService;
import io.jans.util.Pair;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ExternalConstraintsService {
    
    private static final String TOKENS_DN = "ou=tokens,o=jans";

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    ExternalScimService externalScimService;

    public Response applyEntityCheck(Entry entity, HttpHeaders httpHeaders, UriInfo uriInfo,
            String httpMethod, String resourceType) throws Exception {
        
        Response response = null;
        if (externalScimService.isEnabled()) {
            OperationContext ctx = makeContext(httpHeaders, uriInfo, httpMethod, resourceType);

            if (!externalScimService.executeAllowResourceOperation(entity, ctx)) {
                String error = externalScimService.executeRejectedResourceOperationResponse(entity, ctx);
                response = BaseScimWebService.getErrorResponse(Status.FORBIDDEN, error);
            }
        }
        return response;
        
    }

    public Pair<String, Response> applySearchCheck(HttpHeaders httpHeaders, UriInfo uriInfo,
            String httpMethod, String resourceType) throws Exception {
        
        Pair<String, Response> result = new Pair<>();
        if (externalScimService.isEnabled()) {
            OperationContext ctx = makeContext(httpHeaders, uriInfo, httpMethod, resourceType);

            String allow = externalScimService.executeAllowSearchOperation(ctx);
            if (allow == null) {
                String error = externalScimService.executeRejectedSearchOperationResponse(ctx);
                result.setSecond(BaseScimWebService.getErrorResponse(Status.FORBIDDEN, error));
                
            } else if (allow.length() > 0) {
                result.setFirst(allow);                
            }
            // when length is zero, the call is allowed straight
        }
        return result;
    }
      
    private OperationContext makeContext(HttpHeaders httpHeaders, UriInfo uriInfo,
            String httpMethod, String resourceType) {

        OperationContext ctx = new OperationContext();
        ctx.setBaseUri(uriInfo.getBaseUri());
        ctx.setMethod(httpMethod);
        ctx.setResourceType(resourceType);
        ctx.setPath(uriInfo.getPath());
        ctx.setQueryParams(uriInfo.getQueryParameters());
        ctx.setRequestHeaders(httpHeaders.getRequestHeaders());
        ctx.setPassthroughMap(new HashMap<>());
        
        String token = Optional.ofNullable(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION))
                .map(authz -> authz.replaceFirst("Bearer\\s+", "")).orElse(null);

        TokenDetails details = getDatabaseToken(token);
        if (details == null) {
            log.warn("Unable to get token details");
            details = new TokenDetails();
        }

        details.setValue(token);
        ctx.setTokenDetails(details);
        return ctx;

    }

    private TokenDetails getDatabaseToken(String token) {
        
        String hashedToken = token.startsWith("{sha256Hex}") ? token : DigestUtils.sha256Hex(token);
        try {
            return entryManager.find(TokenDetails.class,
                    String.format("tknCde=%s,%s", hashedToken, TOKENS_DN));
        } catch (Exception e) {
            try {
                log.error(e.getMessage());
                return entryManager.find(TokenDetails.class,
                    String.format("tknCde=%s,%s", hashedToken, "ou=uma_rpt," + TOKENS_DN));
            } catch (Exception e2) {
                log.error(e2.getMessage());
                return null;
            }
        }
        
    }
    
}
