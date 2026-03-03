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

    /**
     * Return the provided organization DN or default to "o=jans" when the input is null.
     *
     * @param baseDn the organization distinguished name (DN), or null to use the default
     * @return the organization DN to use; "o=jans" if {@code baseDn} is null
     */
    public String getDnForOrganization(String baseDn) {
        if (baseDn == null) {
            baseDn = "o=jans";
        }
        return baseDn;
    }

    /**
     * Return the default base distinguished name used for the organization.
     *
     * @return the base DN "o=jans"
     */
    public String getBaseDn() {
        return "o=jans";
    }

    /**
 * Retrieve the application type associated with this service.
 *
 * @return the {@link ApplicationType} indicating the type of application for this implementation
 */
public abstract ApplicationType getApplicationType();

}