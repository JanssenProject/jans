package org.xdi.oxd.server.model;

import org.xdi.oxauth.model.uma.UmaScopeType;

/**
 * @author Yuriy Zabrovarnyy
 */

public class UmaTokenFactory {

    private UmaTokenFactory() {
    }

    public static UmaToken newToken(UmaScopeType scopeType) {
        if (scopeType == UmaScopeType.PROTECTION) {
            return new Pat();
        }
        throw new RuntimeException("Unknown scope type: " + scopeType);
    }
}
