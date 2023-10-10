package io.jans.casa.plugins.authnmethod.rs.status.sms;

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
    MATCH,
    NO_MATCH_OR_EXPIRED,
    MISSING_PARAMS;

    public Response getResponse() {

        String json = null;
        Response.Status httpStatus;

        if (equals(MATCH)) {
            httpStatus = OK;
        } else {
            httpStatus = equals(MISSING_PARAMS) ? BAD_REQUEST : INTERNAL_SERVER_ERROR;
            json = Utils.jsonFromObject(Collections.singletonMap("code", toString()));
        }
        return Response.status(httpStatus).entity(json).build();

    }

}
