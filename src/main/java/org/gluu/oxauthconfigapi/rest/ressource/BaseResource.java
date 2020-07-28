/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import javax.ws.rs.core.Response;

import org.gluu.oxauthconfigapi.rest.model.ApiError;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class BaseResource {

	protected static final String READ_ACCESS = "oxauth-config-read";
	protected static final String WRITE_ACCESS = "oxauth-config-write";

	protected Response getServerError(Exception ex) {
		ApiError error = new ApiError(Response.Status.INTERNAL_SERVER_ERROR.toString(), "Server error",
				ex.getMessage());
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
	}

}
