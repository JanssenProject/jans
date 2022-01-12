/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.exception;

/**
 * @author Yuriy Movchan Date: 11.15.2010
 */
public class InvalidConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 8883566613201977058L;

    public InvalidConfigurationException(Throwable root) {
        super(root);
    }

    public InvalidConfigurationException(String string, Throwable root) {
        super(string, root);
    }

    public InvalidConfigurationException(String s) {
        super(s);
    }

}
