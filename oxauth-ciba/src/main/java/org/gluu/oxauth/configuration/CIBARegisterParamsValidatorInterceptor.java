/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.configuration;

import org.apache.logging.log4j.util.Strings;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.interception.CIBARegisterParamsValidatorInterception;
import org.gluu.oxauth.interception.CIBARegisterParamsValidatorInterceptionInterface;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.gluu.oxauth.model.util.Util;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.HttpMethod;
import java.io.Serializable;
import java.util.List;

import static org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode.*;
import static org.gluu.oxauth.model.common.GrantType.CIBA;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Interceptor
@CIBARegisterParamsValidatorInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBARegisterParamsValidatorInterceptor implements CIBARegisterParamsValidatorInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBARegisterParamsValidatorInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBARegisterParamsValidatorInterceptor() {
        log.info("CIBA Register Params Validator Interceptor loaded.");
    }

    @AroundInvoke
    public Object validateParams(InvocationContext ctx) {
        log.debug("CIBA: validate register params...");

        boolean valid = false;
        try {
            BackchannelTokenDeliveryMode backchannelTokenDeliveryMode = (BackchannelTokenDeliveryMode) ctx.getParameters()[0];
            String backchannelClientNotificationEndpoint = (String) ctx.getParameters()[1];
            AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg = (AsymmetricSignatureAlgorithm) ctx.getParameters()[2];
            Boolean backchannelUserCodeParameter = (Boolean) ctx.getParameters()[3];
            List<GrantType> grantTypes = (List<GrantType>) ctx.getParameters()[4];
            SubjectType subjectType = (SubjectType) ctx.getParameters()[5];
            String sectorIdentifierUri = (String) ctx.getParameters()[6];
            String jwksUri = (String) ctx.getParameters()[7];
            valid = validateParams(backchannelTokenDeliveryMode, backchannelClientNotificationEndpoint, backchannelAuthenticationRequestSigningAlg,
                    backchannelUserCodeParameter, grantTypes, subjectType, sectorIdentifierUri, jwksUri);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to validate register params.", e);
        }

        return valid;
    }

    @Override
    public boolean validateParams(
            BackchannelTokenDeliveryMode backchannelTokenDeliveryMode, String backchannelClientNotificationEndpoint,
            AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg, Boolean backchannelUserCodeParameter,
            List<GrantType> grantTypes, SubjectType subjectType, String sectorIdentifierUri, String jwksUri) {
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

            // If the server does not support backchannel_user_code_parameter_supported, the default value is false.
            if (appConfiguration.getBackchannelUserCodeParameterSupported() == null || appConfiguration.getBackchannelUserCodeParameterSupported() == false) {
                backchannelUserCodeParameter = false;
            }

            if (subjectType != null && subjectType == SubjectType.PAIRWISE) {

                if (backchannelTokenDeliveryMode == PING || backchannelTokenDeliveryMode == POLL) {
                    if (Strings.isBlank(jwksUri)) {
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
                        if (!Util.asList(sectorIdentifierJsonArray).contains(jwksUri)) {
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