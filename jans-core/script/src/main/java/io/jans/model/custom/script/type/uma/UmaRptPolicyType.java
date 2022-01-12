/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.uma;

import java.util.List;

import io.jans.model.uma.ClaimDefinition;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author yuriyz on 05/30/2017.
 */
public interface UmaRptPolicyType extends BaseExternalType {

    List<ClaimDefinition> getRequiredClaims(Object authorizationContext);

    boolean authorize(Object authorizationContext);

    String getClaimsGatheringScriptName(Object authorizationContext);
}
