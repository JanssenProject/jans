/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.audit;

/**
 * @version August 20, 2019
 */
public enum Action {
    CLIENT_REGISTRATION("CLIENT_REGISTRATION"),
    CLIENT_UPDATE("CLIENT_UPDATE"),
    CLIENT_READ("CLIENT_READ"),
    CLIENT_DELETE("CLIENT_DELETE"),
    USER_AUTHORIZATION("USER_AUTHORIZATION"),
    BACKCHANNEL_AUTHENTICATION("BACKCHANNEL_AUTHENTICATION"),
    BACKCHANNEL_DEVICE_REGISTRATION("BACKCHANNEL_DEVICE_REGISTRATION"),
    USER_INFO("USER_INFO"),
    TOKEN_REQUEST("TOKEN_REQUEST"),
    TOKEN_VALIDATE("TOKEN_VALIDATE"),
    TOKEN_REVOCATION("TOKEN_REVOCATION"),
    SESSION_UNAUTHENTICATED("SESSION_UNAUTHENTICATED"),
    SESSION_AUTHENTICATED("SESSION_AUTHENTICATED"),
    SESSION_DESTROYED("SESSION_DESTROYED"),
    DEVICE_CODE_AUTHORIZATION("DEVICE_CODE_AUTHORIZATION"),
    SSA_CREATE("SSA_CREATE"),
    SSA_READ("SSA_READ"),
    ;

    private final String value;

    Action(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
