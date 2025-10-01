package io.jans.casa.plugins.authnmethod.rs.status.otp;

import io.jans.casa.misc.Utils;

import jakarta.ws.rs.core.Response;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.*;

/**
 * @author jgomer
 */
public enum FinishCode {
    MISSING_PARAMS,
    NO_MATCH_OR_EXPIRED,
    FAILED,
    SUCCESS;

    public Response getResponse(Object device) {

        Response.Status httpStatus;
        String json;

        if (equals(SUCCESS)) {
            httpStatus = CREATED;
            json = Utils.jsonFromObject(device);
        } else {
            httpStatus = equals(MISSING_PARAMS) ? BAD_REQUEST : INTERNAL_SERVER_ERROR;
            json = Utils.jsonFromObject(Collections.singletonMap("code", toString()));
        }
        return Response.status(httpStatus).entity(json).build();

    }

}
