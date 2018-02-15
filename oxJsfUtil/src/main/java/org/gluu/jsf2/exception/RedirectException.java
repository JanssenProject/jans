package org.gluu.jsf2.exception;

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
