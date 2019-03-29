/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.custom.script.model;

import java.util.Date;

/**
 * Custom script error
 *
 * @author Yuriy Movchan Date: 02/27/2018
 */
public class ScriptError {

    private Date raisedAt;

    private String stackTrace;

    public ScriptError() {
    }

    public ScriptError(Date raisedAt, String stackTrace) {
        this.raisedAt = raisedAt;
        this.stackTrace = stackTrace;
    }

    public final Date getRaisedAt() {
        return raisedAt;
    }

    public final void setRaisedAt(Date raisedAt) {
        this.raisedAt = raisedAt;
    }

    public final String getStackTrace() {
        return stackTrace;
    }

    public final void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

}
