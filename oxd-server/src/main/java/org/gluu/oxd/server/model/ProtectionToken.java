package org.gluu.oxd.server.model;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/06/2016
 */

public class ProtectionToken extends UmaToken {

    public ProtectionToken() {
    }

    public ProtectionToken(String token, String refreshToken, int expiresIn) {
        super(token, refreshToken, expiresIn);
    }
}
