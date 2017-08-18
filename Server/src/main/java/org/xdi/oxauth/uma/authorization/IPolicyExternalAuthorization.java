/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.authorization;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/02/2013
 */

public interface IPolicyExternalAuthorization {

    public boolean authorize(UmaAuthorizationContext authorizationContext);
}
