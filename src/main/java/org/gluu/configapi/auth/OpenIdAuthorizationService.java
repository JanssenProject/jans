package org.gluu.configapi.auth;

import org.gluu.oxauth.model.common.IntrospectionResponse;
import org.gluu.configapi.service.OpenIdService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.apache.commons.lang.StringUtils;

@ApplicationScoped
@Named("openIdAuthorizationService")
public class OpenIdAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger logger;

    @Inject
    OpenIdService openIdService;

    public void validateAuthorization(String token, ResourceInfo resourceInfo) throws Exception {
        if (StringUtils.isBlank(token)) {
            logger.error("Token is blank !!!!!!");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        List<String> resourceScopes = getRequestedScopes(resourceInfo);

        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionService()
                .introspectToken("Bearer " + token, "invalid_token");
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            logger.error("Token is Invalid !!!!!!");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
            logger.error("Inadequate Authorization !!!!!!");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

    }

}