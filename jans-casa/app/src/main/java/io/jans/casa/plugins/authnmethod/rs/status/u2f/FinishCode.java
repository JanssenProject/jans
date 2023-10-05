package io.jans.casa.plugins.authnmethod.rs.status.u2f;

import io.jans.casa.misc.Utils;

import jakarta.ws.rs.core.Response;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;

/**
 * @author jgomer
 */
public enum FinishCode {
    MISSING_PARAMS,
    NO_MATCH_OR_EXPIRED,
    FAILED,
    SUCCESS;

    public Response getResponse() {

        String json = null;
        Response.Status httpStatus;

        if (equals(SUCCESS)) {
            httpStatus = OK;
        } else {
            json = Utils.jsonFromObject(Collections.singletonMap("code", toString()));
            httpStatus = equals(MISSING_PARAMS) ? BAD_REQUEST : INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();

    }

}
