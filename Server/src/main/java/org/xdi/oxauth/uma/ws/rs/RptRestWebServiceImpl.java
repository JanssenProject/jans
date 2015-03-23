/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;

/**
 * @author Yuriy Movchan Date: 10/16/2012
 */
@Name("rptRestWebService")
public class RptRestWebServiceImpl implements RptRestWebService {

	@Logger
	private Log log;
	@In
	private TokenService tokenService;
	@In
	private ErrorResponseFactory errorResponseFactory;
	@In
	private AuthorizationGrantList authorizationGrantList;
	@In
	private RPTManager rptManager;
    @In
    private UmaValidationService umaValidationService;

	public Response getRequesterPermissionToken(String authorization, String amHost) {
		try {
            umaValidationService.validateAuthorizationWithAuthScope(authorization);
			String validatedAmHost = umaValidationService.validateAmHost(amHost);

			return createRpt(authorization, validatedAmHost);
		} catch (Exception ex) {
			log.error("Exception happened", ex);
			if (ex instanceof WebApplicationException) {
				throw (WebApplicationException) ex;
			}

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
		}
	}

	private Response createRpt(String authorization, String amHost) throws IOException {
        UmaRPT rpt = rptManager.createRPT(authorization, amHost);

        // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
        final String entity = ServerUtil.asJson(new RPTResponse(rpt.getCode()));

        final ResponseBuilder builder = Response.status(Response.Status.CREATED);
		builder.entity(entity);
		return builder.build();
	}
}
