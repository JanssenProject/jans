/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.shib;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

public interface ShibPostAuthnApprovalType extends BaseExternalType {

    boolean approve(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

}