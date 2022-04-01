/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.session;

/**
 * @author Javier Rojas Blum
 * @version 0.9 October 30, 2014
 */
public class EndSessionResponseParam {

    private EndSessionResponseParam() {
    }

    /**
     * Opaque value used by the RP to maintain state between the logout request and the callback to the endpoint
     * specified by the post_logout_redirect_uri parameter. If included in the logout request, the OP passes this
     * value back to the RP using the state query parameter when redirecting the User Agent back to the RP.
     */
    public static final String STATE = "state";
}
