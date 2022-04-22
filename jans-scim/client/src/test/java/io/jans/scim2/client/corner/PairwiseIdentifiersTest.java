/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.corner;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Created by jgomer on 2018-03-05.
 */
public class PairwiseIdentifiersTest extends BaseTest {

    @Test
    public void queryAndRemoval() throws Exception{

        //Get a list (of at most 1 user) who has a persisted pairwise identifier
        Response response=client.searchUsers("pairwiseIdentifiers pr", null, 1, null, null, "pairwiseIdentifiers, id", null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse lr = response.readEntity(ListResponse.class);
        //If the list is empty do nothing (successful test)
        if (lr.getItemsPerPage()>0) {
            UserResource user=(UserResource) lr.getResources().get(0);
            assertNotNull(user.getPairwiseIdentifiers());

            //Prepare the removal of the user's PPIDs
            PatchOperation operation = new PatchOperation();
            operation.setOperation("remove");
            operation.setPath("pairwiseIdentitifers");

            PatchRequest pr = new PatchRequest();
            pr.setOperations(Collections.singletonList(operation));
            response=client.patchUser(pr, user.getId(), "pairwiseIdentifiers", null);
            assertEquals(response.getStatus(), OK.getStatusCode());

            //Ensure they are not there anymore.
            user=response.readEntity(UserResource.class);
            assertNull(user.getPairwiseIdentifiers());

            //This test does not guarantee the ou=pairwiseIdentifiers sub-branch disappears... only the jsPPID LDAP attribute
        }

    }

}
