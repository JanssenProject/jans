package io.jans.fido2.model.u2f.error;

import io.jans.as.model.error.DefaultErrorResponse;
import io.jans.as.model.error.IErrorType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class Fido2ErrorResponseFactory {

    public static WebApplicationException createBadRequestException(IErrorType type, String reason, String description, String correlationId) {
        final DefaultErrorResponse response = new DefaultErrorResponse();
        response.setType(type);
        response.setState("");
        response.setReason(reason);
        if (correlationId != null)
            response.setErrorDescription(String.format(description + " CorrelationId: %s", correlationId));
        else
            response.setErrorDescription(description);
        throw new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(response.toJSonString())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }
}
