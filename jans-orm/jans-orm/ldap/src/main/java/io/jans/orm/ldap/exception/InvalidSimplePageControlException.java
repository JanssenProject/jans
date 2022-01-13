/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.exception;

import io.jans.orm.exception.operation.PersistenceException;

import com.unboundid.ldap.sdk.ResultCode;

/**
 * Invalid page control LDAP exception -- thrown when Simple Page Control returns result without cookie
 *
 * @author Yuriy Movchan Date: 12/30/2016
 */
public class InvalidSimplePageControlException extends PersistenceException {

    private static final long serialVersionUID = 1756816743469359856L;

    private ResultCode resultCode;

    public InvalidSimplePageControlException(String message) {
        super(message);
    }

    public InvalidSimplePageControlException(ResultCode resultCode, String message) {
        super(message);

        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

}
