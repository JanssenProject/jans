/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.uma;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * @author yuriyz on 06/16/2017.
 */
public class UmaDummyClaimsGatheringType implements UmaClaimsGatheringType {

    @Override
    public boolean gather(int step, Object gatheringContext) {
        return false;
    }

    @Override
    public int getNextStep(int step, Object gatheringContext) {
        return -1;
    }

    @Override
    public boolean prepareForStep(int step, Object gatheringContext) {
        return false;
    }

    @Override
    public int getStepsCount(Object gatheringContext) {
        return -1;
    }

    @Override
    public String getPageForStep(int step, Object gatheringContext) {
        return null;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }
    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }
    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }

}
