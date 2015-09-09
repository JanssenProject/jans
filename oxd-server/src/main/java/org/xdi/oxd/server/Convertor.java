/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxd.common.response.RegisterClientOpResponse;

/**
 * Convenient static convertor.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class Convertor {

    /**
     * Avoid instance creation
     */
    private Convertor() {
    }

    public static RegisterClientOpResponse asRegisterClientOpResponse(RegisterResponse response) {
        if (response != null) {
            final RegisterClientOpResponse r = new RegisterClientOpResponse();
            r.setClientId(response.getClientId());
            r.setClientSecret(response.getClientSecret());
            r.setRegistrationAccessToken(response.getRegistrationAccessToken());
            r.setRegistrationClientUri(response.getRegistrationClientUri());
            r.setClientIdIssuedAt(response.getClientIdIssuedAt() != null ? response.getClientIdIssuedAt().getTime() / 1000 : 0);
            r.setClientSecretExpiresAt(response.getClientSecretExpiresAt() != null ?
                    response.getClientSecretExpiresAt().getTime() / 1000 : 0);
            return r;
        }
        return null;
    }
}
