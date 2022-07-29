/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.model;

import java.io.Serializable;

/**
 * This class defines a number of constants associated with result codes.
 *
 * @author Yuriy Movchan Date: 12/16/2020
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
