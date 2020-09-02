/**
 * 
 */
package org.gluu.oxauthconfigapi.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.gluu.oxauthconfigapi.rest.model.ApiError;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Provider
public class GlobalErrorHandler implements ExceptionMapper<Exception> {
	public Response toResponse(Exception exception) {
		ApiError error = new ApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
				.withMessage("Internal Server error").andDescription(exception.getMessage()).build();
		return Response.serverError().entity(error).build();
	}
}
