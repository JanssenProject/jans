/**
 * 
 */
package org.gluu.oxauthconfigapi.exception;

import org.gluu.oxauthconfigapi.rest.model.ApiError;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Provider
public class GlobalErrorHandler implements ExceptionMapper<Exception> {
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException && ((WebApplicationException) exception).getResponse() != null) {
            return ((WebApplicationException) exception).getResponse();
        }
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .withMessage("Internal Server error").andDescription(exception.getMessage()).build();
        return Response.serverError().entity(error).build();
    }
}
