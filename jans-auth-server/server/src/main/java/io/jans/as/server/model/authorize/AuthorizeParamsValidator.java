/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.authorize;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates the parameters received for the authorize web service.
 *
 * @author Javier Rojas Blum
 * @version October 6, 2021
 */
public class AuthorizeParamsValidator {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeParamsValidator.class);

    private AuthorizeParamsValidator() {
    }

    /**
     * Validates the parameters for an authorization request.
     *
     * @param responseTypes The response types. This parameter is mandatory.
     * @return Returns <code>true</code> when all the parameters are valid.
     */
    public static boolean validateParams(List<ResponseType> responseTypes, List<Prompt> prompts, String nonce,
                                         boolean fapiCompatibility, ResponseMode responseMode) {
        if (fapiCompatibility) {
            // The authorization server shall require the response_type value code in conjunction with the response_mode value jwt
            if (responseTypes.size() == 1 && responseTypes.contains(ResponseType.CODE) && responseMode != ResponseMode.JWT) {
                return false;
            }
            if (responseMode == ResponseMode.QUERY) {
                log.trace("ResponseMode=query is not allowed for FAPI.");
                return false;
            }
        }

        boolean existsNonce = StringUtils.isNotBlank(nonce);

        if (!existsNonce && ((responseTypes.contains(ResponseType.CODE) && responseTypes.contains(ResponseType.ID_TOKEN))
                || (responseTypes.contains(ResponseType.ID_TOKEN) && responseTypes.size() == 1)
                || (responseTypes.contains(ResponseType.ID_TOKEN) && responseTypes.contains(ResponseType.TOKEN))
                || (responseTypes.contains(ResponseType.TOKEN) && responseTypes.size() == 1))) {
            return false;
        }

        boolean validParams = !responseTypes.isEmpty();
        return validParams && noNonePrompt(prompts);
    }

    public static boolean noNonePrompt(List<Prompt> prompts) {
        return !(prompts.contains(Prompt.NONE) && prompts.size() > 1);
    }

    public static boolean validateResponseTypes(List<ResponseType> responseTypes, Client client) {
        if (responseTypes == null || responseTypes.isEmpty()) {
            log.debug("Response type validation failed. Response type is not specified.");
            return false;
        }
        if (client == null) {
            log.debug("Response type validation failed. Client is null.");
            return false;
        }
        if (client.getResponseTypes() == null) {
            log.debug("Response type validation failed. Client does not have response type configured.");
            return false;
        }

        List<ResponseType> clientSupportedResponseTypes = Arrays.asList(client.getResponseTypes());

        final boolean containsAll = clientSupportedResponseTypes.containsAll(responseTypes);
        if (!containsAll) {
            log.debug("Response type validation failed for {}. Client does not allow all values, clientSupportedResponseTypes {}", responseTypes, clientSupportedResponseTypes);
        }
        return containsAll;
    }

    public static boolean validateGrantType(List<ResponseType> responseTypes, GrantType[] clientGrantTypesArray, AppConfiguration appConfiguration) {
        List<GrantType> clientGrantTypes = Arrays.asList(clientGrantTypesArray);
        final Set<GrantType> grantTypesSupported = appConfiguration.getGrantTypesSupported();

        if (responseTypes == null) {
            log.debug("Grant type validation failed. No response type in request.");
            return false;
        }
        if (grantTypesSupported == null) {
            log.debug("Grant type validation failed. No supported grant types in AS configuration ('grantTypesSupported').");
            return false;
        }
        if (responseTypes.contains(ResponseType.CODE)) {
            GrantType requestedGrantType = GrantType.AUTHORIZATION_CODE;
            if (!clientGrantTypes.contains(requestedGrantType)) {
                log.debug("Grant type validation failed. response_type=code but authorization_code grant type is not allowed by client configuration.");
                return false;
            }
            if (!grantTypesSupported.contains(requestedGrantType)) {
                log.debug("Grant type validation failed. response_type=code but authorization_code grant type is not allowed by AS configuration ('grantTypesSupported').");
                return false;
            }
        }
        if (responseTypes.contains(ResponseType.TOKEN) || (responseTypes.contains(ResponseType.ID_TOKEN) && !appConfiguration.getAllowIdTokenWithoutImplicitGrantType())) {
            GrantType requestedGrantType = GrantType.IMPLICIT;
            if (!clientGrantTypes.contains(requestedGrantType)) {
                log.debug("Grant type validation failed. response_type=token (or response_type=id_token) but 'implicit' grant type is not allowed by client configuration.");
                return false;
            }
            if (!grantTypesSupported.contains(requestedGrantType)) {
                log.debug("Grant type validation failed. response_type=token (or response_type=id_token) but 'implicit' grant type is not allowed by AS configuration ('grantTypesSupported').");
                return false;
            }
        }

        return true;
    }
}