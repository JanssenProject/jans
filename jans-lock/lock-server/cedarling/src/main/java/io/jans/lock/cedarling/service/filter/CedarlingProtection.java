package io.jans.lock.cedarling.service.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public interface CedarlingProtection {

    Response processAuthorization(ContainerRequestContext requestContext, HttpHeaders httpHeaders, ResourceInfo resourceInfo);

    public static Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }
    
}