package io.jans.scim.auth;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public interface IProtectionService {

    Response processAuthorization(HttpHeaders httpHeaders, ResourceInfo resourceInfo);

    public static Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }
    
}
