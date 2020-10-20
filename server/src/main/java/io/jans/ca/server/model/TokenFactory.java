package io.jans.ca.server.model;

import io.jans.as.model.uma.UmaScopeType;

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
