package org.xdi.oxauth.model.ref;

import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.ClientAttributes;

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
