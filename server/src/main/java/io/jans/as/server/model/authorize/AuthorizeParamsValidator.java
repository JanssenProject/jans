/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.authorize;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates the parameters received for the authorize web service.
 *
 * @author Javier Rojas Blum
 * @version July 19, 2017
 */
public class AuthorizeParamsValidator {

    /**
     * Validates the parameters for an authorization request.
     *
     * @param responseTypes The response types. This parameter is mandatory.
     * @return Returns <code>true</code> when all the parameters are valid.
     */
    public static boolean validateParams(List<ResponseType> responseTypes, List<Prompt> prompts, String nonce,
                                         boolean fapiCompatibility) {
        if (fapiCompatibility && responseTypes.size() == 1 && responseTypes.contains(ResponseType.CODE)) {
            return false;
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
        if (responseTypes == null || responseTypes.isEmpty() || client == null || client.getResponseTypes() == null) {
            return false;
        }

        List<ResponseType> clientSupportedResponseTypes = Arrays.asList(client.getResponseTypes());

        return clientSupportedResponseTypes.containsAll(responseTypes);
    }

    public static boolean validateGrantType(List<ResponseType> responseTypes, GrantType[] clientGrantTypesArray, Set<GrantType> grantTypesSupported) {
        List<GrantType> clientGrantTypes = Arrays.asList(clientGrantTypesArray);

        if (responseTypes == null || grantTypesSupported == null) {
            return false;
        }
        if (responseTypes.contains(ResponseType.CODE)) {
            GrantType requestedGrantType = GrantType.AUTHORIZATION_CODE;
            if (!clientGrantTypes.contains(requestedGrantType) || !grantTypesSupported.contains(requestedGrantType)) {
                return false;
            }
        }
        if (responseTypes.contains(ResponseType.TOKEN) || responseTypes.contains(ResponseType.ID_TOKEN)) {
            GrantType requestedGrantType = GrantType.IMPLICIT;
            if (!clientGrantTypes.contains(requestedGrantType) || !grantTypesSupported.contains(requestedGrantType)) {
                return false;
            }
        }

        return true;
    }
}