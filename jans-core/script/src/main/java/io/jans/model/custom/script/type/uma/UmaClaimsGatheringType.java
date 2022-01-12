/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.uma;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author yuriyz on 06/16/2017.
 */
public interface UmaClaimsGatheringType extends BaseExternalType {

    boolean gather(int step, Object gatheringContext);

    int getNextStep(int step, Object gatheringContext);

    boolean prepareForStep(int step, Object gatheringContext);

    int getStepsCount(Object gatheringContext);

    String getPageForStep(int step, Object gatheringContext);
}
