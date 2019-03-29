package org.gluu.oxauth.model.ref;

import org.gluu.oxauth.model.common.AuthenticationMethod;

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
