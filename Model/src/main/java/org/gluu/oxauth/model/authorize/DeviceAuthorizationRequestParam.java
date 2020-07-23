/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

/**
 * RFC8628 section 3.1
 */
public interface DeviceAuthorizationRequestParam {

    /**
     * The client identifier as described in Section 2.2 of [RFC6749].
     */
    String CLIENT_ID = "client_id";

    /**
     * The scope of the access request as defined by Section 3.3 of [RFC6749].
     */
    String SCOPE = "scope";

}