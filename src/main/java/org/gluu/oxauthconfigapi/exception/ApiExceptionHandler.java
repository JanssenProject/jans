package org.gluu.oxauthconfigapi.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;

@Provider
public class ApiExceptionHandler implements ExceptionMapper<ApiException> {

	private ApiError error;

	public Response toResponse(ApiException exception) {
		switch (exception.getType()) {
		case NOT_FOUND:
			error = new ApiError.ErrorBuilder().withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()))
					.withMessage("The requested " + exception.getSubject() + " doesn't exist").build();
			break;
		case MISSING_ATTRIBUTE:
			error = new ApiError.ErrorBuilder().withCode(ApiConstants.MISSING_ATTRIBUTE_CODE)
					.withMessage(ApiConstants.MISSING_ATTRIBUTE_MESSAGE)
					.andDescription("The attribute " + exception.getSubject() + " is required for this operation")
					.build();
			break;
		default:
			error = new ApiError.ErrorBuilder().withCode(ApiConstants.MISSING_ATTRIBUTE_CODE)
					.withMessage(exception.getMessage()).build();
		}
		return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
	}

}
