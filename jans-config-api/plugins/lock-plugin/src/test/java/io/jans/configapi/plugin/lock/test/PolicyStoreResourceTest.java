/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.lock.test;

import io.jans.configapi.plugin.lock.LockBaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class PolicyStoreResourceTest extends LockBaseTest {

    @Parameters({ "test.issuer", "lockPolicyStoresUrl" })
    @Test
    public void getPolicyStores(final String issuer, final String lockPolicyStoresUrl) {
        Builder request = getResteasyService().getClientBuilder(issuer + lockPolicyStoresUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("Response for getPolicyStores - response.getStatus():{}", response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

    @Parameters({ "test.issuer", "lockPolicyStoresUrl" })
    @Test
    public void rejectsNonHttpsPolicyStore(final String issuer, final String lockPolicyStoresUrl) {
        Builder request = getResteasyService().getClientBuilder(issuer + lockPolicyStoresUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.post(Entity.entity("{\"policyStoreUri\":\"http://example.org/store.cjar\"}", MediaType.APPLICATION_JSON));
        log.info("Response for rejectsNonHttpsPolicyStore - response.getStatus():{}", response.getStatus());
        assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
    }

}
