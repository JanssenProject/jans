/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.legacy.service.external.internal;

import io.jans.fido2.legacy.service.AuthenticationServiceJansServer;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.auth.DummyPersonAuthenticationType;
import io.jans.model.security.Credentials;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Map;

/**
 * Wrapper to call internal authentication method
 *
 * @author Yuriy Movchan Date: 06/04/2015
 */
@Stateless
@Named
public class InternalDefaultPersonAuthenticationType extends DummyPersonAuthenticationType {

    @Inject
    private AuthenticationServiceJansServer authenticationService;

    @Inject
    private Credentials credentials;

    public InternalDefaultPersonAuthenticationType() {
    }

    @Override
    public boolean authenticate(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
        if (!credentials.isSet()) {
            return false;
        }

        return authenticationService.authenticate(credentials.getUsername(), credentials.getPassword());
    }

    @Override
    public boolean prepareForStep(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters, int step) {
        if (step == 1) {
            return true;
        }

        return super.prepareForStep(configurationAttributes, requestParameters, step);
    }

    @Override
    public int getCountAuthenticationSteps(Map<String, SimpleCustomProperty> configurationAttributes) {
        return 1;
    }

    @Override
    public boolean logout(Map<String, SimpleCustomProperty> configurationAttributes, Map<String, String[]> requestParameters) {
        return true;
    }

}
