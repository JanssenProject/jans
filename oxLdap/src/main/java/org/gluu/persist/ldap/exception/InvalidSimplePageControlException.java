/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.ldap.exception;

import org.gluu.persist.exception.operation.PersistenceException;

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
