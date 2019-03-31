package org.gluu.oxd.server;

import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.ErrorResponse;
import org.gluu.oxd.common.ErrorResponseCode;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

/**
 * @author Yuriy Zabrovarnyy
 */
public class HttpException extends WebApplicationException {

    private final ErrorResponseCode code;

    public HttpException(ErrorResponseCode code) {
        super(Response.status(code.getHttpStatus()).type(MediaType.APPLICATION_JSON_TYPE).entity(CoreUtils.asJsonSilently(new ErrorResponse(code))).build());
        this.code = code;
    }

    public ErrorResponseCode getCode() {
        return code;
    }

    public static HttpException internalError() {
        return new HttpException(ErrorResponseCode.INTERNAL_ERROR_UNKNOWN);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpException that = (HttpException) o;
        return code == that.code;
    }

    @Override
    public int hashCode() {

        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "HttpException{" +
                "code=" + code +
                "} " + super.toString();
    }
}
