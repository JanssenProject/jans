/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.util.AuthUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.jans.as.model.util.Util.escapeLog;

import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@ApplicationScoped
public class SsaService {

    @Inject
    private Logger logger;

    @Inject
    AuthUtil authUtil;

    public Response getSsa(final String accessToken, final String jti, final String orgId) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("SSA search parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }
        logger.error("Request jti SSA List -  jti:{} , orgId:{}", jti, orgId);
        AuthClientFactory.getSsaList(authUtil.getIssuer(), accessToken, jti, orgId);
        return AuthClientFactory.getSsa(authUtil.getIssuer(), accessToken, jti, orgId);
    }

    public Response createSsa(final String accessToken, final String jsonNode) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("SSA Create parameters - jti:{}, orgId:{}", escapeLog(jsonNode));
        }
        logger.error("Create SSA -  jsonNode:{}", jsonNode);

        return AuthClientFactory.createSsa(authUtil.getIssuer(), accessToken, jsonNode);
    }

    public Response revokeSsa(final String accessToken, final String jti, final String orgId) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("SSA revoke parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }
        logger.error("Delete SSA -  jti:{} ", jti);

        return AuthClientFactory.revokeSsa(authUtil.getIssuer(), accessToken, jti, orgId);
    }

}
