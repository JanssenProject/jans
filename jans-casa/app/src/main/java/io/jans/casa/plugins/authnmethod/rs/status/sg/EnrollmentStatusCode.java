package io.jans.casa.plugins.authnmethod.rs.status.sg;

import io.jans.casa.core.pojo.SuperGluuDevice;
import io.jans.casa.misc.Utils;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jgomer
 */
public enum EnrollmentStatusCode {
    PENDING,
    DUPLICATED,
    FAILED,
    SUCCESS;

    private String getEntity(SuperGluuDevice device) {

        Map<String, Object> map = new LinkedHashMap<>();    //Ensure data can be received in the same order as here
        map.put("code", toString());
        if (device != null) {
            map.put("data", device);
        }
        return Utils.jsonFromObject(map);

    }

    public Response getResponse(SuperGluuDevice device) {

        String json;
        Response.Status httpStatus;

        if (equals(SUCCESS)) {
            httpStatus = Status.CREATED;
            json = Utils.jsonFromObject(device);
        } else {
            httpStatus = equals(PENDING) ? Status.ACCEPTED : Status.INTERNAL_SERVER_ERROR;
            json = Utils.jsonFromObject(Collections.singletonMap("code", toString()));
        }
        return Response.status(httpStatus).entity(json).build();

    }

    public Response getResponse() {
        return getResponse(null);
    }

}
