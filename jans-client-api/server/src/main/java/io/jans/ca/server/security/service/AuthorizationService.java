/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.security.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

public abstract class AuthorizationService implements Serializable {

    private static final long serialVersionUID = 4012335221233316230L;

    @Inject
    transient Logger log;

    public abstract String processAuthorization(String path, String method, String remoteAddress,
                                                String authorization, String authorizationRpId) throws Exception;

    protected Response getErrorResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

    public boolean isEqualCollection(List<String> list1, List<String> list2) {
        return CollectionUtils.isEqualCollection(list1, list2);
    }
}
