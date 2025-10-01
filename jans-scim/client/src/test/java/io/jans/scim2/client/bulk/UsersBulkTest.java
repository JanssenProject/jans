/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.bulk;

import io.jans.scim.model.scim2.bulk.BulkOperation;
import io.jans.scim.model.scim2.bulk.BulkRequest;
import io.jans.scim.model.scim2.bulk.BulkResponse;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-27.
 */
public class UsersBulkTest extends UserBaseTest {

    private String id;

    @Parameters("users_bulk1")
    @Test
    public void bulkJson1(String json){

        logger.info("Sending a bulk with POST, PUT, and PATCH operations...");
        Response response=client.processBulkOperations(json);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        BulkResponse br=response.readEntity(BulkResponse.class);
        List<BulkOperation> ops=br.getOperations();

        assertTrue(ops.size()>2);
        assertSuccessfulOps(ops);

        String location=ops.get(0).getLocation();
        assertNotNull(location);

        id=location.substring(location.lastIndexOf("/")+1);
        logger.info("User id is {}", id);

    }

    @Test(dependsOnMethods = "bulkJson1")
    public void bulkWithObject(){

        logger.info("Sending a bulk with one DELETE...");
        BulkOperation op=new BulkOperation();
        op.setMethod("DELETE");
        op.setPath("/Users/" + id);

        BulkRequest breq=new BulkRequest();
        breq.setOperations(Collections.singletonList(op));

        Response response=client.processBulkOperations(breq);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        BulkResponse bres=response.readEntity(BulkResponse.class);
        List<BulkOperation> ops=bres.getOperations();
        assertEquals(ops.size(), 1);

        //Verify resource was deleted
        assertEquals(Status.NO_CONTENT.getStatusCode(), Integer.parseInt(ops.get(0).getStatus()));

    }

    @Parameters("users_bulk2")
    @Test(dependsOnMethods = "bulkWithObject")
    public void bulkJson2(String json) throws Exception{

        logger.info("Sending a bulk with POSTs and DELETEs operations...");
        Response response=client.processBulkOperations(json);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        BulkResponse br=response.readEntity(BulkResponse.class);
        List<BulkOperation> ops=br.getOperations();

        //Check that the attempt to update non-existing user returned 404
        BulkOperation failed=ops.remove(ops.size()-1);
        assertEquals(Integer.parseInt(failed.getStatus()), Status.NOT_FOUND.getStatusCode());

        assertSuccessfulOps(ops);

    }

    private void assertSuccessfulOps(List<BulkOperation> ops){

        for (BulkOperation operation : ops){
            //Verify sucessful operations
            int code=Integer.parseInt(operation.getStatus());
            assertTrue(Family.familyOf(code).equals(Family.SUCCESSFUL));
        }

    }

}
