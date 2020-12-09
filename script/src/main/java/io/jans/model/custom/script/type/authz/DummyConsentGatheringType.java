/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.authz;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Dummy implementation of interface AuthorizationType
 *
 * @author Yuriy Movchan Date: 10/30/2017
 */
public class DummyConsentGatheringType implements ConsentGatheringType {

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }
    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }
    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return false;
    }

    @Override
    public boolean authorize(int step, Object consentContext) {
        return false;
    }

    @Override
    public int getNextStep(int step, Object consentContext) {
        return -1;
    }

    @Override
    public boolean prepareForStep(int step, Object consentContext) {
        return false;
    }

    @Override
    public int getStepsCount(Object consentContext) {
        return -1;
    }

    @Override
    public String getPageForStep(int step, Object consentContext) {
        return null;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }

}
