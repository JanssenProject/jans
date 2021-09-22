/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.util.Util;
import org.apache.logging.log4j.util.Strings;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HttpMethod;
import java.util.List;

import static io.jans.as.model.common.BackchannelTokenDeliveryMode.PING;
import static io.jans.as.model.common.BackchannelTokenDeliveryMode.POLL;
import static io.jans.as.model.common.BackchannelTokenDeliveryMode.PUSH;
import static io.jans.as.model.common.GrantType.CIBA;

/**
 * @author Javier Rojas Blum
 * @version May 20, 2020
 */
@Stateless
@Named
public class CIBARegisterParamsValidatorService {

    private final static Logger log = LoggerFactory.getLogger(CIBARegisterParamsValidatorService.class);

    @Inject
    private AppConfiguration appConfiguration;

    public boolean validateParams(
            BackchannelTokenDeliveryMode backchannelTokenDeliveryMode, String backchannelClientNotificationEndpoint,
            AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg,
            List<GrantType> grantTypes, SubjectType subjectType, String sectorIdentifierUri, String jwks, String jwksUri) {
        try {
            // Not CIBA Registration
            if (backchannelTokenDeliveryMode == null && Strings.isBlank(backchannelClientNotificationEndpoint) && backchannelAuthenticationRequestSigningAlg == null) {
                return true;
            }

            // Required parameter.
            if (backchannelTokenDeliveryMode == null
                    || !appConfiguration.getBackchannelTokenDeliveryModesSupported().contains(backchannelTokenDeliveryMode.getValue())) {
                return false;
            }

            // Required if the token delivery mode is set to ping or push.
            if ((backchannelTokenDeliveryMode == PING || backchannelTokenDeliveryMode == PUSH)
                    && Strings.isBlank(backchannelClientNotificationEndpoint)) {
                return false;
            }

            // Grant type urn:openid:params:grant-type:ciba is required if the token delivery mode is set to ping or poll.
            if (backchannelTokenDeliveryMode == PING || backchannelTokenDeliveryMode == POLL) {
                if (!appConfiguration.getGrantTypesSupported().contains(CIBA) || !grantTypes.contains(CIBA)) {
                    return false;
                }
            }

            if (subjectType != null && subjectType == SubjectType.PAIRWISE) {

                if (backchannelTokenDeliveryMode == PING || backchannelTokenDeliveryMode == POLL) {
                    if (Strings.isBlank(jwks) && Strings.isBlank(jwksUri)) {
                        return false;
                    }
                }

                if (Strings.isNotBlank(sectorIdentifierUri)) {
                    ClientRequest clientRequest = new ClientRequest(sectorIdentifierUri);
                    clientRequest.setHttpMethod(HttpMethod.GET);

                    ClientResponse<String> clientResponse = clientRequest.get(String.class);
                    int status = clientResponse.getStatus();

                    if (status != 200) {
                        return false;
                    }

                    String entity = clientResponse.getEntity(String.class);
                    JSONArray sectorIdentifierJsonArray = new JSONArray(entity);

                    if (backchannelTokenDeliveryMode == PING || backchannelTokenDeliveryMode == POLL) {
                        // If a sector_identifier_uri is explicitly provided, then the jwks_uri must be included in the list of
                        // URIs pointed to by the sector_identifier_uri.
                        if (!Strings.isBlank(jwksUri) && !Util.asList(sectorIdentifierJsonArray).contains(jwksUri)) {
                            return false;
                        }
                    } else if (backchannelTokenDeliveryMode == PUSH) {
                        // In case a sector_identifier_uri is explicitly provided, then the backchannel_client_notification_endpoint
                        // must be included in the list of URIs pointed to by the sector_identifier_uri.
                        if (!Util.asList(sectorIdentifierJsonArray).contains(backchannelClientNotificationEndpoint)) {
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return false;
        }

        return true;
    }
}