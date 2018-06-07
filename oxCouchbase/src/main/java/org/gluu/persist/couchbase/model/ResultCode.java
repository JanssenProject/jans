/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.model;

import java.io.Serializable;

/**
 * This class defines a number of constants associated with result codes.
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public final class ResultCode implements Serializable {

    private static final long serialVersionUID = -9180126854928558942L;

    private ResultCode() {
    }

    /**
     * The integer value (0) for the "SUCCESS" result code.
     */
    public static final int SUCCESS_INT_VALUE = 0;

    /**
     * The integer value (1) for the "OPERATIONS_ERROR" result code.
     */
    public static final int OPERATIONS_ERROR_INT_VALUE = 1;

    /**
     * The integer value (48) for the "INAPPROPRIATE_AUTHENTICATION" result code.
     */
    public static final int INAPPROPRIATE_AUTHENTICATION_INT_VALUE = 48;

}
