/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest;

import jakarta.ws.rs.core.MultivaluedMap;

@Deprecated
public interface CloseableClient {

    void close();

    void setCustomHeaders(MultivaluedMap<String, String> headers);

}
