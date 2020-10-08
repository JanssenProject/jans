/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package org.gluu.oxauth.model.exception;

/**
 * @author Javier Rojas Blum Date: 03.21.2012
 */
public class InvalidClaimException extends Exception {

    public InvalidClaimException(String message) {
        super(message);
    }
}