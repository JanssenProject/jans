/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.spontaneous;

import io.jans.model.custom.script.type.BaseExternalType;

public interface SpontaneousScopeType extends BaseExternalType {

    void manipulateScopes(Object context);
}
