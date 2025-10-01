package io.jans.scim2.client.singleresource;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.OK;

import static org.testng.Assert.*;

public class QueryParamRetrievalTest extends UserBaseTest {

    private UserResource user;

    @Test
    public void multipleRetrieval(){
        logger.debug("Retrieving test users...");
        String include="displayName, externalId";

        Response response=client.searchUsers("displayName co \"Test\"", null, null, null, null, include, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse = response.readEntity(ListResponse.class);
        List<UserResource> list=listResponse.getResources().stream().map(usrClass::cast).collect(Collectors.toList());

        //Verify users retrieved contain attributes of interest
        for (UserResource usr : list){
            assertNotNull(usr.getDisplayName());
            assertTrue(usr.getDisplayName().toLowerCase().contains("test"));
            //assertNotNull(usr.getExternalId());
        }

    }

    @Test(dependsOnMethods = "multipleRetrieval")
    public void multipleRetrievalExcluding() {
        logger.debug("Retrieving test users...");
        String exclude="displayName, externalId, name, addresses, emails";

        Response response=client.searchUsers("displayName co \"Test\"", null, null, null, null, null, exclude);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse = response.readEntity(ListResponse.class);
        List<UserResource> list=listResponse.getResources().stream().map(UserResource.class::cast).collect(Collectors.toList());

        //Verify users retrieved do not contain attributes of interest
        for (UserResource usr : list) {
            assertNull(usr.getDisplayName());
            assertNull(usr.getExternalId());
            assertNull(usr.getName());
            assertNull(usr.getAddresses());
            assertNull(usr.getEmails());

            assertNotNull(usr.getSchemas());
            assertNotNull(usr.getId());
        }
        user=list.get(0);

    }

    @Test(dependsOnMethods = "multipleRetrievalExcluding")
    public void singleRetrieval(){

        String include="active";
        Response response=client.getUserById(user.getId(), include, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);

        assertNotNull(user.getActive());
        assertNull(user.getExternalId());
        assertNull(user.getName());
        assertNull(user.getAddresses());
        assertNull(user.getEmails());

    }

    @Test(dependsOnMethods = "multipleRetrievalExcluding")
    public void singleRetrievalExcluding() {

        String exclude="id, externalId";
        Response response=client.getUserById(user.getId(), null, exclude);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);

        assertNotNull(user.getId());    //Always returned
        assertNull(user.getExternalId());

    }

}
