/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest;

import javax.ws.rs.GET;

/**
 * This interface exhibits only one method: {@link #close() close}. This is used by {@link io.jans.scim2.client.AbstractScimClient AbstractScimClient}
 * as a workaround method to allow clients obtained via {@link io.jans.scim2.client.factory.ScimClientFactory ScimClientFactory}
 * to be closeable.
 * <p>Existing Java standard interfaces like Closeable or Autocloseable are not suitable since the {@link #close() close}
 * method must be annotated with some javax.ws.rs.HttpMethod.</p>
 */
/*
 * Created by jgomer on 2017-11-25.
 */
public interface CloseableClient {

    /*
    Annotation here is dummy (any HTTP method can be used). In practice calling close will not issue a HTTP request but
    will release resources, see: gluu.scim2.client.AbstractScimClient.invoke
     */
    @GET
    void close();

}
