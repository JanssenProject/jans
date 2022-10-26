/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.bulk;

import com.fasterxml.jackson.core.type.TypeReference;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.bulk.BulkOperation;
import io.jans.scim.model.scim2.bulk.BulkRequest;
import io.jans.scim.model.scim2.bulk.BulkResponse;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;

import java.util.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-28.
 */
public class GroupsBulkTest extends BaseTest {

    private String userId;
    private String groupId;

    @Parameters("groups_bulk")
    @Test
    public void bulkJson(String json){

        logger.info("Creating one user and one group using bulk json string");

        Response response=client.processBulkOperations(json);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        BulkResponse br=response.readEntity(BulkResponse.class);
        List<BulkOperation> ops=br.getOperations();
        assertSuccessfulOps(ops);

        String location=ops.get(0).getLocation();
        assertNotNull(location);

        userId=location.substring(location.lastIndexOf("/")+1);
        logger.info("User id is {}", userId);

        location=ops.get(1).getLocation();
        assertNotNull(location);

        groupId=location.substring(location.lastIndexOf("/")+1);
        logger.info("Group id is {}", groupId);

    }

    @Test(dependsOnMethods = "bulkJson")
    public void bulkObject(){

        logger.info("Sending a bulk with a patch to insert admin user into group");

        //Creates a patch request consisting of adding the admin user to the group created
        PatchOperation po=new PatchOperation();
        po.setOperation("add");
        po.setPath("members.value");
        po.setValue(getAdminId());

        PatchRequest pr=new PatchRequest();
        pr.setOperations(Collections.singletonList(po));

        //Creates the bulk operation associated to the patch request
        BulkOperation bop =new BulkOperation();
        bop.setMethod("PATCH");
        bop.setPath("/Groups/" + groupId);
        bop.setData(mapper.convertValue(pr, new TypeReference<Map<String, Object>>(){}));

        BulkRequest breq=new BulkRequest();
        breq.setOperations(Collections.singletonList(bop));

        //Send bulk and check success of processing
        Response response=client.processBulkOperations(breq);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        BulkResponse bres=response.readEntity(BulkResponse.class);
        assertSuccessfulOps(bres.getOperations());

    }

    @Test(dependsOnMethods = "bulkObject", alwaysRun = true)
    public void delete(){

        logger.info("Cleaning...");

        //Prepare a bulk with 2 deletes
        List<BulkOperation> ops=new ArrayList<>();

        BulkOperation op=new BulkOperation();
        op.setMethod("DELETE");
        op.setPath("/Groups/" + groupId);
        ops.add(op);

        op=new BulkOperation();
        op.setMethod("DELETE");
        op.setPath("/Users/" + userId);
        ops.add(op);

        BulkRequest breq=new BulkRequest();
        breq.setOperations(ops);

        //Execute and check success
        Response response=client.processBulkOperations(breq);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        BulkResponse bres=response.readEntity(BulkResponse.class);
        ops=bres.getOperations();

        assertTrue(ops.stream().allMatch(oper -> Integer.parseInt(oper.getStatus())==Status.NO_CONTENT.getStatusCode()));
    }

    private void assertSuccessfulOps(List<BulkOperation> ops){

        for (BulkOperation operation : ops){
            //Verify sucessful operations
            int code=Integer.parseInt(operation.getStatus());
            assertTrue(Family.familyOf(code).equals(Family.SUCCESSFUL));
        }

    }

    private String getAdminId(){

        //Search the id of the admin user
        SearchRequest sr=new SearchRequest();
        sr.setFilter("userName eq \"admin\"");

        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

        ListResponse lr=response.readEntity(ListResponse.class);
        assertTrue(lr.getResources().size()>0);
        return lr.getResources().get(0).getId();

    }

}
