/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.rest.model.ApiError;
import io.jans.configapi.util.ApiConstants;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class BaseResource {

    protected static final String READ_ACCESS = "config-api-read";
    protected static final String WRITE_ACCESS = "config-api-write";
    protected static final String DEFAULT_LIST_SIZE = "50";

    public static <T> void checkResourceNotNull(T resource, String objectName) {
        if (resource == null) {
            throw new NotFoundException(getNotFoundError(objectName));
        }
    }

    public static <T> void checkNotNull(String attribute, String attributeName) {
        if (attribute == null) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
        }
    }

    public static <T> void checkNotEmpty(List<T> list, String attributeName) {
        if (list == null || list.isEmpty()) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
        }
    }

    /**
     * @param attributeName
     * @return
     */
    protected static Response getMissingAttributeError(String attributeName) {
        ApiError error = new ApiError.ErrorBuilder().withCode(ApiConstants.MISSING_ATTRIBUTE_CODE)
                .withMessage(ApiConstants.MISSING_ATTRIBUTE_MESSAGE)
                .andDescription("The attribute " + attributeName + " is required for this operation").build();
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    protected static Response getNotFoundError(String objectName) {
        ApiError error = new ApiError.ErrorBuilder().withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()))
                .withMessage("The requested " + objectName + " doesn't exist").build();
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }

    protected static Response getNotAcceptableException(String msg) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.NOT_ACCEPTABLE.getStatusCode())).withMessage(msg).build();
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(error).build();
    }

}
