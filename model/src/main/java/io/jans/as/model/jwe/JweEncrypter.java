/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwe;

import io.jans.as.model.exception.InvalidJweException;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public interface JweEncrypter {

    public Jwe encrypt(Jwe jwe) throws InvalidJweException;
}