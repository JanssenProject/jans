package org.gluu.oxd.server.model;

import org.gluu.oxauth.model.uma.UmaScopeType;

/**
 * @author Yuriy Zabrovarnyy
 */

public class TokenFactory {

    private TokenFactory() {
    }

    public static Token newToken(UmaScopeType scopeType) {
        if (scopeType == UmaScopeType.PROTECTION) {
            return new Pat();
        }
        return new Token();
    }
}
