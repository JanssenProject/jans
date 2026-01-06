/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.service.security.protect;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

public interface BaseAuthorizationProtection {
    Response processAuthorization(String bearerToken, ResourceInfo resourceInfo);
}
