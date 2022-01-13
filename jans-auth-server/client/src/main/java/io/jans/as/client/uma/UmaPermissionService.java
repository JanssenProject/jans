/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma;

import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaPermissionList;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the host registers permissions that it anticipates a
 * requester will shortly be asking for from the AM. This AM's endpoint is part
 * of resource registration API.
 * <p/>
 * In response to receiving an access request accompanied by an RPT that is
 * invalid or has insufficient authorization data, the host SHOULD register a
 * permission with the AS that would be sufficient for the type of access
 * sought. The AS returns a permission ticket for the host to give to the
 * requester in its response.
 */
public interface UmaPermissionService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    PermissionTicket registerPermission(
            @HeaderParam("Authorization") String authorization,
            UmaPermissionList permissions);
}