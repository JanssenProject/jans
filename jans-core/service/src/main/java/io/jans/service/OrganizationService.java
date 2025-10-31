/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.Serializable;

import io.jans.model.ApplicationType;

/**
 * @author "Oleksiy Tataryn"
 *
 */
public abstract class OrganizationService implements Serializable {

    private static final long serialVersionUID = -6601700282123372943L;

    public static final int ONE_MINUTE_IN_SECONDS = 60;

    public String getDnForOrganization(String baseDn) {
        if (baseDn == null) {
            baseDn = "o=jans";
        }
        return baseDn;
    }

    public String getBaseDn() {
        return "o=jans";
    }

    public abstract ApplicationType getApplicationType();

}
