/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.security.service;

import io.jans.as.model.util.StringUtils;
import io.jans.as.model.util.Util;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import io.jans.ca.server.service.RpSyncService;
import io.jans.ca.server.service.ValidationService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

@ApplicationScoped
@Named("clientApiAuthorizationService")
@Alternative
@Priority(1)
public class ClientApiAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String AUTHENTICATION_SCHEME = "Bearer ";

    @Inject
    transient Logger LOG;

    @Context
    transient HttpServletRequest request;

    @Context
    transient HttpServletResponse response;

    @Inject
    ValidationService validationService;
    @Inject
    RpSyncService rpSyncService;

    @Inject
    MainPersistenceService jansConfigurationService;

    public String processAuthorization(String path, String method, String remoteAddress,
                                       String authorization, String authorizationRpId) throws Exception {
        LOG.debug("oAuth  Authorization parameters , path:{}, method:{}, authorization: {}, authorizationRpId: {} ",
                path, method, authorization, authorizationRpId);

        final ApiAppConfiguration conf = jansConfigurationService.find();

        validateAuthorizationRpId(conf, authorizationRpId);
        validateAccessToken(authorization, authorizationRpId);

        return "AUTHORIZATION SUCCESS";
    }

    private void validateAuthorizationRpId(ApiAppConfiguration conf, String authorizationRpId) {

        if (Util.isNullOrEmpty(authorizationRpId)) {
            LOG.debug("`AuthorizationRpId` header is null or Empty");
            throw new HttpException(ErrorResponseCode.AUTHORIZATION_RP_ID_HEADER_NOT_FOUND);
        }

        final Rp rp = rpSyncService.getRp(authorizationRpId);

        if (rp == null || Util.isNullOrEmpty(rp.getRpId())) {
            LOG.debug("`rp_id` in `AuthorizationRpId` header is not registered in jans_client_api.");
            throw new HttpException(ErrorResponseCode.AUTHORIZATION_RP_ID_NOT_FOUND);
        }

        if (conf.getProtectCommandsWithRpId() == null || conf.getProtectCommandsWithRpId().isEmpty()) {
            return;
        }

        if (!conf.getProtectCommandsWithRpId().contains(authorizationRpId)) {
            LOG.debug("`rp_id` in `AuthorizationRpId` header is invalid. The `AuthorizationRpId` header should contain `rp_id` from `protect_commands_with_rp_id` field in client-api-server.yml.");
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_RP_ID);
        }
    }

    private void validateAccessToken(String authorization, String authorizationRpId) {
        final String prefix = AUTHENTICATION_SCHEME;
        final ApiAppConfiguration conf = jansConfigurationService.find();

        if (conf.getProtectCommandsWithAccessToken() != null && !conf.getProtectCommandsWithAccessToken()) {
            LOG.debug("Skip protection because protect_commands_with_access_token: false in configuration file.");
            return;
        }

        if (Util.isNullOrEmpty(authorization)) {
            LOG.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        String accessToken = authorization.substring(prefix.length());
        if (Util.isNullOrEmpty(accessToken)) {
            LOG.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }
        validationService.validateAccessToken(accessToken, authorizationRpId);
    }

}