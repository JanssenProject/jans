/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Custom script error
 *
 * @author Yuriy Movchan Date: 02/27/2018
 */
public class ScriptError implements Serializable {

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
