package io.jans.scim.service.scim2;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import io.jans.orm.model.base.Entry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.scim.model.conf.ScimMode;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.service.external.ExternalScimService;
import io.jans.scim.service.external.OperationContext;
import io.jans.scim.service.external.TokenDetails;
import io.jans.scim.ws.rs.scim2.BaseScimWebService;

import org.apache.commons.lang.StringUtils;
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
    private AppConfiguration appConfiguration;

    @Inject
    ExternalScimService externalScimService;

    public Response applyEntityCheck(Entry entity, Object payload, HttpHeaders httpHeaders,
            UriInfo uriInfo, String httpMethod, String resourceType) throws Exception {
        
        Response response = null;
        if (externalScimService.isEnabled()) {
            OperationContext ctx = makeContext(httpHeaders, uriInfo, httpMethod, resourceType);
            response = externalScimService.executeManageResourceOperation(entity, payload, ctx);
        }
        return response;
        
    }

    public Response applySearchCheck(SearchRequest searchReq, HttpHeaders httpHeaders,
            UriInfo uriInfo, String httpMethod, String resourceType) throws Exception {
        
        Response response = null;
        if (externalScimService.isEnabled()) {

            OperationContext ctx = makeContext(httpHeaders, uriInfo, httpMethod, resourceType);
            response = externalScimService.executeManageSearchOperation(searchReq, ctx);
            
            if (response == null) {
                String filterPrepend = ctx.getFilterPrepend();

                if (!StringUtils.isEmpty(filterPrepend)) {
                    if (StringUtils.isEmpty(searchReq.getFilter())) {
                        searchReq.setFilter(filterPrepend);
                    } else {
                        searchReq.setFilter(String.format("%s and (%s)", filterPrepend, searchReq.getFilter()));
                    }
                }
            }
        }
        return response;

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
        
        if (!ScimMode.BYPASS.equals(appConfiguration.getProtectionMode())) {

            String token = Optional.ofNullable(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION))
                    .map(authz -> authz.replaceFirst("Bearer\\s+", "")).orElse(null);

            TokenDetails details = getDatabaseToken(token);
            if (details == null) {
                log.warn("Unable to get token details");
                details = new TokenDetails();
            }

            details.setValue(token);
            ctx.setTokenDetails(details);
        }
        return ctx;

    }

    private TokenDetails getDatabaseToken(String token) {
        
        String hashedToken = token.startsWith("{sha256Hex}") ? token : DigestUtils.sha256Hex(token);
        try {
            return entryManager.find(TokenDetails.class,
                    String.format("tknCde=%s,%s", hashedToken, TOKENS_DN));
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
        
    }
    
}
