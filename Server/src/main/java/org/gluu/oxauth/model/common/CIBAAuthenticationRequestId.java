/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.gluu.oxauth.model.util.Util;

import java.util.UUID;

/**
 * It is the unique identifier to identify the CIBA authentication request
 * (transaction) made by the Client.
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class CIBAAuthenticationRequestId extends AbstractToken {

    public CIBAAuthenticationRequestId(int lifeTime) {
        super(lifeTime);

        // Generate SHA-1 (160 bits)
        setCode(Util.toSHA1HexString(UUID.randomUUID().toString()));
    }
}