package io.jans.lock.cedarling.service;

import io.jans.service.security.protect.BaseAuthorizationProtection;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public interface CedarlingProtection extends BaseAuthorizationProtection {

    Response processAuthorization(String bearerToken, ResourceInfo resourceInfo);

    public static Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }
    
}