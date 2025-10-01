/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.authz;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external OpenId authorization python script
 *
 * @author Yuriy Movchan Date: 10/30/2017
 */
public interface ConsentGatheringType extends BaseExternalType {

    boolean authorize(int step, Object consentContext);

    int getNextStep(int step, Object consentContext);

    boolean prepareForStep(int step, Object consentContext);

    int getStepsCount(Object consentContext);

    String getPageForStep(int step, Object consentContext);

}
