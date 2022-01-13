/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.model.common.GrantType;

/**
 * @author Yuriy Movchan
 * @version 02/13/2017
 */
public class SimpleAuthorizationGrant extends AuthorizationGrant {
    @Override
    public GrantType getGrantType() {
        return GrantType.NONE;
    }
}
