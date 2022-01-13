/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

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