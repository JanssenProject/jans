package org.gluu.oxauth.model.ref;

import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.oxauth.persistence.model.ClientAttributes;

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
