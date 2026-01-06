/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.lock.service.openid;

import io.jans.service.security.protect.BaseAuthorizationProtection;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

public interface OpenIdProtection extends BaseAuthorizationProtection {

    Response processAuthorization(String bearerToken, ResourceInfo resourceInfo);

    public static Response simpleResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }
    
}