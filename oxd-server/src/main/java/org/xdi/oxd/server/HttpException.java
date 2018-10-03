package org.xdi.oxd.server;

import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.ErrorResponseCode;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 */
public class HttpException extends WebApplicationException {

    public HttpException(ErrorResponseCode code) {
        super(Response.status(code.getHttpStatus()).entity(CoreUtils.asJsonSilently(new ErrorResponse(code))).build());
    }
}
