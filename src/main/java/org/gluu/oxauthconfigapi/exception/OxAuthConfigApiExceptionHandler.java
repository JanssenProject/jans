package org.gluu.oxauthconfigapi.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class OxAuthConfigApiExceptionHandler implements ExceptionMapper<ApiException> {

	public Response toResponse(ApiException exception) {
		return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
	}

}
