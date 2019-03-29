/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jwe;

import org.gluu.oxauth.model.exception.InvalidJweException;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public interface JweEncrypter {

    public Jwe encrypt(Jwe jwe) throws InvalidJweException;
}