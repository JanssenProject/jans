package io.jans.casa.plugins.authnmethod.rs.status.sg;

import io.jans.casa.misc.Utils;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;

/**
 * @author jgomer
 */
public enum ComputeRequestCode {
    NO_USER_ID,
    UNKNOWN_USER_ID,
    SUCCESS;

    public Response getResponse(String key, String request) {

        String json;
        Response.Status httpStatus;

        if (equals(SUCCESS)) {
            httpStatus = OK;
            Map<String, Object> map = new LinkedHashMap<>();    //Ensure data can be received in the same order as here
            map.put("key", key);
            map.put("request", request);

            json = Utils.jsonFromObject(map);
        } else {
            httpStatus = equals(NO_USER_ID) ? BAD_REQUEST : NOT_FOUND;
            json = Utils.jsonFromObject(Collections.singletonMap("code", toString()));
        }
        return Response.status(httpStatus).entity(json).build();

    }

    public Response getResponse() {
        return getResponse(null, null);
    }

}
