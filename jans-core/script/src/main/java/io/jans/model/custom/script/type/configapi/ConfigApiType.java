/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.configapi;

import io.jans.model.custom.script.type.BaseExternalType;

public interface ConfigApiType extends BaseExternalType {

    boolean authorize(Object responseAsJsonObject, Object introspectionContext);
}
