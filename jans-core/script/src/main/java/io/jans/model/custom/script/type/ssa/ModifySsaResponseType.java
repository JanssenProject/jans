/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.model.custom.script.type.ssa;

import io.jans.model.custom.script.type.BaseExternalType;

public interface ModifySsaResponseType extends BaseExternalType {

    boolean create(Object jsonWebResponse, Object ssaContext);

    boolean get(Object jsonArray, Object ssaContext);

    boolean revoke(Object ssaList, Object ssaContext);
}
