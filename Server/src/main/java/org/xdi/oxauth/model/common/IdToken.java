/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.util.Date;

/**
 * @author Javier Rojas Blum Date: 02.13.2012
 */
public class IdToken extends AbstractToken {

    public IdToken(int lifeTime) {
        super(lifeTime);
    }

    public IdToken(String code, Date creationDate, Date expirationDate) {
        super(code, creationDate, expirationDate);
    }
}