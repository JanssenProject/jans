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

	protected Response getMissingInumError() {
		ApiError error = new ApiError(ApiConstants.MISSING_INUM_CODE, ApiConstants.MISSING_INUM_MESSAGE,
				"The attribute inuml is required from this operation");
		return Response.status(Response.Status.NOT_FOUND).entity(error).build();
	}

}
