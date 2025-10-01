package io.jans.casa.plugins.authnmethod.rs.status.otp;

import io.jans.casa.misc.Utils;

import jakarta.ws.rs.core.Response;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;

/**
 * @author jgomer
 */
public enum ValidateCode {
    MISSING_PARAMS,
    NO_MATCH,
    INVALID_MODE,
    FAILURE,
    MATCH;

    public Response getResponse() {

        String json;
        Response.Status httpStatus;

        if (equals(MATCH) || equals(NO_MATCH)) {
            httpStatus = OK;
        } else if (equals(FAILURE)){
            httpStatus = INTERNAL_SERVER_ERROR;
        } else {
            httpStatus = BAD_REQUEST;
        }
        json = Utils.jsonFromObject(Collections.singletonMap("code", toString()));
        return Response.status(httpStatus).entity(json).build();

    }

}
