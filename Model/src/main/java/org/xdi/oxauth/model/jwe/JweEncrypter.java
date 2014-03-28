package org.xdi.oxauth.model.jwe;

import org.xdi.oxauth.model.exception.InvalidJweException;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public interface JweEncrypter {

    public Jwe encrypt(Jwe jwe) throws InvalidJweException;
}