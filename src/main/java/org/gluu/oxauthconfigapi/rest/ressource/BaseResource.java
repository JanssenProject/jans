/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import javax.ws.rs.core.Response;

import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class BaseResource {

	protected static final String READ_ACCESS = "oxauth-config-read";
	protected static final String WRITE_ACCESS = "oxauth-config-write";

	protected Response getInternalServerError(Exception ex) {
		ApiError error = new ApiError(Response.Status.INTERNAL_SERVER_ERROR.toString(), "Internal Server error",
				ex.getMessage());
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
	}

	protected Response getResourceNotFoundError() {
		ApiError error = new ApiError(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()), "Resource not found!",
				"The requested resource doesn't exist");
		return Response.status(Response.Status.NOT_FOUND).entity(error).build();
	}

	protected Response getResourceNotFoundError(String object) {
		ApiError error = new ApiError(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()), "Resource not found!",
				"The requested " + object + " doesn't exist");
		return Response.status(Response.Status.NOT_FOUND).entity(error).build();
	}

	protected Response getMissingAttributeError(String attributeName) {
		ApiError error = new ApiError(ApiConstants.MISSING_ATTRIBUTE_CODE, ApiConstants.MISSING_ATTRIBUTE_MESSAGE,
				"The attribute " + attributeName + " is required for this operation");
		return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
	}

}
