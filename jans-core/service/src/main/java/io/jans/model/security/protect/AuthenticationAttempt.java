/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.security.protect;

import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@RequestScoped
public class AuthenticationAttempt implements Serializable {

    private static final long serialVersionUID = -1841823297081861148L;

    private long time;
    private long expiration;
    private boolean success;

    public AuthenticationAttempt() {
    }

    public AuthenticationAttempt(long time, long expiration, boolean success) {
        this.time = time;
        this.expiration = expiration;
        this.success = success;
    }

    public final long getTime() {
        return time;
    }

    public final void setTime(long time) {
        this.time = time;
    }

    public final long getExpiration() {
        return expiration;
    }

    public final void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public final boolean isSuccess() {
        return success;
    }

    public final void setSuccess(boolean success) {
        this.success = success;
    }

}
