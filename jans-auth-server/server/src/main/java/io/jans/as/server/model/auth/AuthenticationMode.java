/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.auth;

import jakarta.enterprise.inject.Vetoed;
import java.io.Serializable;

/**
 * @author Yuriy Movchan
 * Date: 03/17/2017
 */
@Vetoed
public class AuthenticationMode implements Serializable {

    private static final long serialVersionUID = -3187893527945584013L;

    private String name;

    public AuthenticationMode() {
    }

    public AuthenticationMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
