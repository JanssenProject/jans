/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.authorize;

/**
 * RFC8628 section 3.1
 */
public class DeviceAuthorizationRequestParam {

    private DeviceAuthorizationRequestParam() {
    }

    /**
     * The client identifier as described in Section 2.2 of [RFC6749].
     */
    public static final String CLIENT_ID = "client_id";

    /**
     * The scope of the access request as defined by Section 3.3 of [RFC6749].
     */
    public static final String SCOPE = "scope";

}