package org.gluu.service.security;

import javax.ejb.ApplicationException;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@ApplicationException(rollback = true)
public class SecurityEvaluationException extends RuntimeException {

    private static final long serialVersionUID = 7115786700134354355L;

    public SecurityEvaluationException() {
        super();
    }

    public SecurityEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityEvaluationException(String message) {
        super(message);
    }

    public SecurityEvaluationException(Throwable cause) {
        super(cause);
    }

}
