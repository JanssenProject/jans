package org.xdi.oxauth.service.uma.authorization;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/02/2013
 */

public interface IPolicyExternalAuthorization {

    public boolean authorize(AuthorizationContext authorizationContext);
}
