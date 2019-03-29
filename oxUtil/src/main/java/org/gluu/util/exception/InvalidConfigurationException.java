/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.exception;

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
