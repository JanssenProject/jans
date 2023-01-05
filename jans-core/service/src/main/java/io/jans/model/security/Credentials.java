/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.security;

import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@RequestScoped
@Named
public class Credentials implements Serializable {

    private static final long serialVersionUID = 4757835767552243714L;

    private String username;
    private String password;

    private boolean invalid = false;

    private boolean initialized;

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getUsername() {
        if (!isInitialized()) {
            setInitialized(true);
        }

        return username;
    }

    public void setUsername(String username) {
        if ((this.username != username) && (this.username == null || !this.username.equals(username))) {
            this.username = username;
            invalid = false;
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if ((this.password != password) && (this.password == null || !this.password.equals(password))) {
            this.password = password;
            invalid = false;
        }
    }

    public boolean isSet() {
        return (getUsername() != null) && (password != null);
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void invalidate() {
        invalid = true;
    }

    public void clear() {
        username = null;
        password = null;
    }

}
