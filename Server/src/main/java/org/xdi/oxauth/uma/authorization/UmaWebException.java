package org.xdi.oxauth.uma.authorization;

import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author yuriyz on 06/06/2017.
 */
public class UmaWebException extends WebApplicationException {

    public UmaWebException(Response.Status status, ErrorResponseFactory factory, UmaErrorResponseType error) {
        super(Response.status(status).entity(factory.getUmaJsonErrorResponse(error)).build());
    }
}
