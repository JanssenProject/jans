/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.federation.ws.rs;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.federation.FederationErrorResponseType;
import org.xdi.oxauth.model.federation.FederationRequest;
import org.xdi.oxauth.service.FederationDataService;
import org.xdi.oxauth.service.FederationMetadataService;

/**
 * Provides implementation for Federation Data REST web service interface.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/10/2012
 */
@Name("federationDataWS")
public class FederationDataWSImpl implements FederationDataWS {

    @Logger
    private Log log;

    @In
    private FederationDataService federationDataService;

    @In
    private FederationMetadataService federationMetadataService;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @Override
    public Response requestJoin(String federationId, String entityType, String displayName,
                                String opId, String domain, String redirectUri, String x509url, String x509pem,
                                HttpServletRequest request, SecurityContext sec) {
        try {
            log.trace("Federation join request - federationId: {0}, entityType: {1}, displayName: {2}, opId: {3}, domain: {4}, redirectUri: {5}, x509url: {6}, x509pem {7}",
                    federationId, entityType, displayName, opId, domain, redirectUri, x509url, x509pem);

            // check whether federationId exists, if not InvalidIdException is thrown
            federationMetadataService.getMetadata(federationId, false);

            final FederationRequest.Type type = FederationRequest.Type.fromValue(entityType);
            if (type != null) {
                if (StringUtils.isBlank(displayName)) {
                    return errorResponse(FederationErrorResponseType.INVALID_DISPLAY_NAME);
                }

                final FederationRequest federationRequest = new FederationRequest();
                federationRequest.setFederationId(federationId);
                federationRequest.setEntityType(entityType);
                federationRequest.setDisplayName(displayName);

                switch (type) {
                    case OP:
                        if (StringUtils.isBlank(domain)) {
                            return errorResponse(FederationErrorResponseType.INVALID_DOMAIN);
                        }
                        if (StringUtils.isBlank(opId)) {
                            return errorResponse(FederationErrorResponseType.INVALID_OP_ID);
                        }

                        federationRequest.setDomain(domain);
                        federationRequest.setOpId(opId);
                        break;
                    case RP:
                        if (StringUtils.isBlank(redirectUri)) {
                            return errorResponse(FederationErrorResponseType.INVALID_REDIRECT_URI);
                        }
                        final String[] splitedRedirectUris = redirectUri.split(" ");
                        if (splitedRedirectUris != null && splitedRedirectUris.length > 0) {
                            federationRequest.setRedirectUri(Arrays.asList(splitedRedirectUris));
                        } else {
                            return errorResponse(FederationErrorResponseType.INVALID_REDIRECT_URI);
                        }
                        break;
                    default:
                        return errorResponse(FederationErrorResponseType.INVALID_ENTITY_TYPE);
                }

                if (federationDataService.persist(federationRequest)) {
                    return Response.status(Response.Status.OK).build();
                }
            } else {
                return errorResponse(FederationErrorResponseType.INVALID_ENTITY_TYPE);
            }
        } catch (FederationMetadataService.InvalidIdException e) {
            log.trace(e.getMessage(), e);
            return errorResponse(FederationErrorResponseType.INVALID_FEDERATION_ID);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return errorResponse(FederationErrorResponseType.INVALID_REQUEST);
    }

    private Response errorResponse(FederationErrorResponseType p_type) {
        return Response.status(Response.Status.BAD_REQUEST).
                entity(errorResponseFactory.getErrorResponse(p_type).toJSonString()).
                build();
    }
}
