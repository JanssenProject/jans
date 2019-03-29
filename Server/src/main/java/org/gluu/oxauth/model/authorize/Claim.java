/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
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