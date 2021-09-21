/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * An ASCII string values that specifies whether the Authorization Server
 * prompts the End-User for re-authentication and consent.
 *
 * @author Javier Rojas Blum Date: 02.10.2012
 */
public enum Prompt implements HasParamName {

    /**
     * The Authorization Server MUST NOT display any authentication or
     * consent user interface pages. An error is returned if the End-User
     * is not already authenticated or the Client does not have pre-configured
     * consent for the requested scopes. This can be used as a method to
     * check for existing authentication and/or consent.
     */
    NONE("none"),
    /**
     * The Authorization Server MUST prompt the End-User for re-authentication
     */
    LOGIN("login"),
    /**
     * The Authorization Server MUST prompt the End-User for consent before
     * returning information to the Client.
     */
    CONSENT("consent"),
    /**
     * The Authorization Server MUST prompt the End-User to select a user account.
     * This allows a user who has multiple accounts at the Authorization Server to
     * select amongst the multiple accounts that they may have current sessions for.
     */
    SELECT_ACCOUNT("select_account");

    private final String paramName;

    private Prompt(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link Prompt} for a parameter
     * prompt of the authorization endpoint.
     *
     * @param param The parameter.
     * @return The corresponding response type if found, otherwise <code>null</code>.
     */
    public static Prompt fromString(String param) {
        if (param != null) {
            for (Prompt rt : Prompt.values()) {
                if (param.equals(rt.paramName)) {
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * Gets param name.
     *
     * @return param name
     */
    public String getParamName() {
        return paramName;
    }

    /**
     * Returns a list of the corresponding {@link Prompt} from a space-separated
     * list of prompt parameters.
     *
     * @param paramList A space-separated list of prompt parameters.
     * @param separator The separator of the string list.
     * @return A list of the recognized response types.
     */
    @JsonCreator
    public static List<Prompt> fromString(String paramList, String separator) {
        List<Prompt> prompts = new ArrayList<>();

        if (paramList != null && !paramList.isEmpty()) {
            String[] params = paramList.split(separator);
            for (String param : params) {
                for (Prompt p : Prompt.values()) {
                    if (param.equals(p.paramName) && !prompts.contains(p)) {
                        prompts.add(p);
                    }
                }
            }
        }

        return prompts;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     */
    @Override
    @JsonValue
    public String toString() {
        return paramName;
    }
}