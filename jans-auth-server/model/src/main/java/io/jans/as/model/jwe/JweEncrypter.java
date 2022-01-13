/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwe;

import io.jans.as.model.exception.InvalidJweException;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public interface JweEncrypter {

    Jwe encrypt(Jwe jwe) throws InvalidJweException;
}