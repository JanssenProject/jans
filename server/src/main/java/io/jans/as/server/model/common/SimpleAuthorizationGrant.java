/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
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
