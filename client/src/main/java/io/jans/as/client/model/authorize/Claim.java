/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.model.authorize;

/**
 * @author Javier Rojas Blum Date: 03.07.2012
 */
public class Claim {

    private String name;
    private ClaimValue claimValue;

    public Claim(String name, ClaimValue claimValue) {
        this.name = name;
        this.claimValue = claimValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClaimValue getClaimValue() {
        return claimValue;
    }

    public void setClaimValue(ClaimValue claimValue) {
        this.claimValue = claimValue;
    }
}