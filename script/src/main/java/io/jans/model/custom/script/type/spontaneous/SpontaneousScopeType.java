/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.spontaneous;

import io.jans.model.custom.script.type.BaseExternalType;

public interface SpontaneousScopeType extends BaseExternalType {

    void manipulateScopes(Object context);
}
