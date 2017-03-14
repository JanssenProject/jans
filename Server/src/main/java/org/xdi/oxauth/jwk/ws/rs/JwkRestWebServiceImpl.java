/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.jwk.ws.rs;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;

/**
 * Provides interface for JWK REST web services
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class JwkRestWebServiceImpl implements JwkRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Override
    public Response requestJwk(SecurityContext sec) {
        log.debug("Attempting to request JWK, Is Secure = {0}", sec.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        try {
            JSONWebKeySet jwks = configurationFactory.getWebKeys();
            builder.entity(jwks.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
        }

        return builder.build();
    }
}