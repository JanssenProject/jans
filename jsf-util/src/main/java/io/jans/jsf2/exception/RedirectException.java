/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.exception;

import java.io.IOException;

/**
 * @author Yuriy Movchan
 * @version 03/17/2017
 */
public class RedirectException extends RuntimeException {

    public RedirectException(IOException ioe) {
        super(ioe);
    }

    public RedirectException(String message) {
        super(message);
    }
}
