package org.xdi.oxd.server.model;

import org.xdi.oxauth.model.uma.UmaScopeType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/06/2016
 */

public class UmaTokenFactory {

    private UmaTokenFactory() {
    }

    public static UmaToken newToken(UmaScopeType scopeType) {
        if (scopeType == UmaScopeType.AUTHORIZATION) {
            return new Aat();
        } else if (scopeType == UmaScopeType.PROTECTION) {
            return new Pat();
        }
        throw new RuntimeException("Unknown scope type: " + scopeType);
    }
}
