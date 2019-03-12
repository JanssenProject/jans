package org.xdi.oxauth.model.ref;

import org.xdi.oxauth.model.common.AuthenticationMethod;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface ClientReference {

    AuthenticationMethod getAuthenticationMethod();

    String getClientId();

    ClientAttributes getAttributes();

    String getJwksUri();

    String getJwks();
}
